package com.vidacotidiana.warranty.domain;

/**
 * Persisted completion state only — ACTIVE (not yet completed) or COMPLETED
 * (BE-037/09-data-model.md). This is deliberately NOT the 4-value temporal
 * status the API exposes (VIGENTE/POR_VENCER/VENCIDA/COMPLETADO, see
 * com.vidacotidiana.warranty.api.dto.WarrantyResponse) — the temporal bucket
 * is derived at response time from expiresAt vs "now", never stored, to
 * avoid a status column going stale without a periodic recompute job
 * (RECOMMENDATION técnica, 09-data-model.md). Same enum shape as
 * reminder.domain.ReminderStatus (PENDING/COMPLETED), renamed ACTIVE instead
 * of PENDING here because "pending" would collide with the temporal
 * "vencida"/"por vencer" vocabulary already used by this entity.
 */
public enum WarrantyStatus {
    ACTIVE,
    COMPLETED
}
