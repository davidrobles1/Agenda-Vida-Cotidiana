package com.vidacotidiana.shared.api;

import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.DomainException;
import com.vidacotidiana.shared.domain.GoneException;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.RateLimitExceededException;
import com.vidacotidiana.shared.domain.VersionConflictException;
import com.vidacotidiana.shared.infrastructure.TraceIdFilter;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Uniform error handling (AC-006, NFR-006): every error response uses the
 * Error envelope {code, message, traceId}; no stack traces, SQL, or
 * internal names are ever returned to the client (CLAUDE.md, "CÓDIGOS DE
 * ERROR").
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(VersionConflictException.class)
    public ResponseEntity<ErrorResponse> handleVersionConflict(VersionConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(ObjectOptimisticLockingFailureException ex) {
        return build(HttpStatus.CONFLICT, "REMINDER_VERSION_CONFLICT",
                "The resource was modified concurrently; refetch and retry.");
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(GoneException.class)
    public ResponseEntity<ErrorResponse> handleGone(GoneException ex) {
        return build(HttpStatus.GONE, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException ex) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action.");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ErrorResponse> handleValidation(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "One or more fields are invalid.");
    }

    /**
     * A request body Jackson cannot deserialize at all — an unknown enum
     * constant (e.g. {@code frequency: "FORTNIGHTLY"} against
     * RoutineFrequency, or an invalid CommitmentDirection), a malformed date,
     * plain broken JSON. Without this handler it fell through to the generic
     * 500 below, which is wrong: the caller sent a bad request, the server
     * did not fail.
     *
     * <p>Gap preexistente encontrado por RoutineControllerIntegrationTest
     * (ADR-016 Fase 3e2) — no era específico de Routine: el mismo 500
     * ocurría en `POST /commitments` con un `direction` inválido, caso que
     * ningún test cubría. Se corrige aquí, en el punto único donde vive la
     * política de errores (AC-006), en vez de por endpoint.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        // Same opaque message as handleValidation: never echo back Jackson's
        // detail, which names internal types and field paths (AC-006).
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "One or more fields are invalid.");
    }

    /**
     * The caller sent a Content-Type the endpoint does not accept (e.g. JSON
     * to `POST /warranties`, which takes multipart since the warranty-file
     * upload). That is a client error — 415, the status HTTP defines for it —
     * not a server failure.
     *
     * <p>Mismo gap y misma corrección que {@link #handleUnreadableBody}: sin
     * este handler caía en el 500 genérico, lo que además de ser el código
     * equivocado hacía que un desajuste trivial de Content-Type pareciera una
     * caída del servidor. Encontrado al depurar por qué
     * `warranties-maintenance.spec.ts` fallaba (2026-08-28).
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "The request Content-Type is not supported by this endpoint.");
    }

    /** BLOQUE B: a multipart request over `spring.servlet.multipart.max-file-size`
        (application.yml) throws this before VisionBoardImageController/
        VisionBoardImageService's own, lower, domain-level size check ever
        runs — without this handler it falls through to the generic 500
        below, which is wrong for something this ordinary. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "The uploaded file is too large.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        // Never leak the exception message/stack trace to the client (AC-006).
        // The real detail belongs server-side, correlated by traceId.
        //
        // Ese log es lo que este método prometía en un comentario y no hacía:
        // un 500 salía sin dejar ningún rastro, lo que hacía imposible
        // diagnosticar nada en un entorno real (encontrado al depurar un 500
        // real de POST /warranties, 2026-08-28). El traceId va explícito en el
        // mensaje además de en el MDC, para que siga siendo correlacionable
        // aunque el patrón de log configurado no incluya el MDC.
        log.error("Unhandled exception (traceId={})", MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message) {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, traceId));
    }
}
