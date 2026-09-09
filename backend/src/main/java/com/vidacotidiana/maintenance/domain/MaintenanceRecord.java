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

    /**
     * Artículo del inventario al que se le hace este mantenimiento (V31).
     * Nulo = sin enlazar, que es como quedan todos los mantenimientos
     * anteriores a esta relación y también los que no se le hacen a un
     * artículo (el techo, el jardín).
     *
     * Se guarda el id y no la entidad por el mismo motivo que en
     * {@link com.vidacotidiana.warranty.domain.Warranty}: son dos módulos
     * independientes y el vínculo es una referencia, no una composición.
     */
    @Column(name = "inventory_item_id")
    private UUID inventoryItemId;

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

    public UUID getInventoryItemId() {
        return inventoryItemId;
    }

    /**
     * Enlaza o desenlaza el artículo al que se le hace este mantenimiento.
     * `null` desenlaza de forma explícita, a diferencia de los campos de
     * `applyEdit`, donde `null` significa "no tocar" — misma distinción y
     * mismo motivo que {@code Warranty#linkInventoryItem}.
     */
    public void linkInventoryItem(UUID inventoryItemId) {
        this.inventoryItemId = inventoryItemId;
        this.updatedAt = Instant.now();
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

    /**
     * ADR-021: completa la ocurrencia actual.
     *
     * Sustituye funcionalmente a `toggleCompletion` para los mantenimientos
     * RECURRENTES, que antes quedaban COMPLETADO para siempre pese a tener
     * periodicidad guardada. Ahora:
     *
     *   - con `intervalMonths`, la fecha AVANZA un intervalo y el registro
     *     sigue activo: eso es lo que significa "cada 3 meses";
     *   - sin `intervalMonths`, el mantenimiento es puntual y sí termina.
     *
     * REGLA DEL VENCIDO (aprobada por el Product Owner, 2026-08-29): si la
     * fecha ya pasó, el siguiente intervalo se cuenta **desde hoy**, no
     * desde la fecha incumplida. Es lo contrario de lo que hace Pagos, y a
     * propósito: una mensualidad de septiembre sigue siendo la de
     * septiembre, pero si cambias el aceite con dos meses de retraso el
     * siguiente cambio toca tres meses después de hoy — el intervalo mide
     * desgaste, no calendario.
     *
     * Devuelve la fecha que estaba programada, que es lo que el historial
     * necesita guardar para poder deshacer.
     */
    public java.time.Instant completeOccurrence(java.time.Instant now) {
        java.time.Instant scheduled = this.nextDueAt;

        if (this.intervalMonths == null || this.intervalMonths < 1) {
            this.status = MaintenanceStatus.COMPLETED;
            this.updatedAt = Instant.now();
            return scheduled;
        }

        java.time.ZonedDateTime base = (this.nextDueAt.isBefore(now) ? now : this.nextDueAt)
                .atZone(java.time.ZoneOffset.UTC);
        this.nextDueAt = base.plusMonths(this.intervalMonths).toInstant();
        this.status = MaintenanceStatus.ACTIVE;
        this.updatedAt = Instant.now();
        return scheduled;
    }

    /**
     * ADR-021: deshace la última ejecución devolviendo el registro a la
     * fecha que tenía. Existe porque marcar por error es trivial y hasta
     * ahora la interfaz no ofrecía ninguna salida: ocultaba el botón al
     * completarse, así que un clic equivocado era definitivo.
     */
    public void revertToOccurrence(java.time.LocalDate scheduledDate) {
        this.nextDueAt = scheduledDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        this.status = MaintenanceStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    /** Toggles ACTIVE<->COMPLETED, idempotent by design — mirrors Warranty#toggleCompletion.
        Se conserva: `POST /maintenance-records/{id}/complete` sigue existiendo y hay
        tests que lo cubren. Las pantallas usan `completeOccurrence`. */
    public void toggleCompletion() {
        this.status = (this.status == MaintenanceStatus.ACTIVE) ? MaintenanceStatus.COMPLETED : MaintenanceStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    /** Partial update — a null argument leaves the corresponding field unchanged. */
    public void applyEdit(String item, Instant nextDueAt) {
        applyEdit(item, nextDueAt, null);
    }

    public void applyEdit(String item, Instant nextDueAt, Integer intervalMonths) {
        applyEdit(item, nextDueAt, intervalMonths, false);
    }

    /**
     * ADR-021: `clearInterval` quita la periodicidad, que es distinto de no
     * mandarla. Sin esta distinción, un mantenimiento recurrente no podría
     * volver a ser puntual nunca.
     */
    public void applyEdit(String item, Instant nextDueAt, Integer intervalMonths, boolean clearInterval) {
        if (clearInterval) {
            this.intervalMonths = null;
        }
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
