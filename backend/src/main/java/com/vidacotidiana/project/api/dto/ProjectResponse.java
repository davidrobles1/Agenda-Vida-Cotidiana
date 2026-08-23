package com.vidacotidiana.project.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.project.domain.Project;

import java.time.Instant;
import java.util.UUID;

/** Aligned with components.schemas.Project in Documentacion/openapi/openapi.yaml. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProjectResponse(
        UUID id,
        UUID ownerUserId,
        String name,
        UUID clientPersonId,
        String status,
        Instant deadline,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getOwnerUserId(),
                project.getName(),
                project.getClientPersonId(),
                project.getStatus(),
                project.getDeadline(),
                project.getVersion(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
