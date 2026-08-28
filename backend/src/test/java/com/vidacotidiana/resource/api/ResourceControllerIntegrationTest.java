package com.vidacotidiana.resource.api;

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
 * ADR-016 Fase 3e4/FR-034/AC-021. Same pattern as
 * person.api.PersonControllerIntegrationTest, plus the cross-entity
 * ownership cases for personId/projectId (mirrors Note/Document).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResourceControllerIntegrationTest {

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
    void createAndListResource_happyPath() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "alice-rs@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "Manual de instalación", "type", "MANUAL",
                "reference", "https://ejemplo.com/manual.pdf"));

        String createdJson = mockMvc.perform(post("/api/v1/resources")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Manual de instalación")))
                .andExpect(jsonPath("$.type", is("MANUAL")))
                .andExpect(jsonPath("$.reference", is("https://ejemplo.com/manual.pdf")))
                .andExpect(jsonPath("$.version", is(0)))
                .andExpect(openApi().isValid(VALIDATOR))
                .andReturn().getResponse().getContentAsString();

        String resourceId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/resources").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(resourceId)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    /**
     * DECISION (opción A): `reference` es texto libre, no una URL validada —
     * una ruta de carpeta compartida debe aceptarse igual que una URL.
     */
    @Test
    void createResource_referenceAcceptsNonUrlFreeText() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "bob-rs@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "Plantilla de propuesta", "type", "PLANTILLA",
                "reference", "Carpeta compartida del equipo > Plantillas > 2026"));

        mockMvc.perform(post("/api/v1/resources")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference", is("Carpeta compartida del equipo > Plantillas > 2026")))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    /** `reference` es opcional: una HERRAMIENTA puede no tener ningún puntero. */
    @Test
    void createResource_withoutReferenceIsAccepted() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "carol-rs@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "Estación total", "type", "HERRAMIENTA"));

        mockMvc.perform(post("/api/v1/resources")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Estación total")))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void createResource_blankNameIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "dana-rs@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("name", "", "type", "OTRO"));

        mockMvc.perform(post("/api/v1/resources")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    /** Cubre además el fix de BE-046: un enum desconocido debe dar 400, no 500. */
    @Test
    void createResource_unknownTypeIsRejectedWith400() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "erin-rs@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("name", "X", "type", "VIDEO"));

        mockMvc.perform(post("/api/v1/resources")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void createResource_linkedToOwnPerson_persistsTheLink() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "frank-rs@example.com").build());
        String personId = createPerson(principal, "Carlos Martínez");

        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "Guía del cliente", "type", "ENLACE",
                "reference", "https://acme.example/guia", "personId", personId));

        mockMvc.perform(post("/api/v1/resources")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.personId", is(personId)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    /** AC-021: un personId ajeno debe dar 404 (nunca 403, nunca aceptado en silencio). */
    @Test
    void createResource_withAnotherUsersPersonReturnsNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "owner-rs@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "stranger-rs@example.com").build());

        String foreignPersonId = createPerson(owner, "Persona ajena");

        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "Recurso inválido", "type", "OTRO", "personId", foreignPersonId));

        mockMvc.perform(post("/api/v1/resources")
                        .with(stranger)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PERSON_NOT_FOUND")))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void getResource_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "owner2-rs@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "stranger2-rs@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of("name", "Recurso privado", "type", "OTRO"));
        String createdJson = mockMvc.perform(post("/api/v1/resources")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String resourceId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/resources/" + resourceId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void updateResource_matchingVersionAppliesPartialEdit() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "gina-rs@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "Norma técnica", "type", "DOCUMENTO", "reference", "v1"));

        String createdJson = mockMvc.perform(post("/api/v1/resources")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String resourceId = objectMapper.readTree(createdJson).get("id").asText();

        String updateBody = objectMapper.writeValueAsString(Map.of("reference", "v2", "version", 0));

        mockMvc.perform(patch("/api/v1/resources/" + resourceId)
                        .with(principal)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference", is("v2")))
                .andExpect(jsonPath("$.name", is("Norma técnica")))
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void updateResource_mismatchedVersionReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "hugo-rs@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("name", "Recurso X", "type", "OTRO"));

        String createdJson = mockMvc.perform(post("/api/v1/resources")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String resourceId = objectMapper.readTree(createdJson).get("id").asText();

        String staleUpdateBody = objectMapper.writeValueAsString(Map.of("name", "Recurso Y", "version", 99));

        mockMvc.perform(patch("/api/v1/resources/" + resourceId)
                        .with(principal)
                        .contentType("application/json")
                        .content(staleUpdateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("RESOURCE_VERSION_CONFLICT")));
    }

    @Test
    void deleteResource_ownerDeletesThenGetReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "iris-rs@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("name", "Descartable", "type", "OTRO"));

        String createdJson = mockMvc.perform(post("/api/v1/resources")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String resourceId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/resources/" + resourceId).with(principal))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/resources/" + resourceId).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    void resourceEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/resources"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }
}
