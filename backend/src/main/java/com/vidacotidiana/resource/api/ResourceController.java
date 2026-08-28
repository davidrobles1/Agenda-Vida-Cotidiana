package com.vidacotidiana.resource.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.resource.api.dto.CreateResourceRequest;
import com.vidacotidiana.resource.api.dto.ResourceResponse;
import com.vidacotidiana.resource.api.dto.UpdateResourceRequest;
import com.vidacotidiana.resource.application.ResourceService;
import com.vidacotidiana.resource.domain.Resource;
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
 * ADR-016 Fase 3e4/FR-034: Recurso CRUD, owner-only — same shape as
 * person.api.PersonController.
 *
 * <p>No endpoint here accepts a file upload: FR-034 keeps real documents
 * entirely inside DOCUMENT (FR-030). A Resource is metadata plus a text
 * reference, nothing more.
 */
@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {

    private final ResourceService resourceService;
    private final CurrentUser currentUser;

    public ResourceController(ResourceService resourceService, CurrentUser currentUser) {
        this.resourceService = resourceService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<ResourceResponse> create(@Valid @RequestBody CreateResourceRequest request) {
        Resource created = resourceService.create(currentUser.userId(), request.name(), request.type(),
                request.reference(), request.description(), request.personId(), request.projectId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ResourceResponse.from(created));
    }

    @GetMapping
    public PageResponse<ResourceResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Resource> resources = resourceService.listOwnedBy(currentUser.userId(), pageable);
        return PageResponse.from(resources.map(ResourceResponse::from));
    }

    @GetMapping("/{id}")
    public ResourceResponse get(@PathVariable UUID id) {
        Resource resource = resourceService.getOwnedOrThrow(id, currentUser.userId());
        return ResourceResponse.from(resource);
    }

    @PatchMapping("/{id}")
    public ResourceResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateResourceRequest request) {
        Resource resource = resourceService.edit(id, currentUser.userId(), request.name(), request.type(),
                request.reference(), request.description(), request.personId(), request.projectId(),
                request.version());
        return ResourceResponse.from(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        resourceService.delete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
