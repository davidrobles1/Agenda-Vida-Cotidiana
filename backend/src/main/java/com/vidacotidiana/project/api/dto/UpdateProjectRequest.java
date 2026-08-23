package com.vidacotidiana.project.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Aligned with components.schemas.UpdateProjectRequest in openapi.yaml.
 * Partial update: all fields except version are optional and, when omitted
 * (null), leave the stored value unchanged (same contract as
 * UpdatePersonRequest — no way to explicitly clear clientPersonId once set,
 * an accepted limitation for V3).
 */
public record UpdateProjectRequest(
        @Size(min = 1, max = 200) String name,
        UUID clientPersonId,
        @Size(max = 100) String status,
        Instant deadline,
        @NotNull Integer version
) {
}
