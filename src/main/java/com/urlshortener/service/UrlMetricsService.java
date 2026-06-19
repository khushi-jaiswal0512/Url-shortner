package com.urlshortener.service;

import com.urlshortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dedicated service for asynchronous click metric updates.
 * <p>
 * Separated from UrlServiceImpl because Spring's @Async proxy
 * does not work on self-invoked methods within the same bean.
 * By placing the @Async method in a separate bean, the proxy
 * intercept works correctly and the method runs on a different thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UrlMetricsService {

    private final UrlRepository urlRepository;
    private final com.urlshortener.repository.UrlClickEventRepository clickEventRepository;

    /**
     * Atomically increments the click count, updates last_accessed, and logs the click event.
     * Runs asynchronously to avoid blocking the redirect response.
     */
    @Async
    @Transactional
    public void incrementClickAsync(String shortCode) {
        try {
            urlRepository.incrementClickCount(shortCode);
            
            // Log individual click event for time-series analytics
            urlRepository.findByShortCodeAndIsActiveTrue(shortCode).ifPresent(url -> {
                com.urlshortener.entity.UrlClickEvent event = com.urlshortener.entity.UrlClickEvent.builder()
                        .url(url)
                        .build();
                clickEventRepository.save(event);
            });
            
            log.debug("Click metrics updated asynchronously for: {}", shortCode);
        } catch (Exception e) {
            // Metric updates are best-effort — failures must not propagate
            log.error("Failed to update click metrics for '{}': {}", shortCode, e.getMessage());
        }
    }
}
