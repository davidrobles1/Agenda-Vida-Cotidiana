package com.vidacotidiana.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // BE-017: resolves an invitation recipient by email (may or may not have an account,
    // SEC-001 — the API response must never reveal which) or by username (sharing.application.SharingService).
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);
}
