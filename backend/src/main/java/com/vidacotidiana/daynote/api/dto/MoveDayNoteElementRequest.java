package com.vidacotidiana.daynote.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Arrastrar/redimensionar. Todos los campos de posición son obligatorios
 * (a diferencia de UpdateElementRequest de Vision Board) porque el cliente
 * siempre conoce el rectángulo completo tras un drag — no hay actualización
 * parcial de posición aquí. version es obligatorio; un desajuste devuelve
 * 409 DAY_NOTE_ELEMENT_VERSION_CONFLICT.
 */
public record MoveDayNoteElementRequest(
        @NotNull Double x,
        @NotNull Double y,
        @NotNull Double width,
        @NotNull Double height,
        @NotNull Integer version
) {
}
