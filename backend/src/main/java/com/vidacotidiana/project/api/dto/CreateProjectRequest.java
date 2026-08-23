package com.vidacotidiana.project.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Aligned with components.schemas.CreateProjectRequest in openapi.yaml
 * (ADR-016, FR-022). status is optional free text in V3 — no server-side
 * default is invented (CLAUDE.md: no inventar requerimientos de negocio);
 * an omitted status is stored as null.
 */
public record CreateProjectRequest(
        @NotBlank @Size(min = 1, max = 200) String name,
        UUID clientPersonId,
        @Size(max = 100) String status,
        Instant deadline
) {
}
