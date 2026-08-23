package com.vidacotidiana.visionboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * BLOQUE B (post-MVP): the physical bytes for an uploaded/pasted/dropped
 * Vision Board image — see V10__vision_board_images.sql's doc comment for
 * why this is Postgres BYTEA rather than S3/object storage, and
 * VisionBoardImageService's doc comment for the upload/read flow. Owned
 * directly by the uploading user, independent of any one board (an IMAGE
 * element's {@code data.imageId} just references a row here — see
 * VisionBoardElement's own doc comment on `data`).
 */
@Entity
@Table(name = "vision_board_images")
public class VisionBoardImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /** No `@Lob` on purpose — Hibernate 6 maps a plain `byte[]` column to
        Postgres `bytea` by default (matches V10's `data BYTEA`); `@Lob`
        would instead map to the JDBC large-object (`oid`) type, which the
        migration deliberately doesn't use (no separate large-object table,
        simpler backup/restore story for images this size). */
    @Column(nullable = false)
    private byte[] data;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected VisionBoardImage() {
        // JPA
    }

    public VisionBoardImage(UUID ownerUserId, String contentType, byte[] data) {
        this.ownerUserId = ownerUserId;
        this.contentType = contentType;
        this.data = data;
        this.sizeBytes = data.length;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.ownerUserId.equals(userId);
    }
}
