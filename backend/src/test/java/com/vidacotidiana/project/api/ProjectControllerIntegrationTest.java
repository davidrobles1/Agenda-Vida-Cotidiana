package com.vidacotidiana.project.api;

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
 * ADR-016/FR-022. Same pattern as person.api.PersonControllerIntegrationTest,
 * plus coverage of the clientPersonId cross-ownership check (ProjectService
 * reuses PersonService.getOwnedOrThrow).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerIntegrationTest {

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

    private String createPerson(org.springframework.test.web.servlet.request.RequestPostProcessor principal, String name) throws Exception {
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
    void createAndListProject_happyPath() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "alice-pr@example.com").build());
        String clientId = createPerson(principal, "Carlos Martínez");

        String createBody = objectMapper.writeValueAsString(Map.of(
                "name", "Implementación ERP", "clientPersonId", clientId, "status", "En curso"));

        String createdJson = mockMvc.perform(post("/api/v1/projects")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Implementación ERP")))
                .andExpect(jsonPath("$.clientPersonId", is(clientId)))
                .andExpect(jsonPath("$.version", is(0)))
                .andExpect(openApi().isValid(VALIDATOR))
                .andReturn().getResponse().getContentAsString();

        String projectId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/projects").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(projectId)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void createProject_clientPersonIdOwnedByAnotherUserReturnsNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "owner-pr@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "stranger-pr@example.com").build());

        String strangersPersonId = createPerson(stranger, "Ajeno");

        String createBody = objectMapper.writeValueAsString(Map.of("name", "Proyecto", "clientPersonId", strangersPersonId));

        mockMvc.perform(post("/api/v1/projects")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PERSON_NOT_FOUND")));
    }

    @Test
    void createProject_withoutClientPersonIdOmitsIt() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "bob-pr@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("name", "Caso Fuentes vs. Inmobiliaria"));

        mockMvc.perform(post("/api/v1/projects")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Caso Fuentes vs. Inmobiliaria")))
                .andExpect(jsonPath("$.clientPersonId").doesNotExist());
    }

    @Test
    void getProject_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "carol-pr@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "dave-pr@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of("name", "Private project"));
        String createdJson = mockMvc.perform(post("/api/v1/projects")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String projectId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/projects/" + projectId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PROJECT_NOT_FOUND")));
    }

    @Test
    void updateProject_matchingVersionAppliesPartialEdit() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "erin-pr@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("name", "Obra Casa Gómez"));

        String createdJson = mockMvc.perform(post("/api/v1/projects")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String projectId = objectMapper.readTree(createdJson).get("id").asText();

        String updateBody = objectMapper.writeValueAsString(Map.of("status", "En obra", "version", 0));

        mockMvc.perform(patch("/api/v1/projects/" + projectId)
                        .with(principal)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("En obra")))
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void updateProject_mismatchedVersionReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "frank-pr@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("name", "Oportunidad Valle"));

        String createdJson = mockMvc.perform(post("/api/v1/projects")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String projectId = objectMapper.readTree(createdJson).get("id").asText();

        String staleUpdateBody = objectMapper.writeValueAsString(Map.of("status", "Cerrado", "version", 99));

        mockMvc.perform(patch("/api/v1/projects/" + projectId)
                        .with(principal)
                        .contentType("application/json")
                        .content(staleUpdateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("PROJECT_VERSION_CONFLICT")));
    }

    @Test
    void deleteProject_ownerDeletesThenGetReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "grace-pr@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("name", "Throwaway project"));

        String createdJson = mockMvc.perform(post("/api/v1/projects")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String projectId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/projects/" + projectId).with(principal))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/projects/" + projectId).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PROJECT_NOT_FOUND")));
    }

    @Test
    void projectEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }
}
