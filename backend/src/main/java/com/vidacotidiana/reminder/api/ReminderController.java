package com.vidacotidiana.reminder.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.reminder.api.dto.CompleteReminderRequest;
import com.vidacotidiana.reminder.api.dto.CreateReminderRequest;
import com.vidacotidiana.reminder.api.dto.ReminderResponse;
import java.util.Map;
import java.util.List;
import com.vidacotidiana.reminder.application.ReminderStepService;
import com.vidacotidiana.reminder.api.dto.UpdateReminderRequest;
import com.vidacotidiana.reminder.application.ReminderService;
import com.vidacotidiana.reminder.domain.Reminder;
import com.vidacotidiana.shared.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Covers the full Reminder CRUD in Documentacion/openapi/openapi.yaml:
 * create, list (own reminders, paginated), get by id, toggle completion,
 * edit, and delete. See ReminderService.delete for the reduced scope of
 * deletion until sharing/push exist (BE-016..026).
 */
@RestController
@RequestMapping("/api/v1/reminders")
public class ReminderController {

    private final ReminderService reminderService;
    private final ReminderStepService stepService;
    private final CurrentUser currentUser;

    public ReminderController(ReminderService reminderService, ReminderStepService stepService,
                              CurrentUser currentUser) {
        this.reminderService = reminderService;
        this.stepService = stepService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<ReminderResponse> create(@Valid @RequestBody CreateReminderRequest request) {
        Reminder created = reminderService.create(
                currentUser.userId(), request.title(), request.description(), request.dueAt(), request.context(),
                request.iconId(), request.stickerId(), request.personId(), request.projectId(), request.location(),
                request.priority());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReminderResponse.from(created));
    }

    @GetMapping
    public PageResponse<ReminderResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            // ADR-019: sin `context` se devuelve todo (Calendario general);
            // con contexto, el filtro baja hasta la consulta SQL.
            @RequestParam(value = "context", required = false) String context) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Reminder> reminders = reminderService.listAccessibleTo(currentUser.userId(), context, pageable);

        // EL AVANCE DE TODA LA PÁGINA, EN UNA SOLA CONSULTA.
        //
        // El artefacto aprobado pone un anillo con el porcentaje en cada fila
        // de Tareas. Derivarlo exige saber cuántos pasos tiene cada una y
        // cuántos están hechos; preguntarlo tarea por tarea convertiría esta
        // pantalla en cuarenta consultas, y por eso el anillo no existía aquí.
        // Una agregación por los ids de la página cuesta una.
        Map<UUID, ReminderStepService.StepProgress> progress = stepService.progressOf(
                reminders.getContent().stream().map(Reminder::getId).toList());

        return PageResponse.from(reminders.map(r -> ReminderResponse.from(r, progressOrEmpty(progress, r.getId()))));
    }

    @GetMapping("/{id}")
    public ReminderResponse get(@PathVariable UUID id) {
        Reminder reminder = reminderService.getAccessible(id, currentUser.userId());
        // Para una sola tarea la agregación es la misma consulta, así que el
        // detalle responde con el mismo dato que la lista y no pueden discrepar.
        return ReminderResponse.from(reminder, progressOrEmpty(
                stepService.progressOf(List.of(reminder.getId())), reminder.getId()));
    }

    /**
     * El avance de una tarea, con CERO DE CERO cuando no tiene pasos.
     *
     * La agregación solo devuelve las tareas que tienen alguno, así que la
     * ausencia significa «no tiene ninguno» — y eso sí es un hecho, no un
     * desconocimiento: la consulta se hizo. Devolver nulo aquí haría que el
     * cliente creyera que no se miró.
     */
    private static ReminderStepService.StepProgress progressOrEmpty(
            Map<UUID, ReminderStepService.StepProgress> progress, UUID reminderId) {
        return progress.getOrDefault(reminderId, new ReminderStepService.StepProgress(0, 0));
    }

    /**
     * El avance de UNA tarea recién guardada, para que la respuesta de escribir
     * diga lo mismo que la de leer.
     *
     * Existe porque no decirlo tiene consecuencias visibles. `stepCount` nulo
     * significa «no se consultó» (no «cero»), y `complete` y `update` devolvían
     * la tarea sin él: el cliente sustituía la suya por la respuesta y con ello
     * perdía el avance, así que el anillo de la tarjeta desaparecía al marcarla
     * —o, más a la vista, al devolverla a pendiente— y no volvía hasta una
     * recarga completa. Es la misma consulta que ya hace el detalle.
     */
    private ReminderStepService.StepProgress progresoDe(Reminder reminder) {
        return progressOrEmpty(stepService.progressOf(List.of(reminder.getId())), reminder.getId());
    }

    @PostMapping("/{id}/complete")
    public ReminderResponse complete(@PathVariable UUID id,
                                      @RequestBody(required = false) CompleteReminderRequest request) {
        Integer expectedVersion = (request != null) ? request.version() : null;
        Reminder reminder = reminderService.toggleCompletion(id, currentUser.userId(), expectedVersion);
        return ReminderResponse.from(reminder, progresoDe(reminder));
    }

    @PatchMapping("/{id}")
    public ReminderResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateReminderRequest request) {
        Reminder reminder = reminderService.edit(
                id, currentUser.userId(), request.title(), request.description(), request.dueAt(),
                request.iconId(), request.stickerId(), request.personId(), request.projectId(), request.location(),
                request.version());
        return ReminderResponse.from(reminder, progresoDe(reminder));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        reminderService.delete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
