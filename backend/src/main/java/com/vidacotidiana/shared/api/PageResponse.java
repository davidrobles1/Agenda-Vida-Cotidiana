package com.vidacotidiana.shared.api;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Aligned with {@code components.schemas.PageMeta} + {@code items} in
 * Documentacion/openapi/openapi.yaml (pagination added during the V1
 * development gate audit, see 32-v1-development-gate-audit.md).
 */
public record PageResponse<T>(int page, int size, long totalElements, int totalPages, List<T> items) {

    // DEVOPS-002/spotbugs EI_EXPOSE_REP/REP2: defensive copy so this record never shares a mutable
    // List reference with its caller in either direction.
    public PageResponse {
        items = List.copyOf(items);
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getContent()
        );
    }
}
