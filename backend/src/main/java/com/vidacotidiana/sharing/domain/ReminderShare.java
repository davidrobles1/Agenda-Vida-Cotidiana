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
import java.util.UUID;

/** Maps to the REMINDER_SHARE entity in Documentacion/09-data-model.md. */
@Entity
@Table(name = "reminder_shares")
public class ReminderShare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reminder_id", nullable = false)
    private UUID reminderId;

    @Column(name = "collaborator_user_id", nullable = false)
    private UUID collaboratorUserId;

    @Column(name = "invitation_id", nullable = false)
    private UUID invitationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderShareStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected ReminderShare() {
        // JPA
    }

    public ReminderShare(UUID reminderId, UUID collaboratorUserId, UUID invitationId) {
        this.reminderId = reminderId;
        this.collaboratorUserId = collaboratorUserId;
        this.invitationId = invitationId;
        this.status = ReminderShareStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getReminderId() {
        return reminderId;
    }

    public UUID getCollaboratorUserId() {
        return collaboratorUserId;
    }

    public UUID getInvitationId() {
        return invitationId;
    }

    public ReminderShareStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    /** AC-010: effective immediately, no grace window. */
    public void revoke() {
        this.status = ReminderShareStatus.REVOKED;
        this.revokedAt = Instant.now();
    }
}
