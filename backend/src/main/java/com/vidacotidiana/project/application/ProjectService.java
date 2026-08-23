package com.vidacotidiana.project.application;

import com.vidacotidiana.person.application.PersonService;
import com.vidacotidiana.project.domain.Project;
import com.vidacotidiana.project.domain.ProjectRepository;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Application service for the Project vertical slice (ADR-016, FR-022,
 * UC-19). Owner-only, same locking pattern as person.application.
 * PersonService. Depends on PersonService (not PersonRepository directly)
 * to reuse its ownership check for {@code clientPersonId} — same
 * cross-module reuse pattern as reminder.application.ReminderService's
 * getOwnedOrThrow being reused by sharing.application.SharingService
 * (ADR-001: a modular monolith, no separate deployable boundary to
 * preserve here).
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final PersonService personService;

    public ProjectService(ProjectRepository projectRepository, PersonService personService) {
        this.projectRepository = projectRepository;
        this.personService = personService;
    }

    @Transactional
    public Project create(UUID ownerUserId, String name, UUID clientPersonId, String status, Instant deadline) {
        if (clientPersonId != null) {
            personService.getOwnedOrThrow(clientPersonId, ownerUserId);
        }
        Project project = new Project(ownerUserId, name, clientPersonId, status, deadline);
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public Page<Project> listOwnedBy(UUID ownerUserId, Pageable pageable) {
        return projectRepository.findByOwnerUserId(ownerUserId, pageable);
    }

    @Transactional(readOnly = true)
    public Project getOwnedOrThrow(UUID projectId, UUID callerUserId) {
        Project project = findOrThrow(projectId);
        requireOwner(project, callerUserId);
        return project;
    }

    @Transactional
    public Project edit(UUID projectId, UUID callerUserId, String name, UUID clientPersonId, String status,
                         Instant deadline, int expectedVersion) {
        Project project = getOwnedOrThrow(projectId, callerUserId);

        if (clientPersonId != null) {
            personService.getOwnedOrThrow(clientPersonId, callerUserId);
        }

        if (expectedVersion != project.getVersion()) {
            throw new ConflictException("PROJECT_VERSION_CONFLICT",
                    "Project " + projectId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + project.getVersion() + ").");
        }

        project.applyEdit(name, clientPersonId, status, deadline);
        try {
            return projectRepository.save(project);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("PROJECT_VERSION_CONFLICT",
                    "Project " + projectId + " was modified concurrently; refetch and retry.");
        }
    }

    @Transactional
    public void delete(UUID projectId, UUID callerUserId) {
        Project project = getOwnedOrThrow(projectId, callerUserId);
        projectRepository.delete(project);
    }

    private Project findOrThrow(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND", "The requested project was not found."));
    }

    private void requireOwner(Project project, UUID callerUserId) {
        if (!project.isOwnedBy(callerUserId)) {
            throw new NotFoundException("PROJECT_NOT_FOUND", "The requested project was not found.");
        }
    }
}
