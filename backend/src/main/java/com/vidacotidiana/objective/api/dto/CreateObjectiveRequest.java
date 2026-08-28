package com.vidacotidiana.objective.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Aligned with components.schemas.CreateObjectiveRequest in openapi.yaml
 * (ADR-016 Fase 3e1, FR-031). {@code completed} is deliberately absent: a
 * new objective is always created pending (AC-018) — marking it done is a
 * PATCH.
 */
public record CreateObjectiveRequest(
        @NotBlank @Size(min = 1, max = 200) String title,
        @Min(0) Integer targetValue,
        @Min(0) Integer currentValue,
        Instant deadline
) {
}
