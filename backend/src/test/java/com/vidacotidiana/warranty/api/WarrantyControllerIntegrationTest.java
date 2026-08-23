package com.vidacotidiana.warranty.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
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

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BE-037. Same pattern as reminder.api.ReminderControllerIntegrationTest —
 * real PostgreSQL via Testcontainers, mocked JWT principal, full stack.
 *
 * Pedido explícito del usuario (2026-08-21): "al registrar una garantía
 * subir el archivo... en formato imagen o pdf" — POST /warranties pasó de
 * JSON a multipart (mismo shape que DocumentController#upload), así que
 * cada creación aquí ahora adjunta un `MockMultipartFile`. Sin
 * `openApi().isValid(VALIDATOR)` en las llamadas de creación —
 * swagger-request-validator no valida bien multipart/binary, mismo
 * precedente que VisionBoardImageControllerIntegrationTest ya documenta;
 * sí se mantiene en las llamadas GET/PATCH cuyo contrato JSON no cambió.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WarrantyControllerIntegrationTest {

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

    private MockMultipartFile pdfFile() {
        return new MockMultipartFile("file", "garantia.pdf", "application/pdf", new byte[]{'%', 'P', 'D', 'F', 1, 2, 3});
    }

    @Test
    void createListAndCompleteWarranty_happyPath() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "alice-w@example.com").build());
        String expiresAt = Instant.now().plus(400, ChronoUnit.DAYS).toString();

        String createdJson = mockMvc.perform(multipart("/api/v1/warranties")
                        .file(pdfFile())
                        .param("item", "Laptop Dell XPS 13")
                        .param("expiresAt", expiresAt)
                        .with(principal))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.item", is("Laptop Dell XPS 13")))
                .andExpect(jsonPath("$.status", is("VIGENTE")))
                .andExpect(jsonPath("$.version", is(0)))
                .andExpect(jsonPath("$.documentContentType", is("application/pdf")))
                .andReturn().getResponse().getContentAsString();

        String warrantyId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/warranties").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(warrantyId)))
                .andExpect(jsonPath("$.totalElements", is(1)));

        mockMvc.perform(post("/api/v1/warranties/" + warrantyId + "/complete").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETADO")))
                .andExpect(jsonPath("$.version", is(1)));
    }

    @Test
    void createWarranty_documentRoundTrips() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "doc-w@example.com").build());
        byte[] pdfBytes = {'%', 'P', 'D', 'F', '-', 9, 8, 7};
        MockMultipartFile file = new MockMultipartFile("file", "garantia.pdf", "application/pdf", pdfBytes);

        String createdJson = mockMvc.perform(multipart("/api/v1/warranties")
                        .file(file)
                        .param("item", "Refrigerador")
                        .param("expiresAt", Instant.now().plus(200, ChronoUnit.DAYS).toString())
                        .with(principal))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String warrantyId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/warranties/" + warrantyId + "/content").with(principal))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertArrayEquals(pdfBytes, result.getResponse().getContentAsByteArray()));
    }

    @Test
    void createWarranty_missingDocumentIsRejected() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "nodoc-w@example.com").build());
        // MockMvc's multipart(...) requires at least one part; simulate "no
        // real file chosen" the same way a browser would (empty content).
        MockMultipartFile emptyFile = new MockMultipartFile("file", "", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/api/v1/warranties")
                        .file(emptyFile)
                        .param("item", "Sin archivo")
                        .param("expiresAt", Instant.now().plus(30, ChronoUnit.DAYS).toString())
                        .with(principal))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void createWarranty_computesVencidaForPastExpiresAt() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "bob-w@example.com").build());
        String expiresAt = Instant.now().minus(10, ChronoUnit.DAYS).toString();

        mockMvc.perform(multipart("/api/v1/warranties")
                        .file(pdfFile())
                        .param("item", "Lavadora Samsung")
                        .param("expiresAt", expiresAt)
                        .with(principal))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("VENCIDA")));
    }

    @Test
    void createWarranty_computesPorVencerWithinThreshold() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "carol-w@example.com").build());
        String expiresAt = Instant.now().plus(5, ChronoUnit.DAYS).toString();

        mockMvc.perform(multipart("/api/v1/warranties")
                        .file(pdfFile())
                        .param("item", "Seguro de auto")
                        .param("expiresAt", expiresAt)
                        .with(principal))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("POR_VENCER")));
    }

    @Test
    void createWarranty_blankItemIsRejected() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "dave-w@example.com").build());

        mockMvc.perform(multipart("/api/v1/warranties")
                        .file(pdfFile())
                        .param("item", "")
                        .param("expiresAt", Instant.now().toString())
                        .with(principal))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWarranty_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "owner-w@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "stranger-w@example.com").build());

        String createdJson = mockMvc.perform(multipart("/api/v1/warranties")
                        .file(pdfFile())
                        .param("item", "Private warranty")
                        .param("expiresAt", Instant.now().plus(100, ChronoUnit.DAYS).toString())
                        .with(owner))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String warrantyId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/warranties/" + warrantyId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("WARRANTY_NOT_FOUND")));
        mockMvc.perform(get("/api/v1/warranties/" + warrantyId + "/content").with(stranger))
                .andExpect(status().isNotFound());
    }

    @Test
    void completeWarranty_mismatchedVersionReturns409() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "erin-w@example.com").build());
        String createdJson = mockMvc.perform(multipart("/api/v1/warranties")
                        .file(pdfFile())
                        .param("item", "Refrigerador")
                        .param("expiresAt", Instant.now().plus(100, ChronoUnit.DAYS).toString())
                        .with(principal))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String warrantyId = objectMapper.readTree(createdJson).get("id").asText();

        String staleVersionBody = objectMapper.writeValueAsString(Map.of("version", 99));
        mockMvc.perform(post("/api/v1/warranties/" + warrantyId + "/complete")
                        .with(principal)
                        .contentType("application/json")
                        .content(staleVersionBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("WARRANTY_VERSION_CONFLICT")));
    }

    @Test
    void warrantyEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/warranties"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void updateWarranty_matchingVersionAppliesPartialEdit() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "frank-w@example.com").build());
        String createdJson = mockMvc.perform(multipart("/api/v1/warranties")
                        .file(pdfFile())
                        .param("item", "Laptop Dell XPS 13")
                        .param("expiresAt", Instant.now().plus(200, ChronoUnit.DAYS).toString())
                        .with(principal))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String warrantyId = objectMapper.readTree(createdJson).get("id").asText();

        String updateBody = objectMapper.writeValueAsString(Map.of("item", "Laptop Dell XPS 13 (renovada)", "version", 0));
        mockMvc.perform(patch("/api/v1/warranties/" + warrantyId)
                        .with(principal)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item", is("Laptop Dell XPS 13 (renovada)")))
                .andExpect(jsonPath("$.version", is(1)));
    }

    @Test
    void updateWarranty_mismatchedVersionReturns409() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "grace-w@example.com").build());
        String createdJson = mockMvc.perform(multipart("/api/v1/warranties")
                        .file(pdfFile())
                        .param("item", "Sofá")
                        .param("expiresAt", Instant.now().plus(200, ChronoUnit.DAYS).toString())
                        .with(principal))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String warrantyId = objectMapper.readTree(createdJson).get("id").asText();

        String staleUpdateBody = objectMapper.writeValueAsString(Map.of("item", "Sofá reparado", "version", 99));
        mockMvc.perform(patch("/api/v1/warranties/" + warrantyId)
                        .with(principal)
                        .contentType("application/json")
                        .content(staleUpdateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("WARRANTY_VERSION_CONFLICT")));
    }

    @Test
    void updateWarranty_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "ivan-w@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "judy-w@example.com").build());

        String createdJson = mockMvc.perform(multipart("/api/v1/warranties")
                        .file(pdfFile())
                        .param("item", "Private warranty")
                        .param("expiresAt", Instant.now().plus(100, ChronoUnit.DAYS).toString())
                        .with(owner))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String warrantyId = objectMapper.readTree(createdJson).get("id").asText();

        String updateBody = objectMapper.writeValueAsString(Map.of("item", "Hijacked item", "version", 0));
        mockMvc.perform(patch("/api/v1/warranties/" + warrantyId)
                        .with(stranger)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("WARRANTY_NOT_FOUND")));
    }

    @Test
    void deleteWarranty_ownerDeletesThenGetReturnsNotFound() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "kevin-w@example.com").build());
        String createdJson = mockMvc.perform(multipart("/api/v1/warranties")
                        .file(pdfFile())
                        .param("item", "Throwaway warranty")
                        .param("expiresAt", Instant.now().plus(100, ChronoUnit.DAYS).toString())
                        .with(principal))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String warrantyId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/warranties/" + warrantyId).with(principal))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());

        mockMvc.perform(get("/api/v1/warranties/" + warrantyId).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("WARRANTY_NOT_FOUND")));
    }

    @Test
    void deleteWarranty_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "laura-w@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "mallory-w@example.com").build());

        String createdJson = mockMvc.perform(multipart("/api/v1/warranties")
                        .file(pdfFile())
                        .param("item", "Private warranty")
                        .param("expiresAt", Instant.now().plus(100, ChronoUnit.DAYS).toString())
                        .with(owner))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String warrantyId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/warranties/" + warrantyId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("WARRANTY_NOT_FOUND")));

        mockMvc.perform(get("/api/v1/warranties/" + warrantyId).with(owner))
                .andExpect(status().isOk());
    }

    @Test
    void deleteWarranty_missingIdReturnsNotFound() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "nathan-w@example.com").build());

        mockMvc.perform(delete("/api/v1/warranties/" + UUID.randomUUID()).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("WARRANTY_NOT_FOUND")))
                .andExpect(jsonPath("$.traceId").exists());
    }
}
