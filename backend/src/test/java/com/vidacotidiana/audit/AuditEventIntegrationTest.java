package com.vidacotidiana.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vidacotidiana.audit.domain.AuditEvent;
import com.vidacotidiana.audit.domain.AuditEventRepository;
import com.vidacotidiana.audit.domain.AuditEventType;
import com.vidacotidiana.audit.domain.AuditTargetType;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BE-029, real PostgreSQL via Testcontainers. Verifies that each of the six
 * transitions 11-auth-security.md §Auditoría lists actually inserts the
 * expected AUDIT_EVENT row — not just that the service was called (already
 * covered at the unit level in SharingServiceTest/InvitationMaintenanceServiceTest).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditEventIntegrationTest {

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
    private AuditEventRepository auditEventRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private InvitationMaintenanceService invitationMaintenanceService;

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

    private void syncUser(RequestPostProcessor principal) throws Exception {
        mockMvc.perform(get("/api/v1/me").with(principal)).andExpect(status().isOk());
    }

    private String createReminder(RequestPostProcessor principal) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("title", "Family trip"));
        String json = mockMvc.perform(post("/api/v1/reminders").with(principal).contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    private JsonNode createInvitation(RequestPostProcessor principal, String reminderId, String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", email));
        String json = mockMvc.perform(post("/api/v1/reminders/" + reminderId + "/shares")
                        .with(principal).contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json);
    }

    private AuditEvent onlyAuditEventFor(AuditTargetType targetType, UUID targetId) {
        List<AuditEvent> events = auditEventRepository.findByTargetTypeAndTargetId(targetType, targetId);
        assertThat(events).hasSize(1);
        return events.get(0);
    }

    @Test
    void creatingInvitation_insertsInvitationCreatedAuditEvent() throws Exception {
        UUID ownerId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner1@example.com", "owner1");
        String reminderId = createReminder(owner);

        JsonNode invitation = createInvitation(owner, reminderId, "invitee1@example.com");
        UUID invitationId = UUID.fromString(invitation.get("id").asText());

        AuditEvent event = onlyAuditEventFor(AuditTargetType.INVITATION, invitationId);
        assertThat(event.getEventType()).isEqualTo(AuditEventType.INVITATION_CREATED);
        assertThat(event.getActorUserId()).isEqualTo(ownerId);
    }

    @Test
    void cancellingInvitation_insertsInvitationCancelledAuditEvent() throws Exception {
        UUID ownerId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner2@example.com", "owner2");
        String reminderId = createReminder(owner);
        JsonNode invitation = createInvitation(owner, reminderId, "invitee2@example.com");
        UUID invitationId = UUID.fromString(invitation.get("id").asText());

        mockMvc.perform(delete("/api/v1/invitations/" + invitationId).with(owner)).andExpect(status().isNoContent());

        List<AuditEvent> events = auditEventRepository.findByTargetTypeAndTargetId(AuditTargetType.INVITATION, invitationId);
        assertThat(events).extracting(AuditEvent::getEventType).contains(AuditEventType.INVITATION_CANCELLED);
        assertThat(events).filteredOn(e -> e.getEventType() == AuditEventType.INVITATION_CANCELLED)
                .allSatisfy(e -> assertThat(e.getActorUserId()).isEqualTo(ownerId));
    }

    @Test
    void acceptingInvitation_insertsInvitationAcceptedAuditEvent() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner3@example.com", "owner3");
        var invitee = principalFor(inviteeId, "invitee3@example.com", "invitee3");
        syncUser(invitee);
        String reminderId = createReminder(owner);
        JsonNode invitation = createInvitation(owner, reminderId, "invitee3@example.com");
        UUID invitationId = UUID.fromString(invitation.get("id").asText());

        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/accept").with(invitee)).andExpect(status().isOk());

        List<AuditEvent> events = auditEventRepository.findByTargetTypeAndTargetId(AuditTargetType.INVITATION, invitationId);
        assertThat(events).filteredOn(e -> e.getEventType() == AuditEventType.INVITATION_ACCEPTED)
                .hasSize(1)
                .allSatisfy(e -> assertThat(e.getActorUserId()).isEqualTo(inviteeId));
    }

    @Test
    void rejectingInvitation_insertsInvitationRejectedAuditEvent() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID inviteeId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner4@example.com", "owner4");
        var invitee = principalFor(inviteeId, "invitee4@example.com", "invitee4");
        syncUser(invitee);
        String reminderId = createReminder(owner);
        JsonNode invitation = createInvitation(owner, reminderId, "invitee4@example.com");
        UUID invitationId = UUID.fromString(invitation.get("id").asText());

        mockMvc.perform(post("/api/v1/invitations/" + invitationId + "/reject").with(invitee)).andExpect(status().isOk());

        // Same target as the CREATED event recorded when the invitation was made — filter by event type, not just target.
        List<AuditEvent> events = auditEventRepository.findByTargetTypeAndTargetId(AuditTargetType.INVITATION, invitationId);
        assertThat(events).filteredOn(e -> e.getEventType() == AuditEventType.INVITATION_REJECTED)
                .hasSize(1)
                .allSatisfy(e -> assertThat(e.getActorUserId()).isEqualTo(inviteeId));
    }

    @Test
    void revokingShare_insertsShareRevokedAuditEvent() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID collaboratorId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner5@example.com", "owner5");
        var collaborator = principalFor(collaboratorId, "collab5@example.com", "collab5");
        syncUser(collaborator);
        String reminderId = createReminder(owner);
        JsonNode invitation = createInvitation(owner, reminderId, "collab5@example.com");
        String acceptJson = mockMvc.perform(post("/api/v1/invitations/" + invitation.get("id").asText() + "/accept").with(collaborator))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID shareId = UUID.fromString(objectMapper.readTree(acceptJson).get("id").asText());

        mockMvc.perform(delete("/api/v1/reminders/" + reminderId + "/shares/" + shareId).with(owner))
                .andExpect(status().isNoContent());

        AuditEvent event = onlyAuditEventFor(AuditTargetType.REMINDER_SHARE, shareId);
        assertThat(event.getEventType()).isEqualTo(AuditEventType.SHARE_REVOKED);
        assertThat(event.getActorUserId()).isEqualTo(ownerId);
    }

    @Test
    void expiringInvitation_insertsInvitationExpiredAuditEventWithNullActor() throws Exception {
        UUID ownerId = UUID.randomUUID();
        var owner = principalFor(ownerId, "owner6@example.com", "owner6");
        String reminderId = createReminder(owner);
        JsonNode invitation = createInvitation(owner, reminderId, "invitee6@example.com");
        UUID invitationId = UUID.fromString(invitation.get("id").asText());

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE invitations SET expires_at = ? WHERE id = ?")) {
            statement.setObject(1, OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
            statement.setObject(2, invitationId);
            statement.executeUpdate();
        }

        invitationMaintenanceService.expireOverduePendingInvitations();

        List<AuditEvent> events = auditEventRepository.findByTargetTypeAndTargetId(AuditTargetType.INVITATION, invitationId);
        assertThat(events).filteredOn(e -> e.getEventType() == AuditEventType.INVITATION_EXPIRED)
                .hasSize(1)
                // BE-029: a system job has no human actor.
                .allSatisfy(e -> assertThat(e.getActorUserId()).isNull());
    }
}
