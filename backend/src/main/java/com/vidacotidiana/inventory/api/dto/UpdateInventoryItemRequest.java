package com.vidacotidiana.inventory.api.dto;

import com.vidacotidiana.inventory.domain.InventoryCategory;

/** Todos los campos opcionales salvo version (edición parcial, mismo
    contrato que warranty.api.dto.UpdateWarrantyRequest). */
public record UpdateInventoryItemRequest(String name, InventoryCategory category, String location, int version) {
}
