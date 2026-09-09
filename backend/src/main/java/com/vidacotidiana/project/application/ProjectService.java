package com.vidacotidiana.project.application;

import com.vidacotidiana.person.application.PersonService;
import com.vidacotidiana.project.domain.ParticipantRole;
import com.vidacotidiana.project.domain.Project;
import com.vidacotidiana.project.domain.ProjectParticipant;
import com.vidacotidiana.project.domain.ProjectParticipantRepository;
import com.vidacotidiana.project.domain.ProjectRepository;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.ValidationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
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
    private final ProjectParticipantRepository participantRepository;

    public ProjectService(ProjectRepository projectRepository, PersonService personService,
                          ProjectParticipantRepository participantRepository) {
        this.projectRepository = projectRepository;
        this.personService = personService;
        this.participantRepository = participantRepository;
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
            // La misma regla que `addParticipant`, por el otro lado: sin esto se
            // podría nombrar cliente a quien ya es participante y la persona
            // aparecería dos veces con dos papeles distintos (V32).
            if (participantRepository.existsByProjectIdAndPersonId(projectId, clientPersonId)) {
                throw new ValidationException(
                        "Esa persona ya participa en este proyecto con otro rol. Quítala de la lista de "
                                + "participantes antes de nombrarla cliente.");
            }
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

    /* ---------------------------------------------------------------------
       Participantes (V32) — quién más está en este proyecto, y con qué rol.

       El CLIENTE no vive aquí: sigue siendo `Project.clientPersonId`. Cliente
       y participantes son dos mitades disjuntas de "quién está en el
       proyecto", y `requireNotAlreadyTheClient` es lo que las mantiene así.
       --------------------------------------------------------------------- */

    @Transactional(readOnly = true)
    public List<ProjectParticipant> listParticipants(UUID projectId, UUID callerUserId) {
        // Verifica la propiedad del proyecto antes de leer nada: el id llega de
        // la ruta y no puede confiarse.
        getOwnedOrThrow(projectId, callerUserId);
        return participantRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
    }

    /** Todas las participaciones del usuario, para responder "¿en cuántos
        proyectos está esta persona?" sin una consulta por persona. */
    @Transactional(readOnly = true)
    public List<ProjectParticipant> listAllParticipations(UUID callerUserId) {
        return participantRepository.findByOwnerUserId(callerUserId);
    }

    /**
     * Añade a una persona al proyecto.
     *
     * Tres restricciones, todas en el servicio y no solo en la pantalla:
     * proyecto y persona deben ser del mismo dueño (404 si no, nunca 403 —
     * misma regla de no-enumeración que el resto, AC-004/SEC-001); nadie se
     * añade dos veces; y quien ya es el cliente no puede añadirse además como
     * participante, que es lo que evita las dos verdades.
     */
    @Transactional
    public ProjectParticipant addParticipant(UUID projectId, UUID callerUserId, UUID personId,
                                             ParticipantRole role) {
        Project project = getOwnedOrThrow(projectId, callerUserId);
        personService.getOwnedOrThrow(personId, callerUserId);
        requireNotAlreadyTheClient(project, personId);

        if (participantRepository.existsByProjectIdAndPersonId(projectId, personId)) {
            throw new ConflictException("PARTICIPANT_ALREADY_ADDED",
                    "Esa persona ya participa en este proyecto.");
        }

        try {
            return participantRepository.save(
                    new ProjectParticipant(callerUserId, projectId, personId, role));
        } catch (DataIntegrityViolationException lostRaceToConcurrentInsert) {
            // La comprobación de arriba puede perderse con dos peticiones a la
            // vez; el UNIQUE de la tabla es lo que de verdad lo impide.
            throw new ConflictException("PARTICIPANT_ALREADY_ADDED",
                    "Esa persona ya participa en este proyecto.");
        }
    }

    @Transactional
    public void removeParticipant(UUID projectId, UUID participantId, UUID callerUserId) {
        getOwnedOrThrow(projectId, callerUserId);
        ProjectParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new NotFoundException("PARTICIPANT_NOT_FOUND",
                        "The requested participant was not found."));
        // Que el participante exista no basta: tiene que ser de ESTE proyecto y
        // de este dueño. Sin las dos comprobaciones, el id de la ruta permitiría
        // borrar la participación de otro proyecto.
        if (!participant.isOwnedBy(callerUserId) || !participant.getProjectId().equals(projectId)) {
            throw new NotFoundException("PARTICIPANT_NOT_FOUND", "The requested participant was not found.");
        }
        participantRepository.delete(participant);
    }

    /**
     * DECISION del Product Owner (2026-09-06): cliente y participantes
     * conviven. Que convivan sin contradecirse exige que sean disjuntos — si
     * la misma persona pudiera ser el cliente Y un participante CONTACTO, dos
     * pantallas darían dos respuestas distintas a "¿qué es Ana en esta obra?".
     */
    private void requireNotAlreadyTheClient(Project project, UUID personId) {
        if (personId.equals(project.getClientPersonId())) {
            throw new ValidationException(
                    "Esa persona ya es el cliente de este proyecto. El cliente y los participantes "
                            + "son listas distintas: para cambiarle el papel, cambia primero el cliente.");
        }
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
