package com.vidacotidiana.inventory.application;

import com.vidacotidiana.inventory.domain.InventoryCategory;
import com.vidacotidiana.inventory.domain.InventoryItem;
import com.vidacotidiana.inventory.domain.InventoryItemRepository;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Módulo Inventario — mismo patrón exacto que warranty.application.WarrantyService. */
@Service
public class InventoryItemService {

    private final InventoryItemRepository inventoryItemRepository;

    public InventoryItemService(InventoryItemRepository inventoryItemRepository) {
        this.inventoryItemRepository = inventoryItemRepository;
    }

    @Transactional
    public InventoryItem create(UUID ownerUserId, String name, InventoryCategory category, String location) {
        InventoryItem item = new InventoryItem(ownerUserId, name, category, location);
        return inventoryItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public Page<InventoryItem> listOwnedBy(UUID ownerUserId, InventoryCategory category, Pageable pageable) {
        if (category != null) {
            return inventoryItemRepository.findByOwnerUserIdAndCategory(ownerUserId, category, pageable);
        }
        return inventoryItemRepository.findByOwnerUserId(ownerUserId, pageable);
    }

    @Transactional(readOnly = true)
    public InventoryItem getOwnedOrThrow(UUID itemId, UUID callerUserId) {
        InventoryItem item = findOrThrow(itemId);
        requireOwner(item, callerUserId);
        return item;
    }

    @Transactional
    public InventoryItem edit(UUID itemId, UUID callerUserId, String name, InventoryCategory category, String location, int expectedVersion) {
        InventoryItem item = getOwnedOrThrow(itemId, callerUserId);

        if (expectedVersion != item.getVersion()) {
            throw new ConflictException("INVENTORY_ITEM_VERSION_CONFLICT",
                    "Inventory item " + itemId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + item.getVersion() + ").");
        }

        item.applyEdit(name, category, location);
        try {
            return inventoryItemRepository.save(item);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("INVENTORY_ITEM_VERSION_CONFLICT",
                    "Inventory item " + itemId + " was modified concurrently; refetch and retry.");
        }
    }

    @Transactional
    public void delete(UUID itemId, UUID callerUserId) {
        InventoryItem item = getOwnedOrThrow(itemId, callerUserId);
        inventoryItemRepository.delete(item);
    }

    private InventoryItem findOrThrow(UUID itemId) {
        return inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("INVENTORY_ITEM_NOT_FOUND", "The requested inventory item was not found."));
    }

    private void requireOwner(InventoryItem item, UUID callerUserId) {
        if (!item.isOwnedBy(callerUserId)) {
            throw new NotFoundException("INVENTORY_ITEM_NOT_FOUND", "The requested inventory item was not found.");
        }
    }
}
