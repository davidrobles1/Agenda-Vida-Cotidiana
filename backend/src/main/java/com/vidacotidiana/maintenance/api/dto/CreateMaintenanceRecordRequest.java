package com.vidacotidiana.maintenance.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Aligned with components.schemas.CreateMaintenanceRecordRequest in openapi.yaml. */
public record CreateMaintenanceRecordRequest(
        @NotBlank @Size(min = 1, max = 200) String item,
        @NotNull Instant nextDueAt
) {
}
