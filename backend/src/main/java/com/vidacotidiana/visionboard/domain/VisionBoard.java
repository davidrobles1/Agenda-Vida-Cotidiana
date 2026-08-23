package com.vidacotidiana.visionboard.domain;

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
 * A Vision Board — the canvas itself. Owner-only, no sharing/collaborator
 * concept (same reasoning as note.domain.Note/warranty.domain.Warranty: no
 * corresponding "share a board" UI exists for the MVP). Elements live in
 * their own table (VisionBoardElement, FK board_id ON DELETE CASCADE) rather
 * than as a JSON column here — see VisionBoardElement's own doc comment for
 * why.
 */
@Entity
@Table(name = "vision_boards")
public class VisionBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    /** FASE 16: the board's own theme — independent of the app's global
        visual theme. Never null (V9 migration backfilled/defaulted every
        row to LIGHT); the actual color palette per theme lives entirely on
        the frontend (visionBoardThemes.ts), same "stable id, not an asset/
        value" rule already used for icon_id/sticker_id elsewhere in this
        schema — this column only ever stores the id. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisionBoardTheme theme;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VisionBoard() {
        // JPA
    }

    /** {@code theme} must be non-null by the time this is called — the
        service layer is what defaults a missing/omitted theme to LIGHT
        (VisionBoardService#create), the same place that already shapes
        every other creation input. */
    public VisionBoard(UUID ownerUserId, String name, String description, int width, int height,
                        VisionBoardTheme theme) {
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.description = description;
        this.width = width;
        this.height = height;
        this.theme = theme;
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

    public String getDescription() {
        return description;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public VisionBoardTheme getTheme() {
        return theme;
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
     * Partial update — a null argument leaves that field unchanged, same
     * convention as Reminder#applyEdit/Note#applyEdit. width/height are
     * boxed (Integer) here only so "unchanged" can be expressed; the
     * underlying columns stay primitive/NOT NULL.
     */
    public void applyEdit(String name, String description, Integer width, Integer height, VisionBoardTheme theme) {
        if (name != null) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (width != null) {
            this.width = width;
        }
        if (height != null) {
            this.height = height;
        }
        if (theme != null) {
            this.theme = theme;
        }
        this.updatedAt = Instant.now();
    }
}
