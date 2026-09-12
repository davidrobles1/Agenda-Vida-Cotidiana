package com.vidacotidiana.routine.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.routine.api.dto.RoutineProgressRequest;
import com.vidacotidiana.routine.api.dto.RoutineProgressResponse;
import com.vidacotidiana.routine.api.dto.RoutineTargetRequest;
import com.vidacotidiana.routine.application.RoutineProgressService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * El progreso cuelga de su rutina también en la URL, igual que los pasos de su
 * tarea: `/routines/{id}/progress`.
 *
 * `execute` (que ya existía) y `progress` no son lo mismo y conviven a
 * propósito: `execute` cierra la ocurrencia y mueve `nextExecutionDate` —«ya
 * saqué la basura»—, mientras que `progress` suma dentro del día sin cerrar
 * nada —«llevo cuatro vasos»—. Fundirlos obligaría a que beber un vaso
 * adelantara la rutina a mañana.
 */
@RestController
@RequestMapping("/api/v1/routines/{routineId}")
public class RoutineProgressController {

    private final RoutineProgressService progressService;
    private final CurrentUser currentUser;

    public RoutineProgressController(RoutineProgressService progressService, CurrentUser currentUser) {
        this.progressService = progressService;
        this.currentUser = currentUser;
    }

    /** Sumar al día. Sin cuerpo: suma uno, que es tocar el anillo. */
    @PostMapping("/progress")
    public RoutineProgressResponse add(@PathVariable UUID routineId,
                                       @RequestBody(required = false) RoutineProgressRequest request) {
        RoutineProgressRequest req = request == null
                ? new RoutineProgressRequest(null, null, null)
                : request;
        return RoutineProgressResponse.from(
                progressService.add(routineId, currentUser.userId(), req.date(), req.deltaOrOne()));
    }

    /** Fijar el valor exacto de un día. */
    @PutMapping("/progress")
    public RoutineProgressResponse set(@PathVariable UUID routineId,
                                       @Valid @RequestBody RoutineProgressRequest request) {
        int value = request.value() == null ? 0 : request.value();
        return RoutineProgressResponse.from(
                progressService.set(routineId, currentUser.userId(), request.date(), value));
    }

    @GetMapping("/progress")
    public List<RoutineProgressResponse> list(
            @PathVariable UUID routineId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return progressService.list(routineId, currentUser.userId(), from, to).stream()
                .map(RoutineProgressResponse::from)
                .toList();
    }

    /**
     * Declarar la meta diaria — o retirarla enviando `targetCount: null`, que
     * devuelve la rutina a sí/no sin perder su histórico.
     */
    @PutMapping("/target")
    public Map<String, Object> setTarget(@PathVariable UUID routineId,
                                         @Valid @RequestBody RoutineTargetRequest request) {
        var routine = progressService.setTarget(
                routineId, currentUser.userId(), request.targetCount(), request.unit());
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("routineId", routine.getId());
        body.put("targetCount", routine.getTargetCount());
        body.put("unit", routine.getUnit());
        body.put("counted", routine.isCounted());
        return body;
    }
}
