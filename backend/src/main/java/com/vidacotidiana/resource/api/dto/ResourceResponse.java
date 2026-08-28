package com.vidacotidiana.resource.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.resource.domain.Resource;
import com.vidacotidiana.resource.domain.ResourceType;

import java.time.Instant;
import java.util.UUID;

/** Aligned with components.schemas.Resource in Documentacion/openapi/openapi.yaml. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceResponse(
        UUID id,
        UUID ownerUserId,
        String name,
        ResourceType type,
        String reference,
        String description,
        UUID personId,
        UUID projectId,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static ResourceResponse from(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getOwnerUserId(),
                resource.getName(),
                resource.getType(),
                resource.getReference(),
                resource.getDescription(),
                resource.getPersonId(),
                resource.getProjectId(),
                resource.getVersion(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }
}
