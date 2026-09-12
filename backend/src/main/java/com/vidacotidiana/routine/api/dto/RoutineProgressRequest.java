package com.vidacotidiana.routine.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;

/**
 * Sumar o fijar progreso.
 *
 * `date` opcional: sin ella es hoy, que es lo que ocurre al tocar el anillo.
 * `delta` por defecto 1 — el gesto normal es sumar uno.
 */
public record RoutineProgressRequest(
        LocalDate date,
        @Min(-1000) @Max(1000) Integer delta,
        @Min(0) @Max(10000) Integer value
) {
    public int deltaOrOne() {
        return delta == null ? 1 : delta;
    }
}
