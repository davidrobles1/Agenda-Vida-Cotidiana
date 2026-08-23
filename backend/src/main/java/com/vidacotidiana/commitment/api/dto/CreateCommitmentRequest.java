package com.vidacotidiana.commitment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Aligned with components.schemas.CreateCommitmentRequest in openapi.yaml
 * (ADR-016, FR-025). personId is required — see Commitment's own doc
 * comment on the ASSUMPTION behind that. direction/dueAt are required;
 * projectId/originReminderId are optional.
 */
public record CreateCommitmentRequest(
        @NotNull UUID personId,
        @NotBlank @Size(max = 2000) String description,
        @NotNull @Pattern(regexp = "MINE|THEIRS") String direction,
        @NotNull Instant dueAt,
        UUID projectId,
        UUID originReminderId
) {
}
