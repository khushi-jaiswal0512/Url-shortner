package com.urlshortener.exception;

/**
 * Thrown when a short code does not map to any active URL.
 * Produces HTTP 404 Not Found.
 */
public class UrlNotFoundException extends RuntimeException {

    public UrlNotFoundException(String shortCode) {
        super("URL not found for short code: " + shortCode);
    }
}
