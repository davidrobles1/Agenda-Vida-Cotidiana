package com.vidacotidiana.maintenance.domain;

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
 * Maps to the MAINTENANCE_RECORD entity in Documentacion/09-data-model.md
 * (BE-037). Mirrors warranty.domain.Warranty exactly — see that class's
 * javadoc for the reasoning shared by both. Field names match what
 * web/src/core/mock/mockData.ts's MockMaintenanceRecord already specifies:
 * item, nextDueAt, status.
 */
@Entity
@Table(name = "maintenance_records")
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false)
    private String item;

    @Column(name = "next_due_at", nullable = false)
    private Instant nextDueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceStatus status = MaintenanceStatus.ACTIVE;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MaintenanceRecord() {
        // JPA
    }

    public MaintenanceRecord(UUID ownerUserId, String item, Instant nextDueAt) {
        this.ownerUserId = ownerUserId;
        this.item = item;
        this.nextDueAt = nextDueAt;
        this.status = MaintenanceStatus.ACTIVE;
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

    public String getItem() {
        return item;
    }

    public Instant getNextDueAt() {
        return nextDueAt;
    }

    public MaintenanceStatus getStatus() {
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

    public boolean isOwnedBy(UUID userId) {
        return this.ownerUserId.equals(userId);
    }

    /** Toggles ACTIVE<->COMPLETED, idempotent by design — mirrors Warranty#toggleCompletion. */
    public void toggleCompletion() {
        this.status = (this.status == MaintenanceStatus.ACTIVE) ? MaintenanceStatus.COMPLETED : MaintenanceStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    /** Partial update — a null argument leaves the corresponding field unchanged. */
    public void applyEdit(String item, Instant nextDueAt) {
        if (item != null) {
            this.item = item;
        }
        if (nextDueAt != null) {
            this.nextDueAt = nextDueAt;
        }
        this.updatedAt = Instant.now();
    }
}
