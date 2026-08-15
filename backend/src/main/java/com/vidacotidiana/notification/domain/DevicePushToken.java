package com.vidacotidiana.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps to the DEVICE_PUSH_TOKEN entity in Documentacion/09-data-model.md.
 * {@code token} is unique — POST /me/devices is an upsert by token (DEC-005):
 * if the physical token already exists under another user_id, it is
 * reassigned to the current caller (a device changing accounts).
 */
@Entity
@Table(name = "device_push_tokens")
public class DevicePushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DevicePlatform platform;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    protected DevicePushToken() {
        // JPA
    }

    public DevicePushToken(UUID userId, DevicePlatform platform, String token) {
        this.userId = userId;
        this.platform = platform;
        this.token = token;
        Instant now = Instant.now();
        this.createdAt = now;
        this.lastSeenAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public DevicePlatform getPlatform() {
        return platform;
    }

    public String getToken() {
        return token;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.userId.equals(userId);
    }

    /** DEC-005: a token reused under a new caller — normal when a device changes accounts. */
    public void reassignTo(UUID userId, DevicePlatform platform) {
        this.userId = userId;
        this.platform = platform;
        this.lastSeenAt = Instant.now();
    }
}
