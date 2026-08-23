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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * One element placed on a VisionBoard (FASE 1/6: text/image/note/sticker/
 * shape). Deliberately a real table (own id, own @Version) rather than a
 * JSON array embedded in VisionBoard, for reasons that matter for this
 * feature specifically, not as a default choice:
 * <ul>
 *   <li>FASE 2's API addresses elements individually
 *       (POST/PUT/DELETE .../elements/{elementId}) — that maps directly to
 *       rows with their own id, not entries inside a blob.</li>
 *   <li>Per-element optimistic locking: two different elements on the same
 *       board can be edited (dragged, resized) concurrently without
 *       contending on one shared board-level version — a JSON-blob board
 *       would serialize every element edit through the board's own
 *       @Version, causing spurious 409s between unrelated elements during
 *       normal use (e.g. two drags in quick succession).</li>
 *   <li>FASE 8 (layers) and FASE 9 (undo/redo) both want discrete,
 *       independently-addressable element rows to reorder/diff/replay
 *       against — a blob would need to be fully re-diffed on every change.</li>
 * </ul>
 * The one place a JSON column earns its keep here is {@link #data}: the
 * payload genuinely varies per {@link VisionBoardElementType} (text has
 * font/weight/align; image has a URL/crop; shape has fill/stroke — FASE 6)
 * and doesn't need to be queried/indexed at the DB level for the MVP, so a
 * fixed relational shape for it would mean a column per type's every
 * attribute (over-engineering for an MVP) instead of one flexible bag.
 */
@Entity
@Table(name = "vision_board_elements")
public class VisionBoardElement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisionBoardElementType type;

    @Column(nullable = false)
    private double x;

    @Column(nullable = false)
    private double y;

    @Column(nullable = false)
    private double width;

    @Column(nullable = false)
    private double height;

    @Column(nullable = false)
    private double rotation;

    @Column(name = "z_index", nullable = false)
    private int zIndex;

    @Column(nullable = false)
    private boolean locked;

    @Column(nullable = false)
    private boolean visible;

    /** Type-specific payload — see class doc. Never null; defaults to an
        empty object so callers never need a null-check before reading it. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> data;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VisionBoardElement() {
        // JPA
    }

    public VisionBoardElement(UUID boardId, VisionBoardElementType type, double x, double y, double width,
                               double height, Map<String, Object> data) {
        this.boardId = boardId;
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.rotation = 0;
        this.zIndex = 0;
        this.locked = false;
        this.visible = true;
        this.data = (data != null) ? data : Map.of();
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBoardId() {
        return boardId;
    }

    public VisionBoardElementType getType() {
        return type;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getRotation() {
        return rotation;
    }

    public int getZIndex() {
        return zIndex;
    }

    public boolean isLocked() {
        return locked;
    }

    public boolean isVisible() {
        return visible;
    }

    public Map<String, Object> getData() {
        return data;
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

    /**
     * Partial update — a null argument leaves that field unchanged (same
     * convention as VisionBoard#applyEdit), {@code data} included: unlike
     * icon_id/sticker_id elsewhere in this schema, it IS null-guarded here,
     * so a caller that only cares about a subset of fields (e.g. FASE 4's
     * drag persisting only x/y) never has to resend the rest. {@code
     * locked}/{@code visible} are boxed (Boolean) for the same reason
     * width/height are boxed in VisionBoard#applyEdit: to express
     * "unchanged" on an otherwise primitive column.
     */
    public void applyEdit(Double x, Double y, Double width, Double height, Double rotation, Integer zIndex,
                           Boolean locked, Boolean visible, Map<String, Object> data) {
        if (x != null) {
            this.x = x;
        }
        if (y != null) {
            this.y = y;
        }
        if (width != null) {
            this.width = width;
        }
        if (height != null) {
            this.height = height;
        }
        if (rotation != null) {
            this.rotation = rotation;
        }
        if (zIndex != null) {
            this.zIndex = zIndex;
        }
        if (locked != null) {
            this.locked = locked;
        }
        if (visible != null) {
            this.visible = visible;
        }
        if (data != null) {
            this.data = data;
        }
        this.updatedAt = Instant.now();
    }
}
