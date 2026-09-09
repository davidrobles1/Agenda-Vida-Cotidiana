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
        String context,
        /** V31: artículo del inventario al que se le hace. Nulo = sin enlazar. */
        UUID inventoryItemId
) {
    /**
     * ADR-021: UNA sola definición de "próximo" en toda la sección.
     *
     * Antes eran dos y se contradecían: el backend decía 30 días y
     * `dateAlerts` avisaba a 7/3/0, así que el mismo registro estaba
     * "PROXIMO" en la lista y no en el calendario. 7 días es además el
     * umbral que ya usa la sección de Pagos (`DUE_SOON_DAYS`).
     */
    private static final long PROXIMO_THRESHOLD_DAYS = 7;

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
                record.getContext().name(),
                record.getInventoryItemId()
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
