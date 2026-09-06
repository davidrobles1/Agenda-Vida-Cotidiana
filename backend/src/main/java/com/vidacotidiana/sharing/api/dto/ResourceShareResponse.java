package com.vidacotidiana.sharing.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.sharing.application.ResourceSharingService;
import com.vidacotidiana.sharing.domain.ResourceShare;

/**
 * Una fila de la sección Compartidos.
 *
 * Lleva el NOMBRE y la FECHA del recurso, no una copia del recurso: es
 * suficiente para pintar la fila y para navegar a su módulo de origen, y
 * evita que Compartidos se convierta en un segundo lugar donde vive el dato.
 *
 * `counterpartUsername` es "quién me lo compartió" en las recibidas y "con
 * quién lo compartí" en las enviadas — el mismo campo, la otra persona.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceShareResponse(
        String id,
        String resourceType,
        String resourceId,
        String resourceLabel,
        String resourceDate,
        String counterpartUserId,
        String counterpartUsername,
        boolean responsibility,
        String partDoneAt,
        int version) {

    public static ResourceShareResponse from(ResourceSharingService.ShareView view) {
        ResourceShare share = view.share();
        var resource = view.resource();
        return new ResourceShareResponse(
                share.getId().toString(),
                share.getResourceType().name(),
                share.getResourceId().toString(),
                resource.label(),
                resource.date() == null ? null : resource.date().toString(),
                view.counterpart() == null ? null : view.counterpart().getId().toString(),
                view.counterpart() == null ? null : view.counterpart().getUsername(),
                share.hasResponsibility(),
                share.getPartDoneAt() == null ? null : share.getPartDoneAt().toString(),
                share.getVersion());
    }
}
