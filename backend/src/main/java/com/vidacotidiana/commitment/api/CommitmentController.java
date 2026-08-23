package com.vidacotidiana.commitment.api;

import com.vidacotidiana.commitment.api.dto.CommitmentResponse;
import com.vidacotidiana.commitment.api.dto.CreateCommitmentRequest;
import com.vidacotidiana.commitment.api.dto.ResolveCommitmentRequest;
import com.vidacotidiana.commitment.api.dto.UpdateCommitmentRequest;
import com.vidacotidiana.commitment.application.CommitmentService;
import com.vidacotidiana.commitment.domain.Commitment;
import com.vidacotidiana.commitment.domain.CommitmentDirection;
import com.vidacotidiana.identity.infrastructure.CurrentUser;
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
 * ADR-016/FR-025/FR-027: Compromiso CRUD + resolve, owner-only. The
 * "Mías"/"Esperando" tabs (UC-20) are a single GET with an optional
 * direction filter, not two endpoints — same entity, filtered.
 */
@RestController
@RequestMapping("/api/v1/commitments")
public class CommitmentController {

    private final CommitmentService commitmentService;
    private final CurrentUser currentUser;

    public CommitmentController(CommitmentService commitmentService, CurrentUser currentUser) {
        this.commitmentService = commitmentService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<CommitmentResponse> create(@Valid @RequestBody CreateCommitmentRequest request) {
        Commitment created = commitmentService.create(
                currentUser.userId(), request.personId(), request.description(),
                CommitmentDirection.valueOf(request.direction()), request.dueAt(),
                request.projectId(), request.originReminderId());
        return ResponseEntity.status(HttpStatus.CREATED).body(CommitmentResponse.from(created));
    }

    @GetMapping
    public PageResponse<CommitmentResponse> list(
            @RequestParam(required = false) String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        CommitmentDirection parsedDirection = (direction != null) ? CommitmentDirection.valueOf(direction) : null;
        Page<Commitment> commitments = commitmentService.listOwnedBy(currentUser.userId(), parsedDirection, pageable);
        return PageResponse.from(commitments.map(CommitmentResponse::from));
    }

    @GetMapping("/{id}")
    public CommitmentResponse get(@PathVariable UUID id) {
        Commitment commitment = commitmentService.getOwnedOrThrow(id, currentUser.userId());
        return CommitmentResponse.from(commitment);
    }

    @PatchMapping("/{id}")
    public CommitmentResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCommitmentRequest request) {
        CommitmentDirection direction = (request.direction() != null) ? CommitmentDirection.valueOf(request.direction()) : null;
        Commitment commitment = commitmentService.edit(
                id, currentUser.userId(), request.personId(), request.description(), direction,
                request.dueAt(), request.projectId(), request.version());
        return CommitmentResponse.from(commitment);
    }

    @PostMapping("/{id}/resolve")
    public CommitmentResponse resolve(@PathVariable UUID id,
                                       @RequestBody(required = false) ResolveCommitmentRequest request) {
        Integer expectedVersion = (request != null) ? request.version() : null;
        Commitment commitment = commitmentService.resolve(id, currentUser.userId(), expectedVersion);
        return CommitmentResponse.from(commitment);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        commitmentService.delete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
