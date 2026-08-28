package com.vidacotidiana.place.api;

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
 * ADR-016 Fase 3e3/FR-033/AC-020. Same pattern as
 * person.api.PersonControllerIntegrationTest, plus the cross-entity
 * ownership case for personId (mirrors ProjectControllerIntegrationTest's
 * clientPersonId case).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlaceControllerIntegrationTest {

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

    private String createPerson(Object principal, String name) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", name));
        String json = mockMvc.perform(post("/api/v1/people")
                        .with((org.springframework.test.web.servlet.request.RequestPostProcessor) principal)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    @Test
    void createAndListPlace_happyPath() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "alice-pl@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "Oficina ACME", "address", "Av. Reforma 123, CDMX"));

        String createdJson = mockMvc.perform(post("/api/v1/places")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Oficina ACME")))
                .andExpect(jsonPath("$.address", is("Av. Reforma 123, CDMX")))
                .andExpect(jsonPath("$.version", is(0)))
                .andExpect(openApi().isValid(VALIDATOR))
                .andReturn().getResponse().getContentAsString();

        String placeId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/places").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(placeId)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void createPlace_blankNameIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "bob-pl@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("name", ""));

        mockMvc.perform(post("/api/v1/places")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void createPlace_linkedToOwnPerson_persistsTheLink() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "carol-pl@example.com").build());
        String personId = createPerson(principal, "Carlos Martínez");

        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "Oficina del cliente", "address", "Calle 5 #10", "personId", personId));

        mockMvc.perform(post("/api/v1/places")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.personId", is(personId)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    /** AC-020: a personId owned by someone else must 404 (never 403, never silently accepted). */
    @Test
    void createPlace_withAnotherUsersPersonReturnsNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "owner-pl@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "stranger-pl@example.com").build());

        String foreignPersonId = createPerson(owner, "Persona ajena");

        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "Lugar inválido", "personId", foreignPersonId));

        mockMvc.perform(post("/api/v1/places")
                        .with(stranger)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PERSON_NOT_FOUND")))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void getPlace_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "owner2-pl@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "stranger2-pl@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of("name", "Lugar privado"));
        String createdJson = mockMvc.perform(post("/api/v1/places")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String placeId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/places/" + placeId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PLACE_NOT_FOUND")))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void updatePlace_matchingVersionAppliesPartialEdit() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "dana-pl@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("name", "Obra norte", "address", "Km 12"));

        String createdJson = mockMvc.perform(post("/api/v1/places")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String placeId = objectMapper.readTree(createdJson).get("id").asText();

        String updateBody = objectMapper.writeValueAsString(Map.of("address", "Km 14, acceso sur", "version", 0));

        mockMvc.perform(patch("/api/v1/places/" + placeId)
                        .with(principal)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address", is("Km 14, acceso sur")))
                .andExpect(jsonPath("$.name", is("Obra norte")))
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void updatePlace_mismatchedVersionReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "erin-pl@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("name", "Sucursal centro"));

        String createdJson = mockMvc.perform(post("/api/v1/places")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String placeId = objectMapper.readTree(createdJson).get("id").asText();

        String staleUpdateBody = objectMapper.writeValueAsString(Map.of("name", "Otro nombre", "version", 99));

        mockMvc.perform(patch("/api/v1/places/" + placeId)
                        .with(principal)
                        .contentType("application/json")
                        .content(staleUpdateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("PLACE_VERSION_CONFLICT")));
    }

    @Test
    void deletePlace_ownerDeletesThenGetReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "frank-pl@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("name", "Lugar descartable"));

        String createdJson = mockMvc.perform(post("/api/v1/places")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String placeId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/places/" + placeId).with(principal))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/places/" + placeId).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PLACE_NOT_FOUND")));
    }

    @Test
    void placeEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/places"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }
}
