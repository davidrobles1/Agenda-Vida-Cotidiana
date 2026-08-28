package com.vidacotidiana.place.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Aligned with components.schemas.UpdatePlaceRequest in openapi.yaml.
 * Partial update: name/address/personId are optional and, when omitted
 * (null), leave the stored value unchanged (same contract as
 * UpdatePersonRequest). version is required.
 */
public record UpdatePlaceRequest(
        @Size(min = 1, max = 200) String name,
        @Size(max = 500) String address,
        UUID personId,
        @NotNull Integer version
) {
}
