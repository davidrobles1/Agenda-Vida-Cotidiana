package com.vidacotidiana.visionboard.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Partial update — name/description/width/height/theme are optional
 * and, when omitted (null), leave the stored value unchanged (same
 * convention as UpdateReminderRequest/UpdateNoteRequest). This is also how
 * a Board Theme change persists (FASE 16) — no separate endpoint, the
 * caller sends just {@code theme} + the board's current {@code version}.
 * version is required — a mismatch returns 409 VISION_BOARD_VERSION_CONFLICT.
 */
public record UpdateVisionBoardRequest(
        @Size(min = 1, max = 200) String name,
        @Size(max = 2000) String description,
        @Min(1) Integer width,
        @Min(1) Integer height,
        @Pattern(regexp = "LIGHT|DARK|PAPER|NATURAL|CALM|ENERGY") String theme,
        @NotNull Integer version
) {
}
