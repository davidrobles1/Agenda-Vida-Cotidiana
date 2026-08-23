package com.vidacotidiana.inventory.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.inventory.api.dto.CreateInventoryItemRequest;
import com.vidacotidiana.inventory.api.dto.InventoryItemResponse;
import com.vidacotidiana.inventory.api.dto.UpdateInventoryItemRequest;
import com.vidacotidiana.inventory.application.InventoryItemService;
import com.vidacotidiana.inventory.domain.InventoryCategory;
import com.vidacotidiana.inventory.domain.InventoryItem;
import com.vidacotidiana.shared.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Módulo Inventario (pedido explícito del usuario, 2026-08-22) — mismo
    shape que WarrantyController. */
@RestController
@RequestMapping("/api/v1/inventory-items")
public class InventoryItemController {

    private final InventoryItemService inventoryItemService;
    private final CurrentUser currentUser;

    public InventoryItemController(InventoryItemService inventoryItemService, CurrentUser currentUser) {
        this.inventoryItemService = inventoryItemService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<InventoryItemResponse> create(@Valid @RequestBody CreateInventoryItemRequest request) {
        InventoryItem created = inventoryItemService.create(currentUser.userId(), request.name(), request.category(), request.location());
        return ResponseEntity.status(HttpStatus.CREATED).body(InventoryItemResponse.from(created));
    }

    @GetMapping
    public PageResponse<InventoryItemResponse> list(
            @RequestParam(required = false) InventoryCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<InventoryItem> items = inventoryItemService.listOwnedBy(currentUser.userId(), category, pageable);
        return PageResponse.from(items.map(InventoryItemResponse::from));
    }

    @GetMapping("/{id}")
    public InventoryItemResponse get(@PathVariable UUID id) {
        InventoryItem item = inventoryItemService.getOwnedOrThrow(id, currentUser.userId());
        return InventoryItemResponse.from(item);
    }

    @PatchMapping("/{id}")
    public InventoryItemResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateInventoryItemRequest request) {
        InventoryItem item = inventoryItemService.edit(id, currentUser.userId(), request.name(), request.category(), request.location(), request.version());
        return InventoryItemResponse.from(item);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        inventoryItemService.delete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
