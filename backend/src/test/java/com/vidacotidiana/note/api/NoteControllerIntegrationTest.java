package com.vidacotidiana.note.api;

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

import java.time.Instant;
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
 * Same pattern as warranty.api.WarrantyControllerIntegrationTest — real
 * PostgreSQL via Testcontainers, mocked JWT principal, full stack (Spring
 * Security filter chain, USER sync, Flyway, JPA). Owner-only authorization:
 * no collaborator tests here (NoteRepository has no sharing concept). No
 * `/complete` tests — Note has no completion/status concept, unlike
 * Warranty/Reminder.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NoteControllerIntegrationTest {

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
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
    }

    @Test
    void createAndListNote_happyPath() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "alice-n@example.com").build());

        String createBody = objectMapper.writeValueAsString(
                Map.of("title", "Comprar leche 🥛", "description", "Antes del viernes 🎉",
                        "iconId", "shopping", "stickerId", "celebration"));

        String createdJson = mockMvc.perform(post("/api/v1/notes")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Comprar leche 🥛")))
                .andExpect(jsonPath("$.description", is("Antes del viernes 🎉")))
                .andExpect(jsonPath("$.iconId", is("shopping")))
                .andExpect(jsonPath("$.stickerId", is("celebration")))
                .andExpect(jsonPath("$.version", is(0)))
                // TEST-API-001: representative contract check for POST /notes against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR))
                .andReturn().getResponse().getContentAsString();

        String noteId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/notes").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(noteId)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                // TEST-API-001: representative contract check for GET /notes against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void createNote_withoutIconOrSticker_defaultsToNullBoth() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "bob-n@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Nota simple"));

        mockMvc.perform(post("/api/v1/notes")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Nota simple")))
                .andExpect(jsonPath("$.iconId").doesNotExist())
                .andExpect(jsonPath("$.stickerId").doesNotExist());
    }

    @Test
    void createNote_blankTitleIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "carol-n@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("title", ""));

        mockMvc.perform(post("/api/v1/notes")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void getNote_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "owner-n@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "stranger-n@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of("title", "Private note"));
        String createdJson = mockMvc.perform(post("/api/v1/notes")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String noteId = objectMapper.readTree(createdJson).get("id").asText();

        // AC-004-equivalent (SEC-001): a non-owner must get 404, never 403 — never reveals existence.
        mockMvc.perform(get("/api/v1/notes/" + noteId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOTE_NOT_FOUND")))
                // TEST-API-001: representative contract check for GET /notes/{id} 404 against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void noteEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/notes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void updateNote_matchingVersionAppliesPartialEdit() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "frank-n@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Nota original", "iconId", "home"));

        String createdJson = mockMvc.perform(post("/api/v1/notes")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String noteId = objectMapper.readTree(createdJson).get("id").asText();

        String updateBody = objectMapper.writeValueAsString(
                Map.of("title", "Nota editada", "iconId", "work", "version", 0));

        mockMvc.perform(patch("/api/v1/notes/" + noteId)
                        .with(principal)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Nota editada")))
                .andExpect(jsonPath("$.iconId", is("work")))
                .andExpect(jsonPath("$.version", is(1)))
                // TEST-API-001: representative contract check for PATCH /notes/{id} 200 against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void updateNote_omittingIconIdClearsIt() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "grace-n@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Con icono", "iconId", "home"));

        String createdJson = mockMvc.perform(post("/api/v1/notes")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String noteId = objectMapper.readTree(createdJson).get("id").asText();

        // iconId/stickerId no son parciales (a diferencia de title/description):
        // omitirlos en el PATCH los limpia, no los deja intactos — ver
        // UpdateNoteRequest/Note#applyEdit.
        String updateBody = objectMapper.writeValueAsString(Map.of("title", "Sin icono", "version", 0));

        mockMvc.perform(patch("/api/v1/notes/" + noteId)
                        .with(principal)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Sin icono")))
                .andExpect(jsonPath("$.iconId").doesNotExist());
    }

    @Test
    void updateNote_mismatchedVersionReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "grace2-n@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Nota"));

        String createdJson = mockMvc.perform(post("/api/v1/notes")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String noteId = objectMapper.readTree(createdJson).get("id").asText();

        String staleUpdateBody = objectMapper.writeValueAsString(Map.of("title", "Nota (editada)", "version", 99));

        mockMvc.perform(patch("/api/v1/notes/" + noteId)
                        .with(principal)
                        .contentType("application/json")
                        .content(staleUpdateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("NOTE_VERSION_CONFLICT")));
    }

    @Test
    void updateNote_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "ivan-n@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "judy-n@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of("title", "Private note"));
        String createdJson = mockMvc.perform(post("/api/v1/notes")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String noteId = objectMapper.readTree(createdJson).get("id").asText();

        String updateBody = objectMapper.writeValueAsString(Map.of("title", "Hijacked note", "version", 0));

        mockMvc.perform(patch("/api/v1/notes/" + noteId)
                        .with(stranger)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOTE_NOT_FOUND")));
    }

    @Test
    void deleteNote_ownerDeletesThenGetReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "kevin-n@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Throwaway note"));

        String createdJson = mockMvc.perform(post("/api/v1/notes")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String noteId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/notes/" + noteId).with(principal))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());

        mockMvc.perform(get("/api/v1/notes/" + noteId).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOTE_NOT_FOUND")));
    }

    @Test
    void deleteNote_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "laura-n@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "mallory-n@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of("title", "Private note"));
        String createdJson = mockMvc.perform(post("/api/v1/notes")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String noteId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/notes/" + noteId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOTE_NOT_FOUND")));

        // Untouched: the stranger's failed delete attempt must not have removed the owner's note.
        mockMvc.perform(get("/api/v1/notes/" + noteId).with(owner))
                .andExpect(status().isOk())
                // TEST-API-001: representative contract check for GET /notes/{id} 200 against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void deleteNote_missingIdReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "nathan-n@example.com").build());

        mockMvc.perform(delete("/api/v1/notes/" + UUID.randomUUID()).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOTE_NOT_FOUND")))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void createNote_withPersonAndProjectLinks_persistsBoth() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "olga-n@example.com").build());

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
                "title", "Nota de reunión kickoff", "personId", personId, "projectId", projectId));

        mockMvc.perform(post("/api/v1/notes")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.personId", is(personId)))
                .andExpect(jsonPath("$.projectId", is(projectId)))
                // ADR-016 Fase 3a/FR-029: representative contract check for POST /notes (person/project) against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void createNote_personIdOwnedByAnotherUserReturnsNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "peter-n@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "quinn-n@example.com").build());

        String personJson = mockMvc.perform(post("/api/v1/people")
                        .with(stranger).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "Ajeno"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String strangersPersonId = objectMapper.readTree(personJson).get("id").asText();

        String createBody = objectMapper.writeValueAsString(Map.of("title", "x", "personId", strangersPersonId));

        mockMvc.perform(post("/api/v1/notes")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PERSON_NOT_FOUND")));
    }
}
