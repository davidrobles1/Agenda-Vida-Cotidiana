package com.vidacotidiana.maintenance.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.maintenance.domain.MaintenanceLogEntry;

import java.time.LocalDate;
import java.util.UUID;

/** ADR-021: una ejecución, tal como la consume la pantalla. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MaintenanceLogResponse(
        UUID id,
        UUID maintenanceRecordId,
        LocalDate scheduledDate,
        LocalDate completedDate,
        String note
) {
    public static MaintenanceLogResponse from(MaintenanceLogEntry entry) {
        return new MaintenanceLogResponse(
                entry.getId(),
                entry.getMaintenanceRecordId(),
                entry.getScheduledDate(),
                entry.getCompletedDate(),
                entry.getNote());
    }
}
