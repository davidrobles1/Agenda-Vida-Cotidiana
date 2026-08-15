package com.vidacotidiana.reminder.api.dto;

import com.vidacotidiana.reminder.domain.Reminder;

import java.time.Instant;
import java.util.UUID;

/** Aligned with components.schemas.Reminder in Documentacion/openapi/openapi.yaml. */
public record ReminderResponse(
        UUID id,
        UUID ownerUserId,
        String title,
        String description,
        Instant dueAt,
        String status,
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
                reminder.getVersion(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt()
        );
    }
}
