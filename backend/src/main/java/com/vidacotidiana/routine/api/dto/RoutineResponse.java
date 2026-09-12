package com.vidacotidiana.routine.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.routine.domain.Routine;
import com.vidacotidiana.routine.domain.RoutineFrequency;

import java.time.Instant;
import java.util.UUID;

/** Aligned with components.schemas.Routine in Documentacion/openapi/openapi.yaml. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoutineResponse(
        UUID id,
        UUID ownerUserId,
        String title,
        String description,
        RoutineFrequency frequency,
        Instant nextExecutionDate,
        boolean active,
        /**
         * V35 — meta diaria y unidad. Nulos = rutina de sí/no, que es como se
         * comportan todas las anteriores a esta migración. Sin exponerlos aquí,
         * el cliente no podría dibujar el anillo «4 de 8» del artefacto.
         */
        Integer targetCount,
        String unit,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static RoutineResponse from(Routine routine) {
        return new RoutineResponse(
                routine.getId(),
                routine.getOwnerUserId(),
                routine.getTitle(),
                routine.getDescription(),
                routine.getFrequency(),
                routine.getNextExecutionDate(),
                routine.isActive(),
                routine.getTargetCount(),
                routine.getUnit(),
                routine.getVersion(),
                routine.getCreatedAt(),
                routine.getUpdatedAt()
        );
    }
}
