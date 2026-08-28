package com.vidacotidiana.place.domain;

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
 * Maps to the PLACE entity in Documentacion/09-data-model.md (ADR-016
 * adenda Fase 3e3, FR-033). Owner-only, same shape as person.domain.Person.
 *
 * A catalogue entry, not a relation: FR-033 explicitly keeps
 * {@code reminders.place_id} out of scope — picking a Place when creating a
 * Task only copies its text into the existing {@code reminders.location}
 * free-text column (FR-024), so nothing here is referenced by Reminder.
 *
 * {@code personId} is optional and, when present, must reference a Person
 * owned by the same user — enforced in place.application.PlaceService, not
 * here (same split as Project#clientPersonId).
 */
@Entity
@Table(name = "places")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String name;

    @Column
    private String address;

    @Column(name = "person_id")
    private UUID personId;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Place() {
        // JPA
    }

    public Place(UUID ownerUserId, String name, String address, UUID personId) {
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.address = address;
        this.personId = personId;
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

    public String getAddress() {
        return address;
    }

    public UUID getPersonId() {
        return personId;
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

    /** Partial update — a null argument leaves the corresponding field unchanged. personId ownership is validated by the caller (PlaceService) before this runs. */
    public void applyEdit(String name, String address, UUID personId) {
        if (name != null) {
            this.name = name;
        }
        if (address != null) {
            this.address = address;
        }
        if (personId != null) {
            this.personId = personId;
        }
        this.updatedAt = Instant.now();
    }
}
