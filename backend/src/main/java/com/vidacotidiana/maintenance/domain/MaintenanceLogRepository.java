package com.vidacotidiana.maintenance.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaintenanceLogRepository extends JpaRepository<MaintenanceLogEntry, UUID> {

    /** Historial de un mantenimiento, lo más reciente primero. */
    List<MaintenanceLogEntry> findByMaintenanceRecordIdOrderByScheduledDateDesc(UUID maintenanceRecordId);

    /** ADR-021: todo el historial del usuario en una consulta. La lista lo
        necesita en bloque para mostrar "última vez" en cada fila; pedirlo
        registro a registro sería una consulta por fila. */
    List<MaintenanceLogEntry> findByOwnerUserId(UUID ownerUserId);

    /** Sostiene la idempotencia de "Hecho" junto al índice único de V27. */
    Optional<MaintenanceLogEntry> findByMaintenanceRecordIdAndScheduledDate(UUID maintenanceRecordId,
                                                                            LocalDate scheduledDate);
}
