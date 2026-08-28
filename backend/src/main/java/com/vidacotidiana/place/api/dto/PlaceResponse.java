package com.vidacotidiana.place.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.place.domain.Place;

import java.time.Instant;
import java.util.UUID;

/** Aligned with components.schemas.Place in Documentacion/openapi/openapi.yaml. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlaceResponse(
        UUID id,
        UUID ownerUserId,
        String name,
        String address,
        UUID personId,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static PlaceResponse from(Place place) {
        return new PlaceResponse(
                place.getId(),
                place.getOwnerUserId(),
                place.getName(),
                place.getAddress(),
                place.getPersonId(),
                place.getVersion(),
                place.getCreatedAt(),
                place.getUpdatedAt()
        );
    }
}
