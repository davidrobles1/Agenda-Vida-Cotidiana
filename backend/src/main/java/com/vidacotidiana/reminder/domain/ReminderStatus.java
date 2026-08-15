package com.vidacotidiana.reminder.domain;

/**
 * Single global completion state per reminder, shared by owner and all
 * active collaborators — no per-collaborator completion in V1 (DEC-001,
 * Documentacion/28-v1-decision-pack.md). Do not add states here without a
 * corresponding approved decision.
 */
public enum ReminderStatus {
    PENDING,
    COMPLETED
}
