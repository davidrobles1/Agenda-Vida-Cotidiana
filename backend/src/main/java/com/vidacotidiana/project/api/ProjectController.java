package com.vidacotidiana.project.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.project.api.dto.AddParticipantRequest;
import com.vidacotidiana.project.api.dto.CreateProjectRequest;
import com.vidacotidiana.project.api.dto.ProjectParticipantResponse;
import com.vidacotidiana.project.api.dto.ProjectResponse;
import com.vidacotidiana.project.api.dto.UpdateProjectRequest;
import com.vidacotidiana.project.application.ProjectService;
import com.vidacotidiana.project.domain.Project;
import com.vidacotidiana.project.domain.ProjectParticipant;
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

import java.util.List;
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

    /* ---------------------------------------------------------------------
       Participantes (V32). Rutas anidadas bajo el proyecto porque una
       participación no existe fuera de él: `/projects/{id}/participants` dice
       eso en la propia URL, y hace que la comprobación de propiedad del
       proyecto sea obligatoria en todas ellas.
       --------------------------------------------------------------------- */

    @GetMapping("/{id}/participants")
    public List<ProjectParticipantResponse> listParticipants(@PathVariable UUID id) {
        return projectService.listParticipants(id, currentUser.userId()).stream()
                .map(ProjectParticipantResponse::from)
                .toList();
    }

    /**
     * Todas las participaciones del usuario, para "¿en cuántos proyectos está
     * esta persona?" sin una llamada por persona.
     *
     * `/participations` es un segmento literal y convive con `/{id}` sin
     * ambigüedad: Spring ordena los patrones y un segmento literal siempre
     * gana a uno con variable. Además ningún id real podría chocar — son UUID.
     */
    @GetMapping("/participations")
    public List<ProjectParticipantResponse> listAllParticipations() {
        return projectService.listAllParticipations(currentUser.userId()).stream()
                .map(ProjectParticipantResponse::from)
                .toList();
    }

    @PostMapping("/{id}/participants")
    public ResponseEntity<ProjectParticipantResponse> addParticipant(
            @PathVariable UUID id, @Valid @RequestBody AddParticipantRequest request) {
        ProjectParticipant added = projectService.addParticipant(
                id, currentUser.userId(), request.personId(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectParticipantResponse.from(added));
    }

    @DeleteMapping("/{id}/participants/{participantId}")
    public ResponseEntity<Void> removeParticipant(@PathVariable UUID id, @PathVariable UUID participantId) {
        projectService.removeParticipant(id, participantId, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
