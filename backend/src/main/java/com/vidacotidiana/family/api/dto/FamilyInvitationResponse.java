package com.vidacotidiana.family.api.dto;

import com.vidacotidiana.family.application.FamilyService;
import com.vidacotidiana.family.domain.FamilyInvitation;

/**
 * Una invitación familiar, vista desde cualquiera de los dos lados.
 *
 * `counterpartUsername` es "quién me invita" en las recibidas y "a quién
 * invité" en las enviadas — el mismo campo, porque la contraparte siempre es
 * la otra persona. No se expone el correo de nadie.
 */
public record FamilyInvitationResponse(
        String id,
        String counterpartUserId,
        String counterpartUsername,
        String status,
        String createdAt) {

    public static FamilyInvitationResponse from(FamilyService.InvitationView view) {
        FamilyInvitation invitation = view.invitation();
        return new FamilyInvitationResponse(
                invitation.getId().toString(),
                view.counterpart() == null ? null : view.counterpart().getId().toString(),
                view.counterpart() == null ? null : view.counterpart().getUsername(),
                invitation.getStatus().name(),
                invitation.getCreatedAt().toString());
    }
}
