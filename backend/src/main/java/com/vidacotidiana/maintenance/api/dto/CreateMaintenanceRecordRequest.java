package com.vidacotidiana.maintenance.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Aligned with components.schemas.CreateMaintenanceRecordRequest in openapi.yaml. */
public record CreateMaintenanceRecordRequest(
        @NotBlank @Size(min = 1, max = 200) String item,
        @NotNull Instant nextDueAt,
        /** "¿Cada cuánto?" en meses. Opcional: sin valor, el mantenimiento
            tiene una sola fecha y no se repite (ver migración V24). */
        @Min(1) @Max(120) Integer intervalMonths,
        /** ADR-019: módulo desde el que se crea. Ausente ⇒ PERSONAL. */
        @Pattern(regexp = "PERSONAL|LABORAL") String context
) {
}
