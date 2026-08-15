package com.vidacotidiana.shared.domain;

/**
 * Optimistic locking conflict (AC-004b/AC-005, 09-data-model.md
 * "RECOMMENDATION técnica" — REMINDER.version). Maps to HTTP 409 with
 * code REMINDER_VERSION_CONFLICT.
 */
public class VersionConflictException extends DomainException {

    public VersionConflictException(String message) {
        super("REMINDER_VERSION_CONFLICT", message);
    }
}
