package com.vidacotidiana.routine.api.dto;

import com.vidacotidiana.routine.domain.RoutineFrequency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Aligned with components.schemas.UpdateRoutineRequest in openapi.yaml.
 * Partial update: every field except version is optional and, when omitted
 * (null), leaves the stored value unchanged. version is required.
 *
 * Note there is no {@code completed} field — by design (FR-032): a routine
 * is completed repeatedly, so its permanent state is {@code active} and the
 * current occurrence is expressed through {@code nextExecutionDate}.
 */
public record UpdateRoutineRequest(
        @Size(min = 1, max = 200) String title,
        @Size(max = 2000) String description,
        RoutineFrequency frequency,
        Instant nextExecutionDate,
        Boolean active,
        @NotNull Integer version
) {
}
