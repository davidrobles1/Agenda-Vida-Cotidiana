package com.vidacotidiana.commitment.application;

import com.vidacotidiana.commitment.domain.Commitment;
import com.vidacotidiana.commitment.domain.CommitmentDirection;
import com.vidacotidiana.commitment.domain.CommitmentRepository;
import com.vidacotidiana.person.application.PersonService;
import com.vidacotidiana.project.application.ProjectService;
import com.vidacotidiana.reminder.application.ReminderService;
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
 * Application service for the Commitment vertical slice (ADR-016,
 * FR-025/FR-027, UC-18/UC-20). Owner-only. Depends on PersonService (person
 * is mandatory, ADR-016 ASSUMPTION), ProjectService (optional) and
 * ReminderService (optional originReminderId) purely to reuse their
 * ownership checks — same ADR-001 modular-monolith reuse pattern as
 * ProjectService reusing PersonService.
 */
@Service
public class CommitmentService {

    private final CommitmentRepository commitmentRepository;
    private final PersonService personService;
    private final ProjectService projectService;
    private final ReminderService reminderService;

    public CommitmentService(CommitmentRepository commitmentRepository, PersonService personService,
                              ProjectService projectService, ReminderService reminderService) {
        this.commitmentRepository = commitmentRepository;
        this.personService = personService;
        this.projectService = projectService;
        this.reminderService = reminderService;
    }

    @Transactional
    public Commitment create(UUID ownerUserId, UUID personId, String description, CommitmentDirection direction,
                              Instant dueAt, UUID projectId, UUID originReminderId) {
        personService.getOwnedOrThrow(personId, ownerUserId);
        if (projectId != null) {
            projectService.getOwnedOrThrow(projectId, ownerUserId);
        }
        if (originReminderId != null) {
            reminderService.getOwnedOrThrow(originReminderId, ownerUserId);
        }

        Commitment commitment = new Commitment(ownerUserId, personId, description, direction, dueAt, projectId, originReminderId);
        return commitmentRepository.save(commitment);
    }

    /** FR-027: direction is optional — absent means "Mías" + "Esperando" together (no tab filter). */
    @Transactional(readOnly = true)
    public Page<Commitment> listOwnedBy(UUID ownerUserId, CommitmentDirection direction, Pageable pageable) {
        if (direction != null) {
            return commitmentRepository.findByOwnerUserIdAndDirection(ownerUserId, direction, pageable);
        }
        return commitmentRepository.findByOwnerUserId(ownerUserId, pageable);
    }

    @Transactional(readOnly = true)
    public Commitment getOwnedOrThrow(UUID commitmentId, UUID callerUserId) {
        Commitment commitment = findOrThrow(commitmentId);
        requireOwner(commitment, callerUserId);
        return commitment;
    }

    /**
     * UC-20. If expectedVersion is present, validates it against the stored
     * version first; if absent, resolves without a concurrency check — same
     * optional-version contract as Reminder/Warranty's complete action.
     */
    @Transactional
    public Commitment resolve(UUID commitmentId, UUID callerUserId, Integer expectedVersion) {
        Commitment commitment = getOwnedOrThrow(commitmentId, callerUserId);

        if (expectedVersion != null && expectedVersion != commitment.getVersion()) {
            throw new ConflictException("COMMITMENT_VERSION_CONFLICT",
                    "Commitment " + commitmentId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + commitment.getVersion() + ").");
        }

        commitment.resolve();
        try {
            return commitmentRepository.save(commitment);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("COMMITMENT_VERSION_CONFLICT",
                    "Commitment " + commitmentId + " was modified concurrently; refetch and retry.");
        }
    }

    /** UC-20 "reprogramar": a plain PATCH updating dueAt (and/or any other field) — no separate reschedule action, version is mandatory. */
    @Transactional
    public Commitment edit(UUID commitmentId, UUID callerUserId, UUID personId, String description,
                            CommitmentDirection direction, Instant dueAt, UUID projectId, int expectedVersion) {
        Commitment commitment = getOwnedOrThrow(commitmentId, callerUserId);

        if (personId != null) {
            personService.getOwnedOrThrow(personId, callerUserId);
        }
        if (projectId != null) {
            projectService.getOwnedOrThrow(projectId, callerUserId);
        }

        if (expectedVersion != commitment.getVersion()) {
            throw new ConflictException("COMMITMENT_VERSION_CONFLICT",
                    "Commitment " + commitmentId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + commitment.getVersion() + ").");
        }

        commitment.applyEdit(personId, description, direction, dueAt, projectId);
        try {
            return commitmentRepository.save(commitment);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("COMMITMENT_VERSION_CONFLICT",
                    "Commitment " + commitmentId + " was modified concurrently; refetch and retry.");
        }
    }

    @Transactional
    public void delete(UUID commitmentId, UUID callerUserId) {
        Commitment commitment = getOwnedOrThrow(commitmentId, callerUserId);
        commitmentRepository.delete(commitment);
    }

    private Commitment findOrThrow(UUID commitmentId) {
        return commitmentRepository.findById(commitmentId)
                .orElseThrow(() -> new NotFoundException("COMMITMENT_NOT_FOUND", "The requested commitment was not found."));
    }

    private void requireOwner(Commitment commitment, UUID callerUserId) {
        if (!commitment.isOwnedBy(callerUserId)) {
            throw new NotFoundException("COMMITMENT_NOT_FOUND", "The requested commitment was not found.");
        }
    }
}
