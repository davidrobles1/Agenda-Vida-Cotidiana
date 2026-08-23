package com.vidacotidiana.maintenance.api.dto;

/**
 * Aligned with components.schemas.CompleteMaintenanceRecordRequest in
 * openapi.yaml. version optional — same optimistic-locking contract as
 * warranty.api.dto.CompleteWarrantyRequest / reminder's CompleteReminderRequest.
 */
public record CompleteMaintenanceRecordRequest(Integer version) {
}
