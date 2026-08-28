package com.vidacotidiana.objective.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** ADR-016 Fase 3e1/FR-031. Owner-only entity, same shape as person.domain.PersonRepository. */
public interface ObjectiveRepository extends JpaRepository<Objective, UUID> {

    Page<Objective> findByOwnerUserId(UUID ownerUserId, Pageable pageable);
}
