package com.vidacotidiana.sharing.api.dto;

import com.vidacotidiana.sharing.domain.ReminderShare;

import java.time.Instant;
import java.util.UUID;

/** Aligned with components.schemas.ReminderShare in openapi.yaml. */
public record ReminderShareResponse(
        UUID id,
        UUID reminderId,
        UUID collaboratorUserId,
        String status,
        Instant createdAt,
        Instant revokedAt
) {
    public static ReminderShareResponse from(ReminderShare share) {
        return new ReminderShareResponse(
                share.getId(),
                share.getReminderId(),
                share.getCollaboratorUserId(),
                share.getStatus().name(),
                share.getCreatedAt(),
                share.getRevokedAt()
        );
    }
}
