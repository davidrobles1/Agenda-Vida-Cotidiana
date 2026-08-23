package com.vidacotidiana.inventory.api;

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

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Módulo Inventario — mismo patrón que WarrantyControllerIntegrationTest,
    cobertura de los caminos críticos (CRUD, filtro por categoría,
    version-conflict, 404-nunca-403, auth requerida). */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryItemControllerIntegrationTest {

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
    void createListFilterEditAndDelete_happyPath() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "inv-owner@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of("name", "Laptop Dell XPS 13", "category", "ELECTRONICOS", "location", "En uso"));
        String createdJson = mockMvc.perform(post("/api/v1/inventory-items")
                        .with(principal).contentType("application/json").content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Laptop Dell XPS 13")))
                .andExpect(jsonPath("$.category", is("ELECTRONICOS")))
                .andExpect(jsonPath("$.version", is(0)))
                .andReturn().getResponse().getContentAsString();
        String itemId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(post("/api/v1/inventory-items")
                        .with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "Sofá", "category", "HOGAR"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/inventory-items").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)));

        mockMvc.perform(get("/api/v1/inventory-items").param("category", "ELECTRONICOS").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.items[0].id", is(itemId)));

        String updateBody = objectMapper.writeValueAsString(Map.of("name", "Laptop Dell XPS 13 (actualizada)", "version", 0));
        mockMvc.perform(patch("/api/v1/inventory-items/" + itemId)
                        .with(principal).contentType("application/json").content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Laptop Dell XPS 13 (actualizada)")))
                .andExpect(jsonPath("$.version", is(1)));

        mockMvc.perform(delete("/api/v1/inventory-items/" + itemId).with(principal))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/inventory-items/" + itemId).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("INVENTORY_ITEM_NOT_FOUND")));
    }

    @Test
    void updateInventoryItem_mismatchedVersionReturns409() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "inv-conflict@example.com").build());
        String createdJson = mockMvc.perform(post("/api/v1/inventory-items")
                        .with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "TV", "category", "ELECTRONICOS"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String itemId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(patch("/api/v1/inventory-items/" + itemId)
                        .with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "TV 2", "version", 99))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INVENTORY_ITEM_VERSION_CONFLICT")));
    }

    @Test
    void inventoryItem_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "inv-owner2@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "inv-stranger@example.com").build());

        String createdJson = mockMvc.perform(post("/api/v1/inventory-items")
                        .with(owner).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "Auto", "category", "VEHICULOS"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String itemId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/inventory-items/" + itemId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("INVENTORY_ITEM_NOT_FOUND")));
        mockMvc.perform(delete("/api/v1/inventory-items/" + itemId).with(stranger))
                .andExpect(status().isNotFound());
    }

    @Test
    void createInventoryItem_blankNameIsRejected() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "inv-blank@example.com").build());
        mockMvc.perform(post("/api/v1/inventory-items")
                        .with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "", "category", "HOGAR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void inventoryEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/inventory-items"))
                .andExpect(status().isUnauthorized());
    }
}
