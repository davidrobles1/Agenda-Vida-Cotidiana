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

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static com.vidacotidiana.OpenApiContractSupport.VALIDATOR;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
 * Traceability: UC-03/UC-04/UC-05, AC-003/AC-004/AC-004b/AC-005/AC-006/AC-013,
 * docs/development/01-technical-backlog.md BE-007..BE-013, BE-014, BE-015.
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
                // TEST-API-001: representative contract check for POST /reminders against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR))
                .andReturn().getResponse().getContentAsString();

        String reminderId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/reminders").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(reminderId)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                // TEST-API-001: representative contract check for GET /reminders against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));

        mockMvc.perform(post("/api/v1/reminders/" + reminderId + "/complete").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.version", is(1)));
    }

    @Test
    void createReminder_withIconAndSticker_persistsBoth() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "olga@example.com").build());
        String createBody = objectMapper.writeValueAsString(
                Map.of("title", "Cumpleaños de mamá 🎂", "iconId", "birthday", "stickerId", "celebration"));

        mockMvc.perform(post("/api/v1/reminders")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Cumpleaños de mamá 🎂")))
                .andExpect(jsonPath("$.iconId", is("birthday")))
                .andExpect(jsonPath("$.stickerId", is("celebration")))
                // TEST-API-001: representative contract check for POST /reminders (icon/sticker) against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void createReminder_withPersonAndProjectLinks_persistsBoth() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "quinn@example.com").build());

        String personJson = mockMvc.perform(post("/api/v1/people")
                        .with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "Carlos Martínez"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String personId = objectMapper.readTree(personJson).get("id").asText();

        String projectJson = mockMvc.perform(post("/api/v1/projects")
                        .with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "Implementación ERP"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String projectId = objectMapper.readTree(projectJson).get("id").asText();

        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Reunión kickoff ACME", "context", "LABORAL",
                "personId", personId, "projectId", projectId, "location", "Oficina ACME"));

        mockMvc.perform(post("/api/v1/reminders")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.personId", is(personId)))
                .andExpect(jsonPath("$.projectId", is(projectId)))
                .andExpect(jsonPath("$.location", is("Oficina ACME")))
                // ADR-016/FR-023/FR-024: representative contract check for POST /reminders (person/project/location) against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void createReminder_personIdOwnedByAnotherUserReturnsNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "rachel@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "sam@example.com").build());

        String personJson = mockMvc.perform(post("/api/v1/people")
                        .with(stranger).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "Ajeno"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String strangersPersonId = objectMapper.readTree(personJson).get("id").asText();

        String createBody = objectMapper.writeValueAsString(Map.of("title", "x", "personId", strangersPersonId));

        mockMvc.perform(post("/api/v1/reminders")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PERSON_NOT_FOUND")));
    }

    @Test
    void updateReminder_omittingIconIdClearsIt() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "peter@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Con icono", "iconId", "home"));

        String createdJson = mockMvc.perform(post("/api/v1/reminders")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String reminderId = objectMapper.readTree(createdJson).get("id").asText();

        // iconId/stickerId no son parciales (a diferencia de title/description/
        // dueAt): omitirlos en el PATCH los limpia, no los deja intactos.
        String updateBody = objectMapper.writeValueAsString(Map.of("title", "Sin icono", "version", 0));

        mockMvc.perform(patch("/api/v1/reminders/" + reminderId)
                        .with(principal)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Sin icono")))
                .andExpect(jsonPath("$.iconId").doesNotExist());
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
                .andExpect(jsonPath("$.code", is("REMINDER_NOT_FOUND")))
                // TEST-API-001: representative contract check for GET /reminders/{id} 404 against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
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

    @Test
    void updateReminder_matchingVersionAppliesPartialEdit() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "frank@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Buy milk", "description", "2%"));

        String createdJson = mockMvc.perform(post("/api/v1/reminders")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String reminderId = objectMapper.readTree(createdJson).get("id").asText();

        String updateBody = objectMapper.writeValueAsString(Map.of("title", "Buy oat milk", "version", 0));

        mockMvc.perform(patch("/api/v1/reminders/" + reminderId)
                        .with(principal)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Buy oat milk")))
                .andExpect(jsonPath("$.description", is("2%"))) // omitted in the request: unchanged
                .andExpect(jsonPath("$.version", is(1)))
                // TEST-API-001: representative contract check for PATCH /reminders/{id} 200 against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void updateReminder_mismatchedVersionReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "grace@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Water plants"));

        String createdJson = mockMvc.perform(post("/api/v1/reminders")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String reminderId = objectMapper.readTree(createdJson).get("id").asText();

        String staleUpdateBody = objectMapper.writeValueAsString(Map.of("title", "Water the ferns", "version", 99));

        mockMvc.perform(patch("/api/v1/reminders/" + reminderId)
                        .with(principal)
                        .contentType("application/json")
                        .content(staleUpdateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("REMINDER_VERSION_CONFLICT")))
                // TEST-API-001: representative contract check for PATCH /reminders/{id} 409 against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void updateReminder_missingVersionIsRejected() throws Exception {
        // AC-004b: version is required on PATCH, unlike the optional version on /complete.
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "heidi@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Water plants"));

        String createdJson = mockMvc.perform(post("/api/v1/reminders")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String reminderId = objectMapper.readTree(createdJson).get("id").asText();

        String bodyWithoutVersion = objectMapper.writeValueAsString(Map.of("title", "Water the ferns"));

        mockMvc.perform(patch("/api/v1/reminders/" + reminderId)
                        .with(principal)
                        .contentType("application/json")
                        .content(bodyWithoutVersion))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void updateReminder_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "ivan@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "judy@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of("title", "Private reminder"));
        String createdJson = mockMvc.perform(post("/api/v1/reminders")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String reminderId = objectMapper.readTree(createdJson).get("id").asText();

        String updateBody = objectMapper.writeValueAsString(Map.of("title", "Hijacked title", "version", 0));

        mockMvc.perform(patch("/api/v1/reminders/" + reminderId)
                        .with(stranger)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("REMINDER_NOT_FOUND")));
    }

    @Test
    void deleteReminder_ownerDeletesThenGetReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "kevin@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Throwaway reminder"));

        String createdJson = mockMvc.perform(post("/api/v1/reminders")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String reminderId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/reminders/" + reminderId).with(principal))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());

        mockMvc.perform(get("/api/v1/reminders/" + reminderId).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("REMINDER_NOT_FOUND")));
    }

    @Test
    void deleteReminder_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "laura@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "mallory@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of("title", "Private reminder"));
        String createdJson = mockMvc.perform(post("/api/v1/reminders")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String reminderId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/reminders/" + reminderId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("REMINDER_NOT_FOUND")));

        // Untouched: the stranger's failed delete attempt must not have removed the owner's reminder.
        mockMvc.perform(get("/api/v1/reminders/" + reminderId).with(owner))
                .andExpect(status().isOk())
                // TEST-API-001: representative contract check for GET /reminders/{id} 200 against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void deleteReminder_missingIdReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "nathan@example.com").build());

        mockMvc.perform(delete("/api/v1/reminders/" + UUID.randomUUID()).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("REMINDER_NOT_FOUND")))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void deleteReminder_requiresAuthentication() throws Exception {
        // AC-006: even a 401 raised inside the Spring Security filter chain must carry the uniform Error envelope.
        mockMvc.perform(delete("/api/v1/reminders/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.traceId").exists());
    }
}
