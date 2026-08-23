package com.vidacotidiana.commitment.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.commitment.domain.Commitment;

import java.time.Instant;
import java.util.UUID;

/** Aligned with components.schemas.Commitment in Documentacion/openapi/openapi.yaml. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommitmentResponse(
        UUID id,
        UUID ownerUserId,
        UUID personId,
        UUID projectId,
        String description,
        String direction,
        Instant dueAt,
        String status,
        UUID originReminderId,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommitmentResponse from(Commitment commitment) {
        return new CommitmentResponse(
                commitment.getId(),
                commitment.getOwnerUserId(),
                commitment.getPersonId(),
                commitment.getProjectId(),
                commitment.getDescription(),
                commitment.getDirection().name(),
                commitment.getDueAt(),
                commitment.getStatus().name(),
                commitment.getOriginReminderId(),
                commitment.getVersion(),
                commitment.getCreatedAt(),
                commitment.getUpdatedAt()
        );
    }
}
