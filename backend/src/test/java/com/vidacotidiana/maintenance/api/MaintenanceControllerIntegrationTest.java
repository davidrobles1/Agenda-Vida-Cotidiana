package com.vidacotidiana.maintenance.api;

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
 * BE-037. Mirrors warranty.api.WarrantyControllerIntegrationTest exactly —
 * same infra pattern (Testcontainers PostgreSQL, mocked JWT, full stack),
 * same owner-only coverage shape.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaintenanceControllerIntegrationTest {

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
    void createListAndCompleteMaintenanceRecord_happyPath() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "alice-m@example.com").build());

        String nextDueAt = Instant.now().plus(400, ChronoUnit.DAYS).toString();
        String createBody = objectMapper.writeValueAsString(Map.of("item", "Cambio de aceite", "nextDueAt", nextDueAt));

        String createdJson = mockMvc.perform(post("/api/v1/maintenance-records")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.item", is("Cambio de aceite")))
                .andExpect(jsonPath("$.status", is("AL_DIA")))
                .andExpect(jsonPath("$.version", is(0)))
                // TEST-API-001: representative contract check for POST /maintenance-records against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR))
                .andReturn().getResponse().getContentAsString();

        String recordId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/maintenance-records").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(recordId)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                // TEST-API-001: representative contract check for GET /maintenance-records against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));

        // No content-type/body on this call (optional requestBody, same as
        // reminder's completeReminder_omittedVersionSkipsConcurrencyCheck) —
        // the openapi-request-validator flags a missing Content-Type even on
        // a genuinely empty optional body, so this call is deliberately not
        // contract-checked here, same precedent as ReminderControllerIntegrationTest.
        mockMvc.perform(post("/api/v1/maintenance-records/" + recordId + "/complete").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETADO")))
                .andExpect(jsonPath("$.version", is(1)));
    }

    @Test
    void createMaintenanceRecord_computesVencidoForPastDueDate() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "bob-m@example.com").build());

        String nextDueAt = Instant.now().minus(10, ChronoUnit.DAYS).toString();
        String createBody = objectMapper.writeValueAsString(Map.of("item", "Revisión de calentador", "nextDueAt", nextDueAt));

        mockMvc.perform(post("/api/v1/maintenance-records")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("VENCIDO")));
    }

    @Test
    void createMaintenanceRecord_computesProximoWithinThreshold() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "carol-m@example.com").build());

        String nextDueAt = Instant.now().plus(5, ChronoUnit.DAYS).toString();
        String createBody = objectMapper.writeValueAsString(Map.of("item", "Servicio de auto", "nextDueAt", nextDueAt));

        mockMvc.perform(post("/api/v1/maintenance-records")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PROXIMO")));
    }

    @Test
    void createMaintenanceRecord_blankItemIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "dave-m@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("item", "", "nextDueAt", Instant.now().toString()));

        mockMvc.perform(post("/api/v1/maintenance-records")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void getMaintenanceRecord_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "owner-m@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "stranger-m@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of("item", "Private record", "nextDueAt", Instant.now().plus(100, ChronoUnit.DAYS).toString()));
        String createdJson = mockMvc.perform(post("/api/v1/maintenance-records")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String recordId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/maintenance-records/" + recordId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("MAINTENANCE_RECORD_NOT_FOUND")))
                // TEST-API-001: representative contract check for GET /maintenance-records/{id} 404 against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void completeMaintenanceRecord_mismatchedVersionReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "erin-m@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("item", "Filtro de agua", "nextDueAt", Instant.now().plus(100, ChronoUnit.DAYS).toString()));

        String createdJson = mockMvc.perform(post("/api/v1/maintenance-records")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String recordId = objectMapper.readTree(createdJson).get("id").asText();

        String staleVersionBody = objectMapper.writeValueAsString(Map.of("version", 99));

        mockMvc.perform(post("/api/v1/maintenance-records/" + recordId + "/complete")
                        .with(principal)
                        .contentType("application/json")
                        .content(staleVersionBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("MAINTENANCE_VERSION_CONFLICT")));
    }

    @Test
    void maintenanceEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/maintenance-records"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void updateMaintenanceRecord_matchingVersionAppliesPartialEdit() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "frank-m@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("item", "Cambio de aceite", "nextDueAt", Instant.now().plus(200, ChronoUnit.DAYS).toString()));

        String createdJson = mockMvc.perform(post("/api/v1/maintenance-records")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String recordId = objectMapper.readTree(createdJson).get("id").asText();

        String updateBody = objectMapper.writeValueAsString(Map.of("item", "Cambio de aceite y filtro", "version", 0));

        mockMvc.perform(patch("/api/v1/maintenance-records/" + recordId)
                        .with(principal)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item", is("Cambio de aceite y filtro")))
                .andExpect(jsonPath("$.version", is(1)))
                // TEST-API-001: representative contract check for PATCH /maintenance-records/{id} 200 against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void updateMaintenanceRecord_mismatchedVersionReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "grace-m@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("item", "Poda de jardín", "nextDueAt", Instant.now().plus(200, ChronoUnit.DAYS).toString()));

        String createdJson = mockMvc.perform(post("/api/v1/maintenance-records")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String recordId = objectMapper.readTree(createdJson).get("id").asText();

        String staleUpdateBody = objectMapper.writeValueAsString(Map.of("item", "Poda de jardín (retraso)", "version", 99));

        mockMvc.perform(patch("/api/v1/maintenance-records/" + recordId)
                        .with(principal)
                        .contentType("application/json")
                        .content(staleUpdateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("MAINTENANCE_VERSION_CONFLICT")));
    }

    @Test
    void updateMaintenanceRecord_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "ivan-m@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "judy-m@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of("item", "Private record", "nextDueAt", Instant.now().plus(100, ChronoUnit.DAYS).toString()));
        String createdJson = mockMvc.perform(post("/api/v1/maintenance-records")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String recordId = objectMapper.readTree(createdJson).get("id").asText();

        String updateBody = objectMapper.writeValueAsString(Map.of("item", "Hijacked item", "version", 0));

        mockMvc.perform(patch("/api/v1/maintenance-records/" + recordId)
                        .with(stranger)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("MAINTENANCE_RECORD_NOT_FOUND")));
    }

    @Test
    void deleteMaintenanceRecord_ownerDeletesThenGetReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "kevin-m@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("item", "Throwaway record", "nextDueAt", Instant.now().plus(100, ChronoUnit.DAYS).toString()));

        String createdJson = mockMvc.perform(post("/api/v1/maintenance-records")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String recordId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/maintenance-records/" + recordId).with(principal))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());

        mockMvc.perform(get("/api/v1/maintenance-records/" + recordId).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("MAINTENANCE_RECORD_NOT_FOUND")));
    }

    @Test
    void deleteMaintenanceRecord_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "laura-m@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "mallory-m@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of("item", "Private record", "nextDueAt", Instant.now().plus(100, ChronoUnit.DAYS).toString()));
        String createdJson = mockMvc.perform(post("/api/v1/maintenance-records")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String recordId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/maintenance-records/" + recordId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("MAINTENANCE_RECORD_NOT_FOUND")));

        mockMvc.perform(get("/api/v1/maintenance-records/" + recordId).with(owner))
                .andExpect(status().isOk())
                // TEST-API-001: representative contract check for GET /maintenance-records/{id} 200 against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void deleteMaintenanceRecord_missingIdReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "nathan-m@example.com").build());

        mockMvc.perform(delete("/api/v1/maintenance-records/" + UUID.randomUUID()).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("MAINTENANCE_RECORD_NOT_FOUND")))
                .andExpect(jsonPath("$.traceId").exists());
    }
}
