package com.vidacotidiana.reminder.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Aligned with components.schemas.UpdateReminderRequest in openapi.yaml.
 * Partial update: title/description/dueAt are optional and, when omitted
 * (null), leave the stored value unchanged. version is required (AC-004b) —
 * a mismatch against the stored version returns 409 REMINDER_VERSION_CONFLICT
 * without applying any change.
 */
public record UpdateReminderRequest(
        @Size(min = 1, max = 200) String title,
        @Size(max = 2000) String description,
        Instant dueAt,
        @NotNull Integer version
) {
}
