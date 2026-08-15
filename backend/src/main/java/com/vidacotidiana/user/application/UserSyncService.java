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

    @Transactional
    public User syncFromToken(UUID userId, String email, String username) {
        return userRepository.findById(userId)
                .map(existing -> {
                    existing.refreshFromIdentityProvider(email, username);
                    return existing;
                })
                .orElseGet(() -> userRepository.save(new User(userId, email, username)));
    }
}
