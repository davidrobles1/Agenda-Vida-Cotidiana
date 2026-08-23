package com.vidacotidiana.reminder.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Aligned with components.schemas.UpdateReminderRequest in openapi.yaml.
 * Partial update: title/description/dueAt are optional and, when omitted
 * (null), leave the stored value unchanged. version is required (AC-004b) —
 * a mismatch against the stored version returns 409 REMINDER_VERSION_CONFLICT
 * without applying any change.
 *
 * iconId/stickerId are NOT partial like the fields above — they are always
 * applied exactly as sent, including absent (which clears the field). The
 * real edit UI (ReminderDrawer, mirrors NoteDrawer) is a fully controlled
 * form that always resubmits its complete current icon/sticker selection,
 * never a partial one — same contract as note.api.dto.UpdateNoteRequest.
 */
public record UpdateReminderRequest(
        @Size(min = 1, max = 200) String title,
        @Size(max = 2000) String description,
        Instant dueAt,
        @Size(max = 32) String iconId,
        @Size(max = 32) String stickerId,
        UUID personId,
        UUID projectId,
        @Size(max = 500) String location,
        @NotNull Integer version
) {
}
