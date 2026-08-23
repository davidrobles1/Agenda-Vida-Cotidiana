package com.vidacotidiana.commitment.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Aligned with components.schemas.UpdateCommitmentRequest in openapi.yaml.
 * Partial update: all fields except version are optional and, when omitted
 * (null), leave the stored value unchanged. This also covers UC-20
 * "reprogramar" (send only dueAt) and flipping direction (ADR-016: the
 * whole point of the unified model, not a special action).
 */
public record UpdateCommitmentRequest(
        UUID personId,
        @Size(max = 2000) String description,
        @Pattern(regexp = "MINE|THEIRS") String direction,
        Instant dueAt,
        UUID projectId,
        @NotNull Integer version
) {
}
