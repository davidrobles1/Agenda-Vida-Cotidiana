package com.vidacotidiana.person.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Aligned with components.schemas.CreatePersonRequest in openapi.yaml (ADR-016, FR-021). */
public record CreatePersonRequest(
        @NotBlank @Size(min = 1, max = 200) String name,
        @Size(max = 200) String role,
        @Size(max = 200) String organization
) {
}
