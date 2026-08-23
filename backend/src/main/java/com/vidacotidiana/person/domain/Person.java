package com.vidacotidiana.person.domain;

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
 * Maps to the PERSON entity in Documentacion/09-data-model.md (ADR-016).
 * Owner-only, same shape as warranty.domain.Warranty: no collaborator
 * concept, optimistic locking via @Version. {@code organization} is a plain
 * string, not a foreign key — ADR-016(c) deliberately does not introduce an
 * ORGANIZATION entity in this phase (data minimization, NFR-011).
 */
@Entity
@Table(name = "people")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String name;

    @Column
    private String role;

    @Column
    private String organization;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Person() {
        // JPA
    }

    public Person(UUID ownerUserId, String name, String role, String organization) {
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.role = role;
        this.organization = organization;
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

    public String getRole() {
        return role;
    }

    public String getOrganization() {
        return organization;
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

    /** Partial update — a null argument leaves the corresponding field unchanged, same pattern as Warranty#applyEdit. */
    public void applyEdit(String name, String role, String organization) {
        if (name != null) {
            this.name = name;
        }
        if (role != null) {
            this.role = role;
        }
        if (organization != null) {
            this.organization = organization;
        }
        this.updatedAt = Instant.now();
    }
}
