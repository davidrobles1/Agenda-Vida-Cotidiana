package com.vidacotidiana.commitment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * ADR-016/FR-025/FR-027. Same pattern as project.api.ProjectControllerIntegrationTest,
 * plus coverage of the direction filter ("Mías"/"Esperando", UC-20) and the
 * resolve action.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommitmentControllerIntegrationTest {

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

    private String createPerson(RequestPostProcessor principal, String name) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", name));
        String json = mockMvc.perform(post("/api/v1/people")
                        .with(principal)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    @Test
    void createAndListCommitment_happyPath() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "alice-c@example.com").build());
        String personId = createPerson(principal, "Carlos Martínez");

        String dueAt = Instant.now().plus(7, ChronoUnit.DAYS).toString();
        String createBody = objectMapper.writeValueAsString(Map.of(
                "personId", personId, "description", "Confirmar aprobación de la propuesta",
                "direction", "MINE", "dueAt", dueAt));

        String createdJson = mockMvc.perform(post("/api/v1/commitments")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.direction", is("MINE")))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.version", is(0)))
                .andExpect(openApi().isValid(VALIDATOR))
                .andReturn().getResponse().getContentAsString();

        String commitmentId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/commitments").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(commitmentId)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void listCommitments_filteredByDirection_returnsOnlyMatching() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "bob-c@example.com").build());
        String personId = createPerson(principal, "Cliente ACME");
        String dueAt = Instant.now().plus(3, ChronoUnit.DAYS).toString();

        mockMvc.perform(post("/api/v1/commitments").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "personId", personId, "description", "Enviar documentación", "direction", "THEIRS", "dueAt", dueAt))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/commitments").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "personId", personId, "description", "Enviar propuesta", "direction", "MINE", "dueAt", dueAt))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/commitments").param("direction", "THEIRS").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.items[0].direction", is("THEIRS")));

        mockMvc.perform(get("/api/v1/commitments").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));
    }

    @Test
    void createCommitment_personIdOwnedByAnotherUserReturnsNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "owner-c@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "stranger-c@example.com").build());
        String strangersPersonId = createPerson(stranger, "Ajeno");

        String createBody = objectMapper.writeValueAsString(Map.of(
                "personId", strangersPersonId, "description", "x", "direction", "MINE",
                "dueAt", Instant.now().toString()));

        mockMvc.perform(post("/api/v1/commitments")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PERSON_NOT_FOUND")));
    }

    @Test
    void resolveCommitment_marksStatusDone() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "carol-c@example.com").build());
        String personId = createPerson(principal, "Laura Sánchez");

        String createdJson = mockMvc.perform(post("/api/v1/commitments").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "personId", personId, "description", "Confirmar presupuesto", "direction", "THEIRS",
                                "dueAt", Instant.now().plus(2, ChronoUnit.DAYS).toString()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String commitmentId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(post("/api/v1/commitments/" + commitmentId + "/resolve").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DONE")))
                .andExpect(jsonPath("$.version", is(1)));
    }

    @Test
    void resolveCommitment_mismatchedVersionReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "dave-c@example.com").build());
        String personId = createPerson(principal, "Proveedor");

        String createdJson = mockMvc.perform(post("/api/v1/commitments").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "personId", personId, "description", "Enviar cotización", "direction", "THEIRS",
                                "dueAt", Instant.now().toString()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String commitmentId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(post("/api/v1/commitments/" + commitmentId + "/resolve")
                        .with(principal)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("version", 99))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("COMMITMENT_VERSION_CONFLICT")));
    }

    @Test
    void updateCommitment_reprogramsDueAtAndFlipsDirection() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "erin-c@example.com").build());
        String personId = createPerson(principal, "Marta Fuentes");

        String createdJson = mockMvc.perform(post("/api/v1/commitments").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "personId", personId, "description", "Enviar comprobantes", "direction", "THEIRS",
                                "dueAt", Instant.now().toString()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String commitmentId = objectMapper.readTree(createdJson).get("id").asText();

        String newDueAt = Instant.now().plus(10, ChronoUnit.DAYS).toString();
        mockMvc.perform(patch("/api/v1/commitments/" + commitmentId)
                        .with(principal)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("direction", "MINE", "dueAt", newDueAt, "version", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.direction", is("MINE")))
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void getCommitment_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "frank-c@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "grace-c@example.com").build());
        String personId = createPerson(owner, "Contacto privado");

        String createdJson = mockMvc.perform(post("/api/v1/commitments").with(owner).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "personId", personId, "description", "x", "direction", "MINE",
                                "dueAt", Instant.now().toString()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String commitmentId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/commitments/" + commitmentId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("COMMITMENT_NOT_FOUND")));
    }

    @Test
    void deleteCommitment_ownerDeletesThenGetReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "henry-c@example.com").build());
        String personId = createPerson(principal, "Throwaway person");

        String createdJson = mockMvc.perform(post("/api/v1/commitments").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "personId", personId, "description", "x", "direction", "MINE",
                                "dueAt", Instant.now().toString()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String commitmentId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/commitments/" + commitmentId).with(principal))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/commitments/" + commitmentId).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("COMMITMENT_NOT_FOUND")));
    }

    @Test
    void commitmentEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/commitments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }
}
