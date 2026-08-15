package com.vidacotidiana.audit.domain;

/** 11-auth-security.md §Auditoría — exactly the events it lists, nothing more. */
public enum AuditEventType {
    INVITATION_CREATED,
    INVITATION_CANCELLED,
    INVITATION_ACCEPTED,
    INVITATION_REJECTED,
    INVITATION_EXPIRED,
    SHARE_REVOKED
}
