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

    /** ADR-015/FR-019. Inferred from navbar of origin — see ReminderService.create. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderContext context;

    /** Nullable — stores only the stable catalog id (e.g. "birthday"), never
        an asset path or the emoji itself. See note.domain.Note's own doc
        comment; same catalog, same resolution rule, shared frontend
        component (web/src/core/ui/pickers/pickerCatalog.ts). */
    @Column(name = "icon_id")
    private String iconId;

    @Column(name = "sticker_id")
    private String stickerId;

    /** ADR-016/FR-023. Nullable — optional link to a Persona owned by the same user, validated in ReminderService, not here. */
    @Column(name = "person_id")
    private UUID personId;

    /** ADR-016/FR-023. Nullable — optional link to a Proyecto owned by the same user, validated in ReminderService, not here. */
    @Column(name = "project_id")
    private UUID projectId;

    /** ADR-016/FR-024. Nullable free text — only meaningful for a "reunión" (context=LABORAL with location set), not enforced at the database level (business rule, not a constraint). */
    @Column
    private String location;

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

    /** Defaults to PERSONAL — see ReminderContext's own doc comment on why. Kept alongside the other constructors so every existing call site (tests included) keeps compiling unchanged. */
    public Reminder(UUID ownerUserId, String title, String description, Instant dueAt) {
        this(ownerUserId, title, description, dueAt, ReminderContext.PERSONAL);
    }

    /** Kept alongside the 7-arg constructor (iconId/stickerId absent) for the same reason as the 4-arg one above. */
    public Reminder(UUID ownerUserId, String title, String description, Instant dueAt, ReminderContext context) {
        this(ownerUserId, title, description, dueAt, context, null, null);
    }

    /** Kept alongside the 10-arg constructor (personId/projectId/location absent) for the same reason as the 4-arg one above. */
    public Reminder(UUID ownerUserId, String title, String description, Instant dueAt, ReminderContext context,
                     String iconId, String stickerId) {
        this(ownerUserId, title, description, dueAt, context, iconId, stickerId, null, null, null);
    }

    /** ADR-016/FR-023/FR-024: personId/projectId/location, all optional. */
    public Reminder(UUID ownerUserId, String title, String description, Instant dueAt, ReminderContext context,
                     String iconId, String stickerId, UUID personId, UUID projectId, String location) {
        this.ownerUserId = ownerUserId;
        this.title = title;
        this.description = description;
        this.dueAt = dueAt;
        this.status = ReminderStatus.PENDING;
        this.context = context;
        this.iconId = iconId;
        this.stickerId = stickerId;
        this.personId = personId;
        this.projectId = projectId;
        this.location = location;
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

    public ReminderContext getContext() {
        return context;
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

    public String getLocation() {
        return location;
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

    /**
     * AC-004b: partial update — a null argument leaves title/description/
     * dueAt unchanged. Version comparison happens in ReminderService before
     * this is called; this method only applies the already-authorized edit.
     * iconId/stickerId are always overwritten with whatever the caller
     * sends (including null, to clear one) — same reasoning as
     * note.domain.Note#applyEdit: the edit form is fully controlled and
     * always submits its complete current selection.
     */
    public void applyEdit(String title, String description, Instant dueAt, String iconId, String stickerId) {
        applyEdit(title, description, dueAt, iconId, stickerId, null, null, null);
    }

    /**
     * ADR-016/FR-023/FR-024: personId/projectId/location follow the same
     * null-means-unchanged partial-update contract as title/description/
     * dueAt (unlike iconId/stickerId, which are always overwritten as sent) —
     * there is no product requirement yet to explicitly unlink a Persona/
     * Proyecto or clear a location via PATCH, an accepted limitation for V3.
     */
    public void applyEdit(String title, String description, Instant dueAt, String iconId, String stickerId,
                           UUID personId, UUID projectId, String location) {
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (dueAt != null) {
            this.dueAt = dueAt;
        }
        this.iconId = iconId;
        this.stickerId = stickerId;
        if (personId != null) {
            this.personId = personId;
        }
        if (projectId != null) {
            this.projectId = projectId;
        }
        if (location != null) {
            this.location = location;
        }
        this.updatedAt = Instant.now();
    }
}
