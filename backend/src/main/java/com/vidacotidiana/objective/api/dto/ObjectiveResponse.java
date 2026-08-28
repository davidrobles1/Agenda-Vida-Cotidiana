package com.vidacotidiana.objective.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.objective.domain.Objective;

import java.time.Instant;
import java.util.UUID;

/** Aligned with components.schemas.Objective in Documentacion/openapi/openapi.yaml. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ObjectiveResponse(
        UUID id,
        UUID ownerUserId,
        String title,
        Integer targetValue,
        int currentValue,
        Instant deadline,
        boolean completed,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static ObjectiveResponse from(Objective objective) {
        return new ObjectiveResponse(
                objective.getId(),
                objective.getOwnerUserId(),
                objective.getTitle(),
                objective.getTargetValue(),
                objective.getCurrentValue(),
                objective.getDeadline(),
                objective.isCompleted(),
                objective.getVersion(),
                objective.getCreatedAt(),
                objective.getUpdatedAt()
        );
    }
}
