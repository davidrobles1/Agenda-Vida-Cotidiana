package com.vidacotidiana.shared.domain;

/**
 * Business-rule validation that bean validation annotations can't express
 * (e.g. "exactly one of email/username must be provided", or a username
 * that doesn't resolve to any account). Maps to the same 400 VALIDATION_ERROR
 * envelope as Spring's own constraint-violation handling in
 * GlobalExceptionHandler, via the generic DomainException fallback.
 */
public class ValidationException extends DomainException {

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }
}
