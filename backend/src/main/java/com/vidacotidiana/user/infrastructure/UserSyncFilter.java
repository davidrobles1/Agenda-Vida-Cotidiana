package com.vidacotidiana.user.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.shared.api.ErrorResponse;
import com.vidacotidiana.shared.infrastructure.TraceIdFilter;
import com.vidacotidiana.user.application.UserSyncService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs once per authenticated request, after Spring Security has validated
 * the bearer JWT, and upserts the corresponding USER row (see
 * user.application.UserSyncService). Unauthenticated requests (e.g.
 * /actuator/health) are left untouched — there is no identity to sync.
 *
 * This runs as a plain servlet Filter, before the request ever reaches
 * DispatcherServlet — so an exception here (e.g. a malformed Keycloak token
 * missing the expected 'email' claim, or a transient DB error) would
 * otherwise bypass shared.api.GlobalExceptionHandler entirely and produce a
 * raw container error page instead of the uniform Error envelope (AC-006).
 * The catch below closes that gap the same way RestAuthenticationEntryPoint
 * does for authentication failures.
 */
@Component
public class UserSyncFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UserSyncFilter.class);

    private final UserSyncService userSyncService;
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;

    public UserSyncFilter(UserSyncService userSyncService, CurrentUser currentUser, ObjectMapper objectMapper) {
        this.userSyncService = userSyncService;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            try {
                userSyncService.syncFromToken(currentUser.userId(), currentUser.email(), currentUser.username());
            } catch (Exception ex) {
                log.error("USER sync failed for the authenticated caller.", ex);
                writeUnexpectedError(response);
                return; // do not proceed to the controller with a caller that failed to sync
            }
        }
        filterChain.doFilter(request, response);
    }

    private void writeUnexpectedError(HttpServletResponse response) throws IOException {
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        ErrorResponse body = new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred.", traceId);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
