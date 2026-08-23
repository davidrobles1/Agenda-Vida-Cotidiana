package com.vidacotidiana.person.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Aligned with components.schemas.UpdatePersonRequest in openapi.yaml.
 * Partial update: name/role/organization are optional and, when omitted
 * (null), leave the stored value unchanged (same contract as
 * UpdateWarrantyRequest — no way to explicitly clear role/organization once
 * set, an accepted limitation, not a requirement). version is required.
 */
public record UpdatePersonRequest(
        @Size(min = 1, max = 200) String name,
        @Size(max = 200) String role,
        @Size(max = 200) String organization,
        @NotNull Integer version
) {
}
