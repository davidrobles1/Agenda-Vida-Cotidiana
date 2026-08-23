package com.vidacotidiana.inventory.api.dto;

import com.vidacotidiana.inventory.domain.InventoryCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateInventoryItemRequest(
        @NotBlank @Size(min = 1, max = 200) String name,
        @NotNull InventoryCategory category,
        @Size(max = 200) String location
) {
}
