package com.vidacotidiana.note.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Aligned with components.schemas.UpdateNoteRequest in openapi.yaml.
 * Partial update for title/description (optional, null leaves the stored
 * value unchanged); iconId/stickerId are always applied as sent (including
 * absent/null, to clear one) — the edit form always submits its complete
 * current selection. version is required to detect concurrent modifications.
 * personId/projectId (ADR-016 Fase 3a/FR-029): partial, same contract as title/description.
 */
public record UpdateNoteRequest(
        @Size(min = 1, max = 200) String title,
        @Size(max = 2000) String description,
        @Size(max = 32) String iconId,
        @Size(max = 32) String stickerId,
        UUID personId,
        UUID projectId,
        @NotNull Integer version
) {
}
