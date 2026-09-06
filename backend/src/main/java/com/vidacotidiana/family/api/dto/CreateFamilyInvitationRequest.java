package com.vidacotidiana.family.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Se invita por ID de usuario, no por nombre: quien invita acaba de encontrar
 * a la persona en la búsqueda y ya tiene su id. Resolver otra vez el nombre
 * abriría una ventana en la que el nombre podría apuntar a otra cuenta.
 */
public record CreateFamilyInvitationRequest(@NotNull UUID userId) {
}
