package com.vidacotidiana.person.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.person.domain.Person;

import java.time.Instant;
import java.util.UUID;

/** Aligned with components.schemas.Person in Documentacion/openapi/openapi.yaml. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PersonResponse(
        UUID id,
        UUID ownerUserId,
        String name,
        String role,
        String organization,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static PersonResponse from(Person person) {
        return new PersonResponse(
                person.getId(),
                person.getOwnerUserId(),
                person.getName(),
                person.getRole(),
                person.getOrganization(),
                person.getVersion(),
                person.getCreatedAt(),
                person.getUpdatedAt()
        );
    }
}
