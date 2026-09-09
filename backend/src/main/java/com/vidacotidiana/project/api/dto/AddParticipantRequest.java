package com.vidacotidiana.project.api.dto;

import com.vidacotidiana.project.domain.ParticipantRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * V32. `role` es obligatorio: "está en el proyecto" sin decir a qué título no
 * responde nada. Un rol por defecto lo inventaría, y OTRO ya existe para el
 * caso en que el usuario no quiera precisar.
 *
 * CLIENTE no es un valor posible — ver {@link ParticipantRole}.
 */
public record AddParticipantRequest(
        @NotNull UUID personId,
        @NotNull ParticipantRole role
) {
}
