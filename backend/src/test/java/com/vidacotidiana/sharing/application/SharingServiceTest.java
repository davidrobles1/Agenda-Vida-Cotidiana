package com.vidacotidiana.sharing.application;

import com.vidacotidiana.notification.application.PushNotificationSender;
import com.vidacotidiana.reminder.application.ReminderService;
import com.vidacotidiana.reminder.domain.Reminder;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.GoneException;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.RateLimitExceededException;
import com.vidacotidiana.shared.domain.ValidationException;
import com.vidacotidiana.sharing.domain.Invitation;
import com.vidacotidiana.sharing.domain.InvitationRepository;
import com.vidacotidiana.sharing.domain.InvitationStatus;
import com.vidacotidiana.sharing.domain.ReminderShare;
import com.vidacotidiana.sharing.domain.ReminderShareRepository;
import com.vidacotidiana.user.domain.User;
import com.vidacotidiana.user.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the sharing application service.
 * Traceability: UC-07/UC-08/UC-09/UC-10/UC-14,
 * AC-007/AC-008/AC-009/AC-010/AC-012/AC-017 (Documentacion/13-acceptance.md),
 * BE-017..021/BE-026/DEVOPS-001 (docs/development/01-technical-backlog.md).
 */
class SharingServiceTest {

    private ReminderService reminderService;
    private InvitationRepository invitationRepository;
    private ReminderShareRepository reminderShareRepository;
    private UserRepository userRepository;
    private EmailSender emailSender;
    private PushNotificationSender pushNotificationSender;
    private InvitationRateLimiter invitationRateLimiter;
    private SharingService sharingService;

    private final UUID ownerId = UUID.randomUUID();
    private final UUID strangerId = UUID.randomUUID();
    private final UUID invitedUserId = UUID.randomUUID();
    private UUID reminderId;
    private Reminder reminder;

