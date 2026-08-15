package com.vidacotidiana.reminder.api.dto;

/**
 * Aligned with components.schemas.CompleteReminderRequest in openapi.yaml.
 * version is optional: if provided, AC-005 requires the server to validate
 * it (409 REMINDER_VERSION_CONFLICT on mismatch); if omitted, the toggle is
 * applied without a concurrency check.
 */
public record CompleteReminderRequest(Integer version) {
}
