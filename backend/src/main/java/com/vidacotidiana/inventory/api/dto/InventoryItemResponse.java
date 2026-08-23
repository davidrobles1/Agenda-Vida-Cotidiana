package com.vidacotidiana.inventory.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.inventory.domain.InventoryItem;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InventoryItemResponse(
        UUID id,
        UUID ownerUserId,
        String name,
        String category,
        String location,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static InventoryItemResponse from(InventoryItem item) {
        return new InventoryItemResponse(
                item.getId(),
                item.getOwnerUserId(),
                item.getName(),
                item.getCategory().name(),
                item.getLocation(),
                item.getVersion(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
