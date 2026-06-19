package com.urlshortener.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Unified response DTO for URL operations.
 * <p>
 * For POST (create): shortCode, shortUrl, longUrl, createdAt are populated.
 * For GET (analytics): all fields including clickCount and lastAccessed.
 * <p>
 * {@code newlyCreated} is a transient flag (excluded from JSON) used by the
 * controller to decide between 201 Created and 200 OK responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UrlResponse {

    private String shortCode;
    private String shortUrl;
    private String longUrl;
    private Long clickCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessed;

    /**
     * Transient flag — not serialized to JSON.
     * true = newly created (201), false = duplicate hit (200).
     */
    @JsonIgnore
    private boolean newlyCreated;
}
