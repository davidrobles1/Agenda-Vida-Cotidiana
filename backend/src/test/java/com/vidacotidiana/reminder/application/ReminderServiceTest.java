package com.vidacotidiana.reminder.application;

import com.vidacotidiana.reminder.domain.Reminder;
import com.vidacotidiana.reminder.domain.ReminderRepository;
import com.vidacotidiana.reminder.domain.ReminderStatus;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.VersionConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Reminder vertical slice application service.
 * Traceability: UC-03/UC-04, AC-003/AC-004/AC-004b/AC-005
 * (Documentacion/13-acceptance.md), BE-007/BE-009/BE-010
 * (docs/development/01-technical-backlog.md).
 */
class ReminderServiceTest {

    private ReminderRepository reminderRepository;
    private ReminderService reminderService;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID strangerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reminderRepository = Mockito.mock(ReminderRepository.class);
        reminderService = new ReminderService(reminderRepository);
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void create_persistsReminderOwnedByCaller() {
        Reminder created = reminderService.create(ownerId, "Buy milk", "2%", Instant.now());

        assertThat(created.getOwnerUserId()).isEqualTo(ownerId);
        assertThat(created.getTitle()).isEqualTo("Buy milk");
        assertThat(created.getStatus()).isEqualTo(ReminderStatus.PENDING);
    }

    @Test
    void getAccessible_ownerCanRead() {
        Reminder reminder = new Reminder(ownerId, "Pay rent", null, null);
        UUID id = fakeId(reminder);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(reminder));

        Reminder result = reminderService.getAccessible(id, ownerId);

        assertThat(result).isSameAs(reminder);
    }

    @Test
    void getAccessible_nonOwnerGetsNotFound_neverForbidden() {
        // AC-004: "otro usuario recibe 404 ... sin revelar existencia" —
        // a non-owner must get the same NotFoundException as a missing id.
        Reminder reminder = new Reminder(ownerId, "Pay rent", null, null);
        UUID id = fakeId(reminder);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(reminder));

        assertThatThrownBy(() -> reminderService.getAccessible(id, strangerId))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("REMINDER_NOT_FOUND"));
    }

    @Test
    void getAccessible_missingReminderThrowsNotFound() {
        UUID missingId = UUID.randomUUID();
        when(reminderRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.getAccessible(missingId, ownerId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void toggleCompletion_togglesPendingToCompletedAndBack() {
        Reminder reminder = new Reminder(ownerId, "Walk the dog", null, null);
        UUID id = fakeId(reminder);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(reminder));

        Reminder afterFirstToggle = reminderService.toggleCompletion(id, ownerId, null);
        assertThat(afterFirstToggle.getStatus()).isEqualTo(ReminderStatus.COMPLETED);

        Reminder afterSecondToggle = reminderService.toggleCompletion(id, ownerId, null);
        assertThat(afterSecondToggle.getStatus()).isEqualTo(ReminderStatus.PENDING);
    }

    @Test
    void toggleCompletion_matchingVersionSucceeds() {
        Reminder reminder = new Reminder(ownerId, "Water plants", null, null);
        UUID id = fakeId(reminder);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(reminder));

        Reminder result = reminderService.toggleCompletion(id, ownerId, reminder.getVersion());

        assertThat(result.getStatus()).isEqualTo(ReminderStatus.COMPLETED);
    }

    @Test
    void toggleCompletion_mismatchedVersionThrowsVersionConflict() {
        // AC-004b/AC-005: a stale client-supplied version must be rejected with 409 REMINDER_VERSION_CONFLICT.
        Reminder reminder = new Reminder(ownerId, "Water plants", null, null);
        UUID id = fakeId(reminder);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(reminder));

        assertThatThrownBy(() -> reminderService.toggleCompletion(id, ownerId, reminder.getVersion() + 1))
                .isInstanceOf(VersionConflictException.class)
                .satisfies(ex -> assertThat(((VersionConflictException) ex).getCode()).isEqualTo("REMINDER_VERSION_CONFLICT"));
    }

    @Test
    void toggleCompletion_omittedVersionSkipsConcurrencyCheck() {
        Reminder reminder = new Reminder(ownerId, "Water plants", null, null);
        UUID id = fakeId(reminder);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(reminder));

        Reminder result = reminderService.toggleCompletion(id, ownerId, null);

        assertThat(result.getStatus()).isEqualTo(ReminderStatus.COMPLETED);
    }

    @Test
    void toggleCompletion_nonOwnerGetsNotFound() {
        Reminder reminder = new Reminder(ownerId, "Water plants", null, null);
        UUID id = fakeId(reminder);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(reminder));

        assertThatThrownBy(() -> reminderService.toggleCompletion(id, strangerId, null))
                .isInstanceOf(NotFoundException.class);
    }

    /**
     * Reminder.id is only assigned by the database (@GeneratedValue) in a
     * real persistence context; for pure unit tests against a mocked
     * repository we assign a stand-in id via reflection so findById(id)
     * can be stubbed deterministically, without pulling in a persistence
     * context just for id generation.
     */
    private UUID fakeId(Reminder reminder) {
        UUID id = UUID.randomUUID();
        try {
            Field idField = Reminder.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(reminder, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return id;
    }
}
