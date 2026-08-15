package com.vidacotidiana.sharing.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.shared.api.PageResponse;
import com.vidacotidiana.sharing.api.dto.InvitationResponse;
import com.vidacotidiana.sharing.api.dto.ReminderShareResponse;
import com.vidacotidiana.sharing.application.SharingService;
import com.vidacotidiana.sharing.domain.Invitation;
import com.vidacotidiana.sharing.domain.ReminderShare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** BE-018/019/020 — Documentacion/openapi/openapi.yaml, /me/invitations and /invitations/*. */
@RestController
@RequestMapping("/api/v1")
public class InvitationController {

    private final SharingService sharingService;
    private final CurrentUser currentUser;

    public InvitationController(SharingService sharingService, CurrentUser currentUser) {
        this.sharingService = sharingService;
        this.currentUser = currentUser;
    }

    @GetMapping("/me/invitations")
    public PageResponse<InvitationResponse> myInvitations(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Invitation> invitations = sharingService.listMyPendingInvitations(currentUser.userId(), pageable);
        return PageResponse.from(invitations.map(InvitationResponse::from));
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public ReminderShareResponse accept(@PathVariable UUID invitationId) {
        ReminderShare share = sharingService.acceptInvitation(invitationId, currentUser.userId());
        return ReminderShareResponse.from(share);
    }

    @PostMapping("/invitations/{invitationId}/reject")
    public InvitationResponse reject(@PathVariable UUID invitationId) {
        Invitation invitation = sharingService.rejectInvitation(invitationId, currentUser.userId());
        return InvitationResponse.from(invitation);
    }

    @DeleteMapping("/invitations/{invitationId}")
    public ResponseEntity<Void> cancel(@PathVariable UUID invitationId) {
        sharingService.cancelInvitation(invitationId, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
