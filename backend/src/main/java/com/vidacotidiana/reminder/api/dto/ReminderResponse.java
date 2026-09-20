package com.vidacotidiana.reminder.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.reminder.application.ReminderStepService;
import com.vidacotidiana.reminder.domain.Reminder;

import java.time.Instant;
import java.util.UUID;

/**
 * Aligned with components.schemas.Reminder in Documentacion/openapi/openapi.yaml.
 * BE-035: description/dueAt are optional (not in the schema's required list) but
 * declared as plain (non-nullable) string/date-time — the schema's contract is that
 * an absent value is an omitted field, not a JSON null, so nulls must not be serialized.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReminderResponse(
        UUID id,
        UUID ownerUserId,
        String title,
        String description,
        Instant dueAt,
        String status,
        String context,
        String iconId,
        String stickerId,
        UUID personId,
        UUID projectId,
        String location,
        /** V33: LOW | NORMAL | URGENT. Nunca nulo — el default es NORMAL. */
        String priority,
        /**
         * EL AVANCE DE SUS PASOS, para el anillo del artefacto.
         *
         * El diseño aprobado pone un anillo con el porcentaje en CADA fila de
         * Tareas, y ese porcentaje se deriva de los pasos. Sin estos dos
         * campos el cliente tendría que pedir los pasos de cada tarea —una
         * petición por fila— y por eso la lista no tenía anillo.
         *
         * NULOS cuando no se han consultado, y eso NO es cero: «sin pasos» y
         * «no lo sé» son cosas distintas, y un cero afirmaría la primera. El
         * porcentaje se DERIVA de los dos; no se guarda en ninguna parte.
         */
        Integer stepCount,
        Integer stepsDone,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    /** Sin avance consultado: los dos campos van nulos, que es «no lo sé». */
    public static ReminderResponse from(Reminder reminder) {
        return from(reminder, null);
    }

    /**
     * Con el avance de sus pasos, cuando quien responde ya lo tiene.
     *
     * `progress` nulo deja los campos nulos en vez de ponerlos a cero: una
     * tarea sin pasos y una tarea cuyos pasos no se han mirado no son lo mismo,
     * y el cliente necesita distinguirlas para decidir si dibuja el anillo.
     */
    public static ReminderResponse from(Reminder reminder, ReminderStepService.StepProgress progress) {
        return new ReminderResponse(
                reminder.getId(),
                reminder.getOwnerUserId(),
                reminder.getTitle(),
                reminder.getDescription(),
                reminder.getDueAt(),
                reminder.getStatus().name(),
                reminder.getContext().name(),
                reminder.getIconId(),
                reminder.getStickerId(),
                reminder.getPersonId(),
                reminder.getProjectId(),
                reminder.getLocation(),
                reminder.getPriority().name(),
                progress == null ? null : progress.total(),
                progress == null ? null : progress.done(),
                reminder.getVersion(),
                reminder.getCreatedAt(),
                reminder.getUpdatedAt()
        );
    }
}
