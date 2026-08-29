package com.vidacotidiana.maintenance.domain;

import com.vidacotidiana.shared.domain.ModuleContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * BE-037. Owner-only entity — mirrors warranty.domain.WarrantyRepository:
 * no sharing/collaborator concept for MaintenanceRecord in V2.
 */
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, UUID> {

    Page<MaintenanceRecord> findByOwnerUserId(UUID ownerUserId, Pageable pageable);

    /**
     * ADR-019: aislamiento por módulo EN LA CONSULTA. El requisito es
     * explícito en que no basta con ocultar en la vista — un recurso de un
     * módulo no debe salir siquiera de la base de datos cuando se está en el
     * otro.
     */
    Page<MaintenanceRecord> findByOwnerUserIdAndContext(UUID ownerUserId, ModuleContext context, Pageable pageable);
}
