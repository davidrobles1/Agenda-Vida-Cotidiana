package com.vidacotidiana.reminder.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.reminder.domain.Reminder;

import java.time.Instant;
import java.util.UUID;

/**
 * Aligned with components.schemas.Reminder in Documentacion/openapi/openapi.yaml.
 * BE-035: description/dueAt are optional (not in the schema's required list) but
 * declared as plain (non-nullable) string/date-time — the schema's contract is that
 * an absent value is an omitted field, not a JSON null, so nulls must not be serialized.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReminderResponse(
        UUID id,
        UUID ownerUserId,
        String title,
        String description,
        Instant dueAt,
        String status,
        String context,
        String iconId,
        String stickerId,
        UUID personId,
        UUID projectId,
        String location,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReminderResponse from(Reminder reminder) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getOwnerUserId(),
                reminder.getTitle(),
                reminder.getDescription(),
                reminder.getDueAt(),
                reminder.getStatus().name(),
                reminder.getContext().name(),
                reminder.getIconId(),
                reminder.getStickerId(),
                reminder.getPersonId(),
                reminder.getProjectId(),
                reminder.getLocation(),
                reminder.getVersion(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt()
        );
    }
}
