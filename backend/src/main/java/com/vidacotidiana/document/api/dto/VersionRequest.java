package com.vidacotidiana.document.api.dto;

import jakarta.validation.constraints.NotNull;

/** Body compartido por make-public/make-private — solo necesitan el version
    esperado para el chequeo optimista, sin más datos. */
public record VersionRequest(@NotNull Integer version) {
}
