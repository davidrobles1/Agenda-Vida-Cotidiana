package com.vidacotidiana.reminder.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Aligned with components.schemas.CreateReminderRequest in openapi.yaml
 * (AC-003: título obligatorio). RECOMMENDATION (BE-038/ADR-015): {@code
 * context} is optional here, not required — every UI call site that
 * predates ADR-015 (Tareas, the Calendar quick-add) doesn't send it yet,
 * and making it mandatory would break them immediately on merge. A caller
 * that knows its context (the new Personal/Laboral-aware creation flow)
 * sends it explicitly; ReminderService.create defaults to PERSONAL when
 * absent — same reasoning as the migration's own backfill default.
 *
 * iconId/stickerId: optional, same catalog/contract as Note (see
 * note.api.dto.CreateNoteRequest) — a task can be created without either.
 */
public record CreateReminderRequest(
        @NotBlank @Size(min = 1, max = 200) String title,
        @Size(max = 2000) String description,
        Instant dueAt,
        @Pattern(regexp = "PERSONAL|LABORAL") String context,
        @Size(max = 32) String iconId,
        @Size(max = 32) String stickerId,
        UUID personId,
        UUID projectId,
        @Size(max = 500) String location
) {
}
