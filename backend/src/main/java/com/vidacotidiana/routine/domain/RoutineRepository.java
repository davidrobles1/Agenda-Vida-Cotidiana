package com.vidacotidiana.routine.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** ADR-016 Fase 3e2/FR-032. Owner-only entity, same shape as person.domain.PersonRepository. */
public interface RoutineRepository extends JpaRepository<Routine, UUID> {

    Page<Routine> findByOwnerUserId(UUID ownerUserId, Pageable pageable);
}
