package com.vidacotidiana.maintenance.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Aligned with components.schemas.UpdateMaintenanceRecordRequest in
 * openapi.yaml. Partial update: item/nextDueAt optional; version required.
 */
public record UpdateMaintenanceRecordRequest(
        @Size(min = 1, max = 200) String item,
        Instant nextDueAt,
        @Min(1) @Max(120) Integer intervalMonths,
        @NotNull Integer version
) {
}
