package com.vidacotidiana.sharing.api.dto;

import jakarta.validation.constraints.Email;

/**
 * Aligned with components.schemas.CreateInvitationRequest in openapi.yaml.
 * Exactly one of email/username must be provided — enforced in
 * sharing.application.SharingService (a cross-field rule that plain bean
 * validation annotations on a record can't express), not here.
 */
public record CreateInvitationRequest(
        @Email String email,
        String username
) {
}
