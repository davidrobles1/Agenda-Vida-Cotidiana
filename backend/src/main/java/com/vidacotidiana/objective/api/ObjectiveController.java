package com.vidacotidiana.objective.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.objective.api.dto.CreateObjectiveRequest;
import com.vidacotidiana.objective.api.dto.ObjectiveResponse;
import com.vidacotidiana.objective.api.dto.UpdateObjectiveRequest;
import com.vidacotidiana.objective.application.ObjectiveService;
import com.vidacotidiana.objective.domain.Objective;
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
 * ADR-016 Fase 3e1/FR-031: Objetivo CRUD, owner-only — same shape as
 * person.api.PersonController. Marking an objective as completed is a plain
 * PATCH (no dedicated /complete action, unlike Reminder): AC-018 defines it
 * as an ordinary field the owner sets, with no collaborator visibility rule
 * behind it.
 */
@RestController
@RequestMapping("/api/v1/objectives")
public class ObjectiveController {

    private final ObjectiveService objectiveService;
    private final CurrentUser currentUser;

    public ObjectiveController(ObjectiveService objectiveService, CurrentUser currentUser) {
        this.objectiveService = objectiveService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<ObjectiveResponse> create(@Valid @RequestBody CreateObjectiveRequest request) {
        Objective created = objectiveService.create(currentUser.userId(), request.title(),
                request.targetValue(), request.currentValue(), request.deadline());
        return ResponseEntity.status(HttpStatus.CREATED).body(ObjectiveResponse.from(created));
    }

    @GetMapping
    public PageResponse<ObjectiveResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Objective> objectives = objectiveService.listOwnedBy(currentUser.userId(), pageable);
        return PageResponse.from(objectives.map(ObjectiveResponse::from));
    }

    @GetMapping("/{id}")
    public ObjectiveResponse get(@PathVariable UUID id) {
        Objective objective = objectiveService.getOwnedOrThrow(id, currentUser.userId());
        return ObjectiveResponse.from(objective);
    }

    @PatchMapping("/{id}")
    public ObjectiveResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateObjectiveRequest request) {
        Objective objective = objectiveService.edit(id, currentUser.userId(), request.title(),
                request.targetValue(), request.currentValue(), request.deadline(),
                request.completed(), request.version());
        return ObjectiveResponse.from(objective);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        objectiveService.delete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
