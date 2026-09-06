package com.vidacotidiana.audit.domain;

public enum AuditTargetType {
    REMINDER,
    INVITATION,
    REMINDER_SHARE,

    // ADR-025
    FAMILY_INVITATION,
    FAMILY_LINK,
    RESOURCE_SHARE
}
