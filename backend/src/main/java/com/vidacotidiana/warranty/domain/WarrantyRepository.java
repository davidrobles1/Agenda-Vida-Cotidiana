package com.vidacotidiana.warranty.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * BE-037. Owner-only entity — unlike ReminderRepository#findAccessibleTo,
 * there is no sharing/collaborator concept for Warranty in V2 (no
 * corresponding UI in web/src/features/warranties, no WARRANTY_SHARE
 * table): a plain owner-scoped query is enough.
 */
public interface WarrantyRepository extends JpaRepository<Warranty, UUID> {

    Page<Warranty> findByOwnerUserId(UUID ownerUserId, Pageable pageable);
}
