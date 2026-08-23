package com.vidacotidiana.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

    /** ADR-015/FR-014/FR-016: independent, only-additive booleans — "activar" a mode, never "desactivar" (open TBD in ADR-015). */
    @Column(name = "personal_enabled", nullable = false)
    private boolean personalEnabled = false;

    @Column(name = "laboral_enabled", nullable = false)
    private boolean laboralEnabled = false;

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

    public Instant getDeletionRequestedAt() {
        return deletionRequestedAt;
    }

    public Instant getPurgeAt() {
        return purgeAt;
    }

    public boolean isPersonalEnabled() {
        return personalEnabled;
    }

    public boolean isLaboralEnabled() {
        return laboralEnabled;
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

    /**
     * UC-15/FR-016/ADR-015: enables a mode — only-additive by design, there
     * is no matching "disable" (ADR-015 lists this as an open, explicitly
     * non-blocking TBD; not implemented here since it isn't decided). Not
     * enum-typed on purpose: "PERSONAL"/"LABORAL" are validated once at the
     * API boundary (see user.api.dto.EnableModeRequest), matching how
     * Reminder.status/ReminderContext are handled elsewhere in this
     * codebase — a domain-level IllegalArgumentException here is a safety
     * net, not the primary validation.
     */
    public void enableMode(String mode) {
        switch (mode) {
            case "PERSONAL" -> this.personalEnabled = true;
            case "LABORAL" -> this.laboralEnabled = true;
            default -> throw new IllegalArgumentException("Unknown mode: " + mode);
        }
        this.updatedAt = Instant.now();
    }

    /** UC-13/AC-015/DEC-015: 30-day grace period before purge. */
    public void requestDeletion() {
        Instant now = Instant.now();
        this.deletionStatus = "PENDING_DELETION";
        this.deletionRequestedAt = now;
        this.purgeAt = now.plus(30, ChronoUnit.DAYS);
        this.updatedAt = now;
    }

    /**
     * UC-13 step 4/AC-015. Irreversibly anonymizes personal data on this
     * USER row only — REMINDER/REMINDER_SHARE/INVITATION belonging to this
     * user are untouched (no approved decision specifies their fate on
     * purge; inventing one would violate the no-new-requirements rule, see
     * docs/development/01-technical-backlog.md BE-027). email is
     * regenerated (not nulled) because it has a UNIQUE constraint and this
     * method must remain safe to run repeatedly across different users.
     */
    public void purge() {
        this.email = "deleted-" + this.id + "@purged.invalid";
        this.username = null;
        this.deletionStatus = "DELETED";
        this.updatedAt = Instant.now();
    }
}
