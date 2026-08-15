package com.vidacotidiana.identity.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidacotidiana.shared.api.ErrorResponse;
import com.vidacotidiana.shared.infrastructure.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Authentication failures (missing/invalid/expired bearer token) are raised
 * inside Spring Security's filter chain, before the request ever reaches a
 * @RestControllerAdvice — so without this entry point, 401 responses would
 * bypass shared.api.GlobalExceptionHandler entirely and not carry the
 * uniform Error envelope required by AC-006 / components.schemas.Error in
 * openapi.yaml (code/message/traceId, e.g. the '401' -> Unauthorized
 * response documented on every operation).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        ErrorResponse body = new ErrorResponse("UNAUTHORIZED", "Authentication is required to access this resource.", traceId);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
