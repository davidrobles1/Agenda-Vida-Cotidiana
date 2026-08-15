package com.vidacotidiana.user.application;

import com.vidacotidiana.user.domain.User;
import com.vidacotidiana.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UC-01/UC-02 sync and the BE-027 correctness fix (found
 * while validating the purge job): a DELETED (purged) account must never
 * be re-synced from the JWT, or purge would be silently undone on the
 * next authenticated request.
 */
class UserSyncServiceTest {

    private UserRepository userRepository;
    private UserSyncService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        service = new UserSyncService(userRepository);
    }

    @Test
    void syncFromToken_existingActiveUser_refreshesEmailAndUsername() {
        User user = new User(userId, "old@example.com", "oldname");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        service.syncFromToken(userId, "new@example.com", "newname");

        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getUsername()).isEqualTo("newname");
    }

    @Test
    void syncFromToken_purgedUser_isNeverResyncedFromJwt() {
        User user = new User(userId, "original@example.com", "originalname");
        user.purge();
        String anonymizedEmail = user.getEmail();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Same JWT claims as before the account existed under this identity — must not undo the purge.
        service.syncFromToken(userId, "original@example.com", "originalname");

        assertThat(user.getDeletionStatus()).isEqualTo("DELETED");
        assertThat(user.getEmail()).isEqualTo(anonymizedEmail);
        assertThat(user.getUsername()).isNull();
    }

    @Test
    void syncFromToken_newUser_createsRow() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = service.syncFromToken(userId, "brand-new@example.com", "brandnew");

        assertThat(result.getEmail()).isEqualTo("brand-new@example.com");
    }
}
