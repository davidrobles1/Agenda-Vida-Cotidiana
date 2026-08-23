package com.vidacotidiana.commitment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps to the COMMITMENT entity in Documentacion/09-data-model.md (ADR-016,
 * FR-025/FR-027). "Seguimientos" = commitments with direction MINE;
 * "Esperando" = commitments with direction THEIRS — same entity, filtered
 * by direction, never two tables (see ADR-016 Alternativas).
 *
 * ASSUMPTION (V11__adr016_laboral_module.sql, not a Product Owner decision —
 * ADR-016 left this an explicit TBD): personId is NOT NULL. Every example in
 * 34-laboral-module-proposal.md pairs a commitment with a person; relaxing
 * this to nullable later is backward-compatible (no existing row or
 * consumer breaks), the reverse would not be.
 */
@Entity
@Table(name = "commitments")
public class Commitment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommitmentDirection direction;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CommitmentStatus status = CommitmentStatus.OPEN;

    @Column(name = "origin_reminder_id")
    private UUID originReminderId;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Commitment() {
        // JPA
    }

    public Commitment(UUID ownerUserId, UUID personId, String description, CommitmentDirection direction,
                       Instant dueAt, UUID projectId, UUID originReminderId) {
        this.ownerUserId = ownerUserId;
        this.personId = personId;
        this.description = description;
        this.direction = direction;
        this.dueAt = dueAt;
        this.projectId = projectId;
        this.originReminderId = originReminderId;
        this.status = CommitmentStatus.OPEN;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getDescription() {
        return description;
    }

    public CommitmentDirection getDirection() {
        return direction;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public CommitmentStatus getStatus() {
        return status;
    }

    public UUID getOriginReminderId() {
        return originReminderId;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.ownerUserId.equals(userId);
    }

    /** UC-20: marks the commitment resolved. Idempotent — resolving an already-DONE commitment is a no-op. */
    public void resolve() {
        this.status = CommitmentStatus.DONE;
        this.updatedAt = Instant.now();
    }

    /**
     * Partial update — a null argument leaves the corresponding field
     * unchanged. {@code direction} is intentionally editable: flipping who
     * must act next is the entire point of the unified model (ADR-016), not
     * a special case. personId/projectId ownership is validated by the
     * caller (CommitmentService) before this runs.
     */
    public void applyEdit(UUID personId, String description, CommitmentDirection direction, Instant dueAt, UUID projectId) {
        if (personId != null) {
            this.personId = personId;
        }
        if (description != null) {
            this.description = description;
        }
        if (direction != null) {
            this.direction = direction;
        }
        if (dueAt != null) {
            this.dueAt = dueAt;
        }
        if (projectId != null) {
            this.projectId = projectId;
        }
        this.updatedAt = Instant.now();
    }
}
