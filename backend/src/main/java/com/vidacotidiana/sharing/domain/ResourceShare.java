package com.vidacotidiana.sharing.domain;

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
 * La RELACIÓN de compartición sobre un recurso que ya existe (ADR-025 §2,
 * tabla {@code resource_shares} de V29).
 *
 * El recurso NO se duplica: aquí solo vive quién comparte, con quién, sobre
 * qué, con qué responsabilidad y en qué estado. El registro original —
 * recordatorio, mantenimiento, pago, garantía, artículo o documento— no se
 * toca ni gana columnas, así que compartir no puede romper nada de lo que ya
 * funcionaba en su módulo.
 *
 * IMPORTANTE sobre {@code partDoneAt}: es la parte de ESTE colaborador, no el
 * estado del recurso. {@code ReminderStatus} sigue siendo un único estado
 * global compartido por dueño y colaboradores (DEC-001, que prohíbe
 * expresamente añadirle estados). Que alguien marque su parte no completa la
 * tarea de nadie más; solo informa al dueño.
 */
@Entity
@Table(name = "resource_shares")
public class ResourceShare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private SharedResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "collaborator_user_id", nullable = false)
    private UUID collaboratorUserId;

    @Column(nullable = false)
    private boolean responsibility;

    @Column(name = "part_done_at")
    private Instant partDoneAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceShareStatus status;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ResourceShare() {
        // JPA
    }

    public ResourceShare(SharedResourceType resourceType, UUID resourceId, UUID ownerUserId,
                          UUID collaboratorUserId, boolean responsibility) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.ownerUserId = ownerUserId;
        this.collaboratorUserId = collaboratorUserId;
        this.responsibility = responsibility;
        this.status = ResourceShareStatus.ACTIVE;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public SharedResourceType getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public UUID getCollaboratorUserId() {
        return collaboratorUserId;
    }

    public boolean hasResponsibility() {
        return responsibility;
    }

    public Instant getPartDoneAt() {
        return partDoneAt;
    }

    public ResourceShareStatus getStatus() {
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

    public boolean isActive() {
        return this.status == ResourceShareStatus.ACTIVE;
    }

    public boolean isPartDone() {
        return this.partDoneAt != null;
    }

    /** Cambia si esta persona queda comprometida con su parte, o solo la ve. */
    public void setResponsibility(boolean responsibility) {
        if (!responsibility) {
            // Sin responsabilidad no puede quedar una parte "hecha" colgando:
            // sería un estado que la restricción ck_resource_shares_part_done
            // rechaza, y además no significaría nada.
            this.partDoneAt = null;
        }
        this.responsibility = responsibility;
        this.updatedAt = Instant.now();
    }

    /** "Ya hice mi parte". Idempotente: repetirlo no mueve la fecha original. */
    public void markPartDone() {
        if (this.partDoneAt == null) {
            this.partDoneAt = Instant.now();
            this.updatedAt = this.partDoneAt;
        }
    }

    /** Deshace lo anterior — la salida de un toque equivocado. */
    public void reopenPart() {
        if (this.partDoneAt != null) {
            this.partDoneAt = null;
            this.updatedAt = Instant.now();
        }
    }

    /** Inmediato, sin ventana de gracia (mismo criterio que ReminderShare#revoke). */
    public void revoke() {
        this.status = ResourceShareStatus.REVOKED;
        this.partDoneAt = null;
        this.responsibility = false;
        this.updatedAt = Instant.now();
    }
}
