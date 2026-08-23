package com.vidacotidiana.project.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** ADR-016. Owner-only entity, same shape as person.domain.PersonRepository. */
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Page<Project> findByOwnerUserId(UUID ownerUserId, Pageable pageable);
}
