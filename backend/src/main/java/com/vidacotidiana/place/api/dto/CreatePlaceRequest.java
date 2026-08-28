package com.vidacotidiana.place.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Aligned with components.schemas.CreatePlaceRequest in openapi.yaml (ADR-016 Fase 3e3, FR-033). */
public record CreatePlaceRequest(
        @NotBlank @Size(min = 1, max = 200) String name,
        @Size(max = 500) String address,
        UUID personId
) {
}
