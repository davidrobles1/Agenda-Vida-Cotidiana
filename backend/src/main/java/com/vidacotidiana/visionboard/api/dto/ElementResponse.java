package com.vidacotidiana.visionboard.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.visionboard.domain.VisionBoardElement;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ElementResponse(
        UUID id,
        UUID boardId,
        String type,
        double x,
        double y,
        double width,
        double height,
        double rotation,
        int zIndex,
        boolean locked,
        boolean visible,
        Map<String, Object> data,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static ElementResponse from(VisionBoardElement element) {
        return new ElementResponse(
                element.getId(),
                element.getBoardId(),
                element.getType().name(),
                element.getX(),
                element.getY(),
                element.getWidth(),
                element.getHeight(),
                element.getRotation(),
                element.getZIndex(),
                element.isLocked(),
                element.isVisible(),
                element.getData(),
                element.getVersion(),
                element.getCreatedAt(),
                element.getUpdatedAt()
        );
    }
}
