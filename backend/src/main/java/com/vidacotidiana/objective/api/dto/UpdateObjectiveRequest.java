package com.vidacotidiana.objective.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Aligned with components.schemas.UpdateObjectiveRequest in openapi.yaml.
 * Partial update: every field except version is optional and, when omitted
 * (null), leaves the stored value unchanged (same contract as
 * UpdatePersonRequest). version is required.
 */
public record UpdateObjectiveRequest(
        @Size(min = 1, max = 200) String title,
        @Min(0) Integer targetValue,
        @Min(0) Integer currentValue,
        Instant deadline,
        Boolean completed,
        @NotNull Integer version
) {
}
