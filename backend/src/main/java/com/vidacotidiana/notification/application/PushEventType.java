package com.vidacotidiana.notification.application;

/** UC-11/AC-012 — exactly the events that list enumerates, nothing more. */
public enum PushEventType {
    INVITATION_RECEIVED,
    INVITATION_ACCEPTED,
    INVITATION_REJECTED,
    INVITATION_CANCELLED,
    REMINDER_SHARE_REVOKED,
    REMINDER_DELETED
}
