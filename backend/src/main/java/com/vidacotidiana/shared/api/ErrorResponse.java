package com.vidacotidiana.shared.api;

/**
 * Uniform error envelope, aligned with {@code components.schemas.Error} in
 * Documentacion/openapi/openapi.yaml. Never carries stack traces, SQL, or
 * internal names (AC-006, NFR-006).
 */
public record ErrorResponse(String code, String message, String traceId) {
}