    @BeforeEach
    void setUp() {
        reminderService = Mockito.mock(ReminderService.class);
        invitationRepository = Mockito.mock(InvitationRepository.class);
        reminderShareRepository = Mockito.mock(ReminderShareRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        emailSender = Mockito.mock(EmailSender.class);
        pushNotificationSender = Mockito.mock(PushNotificationSender.class);
        invitationRateLimiter = new InvitationRateLimiter(); // real instance: exercises DEVOPS-001 for real, generous enough default limit not to interfere with unrelated tests
        sharingService = new SharingService(reminderService, invitationRepository, reminderShareRepository, userRepository,
                emailSender, pushNotificationSender, invitationRateLimiter);

        reminder = new Reminder(ownerId, "Family trip", null, null);
        reminderId = fakeReminderId(reminder);
        when(reminderService.getOwnedOrThrow(reminderId, ownerId)).thenReturn(reminder);
        when(reminderService.getOwnedOrThrow(eq(reminderId), org.mockito.ArgumentMatchers.argThat(id -> !ownerId.equals(id))))
                .thenThrow(new NotFoundException("REMINDER_NOT_FOUND", "The requested reminder was not found."));
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reminderShareRepository.save(any(ReminderShare.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createInvitation_byEmailWithExistingAccount_resolvesInvitedUserIdButSendsNoEmail() {
        when(invitationRepository.findByReminderIdAndInvitedEmailAndStatus(reminderId, "friend@example.com", InvitationStatus.PENDING))
                .thenReturn(Optional.empty());
        User existingUser = new User(invitedUserId, "friend@example.com", "friend");
        when(userRepository.findByEmail("friend@example.com")).thenReturn(Optional.of(existingUser));

        Invitation invitation = sharingService.createInvitation(reminderId, ownerId, "friend@example.com", null);

        assertThat(invitation.getInvitedEmail()).isEqualTo("friend@example.com");
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
        // SEC-001: whether or not the email has an account, the invitation is created identically;
        // only the "no account" branch triggers the email adapter — an account holder gets a push instead (AC-012/BE-026).
        verify(emailSender, never()).sendInvitation(any(), any());
        verify(pushNotificationSender).sendBestEffort(eq(invitedUserId), any());
    }

    @Test
    void createInvitation_byEmailWithoutAccount_sendsNoOpEmailAndResponseIsIdentical() {
        when(invitationRepository.findByReminderIdAndInvitedEmailAndStatus(reminderId, "stranger@example.com", InvitationStatus.PENDING))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("stranger@example.com")).thenReturn(Optional.empty());

        Invitation invitation = sharingService.createInvitation(reminderId, ownerId, "stranger@example.com", null);

        assertThat(invitation.getInvitedEmail()).isEqualTo("stranger@example.com");
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
        verify(emailSender).sendInvitation("stranger@example.com", "Family trip");
        verify(pushNotificationSender, never()).sendBestEffort(any(), any());
    }

    @Test
    void createInvitation_byExistingUsername_resolvesEmailFromAccount() {
        User existingUser = new User(invitedUserId, "bykname@example.com", "bykname");
        when(userRepository.findByUsername("bykname")).thenReturn(Optional.of(existingUser));
        when(invitationRepository.findByReminderIdAndInvitedEmailAndStatus(reminderId, "bykname@example.com", InvitationStatus.PENDING))
                .thenReturn(Optional.empty());

        Invitation invitation = sharingService.createInvitation(reminderId, ownerId, null, "bykname");

        assertThat(invitation.getInvitedEmail()).isEqualTo("bykname@example.com");
        verify(emailSender, never()).sendInvitation(any(), any());
        verify(pushNotificationSender).sendBestEffort(eq(invitedUserId), any());
    }

    @Test
    void createInvitation_exceedingRateLimit_returns429() {
        // DEVOPS-001: the 11th invitation from the same caller within the window is rejected.
        for (int i = 0; i < 10; i++) {
            String email = "bulk" + i + "@example.com";
            when(invitationRepository.findByReminderIdAndInvitedEmailAndStatus(reminderId, email, InvitationStatus.PENDING))
                    .thenReturn(Optional.empty());
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
            sharingService.createInvitation(reminderId, ownerId, email, null);
        }

        assertThatThrownBy(() -> sharingService.createInvitation(reminderId, ownerId, "onemore@example.com", null))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(ex -> assertThat(((RateLimitExceededException) ex).getCode()).isEqualTo("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void createInvitation_byUnknownUsername_isRejectedAsValidationError() {
        // Implementation note (SEC-001 only covers email enumeration): an unknown username is a plain 400.
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharingService.createInvitation(reminderId, ownerId, null, "ghost"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createInvitation_bothEmailAndUsername_isRejected() {
        assertThatThrownBy(() -> sharingService.createInvitation(reminderId, ownerId, "a@example.com", "someone"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createInvitation_neitherEmailNorUsername_isRejected() {
        assertThatThrownBy(() -> sharingService.createInvitation(reminderId, ownerId, null, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createInvitation_duplicatePending_isConflict() {
        Invitation existingPending = new Invitation(reminderId, ownerId, "dup@example.com", null);
        when(invitationRepository.findByReminderIdAndInvitedEmailAndStatus(reminderId, "dup@example.com", InvitationStatus.PENDING))
                .thenReturn(Optional.of(existingPending));
        when(userRepository.findByEmail("dup@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sharingService.createInvitation(reminderId, ownerId, "dup@example.com", null))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getCode()).isEqualTo("INVITATION_ALREADY_PENDING"));
    }

    @Test
    void createInvitation_nonOwnerGetsNotFound() {
        assertThatThrownBy(() -> sharingService.createInvitation(reminderId, strangerId, "x@example.com", null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void acceptInvitation_pendingInvitation_createsActiveShare() {
        Invitation invitation = new Invitation(reminderId, ownerId, "friend@example.com", invitedUserId);
        UUID invitationId = fakeInvitationId(invitation);
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(invitationRepository.resolveIfPending(invitationId, InvitationStatus.ACCEPTED)).thenReturn(1);

        ReminderShare share = sharingService.acceptInvitation(invitationId, invitedUserId);

        assertThat(share.getReminderId()).isEqualTo(reminderId);
        assertThat(share.getCollaboratorUserId()).isEqualTo(invitedUserId);
        assertThat(share.getStatus().name()).isEqualTo("ACTIVE");
        // AC-012/BE-026: the inviter (owner) is notified that their invitation was accepted.
        verify(pushNotificationSender).sendBestEffort(eq(ownerId), any());
    }

    @Test
    void acceptInvitation_alreadyResolved_isGone() {
        // AC-008: two concurrent resolutions — the atomic UPDATE affecting 0 rows must produce 410, not apply the accept.
        Invitation invitation = new Invitation(reminderId, ownerId, "friend@example.com", invitedUserId);
        UUID invitationId = fakeInvitationId(invitation);
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(invitationRepository.resolveIfPending(invitationId, InvitationStatus.ACCEPTED)).thenReturn(0);

        assertThatThrownBy(() -> sharingService.acceptInvitation(invitationId, invitedUserId))
                .isInstanceOf(GoneException.class);
        verify(reminderShareRepository, never()).save(any(ReminderShare.class));
    }

    @Test
    void acceptInvitation_nonRecipientGetsNotFound() {
        Invitation invitation = new Invitation(reminderId, ownerId, "friend@example.com", invitedUserId);
        UUID invitationId = fakeInvitationId(invitation);
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> sharingService.acceptInvitation(invitationId, strangerId))
                .isInstanceOf(NotFoundException.class);
        verify(invitationRepository, never()).resolveIfPending(any(), any());
    }

    @Test
    void rejectInvitation_pendingInvitation_doesNotCreateShare() {
        Invitation invitation = new Invitation(reminderId, ownerId, "friend@example.com", invitedUserId);
        UUID invitationId = fakeInvitationId(invitation);
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(invitationRepository.resolveIfPending(invitationId, InvitationStatus.REJECTED)).thenReturn(1);

        Invitation result = sharingService.rejectInvitation(invitationId, invitedUserId);

        assertThat(result).isSameAs(invitation);
        verify(reminderShareRepository, never()).save(any(ReminderShare.class));
        verify(pushNotificationSender).sendBestEffort(eq(ownerId), any());
    }

    @Test
    void rejectInvitation_alreadyResolved_isGone() {
        Invitation invitation = new Invitation(reminderId, ownerId, "friend@example.com", invitedUserId);
        UUID invitationId = fakeInvitationId(invitation);
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(invitationRepository.resolveIfPending(invitationId, InvitationStatus.REJECTED)).thenReturn(0);

        assertThatThrownBy(() -> sharingService.rejectInvitation(invitationId, invitedUserId))
                .isInstanceOf(GoneException.class);
    }

    @Test
    void cancelInvitation_byInviter_succeeds() {
        Invitation invitation = new Invitation(reminderId, ownerId, "friend@example.com", invitedUserId);
        UUID invitationId = fakeInvitationId(invitation);
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(invitationRepository.resolveIfPending(invitationId, InvitationStatus.CANCELLED)).thenReturn(1);

        sharingService.cancelInvitation(invitationId, ownerId);

        verify(invitationRepository).resolveIfPending(invitationId, InvitationStatus.CANCELLED);
        // AC-012/BE-026: the invitee (who has an account here) is notified of the cancellation.
        verify(pushNotificationSender).sendBestEffort(eq(invitedUserId), any());
    }

    @Test
    void cancelInvitation_alreadyResolved_isGone() {
        Invitation invitation = new Invitation(reminderId, ownerId, "friend@example.com", invitedUserId);
        UUID invitationId = fakeInvitationId(invitation);
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(invitationRepository.resolveIfPending(invitationId, InvitationStatus.CANCELLED)).thenReturn(0);

        assertThatThrownBy(() -> sharingService.cancelInvitation(invitationId, ownerId))
                .isInstanceOf(GoneException.class);
    }

    @Test
    void cancelInvitation_nonInviterGetsNotFound() {
        Invitation invitation = new Invitation(reminderId, ownerId, "friend@example.com", invitedUserId);
        UUID invitationId = fakeInvitationId(invitation);
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> sharingService.cancelInvitation(invitationId, strangerId))
                .isInstanceOf(NotFoundException.class);
        verify(invitationRepository, never()).resolveIfPending(any(), any());
    }

    @Test
    void revokeShare_activeShare_getsRevoked() {
        ReminderShare share = new ReminderShare(reminderId, invitedUserId, UUID.randomUUID());
        UUID shareId = fakeShareId(share);
        when(reminderShareRepository.findById(shareId)).thenReturn(Optional.of(share));

        sharingService.revokeShare(reminderId, shareId, ownerId);

        assertThat(share.getStatus().name()).isEqualTo("REVOKED");
        verify(reminderShareRepository).save(share);
        verify(pushNotificationSender).sendBestEffort(eq(invitedUserId), any());
    }

    @Test
    void revokeShare_alreadyRevoked_isIdempotent() {
        ReminderShare share = new ReminderShare(reminderId, invitedUserId, UUID.randomUUID());
        share.revoke();
        UUID shareId = fakeShareId(share);
        when(reminderShareRepository.findById(shareId)).thenReturn(Optional.of(share));

        sharingService.revokeShare(reminderId, shareId, ownerId);

        verify(reminderShareRepository, never()).save(any(ReminderShare.class));
    }

    @Test
    void revokeShare_nonOwnerGetsNotFound() {
        ReminderShare share = new ReminderShare(reminderId, invitedUserId, UUID.randomUUID());
        UUID shareId = fakeShareId(share);

        assertThatThrownBy(() -> sharingService.revokeShare(reminderId, shareId, strangerId))
                .isInstanceOf(NotFoundException.class);
    }

    private UUID fakeReminderId(Reminder reminder) {
        return setId(reminder, Reminder.class);
    }

    private UUID fakeInvitationId(Invitation invitation) {
        return setId(invitation, Invitation.class);
    }

    private UUID fakeShareId(ReminderShare share) {
        return setId(share, ReminderShare.class);
    }

    private <T> UUID setId(T entity, Class<T> type) {
        UUID id = UUID.randomUUID();
        try {
            Field idField = type.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return id;
    }
}
