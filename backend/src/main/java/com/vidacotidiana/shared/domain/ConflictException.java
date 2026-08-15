package com.vidacotidiana.shared.domain;

/**
 * Generic 409 domain conflict with a caller-supplied code (e.g.
 * INVITATION_ALREADY_PENDING, AC-007) — distinct from
 * VersionConflictException, which is specific to optimistic locking on
 * Reminder and always carries the fixed REMINDER_VERSION_CONFLICT code.
 */
public class ConflictException extends DomainException {

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
