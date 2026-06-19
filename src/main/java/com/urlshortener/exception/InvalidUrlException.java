package com.urlshortener.exception;

/**
 * Thrown when the submitted URL fails business validation
 * (e.g., circular redirect detection).
 * Produces HTTP 400 Bad Request.
 */
public class InvalidUrlException extends RuntimeException {

    public InvalidUrlException(String message) {
        super(message);
    }
}
