package com.vidacotidiana.sharing.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.sharing.api.dto.CreateInvitationRequest;
import com.vidacotidiana.sharing.api.dto.InvitationResponse;
import com.vidacotidiana.sharing.api.dto.SharesAndInvitationsResponse;
import com.vidacotidiana.sharing.application.SharingService;
import com.vidacotidiana.sharing.domain.Invitation;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** BE-017/018/021 — Documentacion/openapi/openapi.yaml, /reminders/{id}/shares*. Owner-only throughout. */
@RestController
@RequestMapping("/api/v1/reminders/{id}/shares")
public class ReminderShareController {

    private final SharingService sharingService;
    private final CurrentUser currentUser;

    public ReminderShareController(SharingService sharingService, CurrentUser currentUser) {
        this.sharingService = sharingService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<InvitationResponse> create(@PathVariable UUID id, @Valid @RequestBody CreateInvitationRequest request) {
        Invitation invitation = sharingService.createInvitation(id, currentUser.userId(), request.email(), request.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(InvitationResponse.from(invitation));
    }

    @GetMapping
    public SharesAndInvitationsResponse list(@PathVariable UUID id,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        SharingService.SharesAndInvitations result = sharingService.listSharesAndInvitations(id, currentUser.userId(), pageable);
        return SharesAndInvitationsResponse.from(result);
    }

    @DeleteMapping("/{shareId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID id, @PathVariable UUID shareId) {
        sharingService.revokeShare(id, shareId, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
