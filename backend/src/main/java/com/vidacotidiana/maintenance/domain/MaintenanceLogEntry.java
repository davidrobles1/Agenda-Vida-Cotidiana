package com.vidacotidiana.maintenance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * ADR-021: una ejecución de un mantenimiento.
 *
 * Mismo papel que `PaymentRecord` en Pagos y por el mismo motivo: al
 * completar una ocurrencia la fecha del registro AVANZA, así que sin este
 * historial se perdería el rastro de que la anterior se hizo.
 *
 * `scheduledDate` es la fecha que estaba programada al completar —no la
 * fecha real— y es lo que sostiene el deshacer: permite devolver el
 * registro exactamente a donde estaba. `completedDate` es cuándo se hizo de
 * verdad; comparadas dan el retraso.
 */
@Entity
@Table(name = "maintenance_log")
public class MaintenanceLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "maintenance_record_id", nullable = false)
    private UUID maintenanceRecordId;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "completed_date", nullable = false)
    private LocalDate completedDate;

    @Column
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MaintenanceLogEntry() {
        // JPA
    }

    public MaintenanceLogEntry(UUID maintenanceRecordId, UUID ownerUserId, LocalDate scheduledDate,
                               LocalDate completedDate, String note) {
        this.maintenanceRecordId = maintenanceRecordId;
        this.ownerUserId = ownerUserId;
        this.scheduledDate = scheduledDate;
        this.completedDate = completedDate;
        this.note = note;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getMaintenanceRecordId() {
        return maintenanceRecordId;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public LocalDate getCompletedDate() {
        return completedDate;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
