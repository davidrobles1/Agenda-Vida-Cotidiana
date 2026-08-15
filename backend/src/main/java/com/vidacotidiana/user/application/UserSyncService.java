package com.vidacotidiana.user.application;

import com.vidacotidiana.user.domain.User;
import com.vidacotidiana.user.domain.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Ensures a USER row exists for every authenticated caller, upserted from
 * the validated Keycloak JWT (UC-01/UC-02: the backend never creates
 * accounts itself, it only mirrors the identity Keycloak already
 * authenticated). Runs once per authenticated request via UserSyncFilter.
 */
@Service
public class UserSyncService {

    private final UserRepository userRepository;

    public UserSyncService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * BE-027 correctness fix (found while validating the purge job, same
     * pattern as BE-030..032): a purged (DELETED) account must never be
     * re-synced from the JWT. Without this guard, any authenticated request
     * after purge (the Keycloak account itself isn't deleted by this
     * backend, DEC-004/ADR-008 — that's out of scope here) would silently
     * overwrite the anonymized email/username back to the real values,
     * defeating AC-015's "los datos personales... se purgan
     * definitivamente".
     */
    @Transactional
    public User syncFromToken(UUID userId, String email, String username) {
        return userRepository.findById(userId)
                .map(existing -> {
                    if (!"DELETED".equals(existing.getDeletionStatus())) {
                        existing.refreshFromIdentityProvider(email, username);
                    }
                    return existing;
                })
                .orElseGet(() -> userRepository.save(new User(userId, email, username)));
    }
}
