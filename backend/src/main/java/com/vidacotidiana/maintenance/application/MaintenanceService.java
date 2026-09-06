package com.vidacotidiana.maintenance.application;

import com.vidacotidiana.maintenance.domain.MaintenanceLogEntry;
import com.vidacotidiana.maintenance.domain.MaintenanceLogRepository;
import com.vidacotidiana.maintenance.domain.MaintenanceRecord;
import com.vidacotidiana.maintenance.domain.MaintenanceRecordRepository;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.ModuleContext;
import com.vidacotidiana.shared.domain.NotFoundException;
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

    public MaintenanceService(MaintenanceRecordRepository maintenanceRecordRepository,
                              MaintenanceLogRepository maintenanceLogRepository) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
        this.maintenanceLogRepository = maintenanceLogRepository;
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
        MaintenanceRecord record = new MaintenanceRecord(ownerUserId, item, nextDueAt, intervalMonths, context);
        return maintenanceRecordRepository.save(record);
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
        MaintenanceRecord record = getOwnedOrThrow(recordId, callerUserId);

        if (expectedVersion != record.getVersion()) {
            throw new ConflictException("MAINTENANCE_VERSION_CONFLICT",
                    "MaintenanceRecord " + recordId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + record.getVersion() + ").");
        }

        record.applyEdit(item, nextDueAt, intervalMonths, clearInterval);
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
