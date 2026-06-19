package com.urlshortener.repository;

import com.urlshortener.entity.UrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for URL entities.
 */
@Repository
public interface UrlRepository extends JpaRepository<UrlEntity, Long> {

    /**
     * Find an active URL mapping by its short code.
     */
    Optional<UrlEntity> findByShortCodeAndIsActiveTrue(String shortCode);

    /**
     * Find an active URL mapping by its SHA-256 hash (duplicate detection).
     */
    Optional<UrlEntity> findByUrlHashAndIsActiveTrue(String urlHash);

    /**
     * Find by short code regardless of active status (used for soft-delete lookups).
     */
    Optional<UrlEntity> findByShortCode(String shortCode);

    /**
     * Atomically increment click count and update last_accessed timestamp.
     * Called asynchronously to avoid blocking the redirect response.
     */
    @Modifying
    @Query("UPDATE UrlEntity u SET u.clickCount = u.clickCount + 1, "
            + "u.lastAccessed = CURRENT_TIMESTAMP WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode);

    /**
     * Get all active URLs ordered by creation date descending.
     */
    java.util.List<UrlEntity> findAllByIsActiveTrueOrderByCreatedAtDesc();

    /**
     * Get the total number of active URLs.
     */
    long countByIsActiveTrue();

    /**
     * Get the sum of all clicks across active URLs.
     */
    @Query("SELECT COALESCE(SUM(u.clickCount), 0) FROM UrlEntity u WHERE u.isActive = true")
    long sumTotalClicks();

    /**
     * Get the most visited active URL.
     */
    Optional<UrlEntity> findFirstByIsActiveTrueOrderByClickCountDesc();

    /**
     * Get the 5 most recent active URLs.
     */
    java.util.List<UrlEntity> findTop5ByIsActiveTrueOrderByCreatedAtDesc();
}
