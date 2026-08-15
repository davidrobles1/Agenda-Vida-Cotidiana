package com.vidacotidiana.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps to the USER entity in Documentacion/09-data-model.md. id equals the
 * Keycloak `sub` claim (see identity.infrastructure.CurrentUser) — Keycloak
 * is the sole identity source of truth (DEC-004/ADR-008), the backend never
 * creates accounts of its own.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String username;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "deletion_status", nullable = false)
    private String deletionStatus = "ACTIVE";

    @Column(name = "deletion_requested_at")
    private Instant deletionRequestedAt;

    @Column(name = "purge_at")
    private Instant purgeAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
        // JPA
    }

    public User(UUID id, String email, String username) {
        this.id = id;
        this.email = email;
        this.username = username;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getStatus() {
        return status;
    }

    public String getDeletionStatus() {
        return deletionStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Keeps email/username in sync with Keycloak on every authenticated request. */
    public void refreshFromIdentityProvider(String email, String username) {
        boolean changed = false;
        if (email != null && !email.equals(this.email)) {
            this.email = email;
            changed = true;
        }
        if (username != null && !username.equals(this.username)) {
            this.username = username;
            changed = true;
        }
        if (changed) {
            this.updatedAt = Instant.now();
        }
    }
}
