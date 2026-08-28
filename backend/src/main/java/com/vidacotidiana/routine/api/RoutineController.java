package com.vidacotidiana.routine.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.routine.api.dto.CreateRoutineRequest;
import com.vidacotidiana.routine.api.dto.ExecuteRoutineRequest;
import com.vidacotidiana.routine.api.dto.RoutineResponse;
import com.vidacotidiana.routine.api.dto.UpdateRoutineRequest;
import com.vidacotidiana.routine.application.RoutineService;
import com.vidacotidiana.routine.domain.Routine;
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
 * ADR-016 Fase 3e2/FR-032: Rutina CRUD, owner-only — same shape as
 * person.api.PersonController.
 *
 * <p>The occurrence action is {@code POST /routines/{id}/execute}, not
 * {@code /complete}: a Routine deliberately has no {@code completed} state
 * (FR-032) — "executing" it advances its next date and leaves it active, so
 * naming it /complete would suggest a terminal state that does not exist.
 */
@RestController
@RequestMapping("/api/v1/routines")
public class RoutineController {

    private final RoutineService routineService;
    private final CurrentUser currentUser;

    public RoutineController(RoutineService routineService, CurrentUser currentUser) {
        this.routineService = routineService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<RoutineResponse> create(@Valid @RequestBody CreateRoutineRequest request) {
        Routine created = routineService.create(currentUser.userId(), request.title(), request.description(),
                request.frequency(), request.nextExecutionDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(RoutineResponse.from(created));
    }

    @GetMapping
    public PageResponse<RoutineResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Routine> routines = routineService.listOwnedBy(currentUser.userId(), pageable);
        return PageResponse.from(routines.map(RoutineResponse::from));
    }

    @GetMapping("/{id}")
    public RoutineResponse get(@PathVariable UUID id) {
        Routine routine = routineService.getOwnedOrThrow(id, currentUser.userId());
        return RoutineResponse.from(routine);
    }

    @PatchMapping("/{id}")
    public RoutineResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateRoutineRequest request) {
        Routine routine = routineService.edit(id, currentUser.userId(), request.title(), request.description(),
                request.frequency(), request.nextExecutionDate(), request.active(), request.version());
        return RoutineResponse.from(routine);
    }

    /** UC-25/AC-019: marca la ocurrencia actual como realizada y avanza `nextExecutionDate` desde la fecha programada. */
    @PostMapping("/{id}/execute")
    public RoutineResponse execute(@PathVariable UUID id,
                                   @RequestBody(required = false) ExecuteRoutineRequest request) {
        Integer expectedVersion = (request != null) ? request.version() : null;
        Routine routine = routineService.markExecuted(id, currentUser.userId(), expectedVersion);
        return RoutineResponse.from(routine);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        routineService.delete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
