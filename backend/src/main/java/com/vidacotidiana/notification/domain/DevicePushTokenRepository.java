package com.vidacotidiana.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DevicePushTokenRepository extends JpaRepository<DevicePushToken, UUID> {

    List<DevicePushToken> findByUserId(UUID userId);

    // DEC-005: upsert key for POST /me/devices.
    Optional<DevicePushToken> findByToken(String token);
}
