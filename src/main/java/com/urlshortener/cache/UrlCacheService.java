package com.urlshortener.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Abstraction layer over Redis for URL caching.
 * <p>
 * Key pattern: "url:{shortCode}" → longUrl
 * TTL: configurable via app.cache.ttl-hours (default 24h).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UrlCacheService {

    private static final String CACHE_PREFIX = "url:";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.cache.ttl-hours:24}")
    private long ttlHours;

    /**
     * Store a short-code → long-URL mapping in Redis with TTL.
     */
    public void cacheUrl(String shortCode, String longUrl, java.time.LocalDateTime expiresAt) {
        try {
            String key = CACHE_PREFIX + shortCode;
            long ttlSeconds = ttlHours * 3600;
            
            if (expiresAt != null) {
                long secondsUntilExpiry = java.time.Duration.between(java.time.LocalDateTime.now(), expiresAt).getSeconds();
                if (secondsUntilExpiry <= 0) {
                    return; // Already expired, do not cache
                }
                ttlSeconds = Math.min(ttlSeconds, secondsUntilExpiry);
            }
            
            redisTemplate.opsForValue().set(key, longUrl, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Cached URL: {} -> {} (TTL: {}s)", key, longUrl, ttlSeconds);
        } catch (Exception e) {
            log.warn("Failed to cache URL for short code '{}': {}", shortCode, e.getMessage());
            // Cache failures should not break the application
        }
    }

    /**
     * Retrieve a long URL from Redis by its short code.
     *
     * @return the cached long URL, or null on cache miss / Redis failure
     */
    public String getCachedUrl(String shortCode) {
        try {
            String key = CACHE_PREFIX + shortCode;
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Failed to read cache for short code '{}': {}", shortCode, e.getMessage());
            return null;
        }
    }

    /**
     * Evict a cached URL entry (called on soft-deletion).
     */
    public void evictUrl(String shortCode) {
        try {
            String key = CACHE_PREFIX + shortCode;
            redisTemplate.delete(key);
            log.debug("Evicted cache key: {}", key);
        } catch (Exception e) {
            log.warn("Failed to evict cache for short code '{}': {}", shortCode, e.getMessage());
        }
    }
}
