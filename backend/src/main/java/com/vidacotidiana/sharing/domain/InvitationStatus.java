package com.vidacotidiana.sharing.domain;

/** DEC-003: REVOKED never applies here — revocation lives only on ReminderShare. */
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    CANCELLED
}
