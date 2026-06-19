package com.urlshortener.util;

import com.urlshortener.exception.InvalidUrlException;

import java.util.Set;

public class SlugValidator {
    
    private static final Set<String> BLACKLIST = Set.of(
            "admin", "login", "api", "dashboard", "swagger", "health", "metrics", "actuator"
    );

    public static void validateAlias(String customAlias) {
        if (customAlias == null || customAlias.isBlank()) {
            return;
        }

        if (!customAlias.matches("^[a-zA-Z0-9_-]{3,30}$")) {
            throw new InvalidUrlException("Custom alias must be 3-30 characters long and contain only letters, numbers, hyphens, and underscores.");
        }

        if (BLACKLIST.contains(customAlias.toLowerCase())) {
            throw new InvalidUrlException("Custom alias '" + customAlias + "' is reserved and cannot be used.");
        }
    }
}
