package com.vidacotidiana.resource.api.dto;

import com.vidacotidiana.resource.domain.ResourceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Aligned with components.schemas.UpdateResourceRequest in openapi.yaml.
 * Partial update: every field except version is optional and, when omitted
 * (null), leaves the stored value unchanged. version is required.
 */
public record UpdateResourceRequest(
        @Size(min = 1, max = 200) String name,
        ResourceType type,
        @Size(max = 2000) String reference,
        @Size(max = 2000) String description,
        UUID personId,
        UUID projectId,
        @NotNull Integer version
) {
}
