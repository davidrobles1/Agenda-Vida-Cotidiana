package com.vidacotidiana.person.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** ADR-016. Owner-only entity, same shape as warranty.domain.WarrantyRepository. */
public interface PersonRepository extends JpaRepository<Person, UUID> {

    Page<Person> findByOwnerUserId(UUID ownerUserId, Pageable pageable);
}
