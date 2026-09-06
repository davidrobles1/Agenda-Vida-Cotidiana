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
        /**
         * ADR-021. Un `intervalMonths` nulo significa "no lo toques" —es la
         * regla de toda la edición parcial—, así que sin esta bandera no
         * habría forma de QUITAR una periodicidad ya asignada: la opción
         * "Sin repetición" del detalle no tendría efecto. Encontrado
         * validando con un registro real.
         */
        Boolean clearInterval,
        @NotNull Integer version
) {
}
