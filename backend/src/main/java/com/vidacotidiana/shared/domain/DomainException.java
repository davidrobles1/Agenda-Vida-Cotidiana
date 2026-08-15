package com.vidacotidiana.shared.domain;

/**
 * Base type for controlled domain exceptions (15-coding-standards.md:
 * "excepciones de dominio controladas"). Each subclass carries a stable
 * {@code code}, surfaced verbatim in the Error envelope
 * ({@code components.schemas.Error} in openapi.yaml) so clients can branch
 * on it (e.g. REMINDER_VERSION_CONFLICT vs. INVITATION_ALREADY_PENDING).
 */
public abstract class DomainException extends RuntimeException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
