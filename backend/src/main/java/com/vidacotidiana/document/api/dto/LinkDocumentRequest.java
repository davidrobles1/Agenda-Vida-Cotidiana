package com.vidacotidiana.document.api.dto;

import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * Colgar un documento de un recurso — o soltarlo (V37).
 *
 * Enviar los dos campos nulos DESENGANCHA el documento sin borrarlo: sigue
 * existiendo en Documentos, solo deja de pertenecer a ese registro. Borrar al
 * desenganchar sería destruir un archivo por reordenar.
 *
 * Sin clave ajena en la base: `resourceId` apunta a cinco tablas distintas y
 * una FK solo puede apuntar a una. La integridad la sostiene el servicio.
 */
public record LinkDocumentRequest(
        @Pattern(regexp = "REMINDER|MAINTENANCE|WARRANTY|INVENTORY_ITEM|SUBSCRIPTION") String resourceType,
        UUID resourceId
) {
}
