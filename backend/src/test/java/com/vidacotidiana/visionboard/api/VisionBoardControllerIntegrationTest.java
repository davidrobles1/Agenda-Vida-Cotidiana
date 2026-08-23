package com.vidacotidiana.visionboard.api;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FASE 2: same pattern as note.api.NoteControllerIntegrationTest — real
 * PostgreSQL via Testcontainers, mocked JWT principal, full stack (Spring
 * Security filter chain, USER sync, Flyway, JPA). Owner-only authorization:
 * no collaborator tests (VisionBoardRepository has no sharing concept, same
 * as Note).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VisionBoardControllerIntegrationTest {

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

    private String createBoard(org.springframework.test.web.servlet.request.RequestPostProcessor principal,
                                String name) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("name", name, "width", 1200, "height", 800));
        String json = mockMvc.perform(post("/api/v1/vision-boards")
                        .with(principal)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    @Test
    void createAndListVisionBoard_happyPath() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "alice-vb@example.com").build());

        String createBody = objectMapper.writeValueAsString(
                Map.of("name", "Metas 2026", "description", "Tablero de metas", "width", 1200, "height", 800,
                        "theme", "PAPER"));

        String createdJson = mockMvc.perform(post("/api/v1/vision-boards")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Metas 2026")))
                .andExpect(jsonPath("$.width", is(1200)))
                .andExpect(jsonPath("$.height", is(800)))
                .andExpect(jsonPath("$.theme", is("PAPER")))
                .andExpect(jsonPath("$.version", is(0)))
                .andExpect(jsonPath("$.elements").doesNotExist())
                // TEST-API-001: representative contract check for POST /vision-boards against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR))
                .andReturn().getResponse().getContentAsString();

        String boardId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/vision-boards").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(boardId)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.items[0].elements").doesNotExist())
                // TEST-API-001: representative contract check for GET /vision-boards against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void createVisionBoard_invalidWidthIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "bob-vb@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of("name", "Board", "width", 0, "height", 800));

        mockMvc.perform(post("/api/v1/vision-boards")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void getVisionBoard_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "owner-vb@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "stranger-vb@example.com").build());

        String boardId = createBoard(owner, "Private board");

        mockMvc.perform(get("/api/v1/vision-boards/" + boardId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("VISION_BOARD_NOT_FOUND")))
                // TEST-API-001: representative contract check for GET /vision-boards/{id} 404 against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void visionBoardEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/vision-boards"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    void updateVisionBoard_matchingVersionAppliesPartialEdit() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "frank-vb@example.com").build());
        String boardId = createBoard(principal, "Original");

        String updateBody = objectMapper.writeValueAsString(Map.of("name", "Editado", "version", 0));

        mockMvc.perform(put("/api/v1/vision-boards/" + boardId)
                        .with(principal)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Editado")))
                .andExpect(jsonPath("$.width", is(1200)))
                .andExpect(jsonPath("$.version", is(1)))
                // TEST-API-001: representative contract check for PUT /vision-boards/{id} 200 against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void updateVisionBoard_mismatchedVersionReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "grace-vb@example.com").build());
        String boardId = createBoard(principal, "Board");

        String staleUpdateBody = objectMapper.writeValueAsString(Map.of("name", "Stale edit", "version", 99));

        mockMvc.perform(put("/api/v1/vision-boards/" + boardId)
                        .with(principal)
                        .contentType("application/json")
                        .content(staleUpdateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("VISION_BOARD_VERSION_CONFLICT")));
    }

    @Test
    void updateVisionBoard_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "ivan-vb@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "judy-vb@example.com").build());

        String boardId = createBoard(owner, "Private board");
        String updateBody = objectMapper.writeValueAsString(Map.of("name", "Hijacked", "version", 0));

        mockMvc.perform(put("/api/v1/vision-boards/" + boardId)
                        .with(stranger)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("VISION_BOARD_NOT_FOUND")));
    }

    // FASE 16 (Board Themes): the previously-unused `background` column was
    // renamed to `theme` (V9 migration) and given a NOT NULL DEFAULT —
    // these cover the contract that change relies on: a board always has a
    // concrete theme, defaulting to LIGHT when the caller never specifies
    // one, and persisting through the same PUT every other board field uses.
    @Test
    void createVisionBoard_omittedThemeDefaultsToLight() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "leo-vb@example.com").build());

        mockMvc.perform(post("/api/v1/vision-boards")
                        .with(principal)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("name", "Sin tema", "width", 1200, "height", 800))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.theme", is("LIGHT")));
    }

    @Test
    void createVisionBoard_invalidThemeIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "mona-vb@example.com").build());

        String createBody = objectMapper.writeValueAsString(
                Map.of("name", "Board", "width", 1200, "height", 800, "theme", "NEON"));

        mockMvc.perform(post("/api/v1/vision-boards")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void updateVisionBoard_themeChangePersistsAcrossReload() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "nina-vb@example.com").build());
        String boardId = createBoard(principal, "Board");

        String updateBody = objectMapper.writeValueAsString(Map.of("theme", "DARK", "version", 0));

        mockMvc.perform(put("/api/v1/vision-boards/" + boardId)
                        .with(principal)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme", is("DARK")))
                .andExpect(jsonPath("$.version", is(1)));

        // "cerrarse y volver a abrirlo" — a fresh GET (a real reload) must
        // still show the persisted theme, not just the PUT's own response.
        mockMvc.perform(get("/api/v1/vision-boards/" + boardId).with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme", is("DARK")));
    }

    @Test
    void deleteVisionBoard_ownerDeletesThenGetReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "kevin-vb@example.com").build());
        String boardId = createBoard(principal, "Throwaway board");

        mockMvc.perform(delete("/api/v1/vision-boards/" + boardId).with(principal))
                .andExpect(status().isNoContent())
                .andExpect(jsonPath("$").doesNotExist());

        mockMvc.perform(get("/api/v1/vision-boards/" + boardId).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("VISION_BOARD_NOT_FOUND")));
    }

    @Test
    void addElement_happyPathAndGetBoardIncludesIt() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "laura-vb@example.com").build());
        String boardId = createBoard(principal, "With elements");

        String createElementBody = objectMapper.writeValueAsString(
                Map.of("type", "TEXT", "x", 10.0, "y", 20.0, "width", 100.0, "height", 40.0,
                        "data", Map.of("text", "Hola")));

        mockMvc.perform(post("/api/v1/vision-boards/" + boardId + "/elements")
                        .with(principal)
                        .contentType("application/json")
                        .content(createElementBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("TEXT")))
                .andExpect(jsonPath("$.x", is(10.0)))
                .andExpect(jsonPath("$.zIndex", is(0)))
                .andExpect(jsonPath("$.locked", is(false)))
                .andExpect(jsonPath("$.visible", is(true)))
                .andExpect(jsonPath("$.version", is(0)))
                // TEST-API-001: representative contract check for POST .../elements against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));

        mockMvc.perform(get("/api/v1/vision-boards/" + boardId).with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elements.length()", is(1)))
                .andExpect(jsonPath("$.elements[0].type", is("TEXT")))
                // TEST-API-001: representative contract check for GET /vision-boards/{id} (with elements) against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void addElement_invalidTypeIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "mallory-vb@example.com").build());
        String boardId = createBoard(principal, "Board");

        String createElementBody = objectMapper.writeValueAsString(
                Map.of("type", "NOT_A_TYPE", "x", 0.0, "y", 0.0, "width", 10.0, "height", 10.0));

        mockMvc.perform(post("/api/v1/vision-boards/" + boardId + "/elements")
                        .with(principal)
                        .contentType("application/json")
                        .content(createElementBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void addElement_toAnotherUsersBoardReturnsNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "nathan-vb@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "olivia-vb@example.com").build());
        String boardId = createBoard(owner, "Private board");

        String createElementBody = objectMapper.writeValueAsString(
                Map.of("type", "SHAPE", "x", 0.0, "y", 0.0, "width", 10.0, "height", 10.0));

        mockMvc.perform(post("/api/v1/vision-boards/" + boardId + "/elements")
                        .with(stranger)
                        .contentType("application/json")
                        .content(createElementBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("VISION_BOARD_NOT_FOUND")));
    }

    @Test
    void updateElement_matchingVersionAppliesPartialEdit() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "peter-vb@example.com").build());
        String boardId = createBoard(principal, "Board");

        String createElementBody = objectMapper.writeValueAsString(
                Map.of("type", "SHAPE", "x", 0.0, "y", 0.0, "width", 10.0, "height", 10.0));
        String elementJson = mockMvc.perform(post("/api/v1/vision-boards/" + boardId + "/elements")
                        .with(principal)
                        .contentType("application/json")
                        .content(createElementBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String elementId = objectMapper.readTree(elementJson).get("id").asText();

        // FASE 4/5: a drag/resize touches only x/y/width/height, matching what an
        // actual move/resize interaction sends.
        String updateBody = objectMapper.writeValueAsString(Map.of("x", 50.0, "y", 60.0, "version", 0));

        mockMvc.perform(put("/api/v1/vision-boards/" + boardId + "/elements/" + elementId)
                        .with(principal)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.x", is(50.0)))
                .andExpect(jsonPath("$.y", is(60.0)))
                .andExpect(jsonPath("$.width", is(10.0)))
                .andExpect(jsonPath("$.version", is(1)))
                // TEST-API-001: representative contract check for PUT .../elements/{elementId} against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void updateElement_mismatchedVersionReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "quinn-vb@example.com").build());
        String boardId = createBoard(principal, "Board");

        String createElementBody = objectMapper.writeValueAsString(
                Map.of("type", "STICKER", "x", 0.0, "y", 0.0, "width", 10.0, "height", 10.0));
        String elementJson = mockMvc.perform(post("/api/v1/vision-boards/" + boardId + "/elements")
                        .with(principal)
                        .contentType("application/json")
                        .content(createElementBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String elementId = objectMapper.readTree(elementJson).get("id").asText();

        String staleUpdateBody = objectMapper.writeValueAsString(Map.of("x", 1.0, "version", 99));

        mockMvc.perform(put("/api/v1/vision-boards/" + boardId + "/elements/" + elementId)
                        .with(principal)
                        .contentType("application/json")
                        .content(staleUpdateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("VISION_BOARD_ELEMENT_VERSION_CONFLICT")));
    }

    // FASE 22 audit gap: `locked` has been part of the UpdateElementRequest
    // contract since FASE 4/8, and the entire FASE 17 lock/unlock UI is
    // built on top of it (PUT is the only path that ever sets it — see
    // VisionBoardCanvas.tsx's handleToggleLock), but no backend test ever
    // exercised it persisting across a reload.
    @Test
    void updateElement_lockedFieldPersistsAcrossReload() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "opal-vb@example.com").build());
        String boardId = createBoard(principal, "Board");
        String elementId = addShapeElement(principal, boardId);

        String lockBody = objectMapper.writeValueAsString(Map.of("locked", true, "version", 0));

        mockMvc.perform(put("/api/v1/vision-boards/" + boardId + "/elements/" + elementId)
                        .with(principal)
                        .contentType("application/json")
                        .content(lockBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked", is(true)))
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(openApi().isValid(VALIDATOR));

        // "cerrarlo y volver a abrirlo" — a fresh GET must still show it locked.
        mockMvc.perform(get("/api/v1/vision-boards/" + boardId).with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elements[0].locked", is(true)));
    }

    @Test
    void deleteVisionBoard_nonexistentReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "pablo-vb@example.com").build());

        mockMvc.perform(delete("/api/v1/vision-boards/" + UUID.randomUUID()).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("VISION_BOARD_NOT_FOUND")));
    }

    @Test
    void deleteElement_nonexistentReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "quiana-vb@example.com").build());
        String boardId = createBoard(principal, "Board");

        mockMvc.perform(delete("/api/v1/vision-boards/" + boardId + "/elements/" + UUID.randomUUID()).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("VISION_BOARD_ELEMENT_NOT_FOUND")));
    }

    @Test
    void deleteElement_ownerDeletesThenBoardNoLongerIncludesIt() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "ruth-vb@example.com").build());
        String boardId = createBoard(principal, "Board");

        String createElementBody = objectMapper.writeValueAsString(
                Map.of("type", "IMAGE", "x", 0.0, "y", 0.0, "width", 10.0, "height", 10.0));
        String elementJson = mockMvc.perform(post("/api/v1/vision-boards/" + boardId + "/elements")
                        .with(principal)
                        .contentType("application/json")
                        .content(createElementBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String elementId = objectMapper.readTree(elementJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/vision-boards/" + boardId + "/elements/" + elementId).with(principal))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/vision-boards/" + boardId).with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elements.length()", is(0)));
    }

    private String addShapeElement(org.springframework.test.web.servlet.request.RequestPostProcessor principal,
                                    String boardId) throws Exception {
        String createElementBody = objectMapper.writeValueAsString(
                Map.of("type", "SHAPE", "x", 0.0, "y", 0.0, "width", 10.0, "height", 10.0));
        String elementJson = mockMvc.perform(post("/api/v1/vision-boards/" + boardId + "/elements")
                        .with(principal)
                        .contentType("application/json")
                        .content(createElementBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(elementJson).get("id").asText();
    }

    // FASE 9 fix (layers): RAISE/LOWER/FRONT/BACK must swap paint-order
    // position relative to neighbors, not just increment/decrement zIndex —
    // every element created here starts at zIndex 0 (VisionBoardElement's
    // constructor), so a naive zIndex+-1 would be meaningless; only a real
    // renumbering makes "immediately above/below" well-defined.
    @Test
    void reorderElement_raiseSwapsWithImmediatelyAdjacentNeighbor() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "tara-vb@example.com").build());
        String boardId = createBoard(principal, "Board");

        String bottomId = addShapeElement(principal, boardId);
        String middleId = addShapeElement(principal, boardId);
        String topId = addShapeElement(principal, boardId);

        String reorderBody = objectMapper.writeValueAsString(Map.of("direction", "RAISE", "version", 0));

        mockMvc.perform(post("/api/v1/vision-boards/" + boardId + "/elements/" + bottomId + "/reorder")
                        .with(principal)
                        .contentType("application/json")
                        .content(reorderBody))
                .andExpect(status().isOk())
                // TEST-API-001: representative contract check for POST .../reorder against the real openapi.yaml.
                .andExpect(openApi().isValid(VALIDATOR));

        mockMvc.perform(get("/api/v1/vision-boards/" + boardId).with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elements[0].id", is(middleId)))
                .andExpect(jsonPath("$.elements[0].zIndex", is(0)))
                .andExpect(jsonPath("$.elements[1].id", is(bottomId)))
                .andExpect(jsonPath("$.elements[1].zIndex", is(1)))
                .andExpect(jsonPath("$.elements[2].id", is(topId)))
                .andExpect(jsonPath("$.elements[2].zIndex", is(2)));
    }

    @Test
    void reorderElement_frontMovesToTopOfPaintOrder() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "uma-vb@example.com").build());
        String boardId = createBoard(principal, "Board");

        String bottomId = addShapeElement(principal, boardId);
        String middleId = addShapeElement(principal, boardId);
        String topId = addShapeElement(principal, boardId);

        String reorderBody = objectMapper.writeValueAsString(Map.of("direction", "FRONT", "version", 0));

        mockMvc.perform(post("/api/v1/vision-boards/" + boardId + "/elements/" + bottomId + "/reorder")
                        .with(principal)
                        .contentType("application/json")
                        .content(reorderBody))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/vision-boards/" + boardId).with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elements[0].id", is(middleId)))
                .andExpect(jsonPath("$.elements[1].id", is(topId)))
                .andExpect(jsonPath("$.elements[2].id", is(bottomId)))
                .andExpect(jsonPath("$.elements[2].zIndex", is(2)));
    }

    @Test
    void reorderElement_mismatchedVersionReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "victor-vb@example.com").build());
        String boardId = createBoard(principal, "Board");
        String elementId = addShapeElement(principal, boardId);

        String staleReorderBody = objectMapper.writeValueAsString(Map.of("direction", "RAISE", "version", 99));

        mockMvc.perform(post("/api/v1/vision-boards/" + boardId + "/elements/" + elementId + "/reorder")
                        .with(principal)
                        .contentType("application/json")
                        .content(staleReorderBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("VISION_BOARD_ELEMENT_VERSION_CONFLICT")));
    }

    @Test
    void reorderElement_invalidDirectionIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "wendy-vb@example.com").build());
        String boardId = createBoard(principal, "Board");
        String elementId = addShapeElement(principal, boardId);

        String invalidBody = objectMapper.writeValueAsString(Map.of("direction", "SIDEWAYS", "version", 0));

        mockMvc.perform(post("/api/v1/vision-boards/" + boardId + "/elements/" + elementId + "/reorder")
                        .with(principal)
                        .contentType("application/json")
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void reorderElement_onAnotherUsersBoardReturnsNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "xena-vb@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "yusuf-vb@example.com").build());
        String boardId = createBoard(owner, "Private board");
        String elementId = addShapeElement(owner, boardId);

        String reorderBody = objectMapper.writeValueAsString(Map.of("direction", "RAISE", "version", 0));

        mockMvc.perform(post("/api/v1/vision-boards/" + boardId + "/elements/" + elementId + "/reorder")
                        .with(stranger)
                        .contentType("application/json")
                        .content(reorderBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("VISION_BOARD_NOT_FOUND")));
    }

    @Test
    void deleteVisionBoard_cascadesToItsElements() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "sam-vb@example.com").build());
        String boardId = createBoard(principal, "Board with element");

        String createElementBody = objectMapper.writeValueAsString(
                Map.of("type", "NOTE", "x", 0.0, "y", 0.0, "width", 10.0, "height", 10.0));
        mockMvc.perform(post("/api/v1/vision-boards/" + boardId + "/elements")
                        .with(principal)
                        .contentType("application/json")
                        .content(createElementBody))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/v1/vision-boards/" + boardId).with(principal))
                .andExpect(status().isNoContent());

        // V8 migration: board_id has ON DELETE CASCADE — the board's own
        // deletion must not fail or orphan its elements.
        mockMvc.perform(get("/api/v1/vision-boards/" + boardId).with(principal))
                .andExpect(status().isNotFound());
    }
}
