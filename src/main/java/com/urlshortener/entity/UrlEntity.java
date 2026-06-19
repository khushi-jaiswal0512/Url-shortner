package com.urlshortener.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity mapping to the 'urls' table.
 * <p>
 * Note: {@code shortCode} is nullable to support the two-phase insert flow:
 * 1. Insert row to get AUTO_INCREMENT id (shortCode = null)
 * 2. Compute Base62(id), then UPDATE with the shortCode.
 */
@Entity
@Table(name = "urls", indexes = {
        @Index(name = "idx_short_code", columnList = "short_code"),
        @Index(name = "idx_url_hash", columnList = "url_hash"),
        @Index(name = "idx_is_active", columnList = "is_active")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", length = 10, unique = true)
    private String shortCode;

    @Column(name = "url_hash", length = 64, nullable = false)
    private String urlHash;

    @Column(name = "long_url", length = 2048, nullable = false)
    private String longUrl;

    @Column(name = "click_count")
    @Builder.Default
    private Long clickCount = 0L;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_accessed")
    private LocalDateTime lastAccessed;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
