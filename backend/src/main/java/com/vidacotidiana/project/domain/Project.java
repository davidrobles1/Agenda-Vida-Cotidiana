package com.vidacotidiana.project.domain;

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
 * Maps to the PROJECT entity in Documentacion/09-data-model.md (ADR-016).
 * Owner-only, same shape as person.domain.Person. {@code status} is free
 * text in V3 (ADR-016: "texto libre en V1, TBD si se cierra a un enum") —
 * deliberately not an enum yet. {@code clientPersonId} is optional and,
 * when present, must reference a Person owned by the same user — enforced
 * in project.application.ProjectService, not here (entity stays a plain
 * data holder, no cross-aggregate lookups).
 */
@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String name;

    @Column(name = "client_person_id")
    private UUID clientPersonId;

    @Column
    private String status;

    @Column
    private Instant deadline;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Project() {
        // JPA
    }

    public Project(UUID ownerUserId, String name, UUID clientPersonId, String status, Instant deadline) {
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.clientPersonId = clientPersonId;
        this.status = status;
        this.deadline = deadline;
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

    public String getName() {
        return name;
    }

    public UUID getClientPersonId() {
        return clientPersonId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getDeadline() {
        return deadline;
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

    /** Partial update — a null argument leaves the corresponding field unchanged. clientPersonId ownership is validated by the caller (ProjectService) before this runs. */
    public void applyEdit(String name, UUID clientPersonId, String status, Instant deadline) {
        if (name != null) {
            this.name = name;
        }
        if (clientPersonId != null) {
            this.clientPersonId = clientPersonId;
        }
        if (status != null) {
            this.status = status;
        }
        if (deadline != null) {
            this.deadline = deadline;
        }
        this.updatedAt = Instant.now();
    }
}
