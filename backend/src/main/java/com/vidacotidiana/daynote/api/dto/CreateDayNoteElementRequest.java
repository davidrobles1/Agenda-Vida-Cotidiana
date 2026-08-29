package com.vidacotidiana.daynote.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.Map;

/**
 * Mirrors visionboard.api.dto.CreateElementRequest's shape — {@code type}
 * validated as a string (fails with a clear 400 before ever reaching
 * DayNoteElementType.valueOf) restricted to the 2 values the user asked
 * for explicitly. {@code data} is optional — an element can be created
 * empty and filled in via editData right after (same as Vision Board).
 */
public record CreateDayNoteElementRequest(
        @NotNull LocalDate noteDate,
        @NotBlank @Pattern(regexp = "BANNER|TEXT") String type,
        @NotNull Double x,
        @NotNull Double y,
        @NotNull Double width,
        @NotNull Double height,
        Map<String, Object> data,
        /** ADR-019: módulo desde el que se crea. Ausente ⇒ PERSONAL. */
        @Pattern(regexp = "PERSONAL|LABORAL") String context
) {
}
