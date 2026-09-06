package com.vidacotidiana.document.api.dto;

import java.util.List;
import java.util.UUID;

/**
 * ADR-025 §7. `ids` vacío o ausente significa "todos los del contexto":
 * es la opción "Descargar todos", que no puede depender de que el cliente
 * enumere una lista paginada que solo tiene a medias.
 */
public record DownloadDocumentsRequest(List<UUID> ids, String context) {
}
