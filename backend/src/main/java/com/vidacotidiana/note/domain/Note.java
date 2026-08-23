package com.vidacotidiana.note.domain;

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
 * Maps to the NOTE entity in Documentacion/09-data-model.md. Mirrors
 * warranty.domain.Warranty's shape exactly (owner, optimistic locking via
 * @Version) — owner-only, no sharing/collaborator concept (same reasoning
 * as WarrantyRepository: no corresponding "share a note" UI exists).
 *
 * iconId/stickerId store only the stable catalog id (e.g. "birthday"),
 * never an asset path or the emoji itself — resolution against the real
 * asset lives entirely client-side (web/src/core/ui/pickers/pickerCatalog.ts).
 * Both nullable: a note simply may not have one yet, no default to assign.
 */
@Entity
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(name = "icon_id")
    private String iconId;

    @Column(name = "sticker_id")
    private String stickerId;

    /** ADR-016 Fase 3a/FR-029 (candidato V4). Nullable — vínculo opcional a una Persona propia, validado en NoteService, no aquí. */
    @Column(name = "person_id")
    private UUID personId;

    /** ADR-016 Fase 3a/FR-029 (candidato V4). Nullable — vínculo opcional a un Proyecto propio, validado en NoteService, no aquí. */
    @Column(name = "project_id")
    private UUID projectId;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Note() {
        // JPA
    }

    /** Kept alongside the 7-arg constructor (personId/projectId absent) for the same reason as Reminder's own 7-arg overload. */
    public Note(UUID ownerUserId, String title, String description, String iconId, String stickerId) {
        this(ownerUserId, title, description, iconId, stickerId, null, null);
    }

    /** ADR-016 Fase 3a/FR-029: personId/projectId, both optional. */
    public Note(UUID ownerUserId, String title, String description, String iconId, String stickerId,
                UUID personId, UUID projectId) {
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.description = (description != null) ? description : "";
        this.iconId = iconId;
        this.stickerId = stickerId;
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

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getIconId() {
        return iconId;
    }

    public String getStickerId() {
        return stickerId;
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

    /**
     * Partial update for title/description (mirrors Warranty#applyEdit/
     * Reminder#applyEdit: a null argument leaves the field unchanged).
     * iconId/stickerId are always overwritten with whatever the caller
     * sends (including null, to clear one) — the edit form is a fully
     * controlled component that always submits its complete current
     * selection, never a partial one, so there is no "leave unchanged"
     * case to preserve for these two fields.
     */
    public void applyEdit(String title, String description, String iconId, String stickerId) {
        applyEdit(title, description, iconId, stickerId, null, null);
    }

    /**
     * ADR-016 Fase 3a/FR-029: personId/projectId follow the same
     * null-means-unchanged partial contract as title/description (unlike
     * iconId/stickerId) — no product requirement yet to explicitly unlink
     * via PATCH, same accepted limitation as Reminder#applyEdit.
     */
    public void applyEdit(String title, String description, String iconId, String stickerId, UUID personId, UUID projectId) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        this.iconId = iconId;
        this.stickerId = stickerId;
        if (personId != null) {
            this.personId = personId;
        }
        if (projectId != null) {
            this.projectId = projectId;
        }
        this.updatedAt = Instant.now();
    }
}
