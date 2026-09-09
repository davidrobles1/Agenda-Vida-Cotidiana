package com.vidacotidiana.maintenance.application;

import com.vidacotidiana.inventory.application.InventoryItemService;
import com.vidacotidiana.inventory.domain.InventoryItem;
import com.vidacotidiana.maintenance.domain.MaintenanceLogEntry;
import com.vidacotidiana.maintenance.domain.MaintenanceLogRepository;
import com.vidacotidiana.maintenance.domain.MaintenanceRecord;
import com.vidacotidiana.maintenance.domain.MaintenanceRecordRepository;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.ModuleContext;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.ValidationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Application service for the MaintenanceRecord vertical slice (BE-037).
 * Mirrors warranty.application.WarrantyService exactly — owner-only
 * throughout, same non-enumeration rule (404 never 403).
 */
@Service
public class MaintenanceService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final MaintenanceLogRepository maintenanceLogRepository;
    /** V31: solo para validar que el artículo enlazado es del mismo dueño y
        del mismo módulo. Mantenimiento no depende del agregado Inventario
        para nada más — mismo criterio que WarrantyService. */
    private final InventoryItemService inventoryItemService;

    public MaintenanceService(MaintenanceRecordRepository maintenanceRecordRepository,
                              MaintenanceLogRepository maintenanceLogRepository,
                              InventoryItemService inventoryItemService) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.maintenanceLogRepository = maintenanceLogRepository;
        this.inventoryItemService = inventoryItemService;
    }

    @Transactional
    public MaintenanceRecord create(UUID ownerUserId, String item, Instant nextDueAt) {
        return create(ownerUserId, item, nextDueAt, null);
    }

    /** Sobrecarga aditiva (V24): `intervalMonths` nulo = mantenimiento de una
        sola fecha, el comportamiento que tenían todos hasta ahora. */
    @Transactional
    public MaintenanceRecord create(UUID ownerUserId, String item, Instant nextDueAt, Integer intervalMonths) {
        return create(ownerUserId, item, nextDueAt, intervalMonths, ModuleContext.PERSONAL);
    }

    /** ADR-019: alta con el módulo desde el que se creó. */
    @Transactional
    public MaintenanceRecord create(UUID ownerUserId, String item, Instant nextDueAt, Integer intervalMonths,
                                    ModuleContext context) {
        return create(ownerUserId, item, nextDueAt, intervalMonths, context, null);
    }

    /**
     * V31: alta con el artículo del inventario al que se le hace, OPCIONAL.
     *
     * A diferencia de la garantía, aquí no se exige: también se mantiene lo
     * que no es un artículo inventariado (el techo, el jardín). Lo que sí se
     * exige es que, si viene, sea enlazable — ver {@link #requireLinkableItem}.
     */
    @Transactional
    public MaintenanceRecord create(UUID ownerUserId, String item, Instant nextDueAt, Integer intervalMonths,
                                    ModuleContext context, UUID inventoryItemId) {
        MaintenanceRecord record = new MaintenanceRecord(ownerUserId, item, nextDueAt, intervalMonths, context);
        requireLinkableItem(inventoryItemId, ownerUserId, record.getContext());
        record.linkInventoryItem(inventoryItemId);
        return maintenanceRecordRepository.save(record);
    }

    /**
     * Un artículo solo es enlazable si es del mismo dueño Y del mismo módulo.
     * Copia literal de la regla de `WarrantyService#requireLinkableItem`: el
     * dueño evita filtrar la existencia de artículos ajenos, y el módulo
     * impide que un recurso Personal acabe apuntando a uno Laboral, que es lo
     * que prohíbe la regla 2 del ADR-019.
     */
    private void requireLinkableItem(UUID inventoryItemId, UUID callerUserId, ModuleContext recordContext) {
        if (inventoryItemId == null) {
            return;
        }
        InventoryItem linked = inventoryItemService.getOwnedOrThrow(inventoryItemId, callerUserId);
        if (linked.getContext() != recordContext) {
            throw new ValidationException(
                    "El artículo pertenece al módulo " + linked.getContext()
                            + " y el mantenimiento al módulo " + recordContext
                            + ". Solo se pueden enlazar recursos del mismo módulo.");
        }
    }

    @Transactional(readOnly = true)
    public Page<MaintenanceRecord> listOwnedBy(UUID ownerUserId, Pageable pageable) {
        return listOwnedBy(ownerUserId, null, pageable);
    }

    /**
     * ADR-019: `context` nulo = sin filtrar, que es lo que necesita el
     * Calendario general (el modo que muestra Personal y Laboral juntos).
     * Con contexto, el filtro va en la consulta: los recursos del otro
     * módulo ni siquiera se leen.
     */
    @Transactional(readOnly = true)
    public Page<MaintenanceRecord> listOwnedBy(UUID ownerUserId, ModuleContext context, Pageable pageable) {
        return (context == null)
                ? maintenanceRecordRepository.findByOwnerUserId(ownerUserId, pageable)
                : maintenanceRecordRepository.findByOwnerUserIdAndContext(ownerUserId, context, pageable);
    }

    @Transactional(readOnly = true)
    public MaintenanceRecord getOwnedOrThrow(UUID recordId, UUID callerUserId) {
        MaintenanceRecord record = findOrThrow(recordId);
        requireOwner(record, callerUserId);
        return record;
    }

    @Transactional
    public MaintenanceRecord toggleCompletion(UUID recordId, UUID callerUserId, Integer expectedVersion) {
        MaintenanceRecord record = getOwnedOrThrow(recordId, callerUserId);

        if (expectedVersion != null && expectedVersion != record.getVersion()) {
            throw new ConflictException("MAINTENANCE_VERSION_CONFLICT",
                    "MaintenanceRecord " + recordId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + record.getVersion() + ").");
        }

        record.toggleCompletion();
        try {
            return maintenanceRecordRepository.save(record);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("MAINTENANCE_VERSION_CONFLICT",
                    "MaintenanceRecord " + recordId + " was modified concurrently; refetch and retry.");
        }
    }

    @Transactional
    public MaintenanceRecord edit(UUID recordId, UUID callerUserId, String item, Instant nextDueAt, int expectedVersion) {
        return edit(recordId, callerUserId, item, nextDueAt, null, expectedVersion);
    }

    @Transactional
    public MaintenanceRecord edit(UUID recordId, UUID callerUserId, String item, Instant nextDueAt,
                                  Integer intervalMonths, int expectedVersion) {
        return edit(recordId, callerUserId, item, nextDueAt, intervalMonths, expectedVersion, false);
    }

    /** ADR-021: `clearInterval` convierte un recurrente en puntual. */
    @Transactional
    public MaintenanceRecord edit(UUID recordId, UUID callerUserId, String item, Instant nextDueAt,
                                  Integer intervalMonths, int expectedVersion, boolean clearInterval) {
        return edit(recordId, callerUserId, item, nextDueAt, intervalMonths, expectedVersion, clearInterval,
                null, false);
    }

    /**
     * V31: edición con enlace al artículo del inventario.
     *
     * `linkInventoryItem` distingue "no tocar el enlace" (false) de
     * "cambiarlo" (true), exactamente igual que `clearInterval` aquí mismo
     * (ADR-021(i)) y que `WarrantyService#edit` (ADR-022): sin ese indicador,
     * mandar `null` sería indistinguible de no mandar nada y **desenlazar
     * sería imposible**.
     */
    @Transactional
    public MaintenanceRecord edit(UUID recordId, UUID callerUserId, String item, Instant nextDueAt,
                                  Integer intervalMonths, int expectedVersion, boolean clearInterval,
                                  UUID inventoryItemId, boolean linkInventoryItem) {
        MaintenanceRecord record = getOwnedOrThrow(recordId, callerUserId);

        if (expectedVersion != record.getVersion()) {
            throw new ConflictException("MAINTENANCE_VERSION_CONFLICT",
                    "MaintenanceRecord " + recordId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + record.getVersion() + ").");
        }

        record.applyEdit(item, nextDueAt, intervalMonths, clearInterval);
        if (linkInventoryItem) {
            requireLinkableItem(inventoryItemId, callerUserId, record.getContext());
            record.linkInventoryItem(inventoryItemId);
        }
        try {
            return maintenanceRecordRepository.save(record);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("MAINTENANCE_VERSION_CONFLICT",
                    "MaintenanceRecord " + recordId + " was modified concurrently; refetch and retry.");
        }
    }

    @Transactional
    public void delete(UUID recordId, UUID callerUserId) {
        MaintenanceRecord record = getOwnedOrThrow(recordId, callerUserId);
        maintenanceRecordRepository.delete(record);
    }

    private MaintenanceRecord findOrThrow(UUID recordId) {
        return maintenanceRecordRepository.findById(recordId)
                .orElseThrow(() -> new NotFoundException("MAINTENANCE_RECORD_NOT_FOUND", "The requested maintenance record was not found."));
    }

    private void requireOwner(MaintenanceRecord record, UUID callerUserId) {
        if (!record.isOwnedBy(callerUserId)) {
            throw new NotFoundException("MAINTENANCE_RECORD_NOT_FOUND", "The requested maintenance record was not found.");
        }
    }

    /**
     * ADR-021: completa la ocurrencia actual y programa la siguiente.
     *
     * IDEMPOTENTE: si esa ocurrencia ya estaba registrada, devuelve el
     * registro sin volver a avanzar. Sin esto, un doble clic o un reintento
     * de red saltarían dos ciclos y la próxima revisión aparecería un
     * intervalo entero más tarde de lo real — un error silencioso.
     */
    @Transactional
    public MaintenanceRecord completeOccurrence(UUID recordId, UUID callerUserId, String note) {
        MaintenanceRecord record = getOwnedOrThrow(recordId, callerUserId);

        java.time.LocalDate scheduledDate = record.getNextDueAt()
                .atZone(java.time.ZoneOffset.UTC).toLocalDate();

        if (maintenanceLogRepository
                .findByMaintenanceRecordIdAndScheduledDate(recordId, scheduledDate).isPresent()) {
            return record;
        }

        // Un solo "hoy" para toda la operación. Antes había dos: el avance
        // recibía `Instant.now()` y lo leía en UTC, mientras el historial
        // guardaba `LocalDate.now()` en la zona del servidor. Completando de
        // tarde en UTC-6 eso daba dos días distintos —el historial decía 31
        // de agosto y el avance partía del 1 de septiembre—, así que la
        // siguiente ocurrencia caía un día más tarde de lo debido.
        // Al fijar el día a medianoche UTC queda además en la misma rejilla
        // en que se almacenan las fechas de calendario (`nextDueAt`).
        java.time.LocalDate today = java.time.LocalDate.now();
        record.completeOccurrence(today.atStartOfDay(java.time.ZoneOffset.UTC).toInstant());

        maintenanceLogRepository.save(new MaintenanceLogEntry(
                recordId,
                callerUserId,
                scheduledDate,
                today,
                note));

        return maintenanceRecordRepository.save(record);
    }

    /** ADR-021: deshace la última ejecución y devuelve la fecha anterior. */
    @Transactional
    public MaintenanceRecord undoLastCompletion(UUID recordId, UUID callerUserId) {
        MaintenanceRecord record = getOwnedOrThrow(recordId, callerUserId);
        java.util.List<MaintenanceLogEntry> history =
                maintenanceLogRepository.findByMaintenanceRecordIdOrderByScheduledDateDesc(recordId);
        if (history.isEmpty()) {
            return record;
        }
        MaintenanceLogEntry last = history.get(0);
        maintenanceLogRepository.delete(last);
        record.revertToOccurrence(last.getScheduledDate());
        return maintenanceRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public java.util.List<MaintenanceLogEntry> listLog(UUID ownerUserId) {
        return maintenanceLogRepository.findByOwnerUserId(ownerUserId);
    }

    /** Historial de un mantenimiento concreto. Verifica la propiedad: el id
        llega de la ruta y no puede confiarse. */
    @Transactional(readOnly = true)
    public java.util.List<MaintenanceLogEntry> listLogFor(UUID recordId, UUID callerUserId) {
        getOwnedOrThrow(recordId, callerUserId);
        return maintenanceLogRepository.findByMaintenanceRecordIdOrderByScheduledDateDesc(recordId);
    }
}
