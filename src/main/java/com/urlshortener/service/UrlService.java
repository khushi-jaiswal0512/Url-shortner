package com.urlshortener.service;

import com.urlshortener.dto.CreateUrlRequest;
import com.urlshortener.response.UrlResponse;

/**
 * URL shortening service contract.
 */
public interface UrlService {

    /**
     * Create a short URL from a long URL.
     * Returns existing mapping if the long URL was previously shortened (duplicate hit).
     */
    UrlResponse createShortUrl(CreateUrlRequest request);

    /**
     * Resolve a short code to the original long URL for redirection.
     * Checks Redis cache first, falls back to MySQL on miss.
     */
    String getOriginalUrl(String shortCode);

    /**
     * Retrieve analytics data for a given short code.
     */
    UrlResponse getUrlAnalytics(String shortCode);

    /**
     * Soft-delete a URL mapping and evict it from the cache.
     */
    void deleteUrl(String shortCode);

    /**
     * Get all active URLs.
     */
    java.util.List<UrlResponse> getAllUrls();

    /**
     * Get dashboard statistics including total URLs, clicks, most visited, and recent.
     */
    com.urlshortener.response.DashboardResponse getDashboardStats();
}
