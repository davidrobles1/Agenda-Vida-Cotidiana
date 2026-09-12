package com.vidacotidiana.mood.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

/**
 * Marcar el ánimo de un día. Ver §4.1 de
 * Documentacion/35-artefacto-maestro-matriz-capacidades.md.
 *
 * `date` es opcional: sin ella se entiende hoy, que es el 99 % de las veces —
 * el usuario toca una carita en Inicio y no elige fecha. Se admite una fecha
 * pasada para poder completar un día que quedó sin marcar; una futura la
 * rechaza el servicio.
 */
public record UpsertMoodRequest(
        LocalDate date,
        /** 0 genial · 1 bien · 2 normal · 3 regular · 4 bajo. Escala cerrada. */
        @NotNull @Min(0) @Max(4) Integer value,
        @Size(max = 2000) String note,
        /** Etiquetas del «¿Qué lo hizo así?». Opcionales. */
        Set<@Size(max = 40) String> tags
) {
}
