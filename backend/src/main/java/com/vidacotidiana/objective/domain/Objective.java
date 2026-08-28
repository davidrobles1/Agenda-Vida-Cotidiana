package com.vidacotidiana.objective.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps to the OBJECTIVE entity in Documentacion/09-data-model.md
 * (ADR-016 adenda Fase 3e1, FR-031). Owner-only, same shape as
 * person.domain.Person — optimistic locking via @Version, no collaborator
 * concept.
 *
 * Standalone by design in this increment: no link to PROJECT/PERSON
 * (explicitly out of scope in FR-031). {@code currentValue} is updated
 * manually by the user and {@code completed} is set explicitly — neither is
 * ever derived from the other (AC-018), so there is no automatic progress
 * calculation here.
 */
@Entity
@Table(name = "objectives")
public class Objective {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String title;

    @Column(name = "target_value")
    private Integer targetValue;

    @Column(name = "current_value", nullable = false)
    private int currentValue;

    @Column
    private Instant deadline;

    @Column(nullable = false)
    private boolean completed;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Objective() {
        // JPA
    }

    public Objective(UUID ownerUserId, String title, Integer targetValue, Integer currentValue, Instant deadline) {
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.targetValue = targetValue;
        this.currentValue = currentValue != null ? currentValue : 0;
        this.deadline = deadline;
        this.completed = false;
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

    public String getTitle() {
        return title;
    }

    public Integer getTargetValue() {
        return targetValue;
    }

    public int getCurrentValue() {
        return currentValue;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public boolean isCompleted() {
        return completed;
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

    /**
     * Partial update — a null argument leaves the corresponding field
     * unchanged, same pattern as Person#applyEdit. {@code completed} is a
     * plain field here: marking an objective done is an explicit user action
     * through this same PATCH (AC-018), not a separate endpoint, because
     * unlike Reminder#complete it carries no cross-user visibility rule.
     */
    public void applyEdit(String title, Integer targetValue, Integer currentValue, Instant deadline, Boolean completed) {
        if (title != null) {
            this.title = title;
        }
        if (targetValue != null) {
            this.targetValue = targetValue;
        }
        if (currentValue != null) {
            this.currentValue = currentValue;
        }
        if (deadline != null) {
            this.deadline = deadline;
        }
        if (completed != null) {
            this.completed = completed;
        }
        this.updatedAt = Instant.now();
    }
}
