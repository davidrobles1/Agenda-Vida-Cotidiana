package com.vidacotidiana.commitment.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** ADR-016/FR-027. Owner-only entity; findByOwnerUserIdAndDirection backs the "Mías"/"Esperando" tabs. */
public interface CommitmentRepository extends JpaRepository<Commitment, UUID> {

    Page<Commitment> findByOwnerUserId(UUID ownerUserId, Pageable pageable);

    Page<Commitment> findByOwnerUserIdAndDirection(UUID ownerUserId, CommitmentDirection direction, Pageable pageable);
}
