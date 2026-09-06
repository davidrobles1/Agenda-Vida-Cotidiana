package com.vidacotidiana.family.api;

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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ADR-025 — Familia y Compartidos, verificado por FLUJO.
 *
 * Cada prueba construye a sus propios usuarios y sus propios recursos: no
 * depende de que exista nada previo ni deja estado que condicione a la
 * siguiente. Es lo que hace que comprueben la tubería y no lo que hoy pasa
 * por ella.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FamilyAndSharingIntegrationTest {

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

    /** Un usuario nace en la base la primera vez que llama autenticado
        (UserSyncFilter), así que basta con emitirle un token. */
    private RequestPostProcessor user(UUID id, String username) {
        return jwt().jwt(org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(id.toString())
                .claim("email", username + "@example.com")
                .claim("preferred_username", username)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build());
    }

    private static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Registra al usuario en la base y devuelve su post-procesador. */
    private RequestPostProcessor register(UUID id, String username) throws Exception {
        RequestPostProcessor principal = user(id, username);
        mockMvc.perform(get("/api/v1/me").with(principal)).andExpect(status().isOk());
        return principal;
    }

    // ------------------------------------------------------------------
    // Búsqueda
    // ------------------------------------------------------------------

    @Test
    void search_rejectsFewerThanFiveCharacters() throws Exception {
        RequestPostProcessor caller = register(UUID.randomUUID(), unique("buscador"));

        // La regla del mínimo NO puede vivir solo en el cliente: existe para no
        // lanzar consultas caras y quien llame directo debe toparse con ella.
        mockMvc.perform(get("/api/v1/users/search").param("q", "abcd").with(caller))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_findsByUsernamePrefixAndNeverReturnsEmail() throws Exception {
        String username = unique("encontrable");
        register(UUID.randomUUID(), username);
        RequestPostProcessor caller = register(UUID.randomUUID(), unique("otro"));

        String body = mockMvc.perform(get("/api/v1/users/search")
                        .param("q", username.substring(0, 8)).with(caller))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // SEC-001: el correo no puede salir de un buscador abierto.
        org.junit.jupiter.api.Assertions.assertTrue(body.contains(username));
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("@example.com"));
    }

    @Test
    void search_neverReturnsTheCaller() throws Exception {
        String username = unique("yomismo");
        RequestPostProcessor caller = register(UUID.randomUUID(), username);

        mockMvc.perform(get("/api/v1/users/search").param("q", username.substring(0, 8)).with(caller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ------------------------------------------------------------------
    // Invitaciones
    // ------------------------------------------------------------------

    @Test
    void invite_accept_thenBothSeeEachOther() throws Exception {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        RequestPostProcessor alice = register(aliceId, unique("alicia"));
        RequestPostProcessor bob = register(bobId, unique("roberto"));

        String invitationId = invite(alice, bobId);

        // Se ve desde los dos lados, cada uno en su sitio.
        mockMvc.perform(get("/api/v1/family/invitations/sent").with(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/v1/family/invitations/received").with(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(invitationId)));

        mockMvc.perform(post("/api/v1/family/invitations/" + invitationId + "/accept").with(bob))
                .andExpect(status().isNoContent());

        // El vínculo es SIMÉTRICO: si solo se insertara una fila, uno de los dos
        // vería una familia vacía.
        mockMvc.perform(get("/api/v1/family/members").with(alice))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/v1/family/members").with(bob))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void acceptTwice_returnsGone() throws Exception {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        RequestPostProcessor alice = register(aliceId, unique("alicia"));
        RequestPostProcessor bob = register(bobId, unique("roberto"));

        String invitationId = invite(alice, bobId);
        mockMvc.perform(post("/api/v1/family/invitations/" + invitationId + "/accept").with(bob))
                .andExpect(status().isNoContent());

        // La transición es un UPDATE condicional atómico: la segunda no aplica.
        mockMvc.perform(post("/api/v1/family/invitations/" + invitationId + "/accept").with(bob))
                .andExpect(status().isGone());
    }

    @Test
    void inviteTwice_conflicts() throws Exception {
        UUID bobId = UUID.randomUUID();
        RequestPostProcessor alice = register(UUID.randomUUID(), unique("alicia"));
        register(bobId, unique("roberto"));

        invite(alice, bobId);
        mockMvc.perform(post("/api/v1/family/invitations")
                        .with(alice).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("userId", bobId.toString()))))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectingInvitation_createsNoLink() throws Exception {
        UUID bobId = UUID.randomUUID();
        RequestPostProcessor alice = register(UUID.randomUUID(), unique("alicia"));
        RequestPostProcessor bob = register(bobId, unique("roberto"));

        String invitationId = invite(alice, bobId);
        mockMvc.perform(post("/api/v1/family/invitations/" + invitationId + "/reject").with(bob))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/family/members").with(alice)).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/v1/family/members").with(bob)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void aStrangerCannotAcceptSomeoneElsesInvitation() throws Exception {
        UUID bobId = UUID.randomUUID();
        RequestPostProcessor alice = register(UUID.randomUUID(), unique("alicia"));
        register(bobId, unique("roberto"));
        RequestPostProcessor intruder = register(UUID.randomUUID(), unique("intruso"));

        String invitationId = invite(alice, bobId);

        // 404 y no 403: a quien no le corresponde ni se le confirma que existe.
        mockMvc.perform(post("/api/v1/family/invitations/" + invitationId + "/accept").with(intruder))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // Compartir
    // ------------------------------------------------------------------

    @Test
    void shareReminderWithResponsibility_collaboratorMarksPartDone() throws Exception {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        RequestPostProcessor alice = register(aliceId, unique("alicia"));
        RequestPostProcessor bob = register(bobId, unique("roberto"));
        becomeFamily(alice, bob, bobId);

        String reminderId = createReminder(alice, "Llevar el coche al taller");

        mockMvc.perform(post("/api/v1/shared-resources/REMINDER/" + reminderId)
                        .with(alice).contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                Map.of("collaboratorUserId", bobId.toString(), "responsibility", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responsibility", is(true)));

        String received = mockMvc.perform(get("/api/v1/shared-resources/received").with(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].resourceLabel", is("Llevar el coche al taller")))
                .andReturn().getResponse().getContentAsString();
        String shareId = objectMapper.readTree(received).get(0).get("id").asText();

        mockMvc.perform(post("/api/v1/shared-resources/shares/" + shareId + "/part-done").with(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partDoneAt").exists());

        // Quien comparte tiene que VERLO: es la mitad del requisito §4.
        mockMvc.perform(get("/api/v1/shared-resources/sent").with(alice))
                .andExpect(jsonPath("$[0].partDoneAt").exists());

        // Y deshacerlo es la salida de un toque equivocado.
        mockMvc.perform(delete("/api/v1/shared-resources/shares/" + shareId + "/part-done").with(bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partDoneAt").doesNotExist());
    }

    @Test
    void cannotShareWithSomeoneOutsideTheFamily() throws Exception {
        UUID strangerId = UUID.randomUUID();
        RequestPostProcessor alice = register(UUID.randomUUID(), unique("alicia"));
        register(strangerId, unique("desconocido"));

        String reminderId = createReminder(alice, "Algo privado");

        mockMvc.perform(post("/api/v1/shared-resources/REMINDER/" + reminderId)
                        .with(alice).contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                Map.of("collaboratorUserId", strangerId.toString(), "responsibility", false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotShareAResourceYouDoNotOwn() throws Exception {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        RequestPostProcessor alice = register(aliceId, unique("alicia"));
        RequestPostProcessor bob = register(bobId, unique("roberto"));
        becomeFamily(alice, bob, bobId);

        String reminderId = createReminder(alice, "De Alicia");

        // Bob intenta compartir algo de Alicia. 404, no 403.
        mockMvc.perform(post("/api/v1/shared-resources/REMINDER/" + reminderId)
                        .with(bob).contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                Map.of("collaboratorUserId", aliceId.toString(), "responsibility", false))))
                .andExpect(status().isNotFound());
    }

    @Test
    void documentsAndInventoryRejectResponsibility() throws Exception {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        RequestPostProcessor alice = register(aliceId, unique("alicia"));
        RequestPostProcessor bob = register(bobId, unique("roberto"));
        becomeFamily(alice, bob, bobId);

        String itemJson = mockMvc.perform(post("/api/v1/inventory-items")
                        .with(alice).contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Silla de escritorio", "category", "HOGAR"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String itemId = objectMapper.readTree(itemJson).get("id").asText();

        // "No agregues estados o acciones que no tengan sentido para un recurso
        // determinado": una silla no tiene una parte que hacer.
        mockMvc.perform(post("/api/v1/shared-resources/INVENTORY_ITEM/" + itemId)
                        .with(alice).contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                Map.of("collaboratorUserId", bobId.toString(), "responsibility", true))))
                .andExpect(status().isBadRequest());

        // Pero compartirla para verla sí funciona.
        mockMvc.perform(post("/api/v1/shared-resources/INVENTORY_ITEM/" + itemId)
                        .with(alice).contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                Map.of("collaboratorUserId", bobId.toString(), "responsibility", false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responsibility", is(false)));
    }

    @Test
    void revokingRemovesItFromTheCollaboratorImmediately() throws Exception {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        RequestPostProcessor alice = register(aliceId, unique("alicia"));
        RequestPostProcessor bob = register(bobId, unique("roberto"));
        becomeFamily(alice, bob, bobId);

        String reminderId = createReminder(alice, "Compartida y luego revocada");
        String shareJson = mockMvc.perform(post("/api/v1/shared-resources/REMINDER/" + reminderId)
                        .with(alice).contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                Map.of("collaboratorUserId", bobId.toString(), "responsibility", false))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String shareId = objectMapper.readTree(shareJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/shared-resources/shares/" + shareId).with(alice))
                .andExpect(status().isNoContent());

        // Inmediato, sin ventana de gracia.
        mockMvc.perform(get("/api/v1/shared-resources/received").with(bob))
                .andExpect(jsonPath("$", hasSize(0)));

        // Y revocar dos veces sigue siendo éxito (idempotente).
        mockMvc.perform(delete("/api/v1/shared-resources/shares/" + shareId).with(alice))
                .andExpect(status().isNoContent());
    }

    @Test
    void sharingTheSameResourceTwiceConflicts() throws Exception {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        RequestPostProcessor alice = register(aliceId, unique("alicia"));
        RequestPostProcessor bob = register(bobId, unique("roberto"));
        becomeFamily(alice, bob, bobId);

        String reminderId = createReminder(alice, "Una sola vez");
        String payload = objectMapper.writeValueAsString(
                Map.of("collaboratorUserId", bobId.toString(), "responsibility", false));

        mockMvc.perform(post("/api/v1/shared-resources/REMINDER/" + reminderId)
                .with(alice).contentType("application/json").content(payload))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/shared-resources/REMINDER/" + reminderId)
                .with(alice).contentType("application/json").content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void onlyTheCommittedCollaboratorCanMarkThePartDone() throws Exception {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        RequestPostProcessor alice = register(aliceId, unique("alicia"));
        RequestPostProcessor bob = register(bobId, unique("roberto"));
        becomeFamily(alice, bob, bobId);

        String reminderId = createReminder(alice, "De Bob");
        String shareJson = mockMvc.perform(post("/api/v1/shared-resources/REMINDER/" + reminderId)
                        .with(alice).contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                Map.of("collaboratorUserId", bobId.toString(), "responsibility", true))))
                .andReturn().getResponse().getContentAsString();
        String shareId = objectMapper.readTree(shareJson).get("id").asText();

        // El dueño no puede marcar la parte de otro por él.
        mockMvc.perform(post("/api/v1/shared-resources/shares/" + shareId + "/part-done").with(alice))
                .andExpect(status().isNotFound());
    }

    @Test
    void removingAFamilyMemberIsSymmetric() throws Exception {
        UUID aliceId = UUID.randomUUID();
        UUID bobId = UUID.randomUUID();
        RequestPostProcessor alice = register(aliceId, unique("alicia"));
        RequestPostProcessor bob = register(bobId, unique("roberto"));
        becomeFamily(alice, bob, bobId);

        mockMvc.perform(delete("/api/v1/family/members/" + bobId).with(alice))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/family/members").with(alice)).andExpect(jsonPath("$", hasSize(0)));
        // Media relación viva sería un estado imposible de leer.
        mockMvc.perform(get("/api/v1/family/members").with(bob)).andExpect(jsonPath("$", hasSize(0)));
    }

    // ------------------------------------------------------------------

    private String invite(RequestPostProcessor inviter, UUID invitedUserId) throws Exception {
        String json = mockMvc.perform(post("/api/v1/family/invitations")
                        .with(inviter).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("userId", invitedUserId.toString()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    private void becomeFamily(RequestPostProcessor inviter, RequestPostProcessor invited, UUID invitedId)
            throws Exception {
        String invitationId = invite(inviter, invitedId);
        mockMvc.perform(post("/api/v1/family/invitations/" + invitationId + "/accept").with(invited))
                .andExpect(status().isNoContent());
    }

    private String createReminder(RequestPostProcessor owner, String title) throws Exception {
        String json = mockMvc.perform(post("/api/v1/reminders")
                        .with(owner).contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of("title", title))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }
}
