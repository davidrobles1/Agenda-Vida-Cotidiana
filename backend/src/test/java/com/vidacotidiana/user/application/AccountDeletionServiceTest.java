package com.vidacotidiana.user.application;

import com.vidacotidiana.user.domain.User;
import com.vidacotidiana.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Unit tests for BE-027 (UC-13/AC-015/DEC-015). */
class AccountDeletionServiceTest {

    private UserRepository userRepository;
    private AccountDeletionService service;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        service = new AccountDeletionService(userRepository);
    }

    @Test
    void requestDeletion_marksPendingDeletionWithThirtyDayPurgeWindow() {
        User user = new User(userId, "alice@example.com", "alice");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        service.requestDeletion(userId);

        assertThat(user.getDeletionStatus()).isEqualTo("PENDING_DELETION");
        assertThat(user.getDeletionRequestedAt()).isNotNull();
        assertThat(user.getPurgeAt()).isCloseTo(user.getDeletionRequestedAt().plus(30, ChronoUnit.DAYS), org.assertj.core.api.Assertions.within(1, ChronoUnit.SECONDS));
    }

    @Test
    void purgeAccountsPastGracePeriod_anonymizesDueAccounts() {
        User dueAccount = new User(userId, "bob@example.com", "bob");
        when(userRepository.findByDeletionStatusAndPurgeAtBefore(org.mockito.ArgumentMatchers.eq("PENDING_DELETION"), any(Instant.class)))
                .thenReturn(List.of(dueAccount));

        service.purgeAccountsPastGracePeriod();

        assertThat(dueAccount.getDeletionStatus()).isEqualTo("DELETED");
        assertThat(dueAccount.getEmail()).isNotEqualTo("bob@example.com");
        assertThat(dueAccount.getEmail()).contains(userId.toString());
        assertThat(dueAccount.getUsername()).isNull();
    }

    @Test
    void purgeAccountsPastGracePeriod_noAccountsDue_doesNothing() {
        when(userRepository.findByDeletionStatusAndPurgeAtBefore(anyString(), any(Instant.class))).thenReturn(List.of());

        service.purgeAccountsPastGracePeriod();
        // No exception, no accounts touched — nothing else to assert against a mocked repository with no interactions expected.
    }
}
