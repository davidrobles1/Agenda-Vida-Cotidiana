package com.vidacotidiana.routine.api.dto;

import com.vidacotidiana.routine.domain.RoutineFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Aligned with components.schemas.CreateRoutineRequest in openapi.yaml
 * (ADR-016 Fase 3e2, FR-032).
 *
 * {@code nextExecutionDate} is required and supplied by the caller: AC-019
 * leaves the *derivation* of an initial date as an explicit TBD, so the user
 * picks the first occurrence rather than the backend inventing one.
 */
public record CreateRoutineRequest(
        @NotBlank @Size(min = 1, max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull RoutineFrequency frequency,
        @NotNull Instant nextExecutionDate
) {
}
