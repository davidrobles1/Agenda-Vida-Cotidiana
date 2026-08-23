package com.vidacotidiana.document.api;

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
 * Módulo Documentos (pedido explícito del usuario, 2026-08-22) — mismo
 * patrón que VisionBoardImageControllerIntegrationTest (multipart, real
 * Postgres via Testcontainers) + WarrantyControllerIntegrationTest (CRUD/
 * version-conflict/404-never-403). Cobertura enfocada en los caminos
 * críticos de negocio/seguridad (visibilidad real de compartir/público,
 * autorización dueño-only para editar/borrar) — no repite cada variante de
 * validación que la suite de warranties ya cubre, dado el alcance de los 5
 * módulos de esta misma tarea.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerIntegrationTest {

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

    /** Real user rows matter here (shareWithEmail resolves against the
        `users` table) — a plain JWT principal is enough for auth, but the
        USER-sync filter (same one every other authenticated endpoint
        already relies on) is what actually inserts the row on first
        request, so a create as this principal always precedes any test
        that shares TO this same email. */
    private void ensureUserExists(UUID userId, String email) throws Exception {
        mockMvc.perform(get("/api/v1/warranties").with(jwt().jwt(jwtFor(userId, email).build())));
    }

    @Test
    void uploadThenGetContent_roundTripsBytesAndMetadata() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "doc-owner@example.com").build());
        byte[] pdfBytes = {'%', 'P', 'D', 'F', '-', 1, 2, 3};
        MockMultipartFile file = new MockMultipartFile("file", "id.pdf", "application/pdf", pdfBytes);

        String json = mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("name", "INE")
                        .param("category", "IDENTIFICACION")
                        .with(principal))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("INE")))
                .andExpect(jsonPath("$.category", is("IDENTIFICACION")))
                .andExpect(jsonPath("$.visibility", is("PRIVATE")))
                .andExpect(jsonPath("$.sizeBytes", is(pdfBytes.length)))
                .andReturn().getResponse().getContentAsString();
        String documentId = objectMapper.readTree(json).get("id").asText();

        mockMvc.perform(get("/api/v1/documents/" + documentId + "/content").with(principal))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertArrayEquals(pdfBytes, result.getResponse().getContentAsByteArray()));
    }

    @Test
    void upload_rejectsUnsupportedContentType() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "doc-reject@example.com").build());
        MockMultipartFile file = new MockMultipartFile("file", "script.svg", "image/svg+xml", "<svg></svg>".getBytes());

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("name", "malicious")
                        .param("category", "OTROS")
                        .with(principal))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void privateDocument_isInvisibleToAnotherUser() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "priv-owner@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "priv-stranger@example.com").build());

        MockMultipartFile file = new MockMultipartFile("file", "poliza.pdf", "application/pdf", new byte[]{1, 2, 3});
        String json = mockMvc.perform(multipart("/api/v1/documents")
                        .file(file).param("name", "Póliza").param("category", "SEGUROS").with(owner))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String documentId = objectMapper.readTree(json).get("id").asText();

        mockMvc.perform(get("/api/v1/documents/" + documentId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DOCUMENT_NOT_FOUND")));

        mockMvc.perform(get("/api/v1/documents").with(stranger))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    void sharingByEmail_makesDocumentImmediatelyVisibleToTheResolvedRecipient_neverToAnyoneElse() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        String recipientEmail = "share-recipient@example.com";
        var owner = jwt().jwt(jwtFor(ownerId, "share-owner@example.com").build());
        var recipient = jwt().jwt(jwtFor(recipientId, recipientEmail).build());
        var stranger = jwt().jwt(jwtFor(strangerId, "share-stranger@example.com").build());

        // The recipient's account row must exist before the share resolves it.
        ensureUserExists(recipientId, recipientEmail);

        MockMultipartFile file = new MockMultipartFile("file", "contrato.pdf", "application/pdf", new byte[]{1, 2, 3});
        String json = mockMvc.perform(multipart("/api/v1/documents")
                        .file(file).param("name", "Contrato renta").param("category", "CONTRATOS").with(owner))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String documentId = objectMapper.readTree(json).get("id").asText();

        String shareBody = objectMapper.writeValueAsString(Map.of("email", recipientEmail, "version", 0));
        mockMvc.perform(post("/api/v1/documents/" + documentId + "/share")
                        .with(owner).contentType("application/json").content(shareBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility", is("SHARED")))
                .andExpect(jsonPath("$.sharedWithEmail", is(recipientEmail)));

        // Immediately visible — no invitation/acceptance step, no email round-trip.
        mockMvc.perform(get("/api/v1/documents/" + documentId).with(recipient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(documentId)));
        mockMvc.perform(get("/api/v1/documents").with(recipient))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)));

        // Anyone else still gets a 404 — SHARED is scoped to the one resolved recipient.
        mockMvc.perform(get("/api/v1/documents/" + documentId).with(stranger))
                .andExpect(status().isNotFound());
    }

    @Test
    void sharingToAnUnregisteredEmail_succeedsWithoutRevealingWhetherItMatched() throws Exception {
        var owner = jwt().jwt(jwtFor(UUID.randomUUID(), "unreg-owner@example.com").build());
        MockMultipartFile file = new MockMultipartFile("file", "acta.pdf", "application/pdf", new byte[]{1, 2, 3});
        String json = mockMvc.perform(multipart("/api/v1/documents")
                        .file(file).param("name", "Acta").param("category", "OTROS").with(owner))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String documentId = objectMapper.readTree(json).get("id").asText();

        String shareBody = objectMapper.writeValueAsString(Map.of("email", "nobody-registered@example.com", "version", 0));
        // SEC-001 non-enumeration: the share call itself always succeeds,
        // whether or not the email matched a real account — see
        // DocumentService#shareWithEmail's own doc comment.
        mockMvc.perform(post("/api/v1/documents/" + documentId + "/share")
                        .with(owner).contentType("application/json").content(shareBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility", is("SHARED")));
    }

    @Test
    void makePublic_isVisibleToAnyOtherUser_makePrivateRevokesIt() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "pub-owner@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "pub-stranger@example.com").build());

        MockMultipartFile file = new MockMultipartFile("file", "manual.pdf", "application/pdf", new byte[]{1, 2, 3});
        String json = mockMvc.perform(multipart("/api/v1/documents")
                        .file(file).param("name", "Manual").param("category", "OTROS").with(owner))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String documentId = objectMapper.readTree(json).get("id").asText();

        mockMvc.perform(post("/api/v1/documents/" + documentId + "/make-public")
                        .with(owner).contentType("application/json").content("{\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility", is("FAMILY_PUBLIC")));

        mockMvc.perform(get("/api/v1/documents/" + documentId).with(stranger))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/documents/" + documentId + "/make-private")
                        .with(owner).contentType("application/json").content("{\"version\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility", is("PRIVATE")));

        mockMvc.perform(get("/api/v1/documents/" + documentId).with(stranger))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDocument_mismatchedVersionReturns409() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "ver-owner@example.com").build());
        MockMultipartFile file = new MockMultipartFile("file", "comprobante.pdf", "application/pdf", new byte[]{1, 2, 3});
        String json = mockMvc.perform(multipart("/api/v1/documents")
                        .file(file).param("name", "Comprobante").param("category", "COMPROBANTES").with(principal))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String documentId = objectMapper.readTree(json).get("id").asText();

        String staleBody = objectMapper.writeValueAsString(Map.of("name", "Comprobante actualizado", "version", 99));
        mockMvc.perform(patch("/api/v1/documents/" + documentId)
                        .with(principal).contentType("application/json").content(staleBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DOCUMENT_VERSION_CONFLICT")));
    }

    @Test
    void deleteDocument_ownerDeletesThenGetReturnsNotFound_strangerCannotDelete() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "del-owner@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "del-stranger@example.com").build());

        MockMultipartFile file = new MockMultipartFile("file", "throwaway.pdf", "application/pdf", new byte[]{1, 2, 3});
        String json = mockMvc.perform(multipart("/api/v1/documents")
                        .file(file).param("name", "Throwaway").param("category", "OTROS").with(owner))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String documentId = objectMapper.readTree(json).get("id").asText();

        mockMvc.perform(delete("/api/v1/documents/" + documentId).with(stranger))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/documents/" + documentId).with(owner))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/documents/" + documentId).with(owner))
                .andExpect(status().isNotFound());
    }

    @Test
    void documentEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    void uploadDocument_withPersonAndProjectLinks_persistsBoth() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "doc-quinn@example.com").build());

        String personJson = mockMvc.perform(post("/api/v1/people")
                        .with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "Carlos Martínez"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String personId = objectMapper.readTree(personJson).get("id").asText();

        String projectJson = mockMvc.perform(post("/api/v1/projects")
                        .with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "Implementación ERP"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String projectId = objectMapper.readTree(projectJson).get("id").asText();

        byte[] pdfBytes = {'%', 'P', 'D', 'F', '-', 1, 2, 3};
        MockMultipartFile file = new MockMultipartFile("file", "contrato.pdf", "application/pdf", pdfBytes);

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("name", "Contrato ERP")
                        .param("category", "CONTRATOS")
                        .param("personId", personId)
                        .param("projectId", projectId)
                        .with(principal))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.personId", is(personId)))
                .andExpect(jsonPath("$.projectId", is(projectId)));
    }

    @Test
    void uploadDocument_personIdOwnedByAnotherUserReturnsNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "doc-owner2@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "doc-stranger@example.com").build());

        String personJson = mockMvc.perform(post("/api/v1/people")
                        .with(stranger).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "Ajeno"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String strangersPersonId = objectMapper.readTree(personJson).get("id").asText();

        MockMultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[]{'%', 'P', 'D', 'F'});

        mockMvc.perform(multipart("/api/v1/documents")
                        .file(file)
                        .param("name", "x")
                        .param("category", "OTROS")
                        .param("personId", strangersPersonId)
                        .with(owner))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("PERSON_NOT_FOUND")));
    }
}
