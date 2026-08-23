package com.vidacotidiana.warranty.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.warranty.domain.Warranty;
import com.vidacotidiana.warranty.domain.WarrantyStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Aligned with components.schemas.Warranty in Documentacion/openapi/openapi.yaml.
 * `status` matches web/src/core/mock/mockData.ts's WarrantyStatus contract
 * exactly (VIGENTE/POR_VENCER/VENCIDA), plus COMPLETADO for a
 * user-completed warranty. Computed here from the persisted 2-value
 * WarrantyStatus + expiresAt vs "now" — never stored as a 4-value column
 * (see WarrantyStatus javadoc / 09-data-model.md RECOMMENDATION).
 *
 * ASSUMPTION (BE-037, no product decision exists for this threshold yet):
 * "por vencer" = expires within 30 days of now. Documented explicitly so it
 * can be revisited without hunting through code.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WarrantyResponse(
        UUID id,
        UUID ownerUserId,
        String item,
        Instant expiresAt,
        String status,
        int version,
        Instant createdAt,
        Instant updatedAt,
        /** null cuando la garantía no tiene archivo adjunto — nunca los
            bytes en sí, esos solo viven en GET /{id}/content (mismo split
            que DocumentResponse). */
        String documentContentType
) {
    private static final long POR_VENCER_THRESHOLD_DAYS = 30;

    public static WarrantyResponse from(Warranty warranty) {
        return from(warranty, Instant.now());
    }

    public static WarrantyResponse from(Warranty warranty, Instant now) {
        return new WarrantyResponse(
                warranty.getId(),
                warranty.getOwnerUserId(),
                warranty.getItem(),
                warranty.getExpiresAt(),
                computeStatus(warranty, now),
                warranty.getVersion(),
                warranty.getCreatedAt(),
                warranty.getUpdatedAt(),
                warranty.getDocumentContentType()
        );
    }

    private static String computeStatus(Warranty warranty, Instant now) {
        if (warranty.getStatus() == WarrantyStatus.COMPLETED) {
            return "COMPLETADO";
        }
        if (warranty.getExpiresAt().isBefore(now)) {
            return "VENCIDA";
        }
        if (warranty.getExpiresAt().isBefore(now.plus(POR_VENCER_THRESHOLD_DAYS, ChronoUnit.DAYS))) {
            return "POR_VENCER";
        }
        return "VIGENTE";
    }
}
