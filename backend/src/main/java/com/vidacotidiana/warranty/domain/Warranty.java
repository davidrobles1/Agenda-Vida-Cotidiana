package com.vidacotidiana.warranty.domain;

import com.vidacotidiana.shared.domain.ModuleContext;

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
 * Maps to the WARRANTY entity in Documentacion/09-data-model.md (BE-037).
 * Mirrors reminder.domain.Reminder's shape (owner, optimistic locking via
 * @Version) but with fields matching what web/src/core/mock/mockData.ts's
 * MockWarranty already specifies as the real contract: item, expiresAt,
 * status. See WarrantyStatus for why the persisted status is 2-valued
 * (ACTIVE/COMPLETED) while the API-facing status has 4 values.
 */
@Entity
@Table(name = "warranties")
public class Warranty {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String item;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WarrantyStatus status = WarrantyStatus.ACTIVE;

    /**
     * ADR-019: módulo propietario. Se fija al crear y NO cambia durante el
     * ciclo de vida del recurso — `applyEdit` no lo toca a propósito.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ModuleContext context = ModuleContext.PERSONAL;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Pedido explícito del usuario (2026-08-21): "al registrar una
        garantía subir el archivo de la garantía en formato imagen o pdf" —
        ver V14__warranty_documents.sql. Nullable — una garantía sin
        archivo adjunto sigue siendo válida a nivel de dato (la validación
        de "obligatorio al crear" vive en WarrantyService). */
    @Column(name = "document_content_type")
    private String documentContentType;

    @Column(name = "document_size_bytes")
    private Long documentSizeBytes;

    @Column(name = "document_data")
    private byte[] documentData;

    protected Warranty() {
        // JPA
    }

    public Warranty(UUID ownerUserId, String item, Instant expiresAt) {
        this(ownerUserId, item, expiresAt, ModuleContext.PERSONAL);
    }

    /** ADR-019: alta con módulo propietario explícito. */
    public Warranty(UUID ownerUserId, String item, Instant expiresAt, ModuleContext context) {
        this.context = (context != null) ? context : ModuleContext.PERSONAL;
        this.ownerUserId = ownerUserId;
        this.item = item;
        this.expiresAt = expiresAt;
        this.status = WarrantyStatus.ACTIVE;
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

    public ModuleContext getContext() {
        return context;
    }

    public String getItem() {
        return item;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public WarrantyStatus getStatus() {
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

    public String getDocumentContentType() {
        return documentContentType;
    }

    public Long getDocumentSizeBytes() {
        return documentSizeBytes;
    }

    public byte[] getDocumentData() {
        return documentData;
    }

    public boolean hasDocument() {
        return documentData != null;
    }

    public void attachDocument(String contentType, byte[] data) {
        this.documentContentType = contentType;
        this.documentSizeBytes = (long) data.length;
        this.documentData = data;
        this.updatedAt = Instant.now();
    }

    public boolean isOwnedBy(UUID userId) {
        return this.ownerUserId.equals(userId);
    }

    /** Toggles ACTIVE<->COMPLETED, idempotent by design — mirrors Reminder#toggleCompletion. */
    public void toggleCompletion() {
        this.status = (this.status == WarrantyStatus.ACTIVE) ? WarrantyStatus.COMPLETED : WarrantyStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    /** Partial update — a null argument leaves the corresponding field unchanged, mirrors Reminder#applyEdit. */
    public void applyEdit(String item, Instant expiresAt) {
        if (item != null) {
            this.item = item;
        }
        if (expiresAt != null) {
            this.expiresAt = expiresAt;
        }
        this.updatedAt = Instant.now();
    }
}
