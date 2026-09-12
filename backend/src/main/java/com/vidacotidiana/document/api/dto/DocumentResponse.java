package com.vidacotidiana.document.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.document.domain.Document;

import java.time.Instant;
import java.util.UUID;

/** No incluye `data` (los bytes) — eso vive únicamente en el endpoint de
    descarga/visualización (GET /{id}/content), mismo split que
    VisionBoardImage ya usa (metadata vs bytes en respuestas separadas). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentResponse(
        UUID id,
        UUID ownerUserId,
        String name,
        String category,
        String contentType,
        long sizeBytes,
        String visibility,
        String sharedWithEmail,
        UUID personId,
        UUID projectId,
        /** V37: de qué recurso cuelga. Nulo = documento suelto. */
        String resourceType,
        UUID resourceId,
        /** ADR-022: módulo propietario del recurso (el del dueño, no el de
            quien lo recibe compartido). */
        String context,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getOwnerUserId(),
                document.getName(),
                document.getCategory().name(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getVisibility().name(),
                document.getSharedWithEmail(),
                document.getPersonId(),
                document.getProjectId(),
                document.getResourceType(),
                document.getResourceId(),
                document.getContext().name(),
                document.getVersion(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
