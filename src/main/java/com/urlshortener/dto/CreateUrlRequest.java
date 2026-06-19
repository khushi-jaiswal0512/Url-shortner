package com.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a shortened URL.
 * Validation is enforced at the controller layer via @Valid.
 * Circular redirect detection is handled in the service layer
 * (requires access to the configurable BASE_URL).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUrlRequest {

    @NotBlank(message = "URL cannot be null or empty")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    @Pattern(
            regexp = "^https?://.+",
            message = "URL must start with http:// or https://"
    )
    private String longUrl;

    @Pattern(
            regexp = "^[a-zA-Z0-9_-]{3,30}$",
            message = "Alias must be 3-30 characters long and contain only letters, numbers, hyphens, and underscores"
    )
    private String customAlias;

    private java.time.LocalDateTime expiresAt;
}
