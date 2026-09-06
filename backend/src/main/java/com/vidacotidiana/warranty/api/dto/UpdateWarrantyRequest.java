package com.vidacotidiana.warranty.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

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
        /**
         * ADR-022: artículo del inventario que cubre la garantía.
         *
         * Se acompaña de `linkInventoryItem` porque, igual que
         * `clearInterval` en Mantenimiento (ADR-021(i)), aquí `null`
         * significa "no tocar" — sin un indicador explícito, DESENLAZAR
         * sería imposible de expresar.
         */
        UUID inventoryItemId,
        Boolean linkInventoryItem,
        @NotNull Integer version
) {
}
