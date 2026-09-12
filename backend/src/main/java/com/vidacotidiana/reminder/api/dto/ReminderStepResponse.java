package com.vidacotidiana.reminder.api.dto;

import com.vidacotidiana.reminder.domain.ReminderStep;

import java.time.Instant;
import java.util.UUID;

/** Un paso, tal como lo pinta la lista del detalle en el artefacto. */
public record ReminderStepResponse(
        UUID id,
        UUID reminderId,
        String title,
        boolean done,
        int position,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReminderStepResponse from(ReminderStep step) {
        return new ReminderStepResponse(
                step.getId(),
                step.getReminderId(),
                step.getTitle(),
                step.isDone(),
                step.getPosition(),
                step.getCreatedAt(),
                step.getUpdatedAt()
        );
    }
}
