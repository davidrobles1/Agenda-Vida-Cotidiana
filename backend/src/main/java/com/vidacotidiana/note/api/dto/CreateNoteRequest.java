package com.vidacotidiana.note.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Aligned with components.schemas.CreateNoteRequest in openapi.yaml. */
public record CreateNoteRequest(
        @NotBlank @Size(min = 1, max = 200) String title,
        @Size(max = 2000) String description,
        @Size(max = 32) String iconId,
        @Size(max = 32) String stickerId,
        /** ADR-016 Fase 3a/FR-029 (candidato V4). */
        UUID personId,
        UUID projectId
) {
}
