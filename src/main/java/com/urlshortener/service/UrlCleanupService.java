package com.urlshortener.service;

import com.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlCleanupService {

    private final UrlRepository urlRepository;
    private final com.urlshortener.cache.UrlCacheService cacheService;

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void cleanupExpiredUrls() {
        log.info("Starting scheduled cleanup of expired URLs");
        LocalDateTime now = LocalDateTime.now();
        var expiredUrls = urlRepository.findAll().stream()
                .filter(u -> u.getIsActive() && u.getExpiresAt() != null && u.getExpiresAt().isBefore(now))
                .toList();

        int count = 0;
        for (var url : expiredUrls) {
            url.setIsActive(false);
            urlRepository.save(url);
            cacheService.evictUrl(url.getShortCode());
            count++;
        }
        
        log.info("Completed scheduled cleanup. Marked {} expired URLs as inactive.", count);
    }
}
