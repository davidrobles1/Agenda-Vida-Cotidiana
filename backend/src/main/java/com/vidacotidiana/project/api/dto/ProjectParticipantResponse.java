package com.vidacotidiana.project.api.dto;

import com.vidacotidiana.project.domain.ProjectParticipant;

import java.time.Instant;
import java.util.UUID;

/**
 * V32. Devuelve solo la participación, no la persona: el cliente ya tiene la
 * lista de personas cargada y resolver el nombre por su lado evita repetir
 * los mismos datos en cada fila —y evita que una copia del nombre quede vieja
 * cuando la persona se renombre.
 */
public record ProjectParticipantResponse(
        UUID id,
        UUID projectId,
        UUID personId,
        String role,
        Instant createdAt
) {
    public static ProjectParticipantResponse from(ProjectParticipant participant) {
        return new ProjectParticipantResponse(
                participant.getId(),
                participant.getProjectId(),
                participant.getPersonId(),
                participant.getRole().name(),
                participant.getCreatedAt());
    }
}
