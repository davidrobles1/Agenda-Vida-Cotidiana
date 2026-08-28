package com.vidacotidiana.resource.application;

import com.vidacotidiana.person.application.PersonService;
import com.vidacotidiana.project.application.ProjectService;
import com.vidacotidiana.resource.domain.Resource;
import com.vidacotidiana.resource.domain.ResourceRepository;
import com.vidacotidiana.resource.domain.ResourceType;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for the Resource vertical slice (ADR-016 adenda Fase
 * 3e4, FR-034, UC-27, AC-021). Owner-only, same locking pattern as
 * person.application.PersonService.
 *
 * Depends on PersonService/ProjectService only to reuse their ownership
 * checks for the optional links — exactly the same cross-module reuse
 * pattern as note.application.NoteService and
 * document.application.DocumentService.
 */
@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final PersonService personService;
    private final ProjectService projectService;

    public ResourceService(ResourceRepository resourceRepository, PersonService personService,
                           ProjectService projectService) {
        this.resourceRepository = resourceRepository;
        this.personService = personService;
        this.projectService = projectService;
    }

    @Transactional
    public Resource create(UUID ownerUserId, String name, ResourceType type, String reference,
                           String description, UUID personId, UUID projectId) {
        requireOwnedLinks(ownerUserId, personId, projectId);
        Resource resource = new Resource(ownerUserId, name, type, reference, description, personId, projectId);
        return resourceRepository.save(resource);
    }

    @Transactional(readOnly = true)
    public Page<Resource> listOwnedBy(UUID ownerUserId, Pageable pageable) {
        return resourceRepository.findByOwnerUserId(ownerUserId, pageable);
    }

    @Transactional(readOnly = true)
    public Resource getOwnedOrThrow(UUID resourceId, UUID callerUserId) {
        Resource resource = findOrThrow(resourceId);
        requireOwner(resource, callerUserId);
        return resource;
    }

    @Transactional
    public Resource edit(UUID resourceId, UUID callerUserId, String name, ResourceType type, String reference,
                         String description, UUID personId, UUID projectId, int expectedVersion) {
        Resource resource = getOwnedOrThrow(resourceId, callerUserId);
        requireOwnedLinks(callerUserId, personId, projectId);

        if (expectedVersion != resource.getVersion()) {
            throw new ConflictException("RESOURCE_VERSION_CONFLICT",
                    "Resource " + resourceId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + resource.getVersion() + ").");
        }

        resource.applyEdit(name, type, reference, description, personId, projectId);
        try {
            return resourceRepository.save(resource);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("RESOURCE_VERSION_CONFLICT",
                    "Resource " + resourceId + " was modified concurrently; refetch and retry.");
        }
    }

    @Transactional
    public void delete(UUID resourceId, UUID callerUserId) {
        Resource resource = getOwnedOrThrow(resourceId, callerUserId);
        resourceRepository.delete(resource);
    }

    private void requireOwnedLinks(UUID ownerUserId, UUID personId, UUID projectId) {
        if (personId != null) {
            personService.getOwnedOrThrow(personId, ownerUserId);
        }
        if (projectId != null) {
            projectService.getOwnedOrThrow(projectId, ownerUserId);
        }
    }

    private Resource findOrThrow(UUID resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("RESOURCE_NOT_FOUND", "The requested resource was not found."));
    }

    private void requireOwner(Resource resource, UUID callerUserId) {
        if (!resource.isOwnedBy(callerUserId)) {
            throw new NotFoundException("RESOURCE_NOT_FOUND", "The requested resource was not found.");
        }
    }
}
