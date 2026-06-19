package com.urlshortener.exception;

/**
 * Thrown when a client exceeds the configured rate limit.
 * Produces HTTP 429 Too Many Requests.
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
