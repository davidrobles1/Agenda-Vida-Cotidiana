package com.vidacotidiana.project.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.project.api.dto.CreateProjectRequest;
import com.vidacotidiana.project.api.dto.ProjectResponse;
import com.vidacotidiana.project.api.dto.UpdateProjectRequest;
import com.vidacotidiana.project.application.ProjectService;
import com.vidacotidiana.project.domain.Project;
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

/** ADR-016/FR-022: Proyecto CRUD, owner-only — same shape as person.api.PersonController. */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final CurrentUser currentUser;

    public ProjectController(ProjectService projectService, CurrentUser currentUser) {
        this.projectService = projectService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        Project created = projectService.create(
                currentUser.userId(), request.name(), request.clientPersonId(), request.status(), request.deadline());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectResponse.from(created));
    }

    @GetMapping
    public PageResponse<ProjectResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Project> projects = projectService.listOwnedBy(currentUser.userId(), pageable);
        return PageResponse.from(projects.map(ProjectResponse::from));
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable UUID id) {
        Project project = projectService.getOwnedOrThrow(id, currentUser.userId());
        return ProjectResponse.from(project);
    }

    @PatchMapping("/{id}")
    public ProjectResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProjectRequest request) {
        Project project = projectService.edit(
                id, currentUser.userId(), request.name(), request.clientPersonId(), request.status(),
                request.deadline(), request.version());
        return ProjectResponse.from(project);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        projectService.delete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
