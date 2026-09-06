package com.vidacotidiana.sharing.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * `responsibility` por defecto FALSE: compartir es, de base, dejar ver.
 * Comprometer a alguien es un paso más y hay que pedirlo explícitamente.
 */
public record CreateResourceShareRequest(@NotNull UUID collaboratorUserId, boolean responsibility) {
}
