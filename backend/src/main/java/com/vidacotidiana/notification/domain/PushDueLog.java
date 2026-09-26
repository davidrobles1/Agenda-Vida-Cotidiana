package com.vidacotidiana.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Constancia de que un aviso de vencimiento ya salió.
 *
 * Su clave primaria ES la regla de negocio: un aviso por recurso y día. Ver
 * el comentario de la migración V40 sobre por qué se apoya en la colisión de
 * clave en vez de en un bloqueo.
 */
@Entity
@Table(name = "push_due_log")
@IdClass(PushDueLog.Key.class)
public class PushDueLog {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    @Id
    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Id
    @Column(name = "notified_on", nullable = false)
    private LocalDate notifiedOn;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected PushDueLog() {
        // JPA
    }

    public PushDueLog(UUID userId, String resourceType, UUID resourceId, LocalDate notifiedOn) {
        this.userId = userId;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.notifiedOn = notifiedOn;
        this.createdAt = Instant.now();
    }

    /** Clave compuesta. JPA la exige como clase aparte. */
    public static class Key implements Serializable {
        private UUID userId;
        private String resourceType;
        private UUID resourceId;
        private LocalDate notifiedOn;

        public Key() {
        }

        public Key(UUID userId, String resourceType, UUID resourceId, LocalDate notifiedOn) {
            this.userId = userId;
            this.resourceType = resourceType;
            this.resourceId = resourceId;
            this.notifiedOn = notifiedOn;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Key other)) {
                return false;
            }
            return Objects.equals(userId, other.userId)
                    && Objects.equals(resourceType, other.resourceType)
                    && Objects.equals(resourceId, other.resourceId)
                    && Objects.equals(notifiedOn, other.notifiedOn);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, resourceType, resourceId, notifiedOn);
        }
    }
}
