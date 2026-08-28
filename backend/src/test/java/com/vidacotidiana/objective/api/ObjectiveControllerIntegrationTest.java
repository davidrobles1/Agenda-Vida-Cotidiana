package com.vidacotidiana.objective.api;

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
 * ADR-016 Fase 3e1/FR-031/AC-018. Same pattern as
 * person.api.PersonControllerIntegrationTest — real PostgreSQL via
 * Testcontainers, mocked JWT principal, full stack, every response validated
 * against the real openapi.yaml contract.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ObjectiveControllerIntegrationTest {

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
    void createAndListObjective_happyPath() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "alice-o@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Cerrar 3 proyectos este trimestre", "targetValue", 3));

        String createdJson = mockMvc.perform(post("/api/v1/objectives")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Cerrar 3 proyectos este trimestre")))
                .andExpect(jsonPath("$.targetValue", is(3)))
                .andExpect(jsonPath("$.currentValue", is(0)))
                .andExpect(jsonPath("$.completed", is(false)))
                .andExpect(jsonPath("$.version", is(0)))
                .andExpect(openApi().isValid(VALIDATOR))
                .andReturn().getResponse().getContentAsString();

        String objectiveId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/objectives").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(objectiveId)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void createObjective_blankTitleIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "bob-o@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("title", ""));

        mockMvc.perform(post("/api/v1/objectives")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void getObjective_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "owner-o@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "stranger-o@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of("title", "Objetivo privado"));
        String createdJson = mockMvc.perform(post("/api/v1/objectives")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String objectiveId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/objectives/" + objectiveId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("OBJECTIVE_NOT_FOUND")))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    /**
     * AC-018: progress is manual. Updating currentValue to match targetValue
     * must NOT flip `completed` on its own — the field only changes when the
     * caller sets it explicitly (see the second half of this test).
     */
    @Test
    void updateObjective_progressIsManualAndNeverAutoCompletes() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "carol-o@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Contactar 5 clientes", "targetValue", 5));

        String createdJson = mockMvc.perform(post("/api/v1/objectives")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String objectiveId = objectMapper.readTree(createdJson).get("id").asText();

        String reachTargetBody = objectMapper.writeValueAsString(Map.of("currentValue", 5, "version", 0));

        mockMvc.perform(patch("/api/v1/objectives/" + objectiveId)
                        .with(principal)
                        .contentType("application/json")
                        .content(reachTargetBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentValue", is(5)))
                .andExpect(jsonPath("$.completed", is(false)))
                .andExpect(jsonPath("$.title", is("Contactar 5 clientes")))
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(openApi().isValid(VALIDATOR));

        String markDoneBody = objectMapper.writeValueAsString(Map.of("completed", true, "version", 1));

        mockMvc.perform(patch("/api/v1/objectives/" + objectiveId)
                        .with(principal)
                        .contentType("application/json")
                        .content(markDoneBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed", is(true)))
                .andExpect(jsonPath("$.version", is(2)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void updateObjective_mismatchedVersionReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "dave-o@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Objetivo con conflicto"));

        String createdJson = mockMvc.perform(post("/api/v1/objectives")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String objectiveId = objectMapper.readTree(createdJson).get("id").asText();

        String staleUpdateBody = objectMapper.writeValueAsString(Map.of("title", "Otro título", "version", 99));

        mockMvc.perform(patch("/api/v1/objectives/" + objectiveId)
                        .with(principal)
                        .contentType("application/json")
                        .content(staleUpdateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("OBJECTIVE_VERSION_CONFLICT")));
    }

    @Test
    void deleteObjective_ownerDeletesThenGetReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "erin-o@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("title", "Objetivo descartable"));

        String createdJson = mockMvc.perform(post("/api/v1/objectives")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String objectiveId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/objectives/" + objectiveId).with(principal))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/objectives/" + objectiveId).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("OBJECTIVE_NOT_FOUND")));
    }

    @Test
    void objectiveEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/objectives"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }
}
