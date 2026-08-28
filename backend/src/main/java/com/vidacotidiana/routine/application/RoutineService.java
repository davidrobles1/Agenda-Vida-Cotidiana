package com.vidacotidiana.routine.application;

import com.vidacotidiana.routine.domain.Routine;
import com.vidacotidiana.routine.domain.RoutineFrequency;
import com.vidacotidiana.routine.domain.RoutineRepository;
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
 * Application service for the Routine vertical slice (ADR-016 adenda Fase
 * 3e2, FR-032, UC-25, AC-019). Owner-only, same locking pattern as
 * person.application.PersonService. Depends on no other module.
 *
 * <p>La regla de avance de {@code nextExecutionDate} (TBD abierto hasta el
 * 2026-08-28) fue resuelta por el Product Owner: se calcula desde la fecha
 * <b>originalmente programada</b>, no desde "ahora" — ver
 * {@link com.vidacotidiana.routine.domain.Routine#markExecuted()}.
 *
 * <p>Lo que esta clase <b>nunca</b> hará (FR-032, decisión explícita): crear
 * un REMINDER o un COMMITMENT. Eso pertenece a 3d (Automatizaciones
 * simples), que sigue BLOCKED por separado.
 */
@Service
public class RoutineService {

    private final RoutineRepository routineRepository;

    public RoutineService(RoutineRepository routineRepository) {
        this.routineRepository = routineRepository;
    }

    @Transactional
    public Routine create(UUID ownerUserId, String title, String description, RoutineFrequency frequency,
                          Instant nextExecutionDate) {
        Routine routine = new Routine(ownerUserId, title, description, frequency, nextExecutionDate);
        return routineRepository.save(routine);
    }

    @Transactional(readOnly = true)
    public Page<Routine> listOwnedBy(UUID ownerUserId, Pageable pageable) {
        return routineRepository.findByOwnerUserId(ownerUserId, pageable);
    }

    @Transactional(readOnly = true)
    public Routine getOwnedOrThrow(UUID routineId, UUID callerUserId) {
        Routine routine = findOrThrow(routineId);
        requireOwner(routine, callerUserId);
        return routine;
    }

    @Transactional
    public Routine edit(UUID routineId, UUID callerUserId, String title, String description,
                        RoutineFrequency frequency, Instant nextExecutionDate, Boolean active, int expectedVersion) {
        Routine routine = getOwnedOrThrow(routineId, callerUserId);

        if (expectedVersion != routine.getVersion()) {
            throw new ConflictException("ROUTINE_VERSION_CONFLICT",
                    "Routine " + routineId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + routine.getVersion() + ").");
        }

        routine.applyEdit(title, description, frequency, nextExecutionDate, active);
        try {
            return routineRepository.save(routine);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("ROUTINE_VERSION_CONFLICT",
                    "Routine " + routineId + " was modified concurrently; refetch and retry.");
        }
    }

    /**
     * UC-25/AC-019: marks the current occurrence as done, advancing
     * {@code nextExecutionDate} by exactly one period from the previously
     * scheduled date. Creates no Reminder and no Commitment (FR-032).
     *
     * <p>{@code expectedVersion} is optional, same contract as
     * CommitmentService#resolve and ReminderService#complete: when supplied
     * it is validated (so two devices marking the same occurrence cannot
     * double-advance the date), when omitted the action applies without a
     * concurrency check.
     */
    @Transactional
    public Routine markExecuted(UUID routineId, UUID callerUserId, Integer expectedVersion) {
        Routine routine = getOwnedOrThrow(routineId, callerUserId);

        if (expectedVersion != null && expectedVersion != routine.getVersion()) {
            throw new ConflictException("ROUTINE_VERSION_CONFLICT",
                    "Routine " + routineId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + routine.getVersion() + ").");
        }

        routine.markExecuted();
        try {
            return routineRepository.save(routine);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("ROUTINE_VERSION_CONFLICT",
                    "Routine " + routineId + " was modified concurrently; refetch and retry.");
        }
    }

    @Transactional
    public void delete(UUID routineId, UUID callerUserId) {
        Routine routine = getOwnedOrThrow(routineId, callerUserId);
        routineRepository.delete(routine);
    }

    private Routine findOrThrow(UUID routineId) {
        return routineRepository.findById(routineId)
                .orElseThrow(() -> new NotFoundException("ROUTINE_NOT_FOUND", "The requested routine was not found."));
    }

    private void requireOwner(Routine routine, UUID callerUserId) {
        if (!routine.isOwnedBy(callerUserId)) {
            throw new NotFoundException("ROUTINE_NOT_FOUND", "The requested routine was not found.");
        }
    }
}
