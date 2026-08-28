package com.vidacotidiana.objective.application;

import com.vidacotidiana.objective.domain.Objective;
import com.vidacotidiana.objective.domain.ObjectiveRepository;
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
 * Application service for the Objective vertical slice (ADR-016 adenda
 * Fase 3e1, FR-031, UC-24, AC-018). Same authorization/locking pattern as
 * person.application.PersonService: owner-only throughout, 404 (never 403)
 * on a resource owned by someone else, mandatory version on edit.
 *
 * Unlike ProjectService/CommitmentService this service depends on no other
 * module — an Objective is standalone in this increment (FR-031 explicitly
 * leaves the link to Project/Person out of scope).
 */
@Service
public class ObjectiveService {

    private final ObjectiveRepository objectiveRepository;

    public ObjectiveService(ObjectiveRepository objectiveRepository) {
        this.objectiveRepository = objectiveRepository;
    }

    @Transactional
    public Objective create(UUID ownerUserId, String title, Integer targetValue, Integer currentValue, Instant deadline) {
        Objective objective = new Objective(ownerUserId, title, targetValue, currentValue, deadline);
        return objectiveRepository.save(objective);
    }

    @Transactional(readOnly = true)
    public Page<Objective> listOwnedBy(UUID ownerUserId, Pageable pageable) {
        return objectiveRepository.findByOwnerUserId(ownerUserId, pageable);
    }

    @Transactional(readOnly = true)
    public Objective getOwnedOrThrow(UUID objectiveId, UUID callerUserId) {
        Objective objective = findOrThrow(objectiveId);
        requireOwner(objective, callerUserId);
        return objective;
    }

    @Transactional
    public Objective edit(UUID objectiveId, UUID callerUserId, String title, Integer targetValue,
                          Integer currentValue, Instant deadline, Boolean completed, int expectedVersion) {
        Objective objective = getOwnedOrThrow(objectiveId, callerUserId);

        if (expectedVersion != objective.getVersion()) {
            throw new ConflictException("OBJECTIVE_VERSION_CONFLICT",
                    "Objective " + objectiveId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + objective.getVersion() + ").");
        }

        objective.applyEdit(title, targetValue, currentValue, deadline, completed);
        try {
            return objectiveRepository.save(objective);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("OBJECTIVE_VERSION_CONFLICT",
                    "Objective " + objectiveId + " was modified concurrently; refetch and retry.");
        }
    }

    @Transactional
    public void delete(UUID objectiveId, UUID callerUserId) {
        Objective objective = getOwnedOrThrow(objectiveId, callerUserId);
        objectiveRepository.delete(objective);
    }

    private Objective findOrThrow(UUID objectiveId) {
        return objectiveRepository.findById(objectiveId)
                .orElseThrow(() -> new NotFoundException("OBJECTIVE_NOT_FOUND", "The requested objective was not found."));
    }

    private void requireOwner(Objective objective, UUID callerUserId) {
        if (!objective.isOwnedBy(callerUserId)) {
            throw new NotFoundException("OBJECTIVE_NOT_FOUND", "The requested objective was not found.");
        }
    }
}
