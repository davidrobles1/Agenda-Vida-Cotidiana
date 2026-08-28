package com.vidacotidiana.resource.domain;

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
 * Maps to the RESOURCE entity in Documentacion/09-data-model.md (ADR-016
 * adenda Fase 3e4, FR-034). Owner-only, same shape as person.domain.Person.
 *
 * <p><b>DECISION (Product Owner, 2026-08-28 — opción A):</b> {@code
 * reference} is a single free-text field, not a validated {@code url} plus a
 * separate textual reference. Several approved types (MANUAL, PLANTILLA,
 * HERRAMIENTA) often have no URL at all, and splitting the field would leave
 * them with a permanently empty column.
 *
 * <p>This entity stores <b>no file</b> — only metadata and that text
 * reference. DOCUMENT (FR-030) remains the only entity responsible for real
 * documents; FR-034 explicitly rules out new storage, versioning and shared
 * permissions here.
 *
 * <p>{@code personId}/{@code projectId} are optional and, when present, must
 * reference resources owned by the same user — enforced in
 * resource.application.ResourceService, same split as Note/Document.
 */
@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType type;

    @Column
    private String reference;

    @Column
    private String description;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "project_id")
    private UUID projectId;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Resource() {
        // JPA
    }

    public Resource(UUID ownerUserId, String name, ResourceType type, String reference, String description,
                    UUID personId, UUID projectId) {
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.type = type;
        this.reference = reference;
        this.description = description;
        this.personId = personId;
        this.projectId = projectId;
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

    public ResourceType getType() {
        return type;
    }

    public String getReference() {
        return reference;
    }

    public String getDescription() {
        return description;
    }

    public UUID getPersonId() {
        return personId;
    }

    public UUID getProjectId() {
        return projectId;
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

    /** Partial update — a null argument leaves the corresponding field unchanged. personId/projectId ownership is validated by the caller (ResourceService) before this runs. */
    public void applyEdit(String name, ResourceType type, String reference, String description,
                          UUID personId, UUID projectId) {
        if (name != null) {
            this.name = name;
        }
        if (type != null) {
            this.type = type;
        }
        if (reference != null) {
            this.reference = reference;
        }
        if (description != null) {
            this.description = description;
        }
        if (personId != null) {
            this.personId = personId;
        }
        if (projectId != null) {
            this.projectId = projectId;
        }
        this.updatedAt = Instant.now();
    }
}
