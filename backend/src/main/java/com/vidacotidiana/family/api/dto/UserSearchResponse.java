package com.vidacotidiana.family.api.dto;

import com.vidacotidiana.family.application.FamilyService;

import java.util.UUID;

/**
 * Resultado de buscar personas.
 *
 * SOLO id, nombre de usuario y estado de la relación. Nunca el correo: es el
 * dato que SEC-001 protege, y una búsqueda abierta que lo devolviera
 * convertiría el buscador en un extractor de correos.
 */
public record UserSearchResponse(String userId, String username, String relation) {

    public static UserSearchResponse from(FamilyService.UserSearchResult result) {
        return new UserSearchResponse(
                result.userId().toString(),
                result.username(),
                result.relation().name());
    }

    public static UserSearchResponse of(UUID userId, String username, String relation) {
        return new UserSearchResponse(userId.toString(), username, relation);
    }
}
