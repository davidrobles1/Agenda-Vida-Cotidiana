package com.vidacotidiana.reminder.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Aligned with components.schemas.CreateReminderRequest in openapi.yaml (AC-003: título obligatorio). */
public record CreateReminderRequest(
        @NotBlank @Size(min = 1, max = 200) String title,
        @Size(max = 2000) String description,
        Instant dueAt
) {
}
