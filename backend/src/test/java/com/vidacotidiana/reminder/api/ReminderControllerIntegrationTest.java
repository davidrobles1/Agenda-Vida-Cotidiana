package com.vidacotidiana.reminder.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests against a real PostgreSQL instance (Testcontainers,
 * not H2 — 07-backend-architecture.md/20-testing-qa.md require validating
 * against the real database engine). Exercises the full stack: Spring
 * Security resource-server filter chain (with a mocked JWT principal,
 * bypassing real Keycloak network calls), USER sync, Flyway migration, JPA,
 * and the Reminder vertical slice endpoints.
 *
 * Traceability: UC-03/UC-04, AC-003/AC-004/AC-004b/AC-005/AC-006,
 * docs/development/01-technical-backlog.md BE-007..BE-013.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReminderControllerIntegrationTest {

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

    @Autowired
    private ObjectMapper objectMapper;

    private org.springframework.security.oauth2.jwt.Jwt.Builder jwtFor(UUID userId, String email) {
        return org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .claim("email", email)
                .claim("preferred_username", email)
                .issuedAt(java.time.Instant.now())
                .expiresAt(java.time.Instant.now().plusSeconds(300));
    }

    @Test
    void createListAndCompleteReminder_happyPath() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "alice@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of("title", "Buy milk"));

        String createdJson = mockMvc.perform(post("/api/v1/reminders")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Buy milk")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.version", is(0)))
                .andReturn().getResponse().getContentAsString();

        String reminderId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/reminders").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(reminderId)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        mockMvc.perform(post("/api/v1/reminders/" + reminderId + "/complete").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.version", is(1)));
    }

    @Test
    void createReminder_blankTitleIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "bob@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("title", ""));

        mockMvc.perform(post("/api/v1/reminders")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void getReminder_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "owner@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "stranger@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of("title", "Private reminder"));
        String createdJson = mockMvc.perform(post("/api/v1/reminders")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String reminderId = objectMapper.readTree(createdJson).get("id").asText();

        // AC-004: a non-owner must get 404, never 403 — never reveals existence.
        mockMvc.perform(get("/api/v1/reminders/" + reminderId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("REMINDER_NOT_FOUND")));
    }

    @Test
    void completeReminder_mismatchedVersionReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "carol@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Water plants"));

        String createdJson = mockMvc.perform(post("/api/v1/reminders")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String reminderId = objectMapper.readTree(createdJson).get("id").asText();

        String staleVersionBody = objectMapper.writeValueAsString(Map.of("version", 99));

        mockMvc.perform(post("/api/v1/reminders/" + reminderId + "/complete")
                        .with(principal)
                        .contentType("application/json")
                        .content(staleVersionBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("REMINDER_VERSION_CONFLICT")));
    }

    @Test
    void reminderEndpoints_requireAuthentication() throws Exception {
        // AC-006: even a 401 raised inside the Spring Security filter chain
        // (before DispatcherServlet/GlobalExceptionHandler) must carry the
        // uniform Error envelope — see identity.infrastructure.RestAuthenticationEntryPoint.
        mockMvc.perform(get("/api/v1/reminders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void completeReminder_omittedVersionSkipsConcurrencyCheck() throws Exception {
        // AC-005: "si se omite, la operación se aplica sin verificación de concurrencia."
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "dave@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Feed the cat"));

        String createdJson = mockMvc.perform(post("/api/v1/reminders")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String reminderId = objectMapper.readTree(createdJson).get("id").asText();

        // No body at all (version omitted) must still succeed, unlike a wrong version.
        mockMvc.perform(post("/api/v1/reminders/" + reminderId + "/complete").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    void getReminder_missingIdReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "erin@example.com").build());

        mockMvc.perform(get("/api/v1/reminders/" + UUID.randomUUID()).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("REMINDER_NOT_FOUND")))
                .andExpect(jsonPath("$.traceId").exists());
    }
}
