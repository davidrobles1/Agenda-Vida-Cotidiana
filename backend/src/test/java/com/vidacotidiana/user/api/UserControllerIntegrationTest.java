package com.vidacotidiana.user.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static com.vidacotidiana.OpenApiContractSupport.VALIDATOR;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /api/v1/me (Documentacion/openapi/openapi.yaml) and the USER sync
 * behavior it depends on (09-data-model.md, UserSyncFilter/UserSyncService):
 * the row is created on first authenticated request and kept in sync with
 * the Keycloak claims on subsequent ones — without duplicating rows, which
 * would only happen if UserSyncFilter ran more than once per request (see
 * the disabled generic filter registration in identity.infrastructure.SecurityConfig).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("vidacotidiana_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    private Jwt jwtFor(UUID userId, String email, String username) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .claim("email", email)
                .claim("preferred_username", username)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    void getCurrentUser_createsUserRowOnFirstAuthenticatedRequest() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/me").with(jwt().jwt(jwtFor(userId, "frank@example.com", "frank"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userId.toString())))
                .andExpect(jsonPath("$.email", is("frank@example.com")))
                .andExpect(jsonPath("$.username", is("frank")))
                .andExpect(jsonPath("$.deletionStatus", is("ACTIVE")))
                // TEST-API-001: representative contract check for GET /me against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void getCurrentUser_secondRequestWithChangedEmailUpdatesTheSameRow() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/me").with(jwt().jwt(jwtFor(userId, "old@example.com", "grace"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("old@example.com")));

        // Same Keycloak subject (sub), different email claim on a later request
        // (e.g. the user changed their email in Keycloak) — must update the
        // existing row, not create a second one.
        mockMvc.perform(get("/api/v1/me").with(jwt().jwt(jwtFor(userId, "new@example.com", "grace"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(userId.toString())))
                .andExpect(jsonPath("$.email", is("new@example.com")));
    }

    @Test
    void getCurrentUser_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    /** BE-038/ADR-015: a brand-new user (post-migration) starts with neither mode enabled — the migration's grandfather only touches pre-existing rows, not new signups. */
    @Test
    void getCurrentUser_newUserStartsWithNeitherModeEnabled() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/me").with(jwt().jwt(jwtFor(userId, "new-signup@example.com", "newsignup"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalEnabled", is(false)))
                .andExpect(jsonPath("$.laboralEnabled", is(false)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void enableMode_personal_setsPersonalEnabledTrue() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/me").with(jwt().jwt(jwtFor(userId, "modes@example.com", "modes"))));

        mockMvc.perform(post("/api/v1/me/modes")
                        .with(jwt().jwt(jwtFor(userId, "modes@example.com", "modes")))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"PERSONAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalEnabled", is(true)))
                .andExpect(jsonPath("$.laboralEnabled", is(false)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    /** Only-additive: enabling LABORAL must not touch an already-enabled PERSONAL. */
    @Test
    void enableMode_laboral_doesNotDisablePersonal() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/me").with(jwt().jwt(jwtFor(userId, "both@example.com", "both"))));
        mockMvc.perform(post("/api/v1/me/modes")
                .with(jwt().jwt(jwtFor(userId, "both@example.com", "both")))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"PERSONAL\"}"));

        mockMvc.perform(post("/api/v1/me/modes")
                        .with(jwt().jwt(jwtFor(userId, "both@example.com", "both")))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"LABORAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalEnabled", is(true)))
                .andExpect(jsonPath("$.laboralEnabled", is(true)));
    }

    @Test
    void enableMode_alreadyEnabled_isIdempotent() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/me").with(jwt().jwt(jwtFor(userId, "idem@example.com", "idem"))));
        mockMvc.perform(post("/api/v1/me/modes")
                .with(jwt().jwt(jwtFor(userId, "idem@example.com", "idem")))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"PERSONAL\"}"));

        mockMvc.perform(post("/api/v1/me/modes")
                        .with(jwt().jwt(jwtFor(userId, "idem@example.com", "idem")))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"PERSONAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalEnabled", is(true)));
    }

    @Test
    void enableMode_invalidMode_returnsBadRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/me").with(jwt().jwt(jwtFor(userId, "invalid@example.com", "invalidmode"))));

        mockMvc.perform(post("/api/v1/me/modes")
                        .with(jwt().jwt(jwtFor(userId, "invalid@example.com", "invalidmode")))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"NOT_A_MODE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void enableMode_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/me/modes")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"PERSONAL\"}"))
                .andExpect(status().isUnauthorized());
    }
}
