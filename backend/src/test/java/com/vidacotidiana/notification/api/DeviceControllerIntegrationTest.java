package com.vidacotidiana.notification.api;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** BE-024 — Documentacion/openapi/openapi.yaml, /me/devices*. Testcontainers, real PostgreSQL. */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceControllerIntegrationTest {

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

    private RequestPostProcessor principalFor(UUID userId, String email, String username) {
        var jwtToken = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .claim("email", email)
                .claim("preferred_username", username)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        return jwt().jwt(jwtToken);
    }

    @Test
    void registerListAndDeleteOwnDevice_happyPath() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = principalFor(userId, "device1@example.com", "device1");
        String body = objectMapper.writeValueAsString(Map.of("platform", "ANDROID", "token", "fcm-token-abc"));

        String createdJson = mockMvc.perform(post("/api/v1/me/devices").with(principal).contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.platform", is("ANDROID")))
                .andExpect(jsonPath("$.id").exists())
                // TEST-API-001: representative contract check for POST /me/devices against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR))
                .andReturn().getResponse().getContentAsString();
        String deviceId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/me/devices").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(deviceId)))
                // TEST-API-001: representative contract check for GET /me/devices against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));

        mockMvc.perform(delete("/api/v1/me/devices/" + deviceId).with(principal))
                .andExpect(status().isNoContent())
                // TEST-API-001: representative contract check for DELETE /me/devices/{id} against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));

        mockMvc.perform(get("/api/v1/me/devices").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void registerDevice_upsertsByToken_reassigningFromPreviousOwner() throws Exception {
        UUID firstOwner = UUID.randomUUID();
        UUID secondOwner = UUID.randomUUID();
        var firstPrincipal = principalFor(firstOwner, "first@example.com", "first");
        var secondPrincipal = principalFor(secondOwner, "second@example.com", "second");
        String body = objectMapper.writeValueAsString(Map.of("platform", "WEB", "token", "shared-physical-token"));

        mockMvc.perform(post("/api/v1/me/devices").with(firstPrincipal).contentType("application/json").content(body))
                .andExpect(status().isCreated());

        // Same token registered by a different account (device changed hands) -> reassigned, not duplicated.
        mockMvc.perform(post("/api/v1/me/devices").with(secondPrincipal).contentType("application/json").content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/me/devices").with(firstPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/v1/me/devices").with(secondPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].platform", is("WEB")));
    }

    @Test
    void registerDevice_invalidPlatform_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = principalFor(userId, "device2@example.com", "device2");
        String body = objectMapper.writeValueAsString(Map.of("platform", "PALMPILOT", "token", "some-token"));

        mockMvc.perform(post("/api/v1/me/devices").with(principal).contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void deleteDevice_ownedByAnotherUser_returns403NotFoundUniform() throws Exception {
        // AC-014: explicitly 403 here, unlike the uniform 404 used across Reminder/sharing.
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner-dev@example.com", "ownerdev");
        var stranger = principalFor(strangerId, "stranger-dev@example.com", "strangerdev");
        String body = objectMapper.writeValueAsString(Map.of("platform", "IOS", "token", "owner-token"));

        String createdJson = mockMvc.perform(post("/api/v1/me/devices").with(owner).contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String deviceId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/me/devices/" + deviceId).with(stranger))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("FORBIDDEN")))
                // TEST-API-001/BE-024: representative contract check for the explicit 403 against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void deleteDevice_missingReturns404() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = principalFor(userId, "device3@example.com", "device3");

        mockMvc.perform(delete("/api/v1/me/devices/" + UUID.randomUUID()).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DEVICE_NOT_FOUND")));
    }
}
