package com.vidacotidiana.identity.infrastructure;

import com.vidacotidiana.shared.infrastructure.TraceIdFilter;
import com.vidacotidiana.user.infrastructure.UserSyncFilter;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.DisableEncodeUrlFilter;

/**
 * Pure OAuth2/OIDC resource server (DEC-004/ADR-008): the backend never
 * issues its own session or token and exposes no /auth/* login endpoint of
 * its own (see 11-auth-security.md, 10-api-openapi.md). Every request to
 * /api/v1/** must carry a bearer JWT issued by Keycloak; validation is
 * delegated to Spring Security via spring.security.oauth2.resourceserver.jwt.issuer-uri.
 *
 * The operational healthcheck (/actuator/health) is intentionally public and
 * lives outside the versioned business contract (07-backend-architecture.md
 * "Observabilidad — healthcheck").
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserSyncFilter userSyncFilter;
    private final TraceIdFilter traceIdFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    public SecurityConfig(UserSyncFilter userSyncFilter,
                           TraceIdFilter traceIdFilter,
                           RestAuthenticationEntryPoint restAuthenticationEntryPoint,
                           RestAccessDeniedHandler restAccessDeniedHandler) {
        this.userSyncFilter = userSyncFilter;
        this.traceIdFilter = traceIdFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // stateless bearer-token API, no cookies/session (CSRF not applicable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}))
                // Runs first, so every response — including 401s produced by
                // ExceptionTranslationFilter further down this same chain —
                // already has a traceId in MDC (AC-006).
                .addFilterBefore(traceIdFilter, DisableEncodeUrlFilter.class)
                // Runs the USER upsert right after the bearer token is validated,
                // before the request reaches any controller (09-data-model.md USER sync).
                .addFilterAfter(userSyncFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Both traceIdFilter and userSyncFilter are wired explicitly into the
     * Spring Security chain above. Without this, Spring Boot's generic
     * servlet-filter auto-configuration would ALSO register each
     * {@code @Component}-annotated OncePerRequestFilter as an independent
     * container filter, running it a second time per request. Disabling the
     * generic registration keeps each filter running exactly once, at the
     * position chosen above.
     */
    @Bean
    public FilterRegistrationBean<Filter> disableAutoRegistrationForTraceIdFilter() {
        return disabled(traceIdFilter);
    }

    @Bean
    public FilterRegistrationBean<Filter> disableAutoRegistrationForUserSyncFilter() {
        return disabled(userSyncFilter);
    }

    private FilterRegistrationBean<Filter> disabled(Filter filter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
