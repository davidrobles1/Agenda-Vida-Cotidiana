package com.vidacotidiana.maintenance.domain;

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

    /**
     * Periodicidad en meses elegida por el usuario ("¿Cada cuánto?").
     * Nullable a propósito: NULL significa "mantenimiento de una sola
     * fecha", que es exactamente lo que eran todos los registros antes de
     * la migración V24 — no se inventa una periodicidad para ellos.
     */
    @Column(name = "interval_months")
    private Integer intervalMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceStatus status = MaintenanceStatus.ACTIVE;

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

    protected MaintenanceRecord() {
        // JPA
    }

    public MaintenanceRecord(UUID ownerUserId, String item, Instant nextDueAt) {
        this(ownerUserId, item, nextDueAt, null);
    }

    public MaintenanceRecord(UUID ownerUserId, String item, Instant nextDueAt, Integer intervalMonths) {
        this(ownerUserId, item, nextDueAt, intervalMonths, ModuleContext.PERSONAL);
    }

    /** ADR-019: alta con módulo propietario explícito. */
    public MaintenanceRecord(UUID ownerUserId, String item, Instant nextDueAt, Integer intervalMonths,
                             ModuleContext context) {
        this.context = (context != null) ? context : ModuleContext.PERSONAL;
        this.intervalMonths = intervalMonths;
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

    public ModuleContext getContext() {
        return context;
    }

    public String getItem() {
        return item;
    }

    public Instant getNextDueAt() {
        return nextDueAt;
    }

    public Integer getIntervalMonths() {
        return intervalMonths;
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
        applyEdit(item, nextDueAt, null);
    }

    public void applyEdit(String item, Instant nextDueAt, Integer intervalMonths) {
        if (item != null) {
            this.item = item;
        }
        if (nextDueAt != null) {
            this.nextDueAt = nextDueAt;
        }
        if (intervalMonths != null) {
            this.intervalMonths = intervalMonths;
        }
        this.updatedAt = Instant.now();
    }
}
