package com.vidacotidiana.warranty.api.dto;

/**
 * Aligned with components.schemas.CompleteWarrantyRequest in openapi.yaml.
 * version is optional: if provided, the server validates it for optimistic
 * locking (409 WARRANTY_VERSION_CONFLICT on mismatch); if omitted, the
 * toggle is applied without a concurrency check — same contract as
 * reminder.api.dto.CompleteReminderRequest.
 */
public record CompleteWarrantyRequest(Integer version) {
}
