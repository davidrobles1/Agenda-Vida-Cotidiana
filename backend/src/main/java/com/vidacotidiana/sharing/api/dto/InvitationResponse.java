package com.vidacotidiana.sharing.api.dto;

import com.vidacotidiana.sharing.domain.Invitation;

import java.time.Instant;
import java.util.UUID;

/**
 * Aligned with components.schemas.Invitation in openapi.yaml. Deliberately
 * omits invitedUserId/inviterUserId (not part of the contract's response
 * schema) — SEC-001: the API must never reveal whether the invited email
 * belongs to an existing account.
 */
public record InvitationResponse(
        UUID id,
        UUID reminderId,
        String invitedEmail,
        String status,
        Instant expiresAt,
        Instant createdAt
) {
    public static InvitationResponse from(Invitation invitation) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getReminderId(),
                invitation.getInvitedEmail(),
                invitation.getStatus().name(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }
}
