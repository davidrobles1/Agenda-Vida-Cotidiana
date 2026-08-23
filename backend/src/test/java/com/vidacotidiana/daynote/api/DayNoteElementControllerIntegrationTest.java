package com.vidacotidiana.daynote.api;

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

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Canvas de notas por día (pedido explícito del usuario, 2026-08-22).
 * Mismo patrón de test que reminder.api.ReminderControllerIntegrationTest
 * (Testcontainers PostgreSQL real, JWT mockeado). Además de CRUD/owner-only
 * /409-por-versión ya cubiertos en todos los demás módulos, esta suite
 * cubre la regla NUEVA de este módulo: "las formas no podrán superponerse."
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DayNoteElementControllerIntegrationTest {

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
                .issuedAt(java.time.Instant.now())
                .expiresAt(java.time.Instant.now().plusSeconds(300));
    }

    private Map<String, Object> createRequest(String date, String type, double x, double y, double w, double h) {
        return Map.of(
                "noteDate", date, "type", type,
                "x", x, "y", y, "width", w, "height", h,
                "data", Map.of("text", "hola", "bold", false, "italic", false));
    }

    @Test
    void createListAndGetElement_happyPath() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "alice@example.com").build());

        String createBody = objectMapper.writeValueAsString(createRequest("2026-08-22", "BANNER", 10, 10, 100, 40));

        String createdJson = mockMvc.perform(post("/api/v1/day-notes")
                        .with(principal).contentType("application/json").content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("BANNER")))
                .andExpect(jsonPath("$.noteDate", is("2026-08-22")))
                .andExpect(jsonPath("$.zIndex", is(0)))
                .andExpect(jsonPath("$.version", is(0)))
                .andReturn().getResponse().getContentAsString();
        String elementId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/day-notes").param("date", "2026-08-22").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(elementId)));

        mockMvc.perform(get("/api/v1/day-notes/" + elementId).with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.text", is("hola")));
    }

    @Test
    void listForDay_onlyReturnsElementsOfThatDay() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "bob@example.com").build());

        mockMvc.perform(post("/api/v1/day-notes").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("2026-08-22", "BANNER", 0, 0, 50, 50))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/day-notes").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("2026-08-23", "TEXT", 0, 0, 50, 50))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/day-notes").param("date", "2026-08-22").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].type", is("BANNER")));
    }

    @Test
    void create_overlappingElementIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "carol@example.com").build());

        mockMvc.perform(post("/api/v1/day-notes").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("2026-08-22", "BANNER", 0, 0, 100, 100))))
                .andExpect(status().isCreated());

        // Se solapa con el elemento anterior (0,0)-(100,100).
        mockMvc.perform(post("/api/v1/day-notes").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("2026-08-22", "TEXT", 50, 50, 100, 100))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void create_adjacentNonOverlappingElementIsAccepted() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "dave@example.com").build());

        mockMvc.perform(post("/api/v1/day-notes").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("2026-08-22", "BANNER", 0, 0, 100, 100))))
                .andExpect(status().isCreated());

        // Toca el borde exacto (x=100) pero no se solapa.
        mockMvc.perform(post("/api/v1/day-notes").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("2026-08-22", "TEXT", 100, 0, 100, 100))))
                .andExpect(status().isCreated());
    }

    @Test
    void move_intoOverlapIsRejected_andOriginalPositionIsUnchanged() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "erin@example.com").build());

        mockMvc.perform(post("/api/v1/day-notes").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("2026-08-22", "BANNER", 0, 0, 100, 100))))
                .andExpect(status().isCreated());

        String secondJson = mockMvc.perform(post("/api/v1/day-notes").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("2026-08-22", "TEXT", 200, 0, 100, 100))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String secondId = objectMapper.readTree(secondJson).get("id").asText();

        String moveBody = objectMapper.writeValueAsString(
                Map.of("x", 50, "y", 50, "width", 100, "height", 100, "version", 0));

        mockMvc.perform(put("/api/v1/day-notes/" + secondId + "/position")
                        .with(principal).contentType("application/json").content(moveBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));

        mockMvc.perform(get("/api/v1/day-notes/" + secondId).with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.x", is(200.0)))
                .andExpect(jsonPath("$.version", is(0)));
    }

    @Test
    void editData_replacesContentAndBumpsVersion() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "frank@example.com").build());

        String createdJson = mockMvc.perform(post("/api/v1/day-notes").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("2026-08-22", "TEXT", 0, 0, 100, 40))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String elementId = objectMapper.readTree(createdJson).get("id").asText();

        String editBody = objectMapper.writeValueAsString(
                Map.of("data", Map.of("text", "editado", "bold", true, "italic", true), "version", 0));

        mockMvc.perform(put("/api/v1/day-notes/" + elementId + "/data")
                        .with(principal).contentType("application/json").content(editBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.text", is("editado")))
                .andExpect(jsonPath("$.data.bold", is(true)))
                .andExpect(jsonPath("$.version", is(1)));
    }

    @Test
    void bringToFront_raisesAboveAllSiblings() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "grace@example.com").build());

        String firstJson = mockMvc.perform(post("/api/v1/day-notes").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("2026-08-22", "BANNER", 0, 0, 100, 100))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String firstId = objectMapper.readTree(firstJson).get("id").asText();

        mockMvc.perform(post("/api/v1/day-notes").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("2026-08-22", "TEXT", 200, 0, 100, 100))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/day-notes/" + firstId + "/bring-to-front")
                        .with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("version", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zIndex", is(2)));
    }

    @Test
    void delete_ownerDeletesThenGetReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "heidi@example.com").build());

        String createdJson = mockMvc.perform(post("/api/v1/day-notes").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("2026-08-22", "BANNER", 0, 0, 100, 100))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String elementId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/day-notes/" + elementId).with(principal))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/day-notes/" + elementId).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DAY_NOTE_ELEMENT_NOT_FOUND")));
    }

    @Test
    void getElement_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "ivan@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "judy@example.com").build());

        String createdJson = mockMvc.perform(post("/api/v1/day-notes").with(owner).contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("2026-08-22", "BANNER", 0, 0, 100, 100))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String elementId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/day-notes/" + elementId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DAY_NOTE_ELEMENT_NOT_FOUND")));
    }

    @Test
    void move_mismatchedVersionReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "kevin@example.com").build());

        String createdJson = mockMvc.perform(post("/api/v1/day-notes").with(principal).contentType("application/json")
                        .content(objectMapper.writeValueAsString(createRequest("2026-08-22", "BANNER", 0, 0, 100, 100))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String elementId = objectMapper.readTree(createdJson).get("id").asText();

        String staleMoveBody = objectMapper.writeValueAsString(
                Map.of("x", 10, "y", 10, "width", 100, "height", 100, "version", 99));

        mockMvc.perform(put("/api/v1/day-notes/" + elementId + "/position")
                        .with(principal).contentType("application/json").content(staleMoveBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DAY_NOTE_ELEMENT_VERSION_CONFLICT")));
    }

    @Test
    void dayNoteEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/day-notes").param("date", "2026-08-22"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }
}
