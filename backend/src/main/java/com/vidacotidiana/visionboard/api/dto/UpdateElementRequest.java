package com.vidacotidiana.visionboard.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Partial update for position/size/rotation/layer/lock/visibility — a null
 * argument leaves that field unchanged (FASE 4/5/8: drag, resize, rotate,
 * reorder each touch only the fields they care about). {@code data}, like
 * icon_id/sticker_id elsewhere in this schema, is always applied exactly as
 * sent when present — see VisionBoardElement#applyEdit's own doc comment.
 * version is required — a mismatch returns 409
 * VISION_BOARD_ELEMENT_VERSION_CONFLICT.
 */
public record UpdateElementRequest(
        Double x,
        Double y,
        Double width,
        Double height,
        Double rotation,
        Integer zIndex,
        Boolean locked,
        Boolean visible,
        Map<String, Object> data,
        @NotNull Integer version
) {
}
