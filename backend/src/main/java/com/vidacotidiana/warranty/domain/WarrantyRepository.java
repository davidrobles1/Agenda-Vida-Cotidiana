package com.vidacotidiana.warranty.domain;

import com.vidacotidiana.shared.domain.ModuleContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * BE-037. Owner-only entity — unlike ReminderRepository#findAccessibleTo,
 * there is no sharing/collaborator concept for Warranty in V2 (no
 * corresponding UI in web/src/features/warranties, no WARRANTY_SHARE
 * table): a plain owner-scoped query is enough.
 */
public interface WarrantyRepository extends JpaRepository<Warranty, UUID> {

    Page<Warranty> findByOwnerUserId(UUID ownerUserId, Pageable pageable);

    /**
     * ADR-019: aislamiento por módulo EN LA CONSULTA. El requisito es
     * explícito en que no basta con ocultar en la vista — un recurso de un
     * módulo no debe salir siquiera de la base de datos cuando se está en el
     * otro.
     */
    Page<Warranty> findByOwnerUserIdAndContext(UUID ownerUserId, ModuleContext context, Pageable pageable);
}
