package com.vidacotidiana.daynote.api.dto;

import com.vidacotidiana.daynote.domain.DayNoteElement;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record DayNoteElementResponse(
        UUID id,
        UUID ownerUserId,
        LocalDate noteDate,
        String type,
        double x,
        double y,
        double width,
        double height,
        int zIndex,
        Map<String, Object> data,
        int version,
        Instant createdAt,
        Instant updatedAt,
        /** ADR-019: módulo propietario del recurso. */
        String context
) {
    public static DayNoteElementResponse from(DayNoteElement element) {
        return new DayNoteElementResponse(
                element.getId(),
                element.getOwnerUserId(),
                element.getNoteDate(),
                element.getType().name(),
                element.getX(),
                element.getY(),
                element.getWidth(),
                element.getHeight(),
                element.getZIndex(),
                element.getData(),
                element.getVersion(),
                element.getCreatedAt(),
                element.getUpdatedAt(),
                element.getContext().name()
        );
    }
}
