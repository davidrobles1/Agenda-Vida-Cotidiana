package com.vidacotidiana.visionboard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * FASE 9 fix (layers): "subir/bajar una capa" means moving the element
 * exactly one position relative to its immediately adjacent neighbor in
 * paint order — not an unconditional zIndex+-1 (meaningless once elements
 * share a zIndex, which every element does at creation time). See
 * VisionBoardService#reorderElement for the actual reordering. version is
 * mandatory, same as UpdateElementRequest — a mismatch returns 409
 * VISION_BOARD_ELEMENT_VERSION_CONFLICT.
 */
public record ReorderElementRequest(
        @NotBlank @Pattern(regexp = "FRONT|BACK|RAISE|LOWER") String direction,
        @NotNull Integer version
) {
}
