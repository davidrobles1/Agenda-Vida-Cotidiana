package com.vidacotidiana.family.domain;

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

/**
 * Invitación a formar parte de una familia (ADR-025, tabla
 * {@code family_invitations} de V29).
 *
 * Se dirige a un USUARIO, no a un correo: quien invita ya encontró a la
 * persona buscándola por su nombre de usuario, así que no existe el caso
 * "invitado sin cuenta" que sí tiene {@link com.vidacotidiana.sharing.domain.Invitation}
 * — y con él tampoco aplica SEC-001, cuyo motivo es no revelar si un CORREO
 * pertenece a una cuenta.
 */
@Entity
@Table(name = "family_invitations")
public class FamilyInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "inviter_user_id", nullable = false)
    private UUID inviterUserId;

    @Column(name = "invited_user_id", nullable = false)
    private UUID invitedUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FamilyInvitationStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected FamilyInvitation() {
        // JPA
    }

    public FamilyInvitation(UUID inviterUserId, UUID invitedUserId) {
        this.inviterUserId = inviterUserId;
        this.invitedUserId = invitedUserId;
        this.status = FamilyInvitationStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getInviterUserId() {
        return inviterUserId;
    }

    public UUID getInvitedUserId() {
        return invitedUserId;
    }

    public FamilyInvitationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public boolean isInvitedUser(UUID userId) {
        return this.invitedUserId.equals(userId);
    }

    public boolean isInviter(UUID userId) {
        return this.inviterUserId.equals(userId);
    }
}
