package com.urlshortener.service;

import com.urlshortener.cache.UrlCacheService;
import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.entity.UrlEntity;
import com.urlshortener.exception.InvalidUrlException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.repository.UrlRepository;
import com.urlshortener.response.UrlResponse;
import com.urlshortener.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Core service implementing URL shortening, redirection, analytics, and deletion.
 * <p>
 * Shortening Flow (Alex Xu's design):
 * 1. Validate → 2. Hash(longUrl) → 3. Check duplicates → 4. Insert (shortCode=null)
 * → 5. Base62(id) → 6. Update shortCode → 7. Cache → 8. Return
 * <p>
 * Redirect Flow:
 * Redis cache → (miss) → MySQL → populate cache → return longUrl
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;
    private final UrlCacheService cacheService;
    private final UrlMetricsService metricsService;
    private final com.urlshortener.repository.UrlClickEventRepository clickEventRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    // ─── Create ──────────────────────────────────────────────────

    @Override
    @Transactional
    public UrlResponse createShortUrl(CreateUrlRequest request) {
        String longUrl = request.getLongUrl().trim();
        validateNotCircular(longUrl);

        String urlHash = generateSha256Hash(longUrl);
        log.info("Processing URL shortening — hash: {}", urlHash);

        String customAlias = request.getCustomAlias();
        if (customAlias != null && !customAlias.isBlank()) {
            com.urlshortener.util.SlugValidator.validateAlias(customAlias);
            if (urlRepository.findByShortCodeAndIsActiveTrue(customAlias).isPresent()) {
                throw new InvalidUrlException("Custom alias '" + customAlias + "' is already in use.");
            }
            
            UrlEntity entity = UrlEntity.builder()
                    .longUrl(longUrl)
                    .urlHash(urlHash)
                    .shortCode(customAlias)
                    .expiresAt(request.getExpiresAt())
                    .build();
            entity = urlRepository.save(entity);
            cacheService.cacheUrl(customAlias, longUrl, entity.getExpiresAt());
            log.info("Custom short URL created: {} -> {}", customAlias, longUrl);
            return buildResponse(entity, true);
        }

        // Duplicate detection via SHA-256 hash index
        Optional<UrlEntity> existing = urlRepository.findByUrlHashAndIsActiveTrue(urlHash);
        if (existing.isPresent()) {
            log.info("Duplicate URL detected, returning existing short code: {}",
                    existing.get().getShortCode());
            return buildResponse(existing.get(), false);
        }

        // Phase 1: Insert with null shortCode to obtain AUTO_INCREMENT id
        UrlEntity entity = UrlEntity.builder()
                .longUrl(longUrl)
                .urlHash(urlHash)
                .expiresAt(request.getExpiresAt())
                .build();
        entity = urlRepository.saveAndFlush(entity);

        // Phase 2: Convert id → Base62, update entity
        String shortCode = base62Encoder.encode(entity.getId());
        entity.setShortCode(shortCode);
        entity = urlRepository.save(entity);

        // Phase 3: Populate Redis cache
        cacheService.cacheUrl(shortCode, longUrl, entity.getExpiresAt());

        log.info("Short URL created: {} → {}", shortCode, longUrl);
        return buildResponse(entity, true);
    }

    // ─── Redirect ────────────────────────────────────────────────

    @Override
    public String getOriginalUrl(String shortCode) {
        log.info("Resolving redirect for short code: {}", shortCode);

        // Try Redis cache first
        String cachedUrl = cacheService.getCachedUrl(shortCode);
        if (cachedUrl != null) {
            log.debug("Cache HIT for: {}", shortCode);
            metricsService.incrementClickAsync(shortCode);
            return cachedUrl;
        }

        // Cache miss — fall back to MySQL
        log.debug("Cache MISS for: {}", shortCode);
        UrlEntity entity = urlRepository.findByShortCodeAndIsActiveTrue(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new com.urlshortener.exception.UrlExpiredException(shortCode);
        }

        // Populate cache for next request
        cacheService.cacheUrl(shortCode, entity.getLongUrl(), entity.getExpiresAt());

        // Update click metrics asynchronously
        metricsService.incrementClickAsync(shortCode);

        return entity.getLongUrl();
    }

    // ─── Analytics ───────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public UrlResponse getUrlAnalytics(String shortCode) {
        log.info("Fetching analytics for short code: {}", shortCode);

        UrlEntity entity = urlRepository.findByShortCodeAndIsActiveTrue(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        return UrlResponse.builder()
                .shortCode(entity.getShortCode())
                .shortUrl(baseUrl + "/" + entity.getShortCode())
                .longUrl(entity.getLongUrl())
                .clickCount(entity.getClickCount())
                .createdAt(entity.getCreatedAt())
                .lastAccessed(entity.getLastAccessed())
                .build();
    }

    // ─── Soft Delete ─────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteUrl(String shortCode) {
        log.info("Soft-deleting short code: {}", shortCode);

        UrlEntity entity = urlRepository.findByShortCodeAndIsActiveTrue(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        entity.setIsActive(false);
        urlRepository.save(entity);

        // Evict from Redis
        cacheService.evictUrl(shortCode);

        log.info("Short code '{}' soft-deleted and evicted from cache", shortCode);
    }

    // ─── Dashboard & History ─────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public java.util.List<UrlResponse> getAllUrls() {
        log.info("Fetching all active URLs for history");
        return urlRepository.findAllByIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(entity -> UrlResponse.builder()
                        .shortCode(entity.getShortCode())
                        .shortUrl(baseUrl + "/" + entity.getShortCode())
                        .longUrl(entity.getLongUrl())
                        .clickCount(entity.getClickCount())
                        .createdAt(entity.getCreatedAt())
                        .lastAccessed(entity.getLastAccessed())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public com.urlshortener.response.DashboardResponse getDashboardStats() {
        log.info("Fetching dashboard statistics");
        
        long totalUrls = urlRepository.countByIsActiveTrue();
        long totalClicks = urlRepository.sumTotalClicks();
        
        double averageClicksPerUrl = totalUrls > 0 ? (double) totalClicks / totalUrls : 0.0;
        
        String mostVisitedShortCode = urlRepository.findFirstByIsActiveTrueOrderByClickCountDesc()
                .map(UrlEntity::getShortCode)
                .orElse(null);
                
        java.util.List<UrlResponse> recentUrls = urlRepository.findTop5ByIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(entity -> UrlResponse.builder()
                        .shortCode(entity.getShortCode())
                        .shortUrl(baseUrl + "/" + entity.getShortCode())
                        .longUrl(entity.getLongUrl())
                        .clickCount(entity.getClickCount())
                        .createdAt(entity.getCreatedAt())
                        .lastAccessed(entity.getLastAccessed())
                        .build())
                .toList();
                
        java.time.LocalDateTime sevenDaysAgo = java.time.LocalDateTime.now().minusDays(7);
        java.util.List<Object[]> clicksData = clickEventRepository.countClicksPerDaySince(sevenDaysAgo);
        java.util.Map<String, Long> clicksLast7Days = new java.util.LinkedHashMap<>();
        for (Object[] row : clicksData) {
            clicksLast7Days.put(row[0].toString(), ((Number) row[1]).longValue());
        }
                
        return com.urlshortener.response.DashboardResponse.builder()
                .totalUrls(totalUrls)
                .totalClicks(totalClicks)
                .averageClicksPerUrl(averageClicksPerUrl)
                .mostVisitedShortCode(mostVisitedShortCode)
                .recentUrls(recentUrls)
                .clicksLast7Days(clicksLast7Days)
                .build();
    }

    // ─── Private Helpers ─────────────────────────────────────────

    private void validateNotCircular(String longUrl) {
        String baseDomain = extractDomain(baseUrl);
        String inputDomain = extractDomain(longUrl);

        if (baseDomain.equalsIgnoreCase(inputDomain)) {
            throw new InvalidUrlException(
                    "Cannot shorten URLs from this service's own domain to prevent circular redirects");
        }
    }

    private String extractDomain(String url) {
        return url.replaceFirst("^https?://", "")
                .split("/")[0]
                .split(":")[0]
                .toLowerCase();
    }

    private String generateSha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private UrlResponse buildResponse(UrlEntity entity, boolean isNew) {
        return UrlResponse.builder()
                .shortCode(entity.getShortCode())
                .shortUrl(baseUrl + "/" + entity.getShortCode())
                .longUrl(entity.getLongUrl())
                .createdAt(entity.getCreatedAt())
                .newlyCreated(isNew)
                .build();
    }
}
