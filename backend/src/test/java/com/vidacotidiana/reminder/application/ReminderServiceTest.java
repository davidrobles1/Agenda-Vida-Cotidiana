package com.vidacotidiana.reminder.application;

import com.vidacotidiana.reminder.domain.Reminder;
import com.vidacotidiana.reminder.domain.ReminderRepository;
import com.vidacotidiana.reminder.domain.ReminderStatus;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.VersionConflictException;
import com.vidacotidiana.sharing.domain.ReminderShareRepository;
import com.vidacotidiana.sharing.domain.ReminderShareStatus;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Reminder vertical slice application service.
 * Traceability: UC-03/UC-04/UC-05, AC-003/AC-004/AC-004b/AC-005/AC-011
 * (Documentacion/13-acceptance.md), BE-007/BE-009/BE-010/BE-022
 * (docs/development/01-technical-backlog.md).
 */
class ReminderServiceTest {

    private ReminderRepository reminderRepository;
    private ReminderShareRepository reminderShareRepository;
    private ReminderService reminderService;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID strangerId = UUID.randomUUID();
    private final UUID collaboratorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reminderRepository = Mockito.mock(ReminderRepository.class);
        reminderShareRepository = Mockito.mock(ReminderShareRepository.class);
        reminderService = new ReminderService(reminderRepository, reminderShareRepository);
        when(reminderRepository.save(any(Reminder.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void grantActiveShareTo(UUID reminderId, UUID userId) {
        when(reminderShareRepository.existsByReminderIdAndCollaboratorUserIdAndStatus(reminderId, userId, ReminderShareStatus.ACTIVE))
                .thenReturn(true);
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
    void getAccessible_activeCollaboratorCanRead() {
        // AC-011/BE-022: an active collaborator has the same read access as the owner.
        Reminder reminder = new Reminder(ownerId, "Pay rent", null, null);
        UUID id = fakeId(reminder);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(reminder));
        grantActiveShareTo(id, collaboratorId);

        Reminder result = reminderService.getAccessible(id, collaboratorId);

        assertThat(result).isSameAs(reminder);
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

    @Test
    void toggleCompletion_activeCollaboratorCanToggle() {
        // AC-005/AC-011: any user with access (owner or ACTIVE collaborator) can complete/revert.
        Reminder reminder = new Reminder(ownerId, "Water plants", null, null);
        UUID id = fakeId(reminder);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(reminder));
        grantActiveShareTo(id, collaboratorId);

        Reminder result = reminderService.toggleCompletion(id, collaboratorId, null);

        assertThat(result.getStatus()).isEqualTo(ReminderStatus.COMPLETED);
    }

    @Test
    void edit_activeCollaboratorGetsNotFound_neverForbidden() {
        // AC-011: a collaborator may never edit, even with ACTIVE access — same 404 as a stranger.
        Reminder reminder = new Reminder(ownerId, "Water plants", null, null);
        UUID id = fakeId(reminder);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(reminder));
        grantActiveShareTo(id, collaboratorId);

        assertThatThrownBy(() -> reminderService.edit(id, collaboratorId, "Hijacked title", null, null, reminder.getVersion()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void edit_matchingVersionUpdatesProvidedFieldsOnly() {
        Reminder reminder = new Reminder(ownerId, "Buy milk", "2%", null);
        UUID id = fakeId(reminder);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(reminder));

        Reminder result = reminderService.edit(id, ownerId, "Buy oat milk", null, null, reminder.getVersion());

        assertThat(result.getTitle()).isEqualTo("Buy oat milk");
        assertThat(result.getDescription()).isEqualTo("2%"); // untouched: null argument means "no change"
    }

    @Test
    void edit_mismatchedVersionThrowsVersionConflictWithoutApplyingChange() {
        // AC-004b: a stale version must be rejected with 409, and the edit must not be applied.
        Reminder reminder = new Reminder(ownerId, "Water plants", null, null);
        UUID id = fakeId(reminder);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(reminder));

        assertThatThrownBy(() -> reminderService.edit(id, ownerId, "Water the ferns", null, null, reminder.getVersion() + 1))
                .isInstanceOf(VersionConflictException.class)
                .satisfies(ex -> assertThat(((VersionConflictException) ex).getCode()).isEqualTo("REMINDER_VERSION_CONFLICT"));
        assertThat(reminder.getTitle()).isEqualTo("Water plants");
    }

    @Test
    void edit_nonOwnerGetsNotFound() {
        Reminder reminder = new Reminder(ownerId, "Water plants", null, null);
        UUID id = fakeId(reminder);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(reminder));

        assertThatThrownBy(() -> reminderService.edit(id, strangerId, "Hijacked title", null, null, reminder.getVersion()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_ownerDeletesReminder() {
        Reminder reminder = new Reminder(ownerId, "Buy milk", null, null);
        UUID id = fakeId(reminder);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(reminder));

        reminderService.delete(id, ownerId);

        verify(reminderRepository).delete(reminder);
    }

    @Test
    void delete_nonOwnerGetsNotFoundAndNothingIsDeleted() {
        Reminder reminder = new Reminder(ownerId, "Buy milk", null, null);
        UUID id = fakeId(reminder);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(reminder));

        assertThatThrownBy(() -> reminderService.delete(id, strangerId))
                .isInstanceOf(NotFoundException.class)
                .satisfies(ex -> assertThat(((NotFoundException) ex).getCode()).isEqualTo("REMINDER_NOT_FOUND"));
        verify(reminderRepository, never()).delete(any(Reminder.class));
    }

    @Test
    void delete_activeCollaboratorGetsNotFoundAndNothingIsDeleted() {
        // AC-011: a collaborator may never delete, even with ACTIVE access — same 404 as a stranger.
        Reminder reminder = new Reminder(ownerId, "Buy milk", null, null);
        UUID id = fakeId(reminder);
        when(reminderRepository.findById(id)).thenReturn(Optional.of(reminder));
        grantActiveShareTo(id, collaboratorId);

        assertThatThrownBy(() -> reminderService.delete(id, collaboratorId))
                .isInstanceOf(NotFoundException.class);
        verify(reminderRepository, never()).delete(any(Reminder.class));
    }

    @Test
    void delete_missingReminderThrowsNotFound() {
        UUID missingId = UUID.randomUUID();
        when(reminderRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.delete(missingId, ownerId))
                .isInstanceOf(NotFoundException.class);
        verify(reminderRepository, never()).delete(any(Reminder.class));
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
