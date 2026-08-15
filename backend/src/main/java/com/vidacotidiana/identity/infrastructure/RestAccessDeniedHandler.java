package com.vidacotidiana.identity.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidacotidiana.shared.api.ErrorResponse;
import com.vidacotidiana.shared.infrastructure.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter-level authorization failures (e.g. a future URL-pattern rule in
 * authorizeHttpRequests) are raised inside Spring Security's chain and, like
 * authentication failures, never reach shared.api.GlobalExceptionHandler.
 * This keeps 403 responses on the same uniform Error envelope as everything
 * else (AC-006). Not currently exercised by the Milestone 1 vertical slice
 * (no URL-pattern rule denies an authenticated request yet — reminder
 * ownership is checked in the service layer and returns 404, see
 * ReminderService; see docs/development/03-milestone-1-gate.md for the
 * 404-vs-403 policy note), but required for the components.responses.Forbidden
 * contract entry to be honored consistently once such a rule exists
 * (e.g. Milestone 2 method-security on sharing endpoints).
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        ErrorResponse body = new ErrorResponse("FORBIDDEN", "You do not have permission to perform this action.", traceId);

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
