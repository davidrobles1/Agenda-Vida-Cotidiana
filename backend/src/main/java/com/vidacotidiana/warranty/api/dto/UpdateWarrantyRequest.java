package com.vidacotidiana.warranty.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Aligned with components.schemas.UpdateWarrantyRequest in openapi.yaml.
 * Partial update: item/expiresAt are optional and, when omitted (null),
 * leave the stored value unchanged. version is required — a mismatch
 * against the stored version returns 409 WARRANTY_VERSION_CONFLICT without
 * applying any change.
 */
public record UpdateWarrantyRequest(
        @Size(min = 1, max = 200) String item,
        Instant expiresAt,
        @NotNull Integer version
) {
}
