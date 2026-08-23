package com.vidacotidiana.commitment.api.dto;

/**
 * Aligned with components.schemas.ResolveCommitmentRequest in openapi.yaml.
 * Optional: if version is provided, the server validates it for optimistic
 * locking; if omitted, resolve is applied without a concurrency check —
 * same contract as CompleteWarrantyRequest/CompleteReminderRequest.
 */
public record ResolveCommitmentRequest(Integer version) {
}
