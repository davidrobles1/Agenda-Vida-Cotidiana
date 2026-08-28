package com.vidacotidiana.resource.api.dto;

import com.vidacotidiana.resource.domain.ResourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Aligned with components.schemas.CreateResourceRequest in openapi.yaml
 * (ADR-016 Fase 3e4, FR-034).
 *
 * {@code reference} is one free-text field (DECISION del Product Owner,
 * opción A) — a URL, a shared-folder path, or any textual pointer.
 */
public record CreateResourceRequest(
        @NotBlank @Size(min = 1, max = 200) String name,
        @NotNull ResourceType type,
        @Size(max = 2000) String reference,
        @Size(max = 2000) String description,
        UUID personId,
        UUID projectId
) {
}
