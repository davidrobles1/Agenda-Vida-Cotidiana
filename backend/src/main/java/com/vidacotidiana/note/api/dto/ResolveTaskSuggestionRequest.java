package com.vidacotidiana.note.api.dto;

/**
 * Aligned with components.schemas.ResolveTaskSuggestionRequest in
 * openapi.yaml (ADR-016 Fase 3d, FR-035). Optional: if version is provided
 * the server validates it for optimistic locking; if omitted the action is
 * applied without a concurrency check — same contract as
 * ResolveCommitmentRequest.
 */
public record ResolveTaskSuggestionRequest(Integer version) {
}
