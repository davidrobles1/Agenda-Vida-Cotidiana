package com.vidacotidiana.sharing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Maps to the INVITATION entity in Documentacion/09-data-model.md.
 * expiresAt = createdAt + 7 days (ASSUMPTION, FR-007). Never exposes
 * invitedUserId in the API response (SEC-001: never reveal whether the
 * invited email belongs to an existing account) — see
 * sharing.api.dto.InvitationResponse.
 */
@Entity
@Table(name = "invitations")
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reminder_id", nullable = false)
    private UUID reminderId;

    @Column(name = "inviter_user_id", nullable = false)
    private UUID inviterUserId;

    @Column(name = "invited_email", nullable = false)
    private String invitedEmail;

    @Column(name = "invited_user_id")
    private UUID invitedUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected Invitation() {
        // JPA
    }

    public Invitation(UUID reminderId, UUID inviterUserId, String invitedEmail, UUID invitedUserId) {
        this.reminderId = reminderId;
        this.inviterUserId = inviterUserId;
        this.invitedEmail = invitedEmail;
        this.invitedUserId = invitedUserId;
        this.status = InvitationStatus.PENDING;
        this.createdAt = Instant.now();
        this.expiresAt = this.createdAt.plus(7, ChronoUnit.DAYS);
    }

    public UUID getId() {
        return id;
    }

    public UUID getReminderId() {
        return reminderId;
    }

    public UUID getInviterUserId() {
        return inviterUserId;
    }

    public String getInvitedEmail() {
        return invitedEmail;
    }

    public UUID getInvitedUserId() {
        return invitedUserId;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public boolean isInvitedUser(UUID userId) {
        return userId.equals(this.invitedUserId);
    }

    public boolean isInviter(UUID userId) {
        return userId.equals(this.inviterUserId);
    }
}
