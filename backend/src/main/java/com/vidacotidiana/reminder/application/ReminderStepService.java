package com.vidacotidiana.reminder.application;

import com.vidacotidiana.reminder.domain.ReminderStep;
import com.vidacotidiana.reminder.domain.ReminderStepRepository;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Collection;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Los pasos de una tarea (V34).
 *
 * TODA operación pasa antes por `reminderService.getOwnedOrThrow`: el paso no
 * guarda dueño, lo hereda de su tarea, así que la autorización se comprueba
 * arriba y una sola vez. Un paso al que se llegara sin pasar por ahí sería un
 * agujero silencioso.
 */
@Service
public class ReminderStepService {

    /** Un anillo con más de esto deja de ser una tarea y es una lista. */
    private static final int MAX_STEPS = 50;

    private final ReminderStepRepository stepRepository;
    private final ReminderService reminderService;

    public ReminderStepService(ReminderStepRepository stepRepository, ReminderService reminderService) {
        this.stepRepository = stepRepository;
        this.reminderService = reminderService;
    }

    @Transactional(readOnly = true)
    public List<ReminderStep> list(UUID reminderId, UUID callerUserId) {
        reminderService.getOwnedOrThrow(reminderId, callerUserId);
        return stepRepository.findByReminderIdOrderByPositionAsc(reminderId);
    }

    /**
     * Añadir al final. La posición la pone el servidor, no el cliente: si la
     * enviara el cliente, dos altas seguidas desde dos dispositivos acabarían
     * en el mismo hueco.
     */
    @Transactional
    public ReminderStep add(UUID reminderId, UUID callerUserId, String title) {
        reminderService.getOwnedOrThrow(reminderId, callerUserId);
        if (title == null || title.isBlank()) {
            throw new ValidationException("Un paso necesita un texto.");
        }
        List<ReminderStep> existing = stepRepository.findByReminderIdOrderByPositionAsc(reminderId);
        if (existing.size() >= MAX_STEPS) {
            throw new ValidationException("Una tarea admite como mucho " + MAX_STEPS + " pasos.");
        }
        int next = existing.isEmpty() ? 0 : existing.get(existing.size() - 1).getPosition() + 1;
        return stepRepository.save(new ReminderStep(reminderId, title.trim(), next));
    }

    /** Marcar o desmarcar: el mismo gesto, como la casilla del artefacto. */
    @Transactional
    public ReminderStep toggle(UUID reminderId, UUID stepId, UUID callerUserId) {
        ReminderStep step = owned(reminderId, stepId, callerUserId);
        step.toggle();
        return step;
    }

    @Transactional
    public ReminderStep rename(UUID reminderId, UUID stepId, UUID callerUserId, String title) {
        ReminderStep step = owned(reminderId, stepId, callerUserId);
        step.rename(title);
        return step;
    }

    @Transactional
    public void delete(UUID reminderId, UUID stepId, UUID callerUserId) {
        stepRepository.delete(owned(reminderId, stepId, callerUserId));
    }

    /**
     * Reordenar por la lista completa de ids.
     *
     * Se reciben TODOS los ids y no un par «de aquí a allá»: con la lista
     * entera el resultado es el mismo se apliquen en el orden que se apliquen,
     * y dos reordenaciones simultáneas no dejan huecos ni duplicados.
     */
    @Transactional
    public List<ReminderStep> reorder(UUID reminderId, UUID callerUserId, List<UUID> orderedIds) {
        reminderService.getOwnedOrThrow(reminderId, callerUserId);
        List<ReminderStep> steps = stepRepository.findByReminderIdOrderByPositionAsc(reminderId);
        if (orderedIds == null || orderedIds.size() != steps.size()) {
            throw new ValidationException("El orden debe incluir todos los pasos de la tarea.");
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID id = orderedIds.get(i);
            ReminderStep step = steps.stream()
                    .filter(s -> s.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new ValidationException("Ese paso no pertenece a esta tarea."));
            step.moveTo(i);
        }
        return stepRepository.findByReminderIdOrderByPositionAsc(reminderId);
    }

    /**
     * El avance, DERIVADO. Es el número que pinta el anillo y no se almacena
     * en ninguna parte: se cuenta cada vez que se pide.
     */
    @Transactional(readOnly = true)
    public int percent(UUID reminderId) {
        long total = stepRepository.countByReminderId(reminderId);
        if (total == 0) {
            return 0;
        }
        return (int) Math.round(stepRepository.countByReminderIdAndDoneIsTrue(reminderId) * 100.0 / total);
    }

    private ReminderStep owned(UUID reminderId, UUID stepId, UUID callerUserId) {
        reminderService.getOwnedOrThrow(reminderId, callerUserId);
        return stepRepository.findByIdAndReminderId(stepId, reminderId)
                .orElseThrow(() -> new NotFoundException("STEP_NOT_FOUND", "Ese paso no existe en esta tarea."));
    }

    /**
     * CUÁNTO LLEVA HECHO CADA UNA de estas tareas.
     *
     * Lo usa la lista de Tareas para pintar el anillo de avance del artefacto
     * en cada fila. Vive aquí y no en `ReminderService` porque el avance es un
     * hecho sobre los PASOS, y este es el servicio que responde por ellos.
     *
     * Una sola agregación para toda la página: preguntar por cada tarea
     * convertiría una pantalla en cuarenta consultas. Las tareas sin pasos no
     * salen en el mapa, y eso es lo correcto — «sin pasos» no es «cero de
     * cero», y quien lo pinte tiene que poder distinguirlo.
     */
    @Transactional(readOnly = true)
    public Map<UUID, StepProgress> progressOf(Collection<UUID> reminderIds) {
        if (reminderIds == null || reminderIds.isEmpty()) {
            return Map.of();
        }
        return stepRepository.progressOf(reminderIds).stream()
                .collect(Collectors.toMap(
                        ReminderStepRepository.ReminderStepProgress::getReminderId,
                        row -> new StepProgress((int) row.getTotal(), (int) row.getDone())));
    }

    /** El avance de UNA tarea: cuántos pasos tiene y cuántos están hechos. */
    public record StepProgress(int total, int done) {}
}
