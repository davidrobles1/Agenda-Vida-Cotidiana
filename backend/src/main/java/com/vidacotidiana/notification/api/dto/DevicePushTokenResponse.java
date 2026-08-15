package com.vidacotidiana.notification.api.dto;

import com.vidacotidiana.notification.domain.DevicePushToken;

import java.time.Instant;
import java.util.UUID;

/** Aligned with components.schemas.DevicePushToken in openapi.yaml — never exposes the raw token or userId. */
public record DevicePushTokenResponse(
        UUID id,
        String platform,
        Instant createdAt,
        Instant lastSeenAt
) {
    public static DevicePushTokenResponse from(DevicePushToken device) {
        return new DevicePushTokenResponse(device.getId(), device.getPlatform().name(), device.getCreatedAt(), device.getLastSeenAt());
    }
}
