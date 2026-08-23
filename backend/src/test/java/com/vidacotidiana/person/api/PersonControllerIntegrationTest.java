package com.vidacotidiana.person.api;

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
 * ADR-016/FR-021. Same pattern as warranty.api.WarrantyControllerIntegrationTest —
 * real PostgreSQL via Testcontainers, mocked JWT principal, full stack.
 * Owner-only authorization: no collaborator concept for Person.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PersonControllerIntegrationTest {

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
    void createAndListPerson_happyPath() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "alice-p@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "Carlos Martínez", "role", "Cliente", "organization", "ACME Solutions"));

        String createdJson = mockMvc.perform(post("/api/v1/people")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Carlos Martínez")))
                .andExpect(jsonPath("$.organization", is("ACME Solutions")))
                .andExpect(jsonPath("$.version", is(0)))
                .andExpect(openApi().isValid(VALIDATOR))
                .andReturn().getResponse().getContentAsString();

        String personId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/people").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(personId)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void createPerson_blankNameIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "bob-p@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("name", ""));

        mockMvc.perform(post("/api/v1/people")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void getPerson_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "owner-p@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "stranger-p@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of("name", "Private contact"));
        String createdJson = mockMvc.perform(post("/api/v1/people")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String personId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/people/" + personId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PERSON_NOT_FOUND")))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void updatePerson_matchingVersionAppliesPartialEdit() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "carol-p@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("name", "Ana Torres", "role", "Compañera"));

        String createdJson = mockMvc.perform(post("/api/v1/people")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String personId = objectMapper.readTree(createdJson).get("id").asText();

        String updateBody = objectMapper.writeValueAsString(Map.of("role", "Gerente de proyecto", "version", 0));

        mockMvc.perform(patch("/api/v1/people/" + personId)
                        .with(principal)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("Gerente de proyecto")))
                .andExpect(jsonPath("$.name", is("Ana Torres")))
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void updatePerson_mismatchedVersionReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "dave-p@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("name", "Proveedor X"));

        String createdJson = mockMvc.perform(post("/api/v1/people")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String personId = objectMapper.readTree(createdJson).get("id").asText();

        String staleUpdateBody = objectMapper.writeValueAsString(Map.of("name", "Proveedor Y", "version", 99));

        mockMvc.perform(patch("/api/v1/people/" + personId)
                        .with(principal)
                        .contentType("application/json")
                        .content(staleUpdateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("PERSON_VERSION_CONFLICT")));
    }

    @Test
    void deletePerson_ownerDeletesThenGetReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "erin-p@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("name", "Throwaway contact"));

        String createdJson = mockMvc.perform(post("/api/v1/people")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String personId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/people/" + personId).with(principal))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/people/" + personId).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PERSON_NOT_FOUND")));
    }

    @Test
    void personEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/people"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }
}
