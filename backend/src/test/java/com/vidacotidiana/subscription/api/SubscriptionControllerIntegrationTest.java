package com.vidacotidiana.subscription.api;

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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Módulo Suscripciones — mismo patrón que WarrantyControllerIntegrationTest. */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionControllerIntegrationTest {

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
    void createListEditAndDelete_happyPath() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "sub-owner@example.com").build());
        String nextPaymentDate = Instant.now().plus(15, ChronoUnit.DAYS).toString();

        Map<String, Object> createBody = new HashMap<>();
        createBody.put("service", "Netflix");
        createBody.put("company", "Netflix Inc.");
        createBody.put("plan", "Premium 4K");
        createBody.put("nextPaymentDate", nextPaymentDate);
        createBody.put("billingCycle", "MONTHLY");

        String createdJson = mockMvc.perform(post("/api/v1/subscriptions")
                        .with(principal).contentType("application/json").content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.service", is("Netflix")))
                .andExpect(jsonPath("$.company", is("Netflix Inc.")))
                .andExpect(jsonPath("$.plan", is("Premium 4K")))
                .andExpect(jsonPath("$.billingCycle", is("MONTHLY")))
                .andExpect(jsonPath("$.version", is(0)))
                .andReturn().getResponse().getContentAsString();
        String subscriptionId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/subscriptions").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)));

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("plan", "Premium 4K + extra member");
        updateBody.put("version", 0);
        mockMvc.perform(patch("/api/v1/subscriptions/" + subscriptionId)
                        .with(principal).contentType("application/json").content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan", is("Premium 4K + extra member")))
                .andExpect(jsonPath("$.service", is("Netflix")))
                .andExpect(jsonPath("$.version", is(1)));

        mockMvc.perform(delete("/api/v1/subscriptions/" + subscriptionId).with(principal))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/subscriptions/" + subscriptionId).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("SUBSCRIPTION_NOT_FOUND")));
    }

    @Test
    void createSubscription_weeklyAndYearlyCyclesAccepted() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "sub-cycles@example.com").build());

        for (String cycle : new String[]{"WEEKLY", "YEARLY"}) {
            Map<String, Object> body = new HashMap<>();
            body.put("service", "Servicio " + cycle);
            body.put("nextPaymentDate", Instant.now().plus(10, ChronoUnit.DAYS).toString());
            body.put("billingCycle", cycle);
            mockMvc.perform(post("/api/v1/subscriptions")
                            .with(principal).contentType("application/json").content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.billingCycle", is(cycle)));
        }
    }

    @Test
    void updateSubscription_mismatchedVersionReturns409() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "sub-conflict@example.com").build());
        Map<String, Object> createBody = new HashMap<>();
        createBody.put("service", "Spotify");
        createBody.put("nextPaymentDate", Instant.now().plus(5, ChronoUnit.DAYS).toString());
        createBody.put("billingCycle", "MONTHLY");

        String createdJson = mockMvc.perform(post("/api/v1/subscriptions")
                        .with(principal).contentType("application/json").content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String subscriptionId = objectMapper.readTree(createdJson).get("id").asText();

        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("service", "Spotify Family");
        updateBody.put("version", 99);
        mockMvc.perform(patch("/api/v1/subscriptions/" + subscriptionId)
                        .with(principal).contentType("application/json").content(objectMapper.writeValueAsString(updateBody)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("SUBSCRIPTION_VERSION_CONFLICT")));
    }

    @Test
    void subscription_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "sub-owner2@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "sub-stranger@example.com").build());

        Map<String, Object> createBody = new HashMap<>();
        createBody.put("service", "iCloud+");
        createBody.put("nextPaymentDate", Instant.now().plus(5, ChronoUnit.DAYS).toString());
        createBody.put("billingCycle", "MONTHLY");
        String createdJson = mockMvc.perform(post("/api/v1/subscriptions")
                        .with(owner).contentType("application/json").content(objectMapper.writeValueAsString(createBody)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String subscriptionId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/subscriptions/" + subscriptionId).with(stranger))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/subscriptions/" + subscriptionId).with(stranger))
                .andExpect(status().isNotFound());
    }

    @Test
    void createSubscription_blankServiceIsRejected() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "sub-blank@example.com").build());
        Map<String, Object> body = new HashMap<>();
        body.put("service", "");
        body.put("nextPaymentDate", Instant.now().toString());
        body.put("billingCycle", "MONTHLY");

        mockMvc.perform(post("/api/v1/subscriptions")
                        .with(principal).contentType("application/json").content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void subscriptionEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions"))
                .andExpect(status().isUnauthorized());
    }
}
