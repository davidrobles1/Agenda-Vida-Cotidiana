package com.vidacotidiana.routine.api.dto;

import com.vidacotidiana.routine.domain.RoutineProgress;

import java.time.LocalDate;
import java.util.UUID;

/** Lo hecho de una rutina en un día. El anillo se dibuja con `count` y la meta. */
public record RoutineProgressResponse(
        UUID id,
        UUID routineId,
        LocalDate date,
        int count
) {
    public static RoutineProgressResponse from(RoutineProgress p) {
        return new RoutineProgressResponse(p.getId(), p.getRoutineId(), p.getProgressDate(), p.getCount());
    }
}
