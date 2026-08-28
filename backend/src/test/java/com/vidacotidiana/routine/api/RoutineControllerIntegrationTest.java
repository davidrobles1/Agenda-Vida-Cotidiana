package com.vidacotidiana.routine.api;

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
 * ADR-016 Fase 3e2/FR-032/AC-019. Same pattern as
 * person.api.PersonControllerIntegrationTest.
 *
 * <p>Incluye la regla de avance decidida por el Product Owner el 2026-08-28
 * (opción B): {@code nextExecutionDate} avanza desde la fecha
 * <b>programada</b>, no desde "ahora" — el caso que distingue B de A es
 * {@link #executeRoutine_advancesFromScheduledDateNotFromNow()}.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoutineControllerIntegrationTest {

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

    private static final String NEXT_EXECUTION =
            Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();

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
    void createAndListRoutine_happyPath() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "alice-r@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Revisar correo", "frequency", "DAILY", "nextExecutionDate", NEXT_EXECUTION));

        String createdJson = mockMvc.perform(post("/api/v1/routines")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Revisar correo")))
                .andExpect(jsonPath("$.frequency", is("DAILY")))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.version", is(0)))
                .andExpect(openApi().isValid(VALIDATOR))
                .andReturn().getResponse().getContentAsString();

        String routineId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/routines").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(routineId)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void createRoutine_blankTitleIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "bob-r@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "", "frequency", "WEEKLY", "nextExecutionDate", NEXT_EXECUTION));

        mockMvc.perform(post("/api/v1/routines")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    /** AC-019: frequency es obligatorio y sólo admite DAILY/WEEKLY/MONTHLY. */
    @Test
    void createRoutine_unknownFrequencyIsRejected() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "carol-r@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Rutina inválida", "frequency", "FORTNIGHTLY", "nextExecutionDate", NEXT_EXECUTION));

        mockMvc.perform(post("/api/v1/routines")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRoutine_ownedByAnotherUserReturnsNotFound_neverForbidden() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "owner-r@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "stranger-r@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Rutina privada", "frequency", "WEEKLY", "nextExecutionDate", NEXT_EXECUTION));
        String createdJson = mockMvc.perform(post("/api/v1/routines")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String routineId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(get("/api/v1/routines/" + routineId).with(stranger))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("ROUTINE_NOT_FOUND")))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    /** AC-019: `active=false` es el estado permanente de pausa — no existe `completed`. */
    @Test
    void updateRoutine_canDeactivateAndMoveDateManually() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "dana-r@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Actualizar CRM", "frequency", "WEEKLY", "nextExecutionDate", NEXT_EXECUTION));

        String createdJson = mockMvc.perform(post("/api/v1/routines")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String routineId = objectMapper.readTree(createdJson).get("id").asText();

        String laterDate = Instant.now().plus(8, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        String updateBody = objectMapper.writeValueAsString(Map.of(
                "nextExecutionDate", laterDate, "active", false, "version", 0));

        mockMvc.perform(patch("/api/v1/routines/" + routineId)
                        .with(principal)
                        .contentType("application/json")
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)))
                .andExpect(jsonPath("$.title", is("Actualizar CRM")))
                .andExpect(jsonPath("$.version", is(1)))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void updateRoutine_mismatchedVersionReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "erin-r@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Revisar pendientes", "frequency", "MONTHLY", "nextExecutionDate", NEXT_EXECUTION));

        String createdJson = mockMvc.perform(post("/api/v1/routines")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String routineId = objectMapper.readTree(createdJson).get("id").asText();

        String staleUpdateBody = objectMapper.writeValueAsString(Map.of("title", "Otro título", "version", 99));

        mockMvc.perform(patch("/api/v1/routines/" + routineId)
                        .with(principal)
                        .contentType("application/json")
                        .content(staleUpdateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("ROUTINE_VERSION_CONFLICT")));
    }

    @Test
    void deleteRoutine_ownerDeletesThenGetReturnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "frank-r@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Rutina descartable", "frequency", "DAILY", "nextExecutionDate", NEXT_EXECUTION));

        String createdJson = mockMvc.perform(post("/api/v1/routines")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String routineId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/routines/" + routineId).with(principal))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/routines/" + routineId).with(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("ROUTINE_NOT_FOUND")));
    }

    /**
     * AC-019, DECISION del Product Owner (opción B): la rutina está
     * <b>atrasada</b> — programada hace 3 días, semanal — y se marca hoy. La
     * nueva fecha debe ser la programada + 7 días (todavía en el pasado
     * reciente), NO hoy + 7 días. Este es exactamente el caso donde A y B
     * dan resultados distintos, así que es el que cierra la decisión.
     */
    @Test
    void executeRoutine_advancesFromScheduledDateNotFromNow() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "gina-r@example.com").build());

        Instant scheduled = Instant.now().minus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Status semanal", "frequency", "WEEKLY", "nextExecutionDate", scheduled.toString()));

        String createdJson = mockMvc.perform(post("/api/v1/routines")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String routineId = objectMapper.readTree(createdJson).get("id").asText();

        String executedJson = mockMvc.perform(post("/api/v1/routines/" + routineId + "/execute")
                        .with(principal)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                // Sigue activa: ejecutar no la termina (no existe `completed`).
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(openApi().isValid(VALIDATOR))
                .andReturn().getResponse().getContentAsString();

        Instant advanced = Instant.parse(objectMapper.readTree(executedJson).get("nextExecutionDate").asText());
        Instant expectedFromSchedule = scheduled.plus(7, ChronoUnit.DAYS);

        org.junit.jupiter.api.Assertions.assertEquals(expectedFromSchedule, advanced,
                "Opción B: debe avanzar desde la fecha programada (programada+7d), no desde ahora");
        // Y, por ser una rutina atrasada, la nueva fecha sigue estando cerca
        // del presente en vez de saltar una semana completa hacia el futuro:
        // un clic marca UNA ocurrencia, no se salta las perdidas.
        org.junit.jupiter.api.Assertions.assertTrue(advanced.isBefore(Instant.now().plus(5, ChronoUnit.DAYS)),
                "No debe haberse calculado desde 'ahora' (eso daría ahora+7d)");
    }

    /** MONTHLY sobre un día 31: java.time debe dar un día válido del mes siguiente, no una fecha inválida. */
    @Test
    void executeRoutine_monthlyOnMonthEndLandsOnAValidDate() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "hugo-r@example.com").build());

        // 31 de enero → debe caer en 28 de febrero (2026 no es bisiesto).
        Instant jan31 = Instant.parse("2026-01-31T09:00:00Z");
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Cierre mensual", "frequency", "MONTHLY", "nextExecutionDate", jan31.toString()));

        String createdJson = mockMvc.perform(post("/api/v1/routines")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String routineId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(post("/api/v1/routines/" + routineId + "/execute")
                        .with(principal)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextExecutionDate", is("2026-02-28T09:00:00Z")))
                .andExpect(openApi().isValid(VALIDATOR));
    }

    @Test
    void executeRoutine_mismatchedVersionReturns409() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "iris-r@example.com").build());
        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Rutina con conflicto", "frequency", "DAILY", "nextExecutionDate", NEXT_EXECUTION));

        String createdJson = mockMvc.perform(post("/api/v1/routines")
                        .with(principal)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String routineId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(post("/api/v1/routines/" + routineId + "/execute")
                        .with(principal)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("version", 99))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("ROUTINE_VERSION_CONFLICT")));
    }

    @Test
    void executeRoutine_ownedByAnotherUserReturnsNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = jwt().jwt(jwtFor(ownerId, "owner2-r@example.com").build());
        var stranger = jwt().jwt(jwtFor(strangerId, "stranger2-r@example.com").build());

        String createBody = objectMapper.writeValueAsString(Map.of(
                "title", "Rutina ajena", "frequency", "DAILY", "nextExecutionDate", NEXT_EXECUTION));
        String createdJson = mockMvc.perform(post("/api/v1/routines")
                        .with(owner)
                        .contentType("application/json")
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String routineId = objectMapper.readTree(createdJson).get("id").asText();

        mockMvc.perform(post("/api/v1/routines/" + routineId + "/execute")
                        .with(stranger)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("ROUTINE_NOT_FOUND")));
    }

    @Test
    void routineEndpoints_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/routines"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }
}
