package com.vidacotidiana.notification.application;

import com.vidacotidiana.notification.domain.DevicePlatform;
import com.vidacotidiana.notification.domain.DevicePushToken;
import com.vidacotidiana.notification.domain.DevicePushTokenRepository;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BE-024. AC-014 is explicit: deleting someone else's
 * device is 403 (AccessDeniedException, mapped by GlobalExceptionHandler),
 * not the uniform 404 used across Reminder/sharing.
 */
class DeviceRegistrationServiceTest {

    private DevicePushTokenRepository repository;
    private DeviceRegistrationService service;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID strangerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(DevicePushTokenRepository.class);
        service = new DeviceRegistrationService(repository);
        when(repository.save(any(DevicePushToken.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void listDevices_returnsCallersDevices() {
        DevicePushToken device = new DevicePushToken(ownerId, DevicePlatform.ANDROID, "token-1");
        when(repository.findByUserId(ownerId)).thenReturn(List.of(device));

        List<DevicePushToken> devices = service.listDevices(ownerId);

        assertThat(devices).containsExactly(device);
    }

    @Test
    void registerOrUpdate_newToken_createsDevice() {
        when(repository.findByToken("brand-new-token")).thenReturn(Optional.empty());

        DevicePushToken device = service.registerOrUpdate(ownerId, "ANDROID", "brand-new-token");

        assertThat(device.getUserId()).isEqualTo(ownerId);
        assertThat(device.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(device.getToken()).isEqualTo("brand-new-token");
    }

    @Test
    void registerOrUpdate_existingToken_reassignsToCaller() {
        // DEC-005: a token already registered under another user is reassigned, not rejected.
        DevicePushToken existing = new DevicePushToken(strangerId, DevicePlatform.IOS, "shared-token");
        when(repository.findByToken("shared-token")).thenReturn(Optional.of(existing));

        DevicePushToken result = service.registerOrUpdate(ownerId, "IOS", "shared-token");

        assertThat(result).isSameAs(existing);
        assertThat(result.getUserId()).isEqualTo(ownerId);
    }

    @Test
    void registerOrUpdate_invalidPlatform_isRejected() {
        assertThatThrownBy(() -> service.registerOrUpdate(ownerId, "PALMPILOT", "some-token"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void deleteDevice_owner_succeeds() {
        DevicePushToken device = new DevicePushToken(ownerId, DevicePlatform.WEB, "token-1");
        UUID deviceId = fakeId(device);
        when(repository.findById(deviceId)).thenReturn(Optional.of(device));

        service.deleteDevice(deviceId, ownerId);

        verify(repository).delete(device);
    }

    @Test
    void deleteDevice_nonOwnerGetsForbidden_notNotFound() {
        // AC-014: "403 en caso contrario" — a real 403, unlike Reminder/sharing's uniform 404.
        DevicePushToken device = new DevicePushToken(ownerId, DevicePlatform.WEB, "token-1");
        UUID deviceId = fakeId(device);
        when(repository.findById(deviceId)).thenReturn(Optional.of(device));

        assertThatThrownBy(() -> service.deleteDevice(deviceId, strangerId))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).delete(any(DevicePushToken.class));
    }

    @Test
    void deleteDevice_missingDeviceReturnsNotFound() {
        UUID missingId = UUID.randomUUID();
        when(repository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteDevice(missingId, ownerId))
                .isInstanceOf(NotFoundException.class);
    }

    private UUID fakeId(DevicePushToken device) {
        UUID id = UUID.randomUUID();
        try {
            Field idField = DevicePushToken.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(device, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return id;
    }
}
