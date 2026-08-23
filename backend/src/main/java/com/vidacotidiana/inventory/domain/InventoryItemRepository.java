package com.vidacotidiana.inventory.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Owner-only, mismo motivo que warranty.domain.WarrantyRepository — sin
    concepto de compartir para este módulo. */
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    Page<InventoryItem> findByOwnerUserId(UUID ownerUserId, Pageable pageable);

    Page<InventoryItem> findByOwnerUserIdAndCategory(UUID ownerUserId, InventoryCategory category, Pageable pageable);
}
