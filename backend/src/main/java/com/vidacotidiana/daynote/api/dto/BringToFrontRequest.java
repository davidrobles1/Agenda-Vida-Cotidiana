package com.vidacotidiana.daynote.api.dto;

import jakarta.validation.constraints.NotNull;

/** version es obligatorio; un desajuste devuelve 409 DAY_NOTE_ELEMENT_VERSION_CONFLICT. */
public record BringToFrontRequest(@NotNull Integer version) {
}
