package com.vidacotidiana.document.domain;

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
 * Módulo Documentos (pedido explícito del usuario, 2026-08-22) — ver
 * V12__documents.sql para el razonamiento de almacenamiento (mismo patrón
 * BYTEA que visionboard.domain.VisionBoardImage) y de compartir
 * (shareWith/makePublic/makePrivate, sin flujo de invitación pendiente).
 *
 * NOTA: a diferencia de VisionBoardImage, `data` NO lleva `@Lob` aquí
 * tampoco — Hibernate 6 mapea `byte[]` plano a `bytea` de Postgres por
 * defecto; `@Lob` fue la causa real de un bug ya diagnosticado y corregido
 * en VisionBoardImage esta misma sesión (mismatch bytea/oid), así que
 * nunca se agrega aquí desde el principio.
 */
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentCategory category;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private byte[] data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentVisibility visibility = DocumentVisibility.PRIVATE;

    @Column(name = "shared_with_email")
    private String sharedWithEmail;

    @Column(name = "shared_with_user_id")
    private UUID sharedWithUserId;

    /** ADR-016 Fase 3b/FR-030 (candidato V4). Nullable — vínculo opcional a una Persona propia, validado en DocumentService, no aquí. */
    @Column(name = "person_id")
    private UUID personId;

    /** ADR-016 Fase 3b/FR-030 (candidato V4). Nullable — vínculo opcional a un Proyecto propio, validado en DocumentService, no aquí. */
    @Column(name = "project_id")
    private UUID projectId;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Document() {
        // JPA
    }

    /** Kept alongside the 7-arg constructor (personId/projectId absent) for the same reason as Reminder's/Note's own overloads. */
    public Document(UUID ownerUserId, String name, DocumentCategory category, String contentType, byte[] data) {
        this(ownerUserId, name, category, contentType, data, null, null);
    }

    /** ADR-016 Fase 3b/FR-030: personId/projectId, both optional. */
    public Document(UUID ownerUserId, String name, DocumentCategory category, String contentType, byte[] data,
                     UUID personId, UUID projectId) {
        this.ownerUserId = ownerUserId;
        this.name = name;
        this.category = category;
        this.contentType = contentType;
        this.data = data;
        this.sizeBytes = data.length;
        this.visibility = DocumentVisibility.PRIVATE;
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

    public DocumentCategory getCategory() {
        return category;
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

    public DocumentVisibility getVisibility() {
        return visibility;
    }

    public String getSharedWithEmail() {
        return sharedWithEmail;
    }

    public UUID getSharedWithUserId() {
        return sharedWithUserId;
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

    /** Dueño, o el destinatario exacto de un SHARED, o cualquiera si es FAMILY_PUBLIC. */
    public boolean isVisibleTo(UUID userId) {
        if (isOwnedBy(userId)) {
            return true;
        }
        if (visibility == DocumentVisibility.FAMILY_PUBLIC) {
            return true;
        }
        return visibility == DocumentVisibility.SHARED && userId.equals(sharedWithUserId);
    }

    public void applyEdit(String name, DocumentCategory category) {
        applyEdit(name, category, null, null);
    }

    /** ADR-016 Fase 3b/FR-030: personId/projectId follow the same null-means-unchanged partial contract as name/category. */
    public void applyEdit(String name, DocumentCategory category, UUID personId, UUID projectId) {
        if (name != null) {
            this.name = name;
        }
        if (category != null) {
            this.category = category;
        }
        if (personId != null) {
            this.personId = personId;
        }
        if (projectId != null) {
            this.projectId = projectId;
        }
        this.updatedAt = Instant.now();
    }

    /** email siempre se guarda (lo que el dueño escribió, para su propia vista) —
        resolvedUserId solo si ese correo coincidió con una cuenta real; nunca se
        revela al llamador si hubo match o no (SEC-001, no-enumeration — mismo
        principio que sharing.application.SharingService#createInvitation ya
        aplica para reminders). */
    public void shareWith(String email, UUID resolvedUserId) {
        this.visibility = DocumentVisibility.SHARED;
        this.sharedWithEmail = email;
        this.sharedWithUserId = resolvedUserId;
        this.updatedAt = Instant.now();
    }

    public void makePublic() {
        this.visibility = DocumentVisibility.FAMILY_PUBLIC;
        this.sharedWithEmail = null;
        this.sharedWithUserId = null;
        this.updatedAt = Instant.now();
    }

    public void makePrivate() {
        this.visibility = DocumentVisibility.PRIVATE;
        this.sharedWithEmail = null;
        this.sharedWithUserId = null;
        this.updatedAt = Instant.now();
    }
}
