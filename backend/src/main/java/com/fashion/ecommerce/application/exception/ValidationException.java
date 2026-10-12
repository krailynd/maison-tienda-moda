package com.fashion.ecommerce.application.exception;

import java.util.Map;

/**
 * Business validation error mapped to {@code 400 VALIDATION_ERROR} by the
 * global exception handler. Carries optional field-level details for the
 * {@code {error:{code,message,details}}} envelope.
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> details;

    public ValidationException(String message) {
        this(message, null);
    }

    public ValidationException(String message, Map<String, String> details) {
        super(message);
        this.details = details;
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
