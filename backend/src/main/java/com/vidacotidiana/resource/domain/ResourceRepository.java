package com.vidacotidiana.resource.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** ADR-016 Fase 3e4/FR-034. Owner-only entity, same shape as person.domain.PersonRepository. */
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    Page<Resource> findByOwnerUserId(UUID ownerUserId, Pageable pageable);
}
