package com.vidacotidiana.document.api.dto;

import com.vidacotidiana.document.domain.DocumentCategory;

import java.util.UUID;

/** name/category/personId/projectId opcionales (edición parcial, mismo contrato que
    warranty.api.dto.UpdateWarrantyRequest) — version es obligatorio.
    personId/projectId: ADR-016 Fase 3b/FR-030 (candidato V4). */
public record UpdateDocumentRequest(String name, DocumentCategory category, UUID personId, UUID projectId, int version) {
}
