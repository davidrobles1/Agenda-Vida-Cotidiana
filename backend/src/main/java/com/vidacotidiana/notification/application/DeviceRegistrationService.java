package com.vidacotidiana.notification.application;

import com.vidacotidiana.notification.domain.DevicePlatform;
import com.vidacotidiana.notification.domain.DevicePushToken;
import com.vidacotidiana.notification.domain.DevicePushTokenRepository;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.ValidationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * UC-12/AC-014/FR-012. Unlike Reminder/sharing, deleting someone else's
 * device is a real 403 (Forbidden), not the uniform 404 used elsewhere —
 * AC-014 says so explicitly ("403 en caso contrario"). SEC-001's
 * no-enumeration rule is specific to inviting by email; a device id is not
 * an enumeration vector the same way, so this endpoint doesn't need to hide
 * whether the device exists.
 */
@Service
public class DeviceRegistrationService {

    private final DevicePushTokenRepository devicePushTokenRepository;

    public DeviceRegistrationService(DevicePushTokenRepository devicePushTokenRepository) {
        this.devicePushTokenRepository = devicePushTokenRepository;
    }

    @Transactional(readOnly = true)
    public List<DevicePushToken> listDevices(UUID callerUserId) {
        return devicePushTokenRepository.findByUserId(callerUserId);
    }

    @Transactional
    public DevicePushToken registerOrUpdate(UUID callerUserId, String platformRaw, String token) {
        DevicePlatform platform;
        try {
            platform = DevicePlatform.valueOf(platformRaw);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("platform must be one of ANDROID, IOS, WEB.");
        }

        return devicePushTokenRepository.findByToken(token)
                .map(existing -> {
                    existing.reassignTo(callerUserId, platform);
                    return existing;
                })
                .orElseGet(() -> devicePushTokenRepository.save(new DevicePushToken(callerUserId, platform, token)));
    }

    @Transactional
    public void deleteDevice(UUID deviceId, UUID callerUserId) {
        DevicePushToken device = devicePushTokenRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("DEVICE_NOT_FOUND", "The requested device was not found."));
        if (!device.isOwnedBy(callerUserId)) {
            throw new AccessDeniedException("You do not have permission to remove this device.");
        }
        devicePushTokenRepository.delete(device);
    }
}
