package com.vidacotidiana.user.api;

import com.vidacotidiana.user.domain.User;

import java.util.UUID;

/** Aligned with components.schemas.User in Documentacion/openapi/openapi.yaml. */
public record UserResponse(UUID id, String email, String username, String deletionStatus) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getUsername(), user.getDeletionStatus());
    }
}
