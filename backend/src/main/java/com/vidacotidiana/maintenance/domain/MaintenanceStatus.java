package com.vidacotidiana.maintenance.domain;

/**
 * Persisted completion state only — ACTIVE (not yet completed) or COMPLETED
 * (BE-037/09-data-model.md). Mirrors warranty.domain.WarrantyStatus exactly:
 * the 4-value temporal status the API exposes (AL_DIA/PROXIMO/VENCIDO/
 * COMPLETADO, see com.vidacotidiana.maintenance.api.dto.MaintenanceRecordResponse)
 * is derived at response time from nextDueAt vs "now", never stored.
 */
public enum MaintenanceStatus {
    ACTIVE,
    COMPLETED
}
