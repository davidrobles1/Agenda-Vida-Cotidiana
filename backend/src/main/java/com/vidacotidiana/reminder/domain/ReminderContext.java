package com.vidacotidiana.reminder.domain;

/**
 * ADR-015/FR-019: inferred from the navbar of origin at creation time —
 * there is no explicit selector in the create form. See
 * ReminderService.create for the PERSONAL default applied when a caller
 * doesn't send one (every pre-ADR-015 UI call site).
 */
public enum ReminderContext {
    PERSONAL,
    LABORAL
}
