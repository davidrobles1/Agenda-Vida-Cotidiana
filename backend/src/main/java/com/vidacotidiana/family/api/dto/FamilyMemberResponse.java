package com.vidacotidiana.family.api.dto;

import com.vidacotidiana.family.application.FamilyService;

/** Un integrante ya aceptado de la familia de quien llama. */
public record FamilyMemberResponse(String userId, String username, String since) {

    public static FamilyMemberResponse from(FamilyService.MemberView view) {
        return new FamilyMemberResponse(
                view.user().getId().toString(),
                view.user().getUsername(),
                view.link().getCreatedAt().toString());
    }
}
