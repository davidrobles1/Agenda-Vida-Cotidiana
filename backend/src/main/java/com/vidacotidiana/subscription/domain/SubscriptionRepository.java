package com.vidacotidiana.subscription.domain;

import com.vidacotidiana.shared.domain.ModuleContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Owner-only, mismo motivo que warranty.domain.WarrantyRepository. */
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Page<Subscription> findByOwnerUserId(UUID ownerUserId, Pageable pageable);

    /**
     * ADR-019: aislamiento por módulo EN LA CONSULTA. El requisito es
     * explícito en que no basta con ocultar en la vista — un recurso de un
     * módulo no debe salir siquiera de la base de datos cuando se está en el
     * otro.
     */
    Page<Subscription> findByOwnerUserIdAndContext(UUID ownerUserId, ModuleContext context, Pageable pageable);
}
