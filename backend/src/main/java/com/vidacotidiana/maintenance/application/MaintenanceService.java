package com.vidacotidiana.maintenance.application;

import com.vidacotidiana.maintenance.domain.MaintenanceRecord;
import com.vidacotidiana.maintenance.domain.MaintenanceRecordRepository;
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
 * Application service for the MaintenanceRecord vertical slice (BE-037).
 * Mirrors warranty.application.WarrantyService exactly — owner-only
 * throughout, same non-enumeration rule (404 never 403).
 */
@Service
public class MaintenanceService {

    private final MaintenanceRecordRepository maintenanceRecordRepository;

    public MaintenanceService(MaintenanceRecordRepository maintenanceRecordRepository) {
        this.maintenanceRecordRepository = maintenanceRecordRepository;
    }

    @Transactional
    public MaintenanceRecord create(UUID ownerUserId, String item, Instant nextDueAt) {
        MaintenanceRecord record = new MaintenanceRecord(ownerUserId, item, nextDueAt);
        return maintenanceRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public Page<MaintenanceRecord> listOwnedBy(UUID ownerUserId, Pageable pageable) {
        return maintenanceRecordRepository.findByOwnerUserId(ownerUserId, pageable);
    }

    @Transactional(readOnly = true)
    public MaintenanceRecord getOwnedOrThrow(UUID recordId, UUID callerUserId) {
        MaintenanceRecord record = findOrThrow(recordId);
        requireOwner(record, callerUserId);
        return record;
    }

    @Transactional
    public MaintenanceRecord toggleCompletion(UUID recordId, UUID callerUserId, Integer expectedVersion) {
        MaintenanceRecord record = getOwnedOrThrow(recordId, callerUserId);

        if (expectedVersion != null && expectedVersion != record.getVersion()) {
            throw new ConflictException("MAINTENANCE_VERSION_CONFLICT",
                    "MaintenanceRecord " + recordId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + record.getVersion() + ").");
        }

        record.toggleCompletion();
        try {
            return maintenanceRecordRepository.save(record);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("MAINTENANCE_VERSION_CONFLICT",
                    "MaintenanceRecord " + recordId + " was modified concurrently; refetch and retry.");
        }
    }

    @Transactional
    public MaintenanceRecord edit(UUID recordId, UUID callerUserId, String item, Instant nextDueAt, int expectedVersion) {
        MaintenanceRecord record = getOwnedOrThrow(recordId, callerUserId);

        if (expectedVersion != record.getVersion()) {
            throw new ConflictException("MAINTENANCE_VERSION_CONFLICT",
                    "MaintenanceRecord " + recordId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + record.getVersion() + ").");
        }

        record.applyEdit(item, nextDueAt);
        try {
            return maintenanceRecordRepository.save(record);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("MAINTENANCE_VERSION_CONFLICT",
                    "MaintenanceRecord " + recordId + " was modified concurrently; refetch and retry.");
        }
    }

    @Transactional
    public void delete(UUID recordId, UUID callerUserId) {
        MaintenanceRecord record = getOwnedOrThrow(recordId, callerUserId);
        maintenanceRecordRepository.delete(record);
    }

    private MaintenanceRecord findOrThrow(UUID recordId) {
        return maintenanceRecordRepository.findById(recordId)
                .orElseThrow(() -> new NotFoundException("MAINTENANCE_RECORD_NOT_FOUND", "The requested maintenance record was not found."));
    }

    private void requireOwner(MaintenanceRecord record, UUID callerUserId) {
        if (!record.isOwnedBy(callerUserId)) {
            throw new NotFoundException("MAINTENANCE_RECORD_NOT_FOUND", "The requested maintenance record was not found.");
        }
    }
}
