package com.vidacotidiana.note.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.note.domain.Note;

import java.time.Instant;
import java.util.UUID;

/** Aligned with components.schemas.Note in Documentacion/openapi/openapi.yaml. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NoteResponse(
        UUID id,
        UUID ownerUserId,
        String title,
        String description,
        String iconId,
        String stickerId,
        UUID personId,
        UUID projectId,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static NoteResponse from(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getOwnerUserId(),
                note.getTitle(),
                note.getDescription(),
                note.getIconId(),
                note.getStickerId(),
                note.getPersonId(),
                note.getProjectId(),
                note.getVersion(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
