package com.vidacotidiana.family.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Vínculo familiar aceptado (ADR-025, tabla {@code family_links} de V29).
 *
 * La relación es SIMÉTRICA y se guarda en dos filas —una por cada sentido—,
 * así que "mi familia" es una consulta por {@code userId} y nada más. Ver el
 * comentario de la migración para por qué esto es una relación entre dos
 * personas y no un grupo con identidad propia.
 */
@Entity
@Table(name = "family_links")
public class FamilyLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "relative_user_id", nullable = false)
    private UUID relativeUserId;

    @Column(name = "invitation_id", nullable = false)
    private UUID invitationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FamilyLink() {
        // JPA
    }

    public FamilyLink(UUID userId, UUID relativeUserId, UUID invitationId) {
        this.userId = userId;
        this.relativeUserId = relativeUserId;
        this.invitationId = invitationId;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getRelativeUserId() {
        return relativeUserId;
    }

    public UUID getInvitationId() {
        return invitationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
