package com.vidacotidiana.notification.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.notification.api.dto.DevicePushTokenResponse;
import com.vidacotidiana.notification.api.dto.RegisterDeviceRequest;
import com.vidacotidiana.notification.application.DeviceRegistrationService;
import com.vidacotidiana.notification.domain.DevicePushToken;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** BE-024 — Documentacion/openapi/openapi.yaml, /me/devices*. */
@RestController
@RequestMapping("/api/v1/me/devices")
public class DeviceController {

    private final DeviceRegistrationService deviceRegistrationService;
    private final CurrentUser currentUser;

    public DeviceController(DeviceRegistrationService deviceRegistrationService, CurrentUser currentUser) {
        this.deviceRegistrationService = deviceRegistrationService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<DevicePushTokenResponse> list() {
        return deviceRegistrationService.listDevices(currentUser.userId()).stream()
                .map(DevicePushTokenResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<DevicePushTokenResponse> register(@Valid @RequestBody RegisterDeviceRequest request) {
        DevicePushToken device = deviceRegistrationService.registerOrUpdate(currentUser.userId(), request.platform(), request.token());
        return ResponseEntity.status(HttpStatus.CREATED).body(DevicePushTokenResponse.from(device));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> delete(@PathVariable UUID deviceId) {
        deviceRegistrationService.deleteDevice(deviceId, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
