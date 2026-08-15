package com.vidacotidiana.notification.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Aligned with components.schemas.RegisterDeviceRequest in openapi.yaml.
 * platform is accepted as a plain string (not deserialized straight into
 * DevicePlatform) so an invalid value produces the uniform 400
 * VALIDATION_ERROR envelope (notification.application.DeviceRegistrationService)
 * instead of a raw Jackson deserialization failure.
 */
public record RegisterDeviceRequest(
        @NotBlank String platform,
        @NotBlank String token
) {
}
