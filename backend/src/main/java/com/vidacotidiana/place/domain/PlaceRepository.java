package com.vidacotidiana.place.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** ADR-016 Fase 3e3/FR-033. Owner-only entity, same shape as person.domain.PersonRepository. */
public interface PlaceRepository extends JpaRepository<Place, UUID> {

    Page<Place> findByOwnerUserId(UUID ownerUserId, Pageable pageable);
}
