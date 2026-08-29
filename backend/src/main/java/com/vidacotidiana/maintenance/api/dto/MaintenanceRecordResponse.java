package com.vidacotidiana.maintenance.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.maintenance.domain.MaintenanceRecord;
import com.vidacotidiana.maintenance.domain.MaintenanceStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Aligned with components.schemas.MaintenanceRecord in
 * Documentacion/openapi/openapi.yaml. `status` matches
 * web/src/core/mock/mockData.ts's MaintenanceStatus contract exactly
 * (AL_DIA/PROXIMO/VENCIDO), plus COMPLETADO. Computed the same way as
 * warranty.api.dto.WarrantyResponse — see that class's javadoc for the
 * shared reasoning, including the 30-day ASSUMPTION for the "próximo"
 * threshold.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MaintenanceRecordResponse(
        UUID id,
        UUID ownerUserId,
        String item,
        Instant nextDueAt,
        Integer intervalMonths,
        String status,
        int version,
        Instant createdAt,
        Instant updatedAt,
        /** ADR-019: módulo propietario del recurso. */
        String context
) {
    private static final long PROXIMO_THRESHOLD_DAYS = 30;

    public static MaintenanceRecordResponse from(MaintenanceRecord record) {
        return from(record, Instant.now());
    }

    public static MaintenanceRecordResponse from(MaintenanceRecord record, Instant now) {
        return new MaintenanceRecordResponse(
                record.getId(),
                record.getOwnerUserId(),
                record.getItem(),
                record.getNextDueAt(),
                record.getIntervalMonths(),
                computeStatus(record, now),
                record.getVersion(),
                record.getCreatedAt(),
                record.getUpdatedAt(),
                record.getContext().name()
        );
    }

    private static String computeStatus(MaintenanceRecord record, Instant now) {
        if (record.getStatus() == MaintenanceStatus.COMPLETED) {
            return "COMPLETADO";
        }
        if (record.getNextDueAt().isBefore(now)) {
            return "VENCIDO";
        }
        if (record.getNextDueAt().isBefore(now.plus(PROXIMO_THRESHOLD_DAYS, ChronoUnit.DAYS))) {
            return "PROXIMO";
        }
        return "AL_DIA";
    }
}
