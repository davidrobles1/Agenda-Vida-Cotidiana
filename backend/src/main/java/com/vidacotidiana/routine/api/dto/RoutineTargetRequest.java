package com.vidacotidiana.routine.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * La meta diaria de un hábito.
 *
 * `targetCount` nulo RETIRA la meta y devuelve la rutina a sí/no. No se borra
 * el histórico al hacerlo: si mañana vuelve a ponerse meta, los días que ya
 * estaban contados siguen ahí.
 */
public record RoutineTargetRequest(
        @Min(1) @Max(10000) Integer targetCount,
        @Size(max = 20) String unit
) {
}
