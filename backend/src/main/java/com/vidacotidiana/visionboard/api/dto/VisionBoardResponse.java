package com.vidacotidiana.visionboard.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.visionboard.domain.VisionBoard;
import com.vidacotidiana.visionboard.domain.VisionBoardElement;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Mirrors reminder.api.dto.ReminderResponse's shape. {@code elements} is
 * only populated by GET /vision-boards/{id} (the "open a board to edit"
 * call) — the list endpoint (GET /vision-boards) stays a lightweight
 * summary and omits it (NON_NULL) to avoid an N+1 elements fetch across
 * every board on that page. There is no separate GET .../elements endpoint
 * in this API (FASE 2 scope) — a board's elements are always loaded
 * together with the board itself.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record VisionBoardResponse(
        UUID id,
        UUID ownerUserId,
        String name,
        String description,
        int width,
        int height,
        String theme,
        int version,
        Instant createdAt,
        Instant updatedAt,
        List<ElementResponse> elements
) {
    public static VisionBoardResponse from(VisionBoard board) {
        return from(board, null);
    }

    public static VisionBoardResponse from(VisionBoard board, List<VisionBoardElement> elements) {
        return new VisionBoardResponse(
                board.getId(),
                board.getOwnerUserId(),
                board.getName(),
                board.getDescription(),
                board.getWidth(),
                board.getHeight(),
                board.getTheme().name(),
                board.getVersion(),
                board.getCreatedAt(),
                board.getUpdatedAt(),
                (elements != null) ? elements.stream().map(ElementResponse::from).toList() : null
        );
    }
}
