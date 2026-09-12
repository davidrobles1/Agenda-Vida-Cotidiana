package com.vidacotidiana.reminder.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.reminder.api.dto.ReminderStepResponse;
import com.vidacotidiana.reminder.api.dto.StepTitleRequest;
import com.vidacotidiana.reminder.api.dto.StepReorderRequest;
import com.vidacotidiana.reminder.application.ReminderStepService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Los pasos cuelgan de su tarea también en la URL.
 *
 * `/reminders/{id}/steps` y no `/steps/{id}`: un paso no existe fuera de su
 * tarea, y la ruta anidada hace imposible pedir uno sin decir de cuál es — que
 * es justo lo que permite comprobar el dueño una sola vez y bien.
 *
 * Mismo patrón que `ReminderShareController`, que ya vive en
 * `/reminders/{id}/shares`.
 */
@RestController
@RequestMapping("/api/v1/reminders/{reminderId}/steps")
public class ReminderStepController {

    private final ReminderStepService stepService;
    private final CurrentUser currentUser;

    public ReminderStepController(ReminderStepService stepService, CurrentUser currentUser) {
        this.stepService = stepService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<ReminderStepResponse> list(@PathVariable UUID reminderId) {
        return stepService.list(reminderId, currentUser.userId()).stream()
                .map(ReminderStepResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ReminderStepResponse> add(@PathVariable UUID reminderId,
                                                    @Valid @RequestBody StepTitleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ReminderStepResponse.from(
                        stepService.add(reminderId, currentUser.userId(), request.title())));
    }

    /** Marcar o desmarcar. Un solo gesto, como la casilla del artefacto. */
    @PostMapping("/{stepId}/toggle")
    public ReminderStepResponse toggle(@PathVariable UUID reminderId, @PathVariable UUID stepId) {
        return ReminderStepResponse.from(stepService.toggle(reminderId, stepId, currentUser.userId()));
    }

    @PatchMapping("/{stepId}")
    public ReminderStepResponse rename(@PathVariable UUID reminderId, @PathVariable UUID stepId,
                                       @Valid @RequestBody StepTitleRequest request) {
        return ReminderStepResponse.from(
                stepService.rename(reminderId, stepId, currentUser.userId(), request.title()));
    }

    @DeleteMapping("/{stepId}")
    public ResponseEntity<Void> delete(@PathVariable UUID reminderId, @PathVariable UUID stepId) {
        stepService.delete(reminderId, stepId, currentUser.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reorder")
    public List<ReminderStepResponse> reorder(@PathVariable UUID reminderId,
                                              @Valid @RequestBody StepReorderRequest request) {
        return stepService.reorder(reminderId, currentUser.userId(), request.orderedIds()).stream()
                .map(ReminderStepResponse::from)
                .toList();
    }

    /**
     * El avance derivado, para que el cliente no tenga que contar.
     *
     * Se expone aparte y no dentro de `ReminderResponse` a propósito: meterlo
     * en la respuesta de la lista obligaría a una consulta por tarea (N+1) en
     * una pantalla que muestra veinte.
     */
    @GetMapping("/percent")
    public Map<String, Integer> percent(@PathVariable UUID reminderId) {
        stepService.list(reminderId, currentUser.userId());
        return Map.of("percent", stepService.percent(reminderId));
    }
}
