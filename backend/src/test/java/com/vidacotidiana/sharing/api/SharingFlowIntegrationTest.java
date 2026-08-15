package com.vidacotidiana.sharing.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.vidacotidiana.sharing.application.InvitationMaintenanceService;
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

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the sharing module against a real PostgreSQL
 * instance (Testcontainers) — same pattern as
 * reminder.api.ReminderControllerIntegrationTest. Exercises invitations,
 * shares, the extended Reminder authorization (BE-022), invitation
 * expiration (BE-033), and rate limiting (DEVOPS-001) end to end.
 *
 * Traceability: UC-07/UC-08/UC-09/UC-10/UC-14, AC-007/AC-008/AC-009/
 * AC-010/AC-011/AC-017, docs/development/01-technical-backlog.md
 * BE-016..022, BE-033, DEVOPS-001.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SharingFlowIntegrationTest {

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

    @Autowired
    private DataSource dataSource;

    @Autowired
    private InvitationMaintenanceService invitationMaintenanceService;

    private org.springframework.security.oauth2.jwt.Jwt.Builder jwtFor(UUID userId, String email, String username) {
        return org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .claim("email", email)
                .claim("preferred_username", username)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
    }

    private RequestPostProcessor principalFor(UUID userId, String email, String username) {
        return jwt().jwt(jwtFor(userId, email, username).build());
    }

    /** UserSyncFilter only upserts USER on an authenticated request; call this once per test user before referencing it by email/username. */
    private void syncUser(RequestPostProcessor principal) throws Exception {
        mockMvc.perform(get("/api/v1/me").with(principal)).andExpect(status().isOk());
    }

    private String createReminder(RequestPostProcessor principal, String title) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("title", title));
        String json = mockMvc.perform(post("/api/v1/reminders").with(principal).contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    private JsonNode createInvitation(RequestPostProcessor principal, String reminderId, Map<String, Object> body) throws Exception {
        String json = mockMvc.perform(post("/api/v1/reminders/" + reminderId + "/shares")
                        .with(principal).contentType("application/json").content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    @Test
    void createInvitation_byEmailWithExistingAccount_returns201WithoutRevealingAccountExistence() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner1@example.com", "owner1");
        var invitee = principalFor(inviteeId, "invitee1@example.com", "invitee1");
        syncUser(invitee); // invitee has an account

        String reminderId = createReminder(owner, "Family trip");

        JsonNode invitation = createInvitation(owner, reminderId, Map.of("email", "invitee1@example.com"));

        assertInvitationShape(invitation, "invitee1@example.com");
    }

    @Test
    void createInvitation_byEmailWithoutAccount_returns201WithIdenticalShape() throws Exception {
        UUID ownerId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner2@example.com", "owner2");
        String reminderId = createReminder(owner, "Family trip");

        JsonNode invitation = createInvitation(owner, reminderId, Map.of("email", "nobody-here@example.com"));

        // SEC-001: same shape/fields regardless of whether the email has an account — no invitedUserId field either way.
        assertInvitationShape(invitation, "nobody-here@example.com");
    }

    private void assertInvitationShape(JsonNode invitation, String expectedEmail) {
        org.assertj.core.api.Assertions.assertThat(invitation.has("id")).isTrue();
        org.assertj.core.api.Assertions.assertThat(invitation.get("invitedEmail").asText()).isEqualTo(expectedEmail);
        org.assertj.core.api.Assertions.assertThat(invitation.get("status").asText()).isEqualTo("PENDING");
        org.assertj.core.api.Assertions.assertThat(invitation.has("expiresAt")).isTrue();
        org.assertj.core.api.Assertions.assertThat(invitation.has("invitedUserId")).isFalse();
    }

    @Test
    void createInvitation_byExistingUsername_resolvesInvitationEmail() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner3@example.com", "owner3");
        var invitee = principalFor(inviteeId, "invitee3@example.com", "invitee3user");
        syncUser(invitee);

        String reminderId = createReminder(owner, "Family trip");

        JsonNode invitation = createInvitation(owner, reminderId, Map.of("username", "invitee3user"));

        org.assertj.core.api.Assertions.assertThat(invitation.get("invitedEmail").asText()).isEqualTo("invitee3@example.com");
    }

    @Test
    void createInvitation_byUnknownUsername_returns400() throws Exception {
        UUID ownerId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner4@example.com", "owner4");
        String reminderId = createReminder(owner, "Family trip");

        String body = objectMapper.writeValueAsString(Map.of("username", "ghost-user-does-not-exist"));
        mockMvc.perform(post("/api/v1/reminders/" + reminderId + "/shares").with(owner).contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void createInvitation_duplicatePending_returns409() throws Exception {
        UUID ownerId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner5@example.com", "owner5");
        String reminderId = createReminder(owner, "Family trip");
        createInvitation(owner, reminderId, Map.of("email", "dup@example.com"));

        String body = objectMapper.writeValueAsString(Map.of("email", "dup@example.com"));
        mockMvc.perform(post("/api/v1/reminders/" + reminderId + "/shares").with(owner).contentType("application/json").content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INVITATION_ALREADY_PENDING")));
    }

    @Test
    void createInvitation_nonOwnerGetsNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner6@example.com", "owner6");
        var stranger = principalFor(strangerId, "stranger6@example.com", "stranger6");
        String reminderId = createReminder(owner, "Family trip");

        String body = objectMapper.writeValueAsString(Map.of("email", "x@example.com"));
        mockMvc.perform(post("/api/v1/reminders/" + reminderId + "/shares").with(stranger).contentType("application/json").content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("REMINDER_NOT_FOUND")));
    }

    @Test
    void listSharesAndInvitations_ownerSeesThem_nonOwnerGetsNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner7@example.com", "owner7");
        var stranger = principalFor(strangerId, "stranger7@example.com", "stranger7");
        String reminderId = createReminder(owner, "Family trip");
        createInvitation(owner, reminderId, Map.of("email", "pending7@example.com"));

        mockMvc.perform(get("/api/v1/reminders/" + reminderId + "/shares").with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invitations[0].invitedEmail", is("pending7@example.com")))
                .andExpect(jsonPath("$.shares").isArray());

        mockMvc.perform(get("/api/v1/reminders/" + reminderId + "/shares").with(stranger))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptInvitation_happyPath_createsActiveShareAndGrantsAccess() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID collaboratorId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner8@example.com", "owner8");
        var collaborator = principalFor(collaboratorId, "collab8@example.com", "collab8");
        syncUser(collaborator);
        String reminderId = createReminder(owner, "Family trip");
        JsonNode invitation = createInvitation(owner, reminderId, Map.of("email", "collab8@example.com"));
        String invitationId = invitation.get("id").asText();

        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/accept").with(collaborator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.collaboratorUserId", is(collaboratorId.toString())));

        // AC-011/BE-022: the collaborator now sees this reminder in their own list, and can read/complete it.
        mockMvc.perform(get("/api/v1/reminders").with(collaborator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id", is(reminderId)));
        mockMvc.perform(get("/api/v1/reminders/" + reminderId).with(collaborator))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reminders/" + reminderId + "/complete").with(collaborator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")));
    }

    @Test
    void acceptInvitation_alreadyResolved_returns410() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID collaboratorId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner9@example.com", "owner9");
        var collaborator = principalFor(collaboratorId, "collab9@example.com", "collab9");
        syncUser(collaborator);
        String reminderId = createReminder(owner, "Family trip");
        JsonNode invitation = createInvitation(owner, reminderId, Map.of("email", "collab9@example.com"));
        String invitationId = invitation.get("id").asText();

        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/accept").with(collaborator))
                .andExpect(status().isOk());

        // AC-008: a second resolution attempt on the same invitation (sequential, simulating the losing side
        // of a concurrent race) must get 410, and must not create a second ReminderShare.
        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/accept").with(collaborator))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code", is("INVITATION_ALREADY_RESOLVED")));
    }

    @Test
    void rejectInvitation_happyPath_thenAlreadyResolvedReturns410() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner10@example.com", "owner10");
        var invitee = principalFor(inviteeId, "invitee10@example.com", "invitee10");
        syncUser(invitee);
        String reminderId = createReminder(owner, "Family trip");
        JsonNode invitation = createInvitation(owner, reminderId, Map.of("email", "invitee10@example.com"));
        String invitationId = invitation.get("id").asText();

        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/reject").with(invitee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("REJECTED")));

        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/reject").with(invitee))
                .andExpect(status().isGone());
    }

    @Test
    void cancelInvitation_byInviter_thenAcceptReturns410() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner11@example.com", "owner11");
        var invitee = principalFor(inviteeId, "invitee11@example.com", "invitee11");
        syncUser(invitee);
        String reminderId = createReminder(owner, "Family trip");
        JsonNode invitation = createInvitation(owner, reminderId, Map.of("email", "invitee11@example.com"));
        String invitationId = invitation.get("id").asText();

        mockMvc.perform(delete("/api/v1/invitations/" + invitationId).with(owner))
                .andExpect(status().isNoContent());

        // AC-017: cancelling moves INVITATION out of PENDING; a subsequent accept attempt confirms it, via 410.
        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/accept").with(invitee))
                .andExpect(status().isGone());
    }

    @Test
    void cancelInvitation_nonInviterGetsNotFound() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner12@example.com", "owner12");
        var stranger = principalFor(strangerId, "stranger12@example.com", "stranger12");
        String reminderId = createReminder(owner, "Family trip");
        JsonNode invitation = createInvitation(owner, reminderId, Map.of("email", "invitee12@example.com"));

        mockMvc.perform(delete("/api/v1/invitations/" + invitation.get("id").asText()).with(stranger))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelInvitation_alreadyResolvedReturns410() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner13@example.com", "owner13");
        var invitee = principalFor(inviteeId, "invitee13@example.com", "invitee13");
        syncUser(invitee);
        String reminderId = createReminder(owner, "Family trip");
        JsonNode invitation = createInvitation(owner, reminderId, Map.of("email", "invitee13@example.com"));
        String invitationId = invitation.get("id").asText();
        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/reject").with(invitee)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/invitations/" + invitationId).with(owner))
                .andExpect(status().isGone());
    }

    @Test
    void revokeShare_effectiveImmediately() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID collaboratorId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner14@example.com", "owner14");
        var collaborator = principalFor(collaboratorId, "collab14@example.com", "collab14");
        syncUser(collaborator);
        String reminderId = createReminder(owner, "Family trip");
        JsonNode invitation = createInvitation(owner, reminderId, Map.of("email", "collab14@example.com"));
        String acceptJson = mockMvc.perform(post("/api/v1/invitations/" + invitation.get("id").asText() + "/accept").with(collaborator))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String shareId = objectMapper.readTree(acceptJson).get("id").asText();

        mockMvc.perform(get("/api/v1/reminders/" + reminderId).with(collaborator)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/reminders/" + reminderId + "/shares/" + shareId).with(owner))
                .andExpect(status().isNoContent());

        // AC-010: effective immediately, no grace window.
        mockMvc.perform(get("/api/v1/reminders/" + reminderId).with(collaborator))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("REMINDER_NOT_FOUND")));
    }

    @Test
    void collaborator_canReadAndCompleteButNeverEditDeleteOrInvite() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID collaboratorId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner15@example.com", "owner15");
        var collaborator = principalFor(collaboratorId, "collab15@example.com", "collab15");
        syncUser(collaborator);
        String reminderId = createReminder(owner, "Family trip");
        JsonNode invitation = createInvitation(owner, reminderId, Map.of("email", "collab15@example.com"));
        mockMvc.perform(post("/api/v1/invitations/" + invitation.get("id").asText() + "/accept").with(collaborator))
                .andExpect(status().isOk());

        // AC-011: a collaborator may never edit, delete, or invite — same 404 as a stranger, never 403.
        String patchBody = objectMapper.writeValueAsString(Map.of("title", "Hijacked", "version", 0));
        mockMvc.perform(patch("/api/v1/reminders/" + reminderId).with(collaborator).contentType("application/json").content(patchBody))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/reminders/" + reminderId).with(collaborator))
                .andExpect(status().isNotFound());
        String inviteBody = objectMapper.writeValueAsString(Map.of("email", "someone-else@example.com"));
        mockMvc.perform(post("/api/v1/reminders/" + reminderId + "/shares").with(collaborator).contentType("application/json").content(inviteBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void myInvitations_listsOnlyPendingInvitationsForCaller() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner16@example.com", "owner16");
        var invitee = principalFor(inviteeId, "invitee16@example.com", "invitee16");
        syncUser(invitee);
        String reminderId = createReminder(owner, "Family trip");
        createInvitation(owner, reminderId, Map.of("email", "invitee16@example.com"));

        mockMvc.perform(get("/api/v1/me/invitations").with(invitee))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].invitedEmail", is("invitee16@example.com")))
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void expiredInvitation_cannotBeAcceptedBeforeSweep_thenSweepMarksItExpired() throws Exception {
        // BE-033: the gap this closes — an invitation whose expires_at already passed, but that the
        // sweep job hasn't reached yet, must still be rejected atomically by resolveIfPending.
        UUID ownerId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner17@example.com", "owner17");
        var invitee = principalFor(inviteeId, "invitee17@example.com", "invitee17");
        syncUser(invitee);
        String reminderId = createReminder(owner, "Family trip");
        JsonNode invitation = createInvitation(owner, reminderId, Map.of("email", "invitee17@example.com"));
        String invitationId = invitation.get("id").asText();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE invitations SET expires_at = ? WHERE id = ?")) {
            statement.setObject(1, OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
            statement.setObject(2, UUID.fromString(invitationId));
            statement.executeUpdate();
        }

        // Still PENDING in the status column (the sweep hasn't run) but must be rejected anyway.
        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/accept").with(invitee))
                .andExpect(status().isGone());

        invitationMaintenanceService.expireOverduePendingInvitations();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT status FROM invitations WHERE id = ?")) {
            statement.setObject(1, UUID.fromString(invitationId));
            ResultSet resultSet = statement.executeQuery();
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString("status")).isEqualTo("EXPIRED");
        }
    }

    @Test
    void createInvitation_exceedingRateLimit_returns429() throws Exception {
        // DEVOPS-001, real HTTP round trip (unit-level coverage already exists in SharingServiceTest).
        UUID ownerId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner18@example.com", "owner18");
        String reminderId = createReminder(owner, "Family trip");

        for (int i = 0; i < 10; i++) {
            createInvitation(owner, reminderId, Map.of("email", "bulk" + i + "@example.com"));
        }

        String body = objectMapper.writeValueAsString(Map.of("email", "onemore@example.com"));
        mockMvc.perform(post("/api/v1/reminders/" + reminderId + "/shares").with(owner).contentType("application/json").content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code", is("RATE_LIMIT_EXCEEDED")));
    }
}
