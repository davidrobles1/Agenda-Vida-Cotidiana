package com.vidacotidiana.daynote.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Edición de contenido (texto/negrita/cursiva) — siempre reemplaza el
 * {@code data} completo, nunca un parche parcial (ver
 * DayNoteElement#applyData). version es obligatorio; un desajuste devuelve
 * 409 DAY_NOTE_ELEMENT_VERSION_CONFLICT.
 */
public record UpdateDayNoteElementDataRequest(
        Map<String, Object> data,
        @NotNull Integer version
) {
}
