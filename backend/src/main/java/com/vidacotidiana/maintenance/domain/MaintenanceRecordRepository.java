package com.vidacotidiana.maintenance.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * BE-037. Owner-only entity — mirrors warranty.domain.WarrantyRepository:
 * no sharing/collaborator concept for MaintenanceRecord in V2.
 */
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, UUID> {

    Page<MaintenanceRecord> findByOwnerUserId(UUID ownerUserId, Pageable pageable);
}
