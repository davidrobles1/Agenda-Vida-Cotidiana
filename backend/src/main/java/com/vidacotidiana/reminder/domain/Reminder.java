package com.vidacotidiana.reminder.domain;

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
 * Maps to the REMINDER entity in Documentacion/09-data-model.md. The
 * {@code version} column implements optimistic locking (RECOMMENDATION
 * técnica, not a business decision) via JPA's native @Version support:
 * every UPDATE is guarded by "WHERE version = ?" and increments version on
 * success, which is exactly the mechanism 09-data-model.md and
 * openapi.yaml describe. Application-level pre-checks against a
 * client-supplied version (see ReminderService) produce the domain-specific
 * 409 REMINDER_VERSION_CONFLICT before Hibernate would otherwise throw a
 * generic ObjectOptimisticLockingFailureException.
 */
@Entity
@Table(name = "reminders")
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(name = "due_at")
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderStatus status = ReminderStatus.PENDING;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Reminder() {
        // JPA
    }

    public Reminder(UUID ownerUserId, String title, String description, Instant dueAt) {
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
        this.status = ReminderStatus.PENDING;
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

    public String getDescription() {
        return description;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public ReminderStatus getStatus() {
        return status;
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

    /** UC-04/AC-005: toggles the single global completion state. Idempotent by design (PENDING<->COMPLETED). */
    public void toggleCompletion() {
        this.status = (this.status == ReminderStatus.PENDING) ? ReminderStatus.COMPLETED : ReminderStatus.PENDING;
        this.updatedAt = Instant.now();
    }
}
