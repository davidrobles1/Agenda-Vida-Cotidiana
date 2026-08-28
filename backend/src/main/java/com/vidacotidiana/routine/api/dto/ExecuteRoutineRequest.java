package com.vidacotidiana.routine.api.dto;

/**
 * Aligned with components.schemas.ExecuteRoutineRequest in openapi.yaml.
 * Optional: if version is provided, the server validates it for optimistic
 * locking; if omitted, the action is applied without a concurrency check —
 * same contract as ResolveCommitmentRequest/CompleteReminderRequest.
 */
public record ExecuteRoutineRequest(Integer version) {
}
