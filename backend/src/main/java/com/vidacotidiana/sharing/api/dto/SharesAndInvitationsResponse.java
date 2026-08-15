package com.vidacotidiana.sharing.api.dto;

import com.vidacotidiana.sharing.application.SharingService;

import java.util.List;

/**
 * Aligned with the GET /reminders/{id}/shares response in openapi.yaml:
 * PageMeta plus two separate arrays (shares, invitations), not one merged
 * list. The contract doesn't specify how a single PageMeta applies to two
 * independent collections; this implementation pages each query with the
 * same page/size (V1 scope: the collaborator/invitation list for a single
 * reminder is small) and reports totalElements as the sum of both and
 * totalPages as the larger of the two — a pragmatic, documented choice, not
 * a claim that the contract mandates this exact combination rule.
 */
public record SharesAndInvitationsResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<ReminderShareResponse> shares,
        List<InvitationResponse> invitations
) {
    // DEVOPS-002/spotbugs EI_EXPOSE_REP/REP2: defensive copy, same reasoning as shared.api.PageResponse.
    public SharesAndInvitationsResponse {
        shares = List.copyOf(shares);
        invitations = List.copyOf(invitations);
    }

    public static SharesAndInvitationsResponse from(SharingService.SharesAndInvitations result) {
        return new SharesAndInvitationsResponse(
                result.shares().getNumber(),
                result.shares().getSize(),
                result.shares().getTotalElements() + result.invitations().getTotalElements(),
                Math.max(result.shares().getTotalPages(), result.invitations().getTotalPages()),
                result.shares().getContent().stream().map(ReminderShareResponse::from).toList(),
                result.invitations().getContent().stream().map(InvitationResponse::from).toList()
        );
    }
}
