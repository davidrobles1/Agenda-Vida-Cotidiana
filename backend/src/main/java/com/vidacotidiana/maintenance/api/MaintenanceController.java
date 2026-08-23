package com.vidacotidiana.maintenance.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.maintenance.api.dto.CompleteMaintenanceRecordRequest;
import com.vidacotidiana.maintenance.api.dto.CreateMaintenanceRecordRequest;
import com.vidacotidiana.maintenance.api.dto.MaintenanceRecordResponse;
import com.vidacotidiana.maintenance.api.dto.UpdateMaintenanceRecordRequest;
import com.vidacotidiana.maintenance.application.MaintenanceService;
import com.vidacotidiana.maintenance.domain.MaintenanceRecord;
import com.vidacotidiana.shared.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * BE-037/WEB-009: full MaintenanceRecord CRUD, mirroring
 * warranty.api.WarrantyController exactly — owner-only throughout.
 */
@RestController
@RequestMapping("/api/v1/maintenance-records")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final CurrentUser currentUser;

    public MaintenanceController(MaintenanceService maintenanceService, CurrentUser currentUser) {
        this.maintenanceService = maintenanceService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<MaintenanceRecordResponse> create(@Valid @RequestBody CreateMaintenanceRecordRequest request) {
        MaintenanceRecord created = maintenanceService.create(currentUser.userId(), request.item(), request.nextDueAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(MaintenanceRecordResponse.from(created));
    }

    @GetMapping
    public PageResponse<MaintenanceRecordResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<MaintenanceRecord> records = maintenanceService.listOwnedBy(currentUser.userId(), pageable);
        return PageResponse.from(records.map(MaintenanceRecordResponse::from));
    }

    @GetMapping("/{id}")
    public MaintenanceRecordResponse get(@PathVariable UUID id) {
        MaintenanceRecord record = maintenanceService.getOwnedOrThrow(id, currentUser.userId());
        return MaintenanceRecordResponse.from(record);
    }

    @PostMapping("/{id}/complete")
    public MaintenanceRecordResponse complete(@PathVariable UUID id,
                                               @RequestBody(required = false) CompleteMaintenanceRecordRequest request) {
        Integer expectedVersion = (request != null) ? request.version() : null;
        MaintenanceRecord record = maintenanceService.toggleCompletion(id, currentUser.userId(), expectedVersion);
        return MaintenanceRecordResponse.from(record);
    }

    @PatchMapping("/{id}")
    public MaintenanceRecordResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateMaintenanceRecordRequest request) {
        MaintenanceRecord record = maintenanceService.edit(id, currentUser.userId(), request.item(), request.nextDueAt(), request.version());
        return MaintenanceRecordResponse.from(record);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        maintenanceService.delete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
