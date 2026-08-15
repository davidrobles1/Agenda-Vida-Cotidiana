package com.vidacotidiana.sharing.application;

import com.vidacotidiana.reminder.application.ReminderService;
import com.vidacotidiana.reminder.domain.Reminder;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.GoneException;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.ValidationException;
import com.vidacotidiana.sharing.domain.Invitation;
import com.vidacotidiana.sharing.domain.InvitationRepository;
import com.vidacotidiana.sharing.domain.InvitationStatus;
import com.vidacotidiana.sharing.domain.ReminderShare;
import com.vidacotidiana.sharing.domain.ReminderShareRepository;
import com.vidacotidiana.sharing.domain.ReminderShareStatus;
import com.vidacotidiana.user.domain.User;
import com.vidacotidiana.user.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for the sharing module (UC-07..UC-10/UC-14,
 * AC-007..AC-010/AC-017, BE-017..021). Reuses
 * reminder.application.ReminderService.getOwnedOrThrow for every owner-only
 * operation here instead of duplicating the ownership check against
 * ReminderRepository directly.
 */
@Service
public class SharingService {

    private static final Logger log = LoggerFactory.getLogger(SharingService.class);

    private final ReminderService reminderService;
    private final InvitationRepository invitationRepository;
    private final ReminderShareRepository reminderShareRepository;
    private final UserRepository userRepository;
    private final EmailSender emailSender;

    public SharingService(ReminderService reminderService, InvitationRepository invitationRepository,
                           ReminderShareRepository reminderShareRepository, UserRepository userRepository,
                           EmailSender emailSender) {
        this.reminderService = reminderService;
        this.invitationRepository = invitationRepository;
        this.reminderShareRepository = reminderShareRepository;
        this.userRepository = userRepository;
        this.emailSender = emailSender;
    }

    /**
     * BE-017/AC-007. Owner-only. Exactly one of email/username. SEC-001: the
     * 201 response is identical whether or not the email belongs to an
     * existing account — invitedUserId is resolved server-side but never
     * exposed (see sharing.api.dto.InvitationResponse).
     */
    @Transactional
    public Invitation createInvitation(UUID reminderId, UUID callerUserId, String email, String username) {
        if ((email == null) == (username == null)) {
            throw new ValidationException("Exactly one of email or username must be provided.");
        }
        Reminder reminder = reminderService.getOwnedOrThrow(reminderId, callerUserId);

        String invitedEmail;
        UUID invitedUserId;
        if (email != null) {
            invitedEmail = email;
            invitedUserId = userRepository.findByEmail(email).map(User::getId).orElse(null);
        } else {
            // Implementation note (not a new business decision — a clarification of how SEC-001 applies):
            // SEC-001 only prohibits distinguishable responses for EMAIL invitations, because email is a
            // guessable enumeration vector at scale. A username is an identifier the owner typed on purpose,
            // already knowing it (they're inviting someone they know); rejecting an unknown username outright
            // is not a mass-enumeration vector the same way email is, so a plain 400 here is safe.
            User invitedUser = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ValidationException("No user found with that username."));
            invitedEmail = invitedUser.getEmail();
            invitedUserId = invitedUser.getId();
        }

        if (invitationRepository.findByReminderIdAndInvitedEmailAndStatus(reminderId, invitedEmail, InvitationStatus.PENDING).isPresent()) {
            throw new ConflictException("INVITATION_ALREADY_PENDING", "A pending invitation for this recipient already exists.");
        }

        Invitation invitation = new Invitation(reminderId, callerUserId, invitedEmail, invitedUserId);
        try {
            invitation = invitationRepository.save(invitation);
        } catch (DataIntegrityViolationException raceLostToConcurrentInsert) {
            // Safety net for the genuine race the partial unique index (V2__sharing.sql) guards against —
            // same pattern as ObjectOptimisticLockingFailureException handling for Reminder.version.
            throw new ConflictException("INVITATION_ALREADY_PENDING", "A pending invitation for this recipient already exists.");
        }

        log.info("Invitation created: id={}, reminderId={}, inviterUserId={}, hasExistingAccount={}",
                invitation.getId(), reminderId, callerUserId, invitedUserId != null);

        // Only the no-account case gets an email today; an invitee with an existing account would instead
        // get a push notification once notification/BE-025/026 exist — not implemented here, same
        // deliberate no-op scope as ReminderService.delete (BE-015) for the parts sharing/push don't cover yet.
        if (invitedUserId == null) {
            emailSender.sendInvitation(invitedEmail, reminder.getTitle());
        }

        return invitation;
    }

    /** BE-018/AC-007. Owner-only: the collaborators and pending invitations on a reminder. */
    @Transactional(readOnly = true)
    public SharesAndInvitations listSharesAndInvitations(UUID reminderId, UUID callerUserId, Pageable pageable) {
        reminderService.getOwnedOrThrow(reminderId, callerUserId);
        Page<ReminderShare> shares = reminderShareRepository.findByReminderIdAndStatus(reminderId, ReminderShareStatus.ACTIVE, pageable);
        Page<Invitation> invitations = invitationRepository.findByReminderIdAndStatus(reminderId, InvitationStatus.PENDING, pageable);
        return new SharesAndInvitations(shares, invitations);
    }

    /** BE-018. Pending invitations received by the caller. */
    @Transactional(readOnly = true)
    public Page<Invitation> listMyPendingInvitations(UUID callerUserId, Pageable pageable) {
        return invitationRepository.findByInvitedUserIdAndStatus(callerUserId, InvitationStatus.PENDING, pageable);
    }

    /**
     * BE-019/AC-008. Recipient-only. The PENDING -> ACCEPTED transition is
     * the atomic conditional UPDATE in InvitationRepository.resolveIfPending
     * — 0 rows affected means the invitation was no longer PENDING when this
     * statement ran (already resolved, or a concurrent request won the
     * race), which must respond 410, never apply the acceptance based on a
     * status read earlier in this method.
     */
    @Transactional
    public ReminderShare acceptInvitation(UUID invitationId, UUID callerUserId) {
        Invitation invitation = findInvitationVisibleTo(invitationId, callerUserId);

        int updated = invitationRepository.resolveIfPending(invitationId, InvitationStatus.ACCEPTED);
        if (updated == 0) {
            throw new GoneException("INVITATION_ALREADY_RESOLVED", "This invitation is no longer pending.");
        }

        ReminderShare share = new ReminderShare(invitation.getReminderId(), callerUserId, invitationId);
        share = reminderShareRepository.save(share);
        log.info("Invitation accepted: id={}, reminderId={}, collaboratorUserId={}", invitationId, invitation.getReminderId(), callerUserId);
        return share;
    }

    /** BE-019/AC-009. Recipient-only. Same atomic transition as accept; rejecting never creates a ReminderShare. */
    @Transactional
    public Invitation rejectInvitation(UUID invitationId, UUID callerUserId) {
        findInvitationVisibleTo(invitationId, callerUserId);

        int updated = invitationRepository.resolveIfPending(invitationId, InvitationStatus.REJECTED);
        if (updated == 0) {
            throw new GoneException("INVITATION_ALREADY_RESOLVED", "This invitation is no longer pending.");
        }

        log.info("Invitation rejected: id={}, invitedUserId={}", invitationId, callerUserId);
        return invitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalStateException("Invitation " + invitationId + " vanished immediately after being resolved."));
    }

    /**
     * BE-020/AC-017/UC-14. Inviter-only. status stays within INVITATION —
     * CANCELLED, never REVOKED (DEC-003). Same atomic transition pattern.
     */
    @Transactional
    public void cancelInvitation(UUID invitationId, UUID callerUserId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("INVITATION_NOT_FOUND", "The requested invitation was not found."));
        if (!invitation.isInviter(callerUserId)) {
            throw new NotFoundException("INVITATION_NOT_FOUND", "The requested invitation was not found.");
        }

        int updated = invitationRepository.resolveIfPending(invitationId, InvitationStatus.CANCELLED);
        if (updated == 0) {
            throw new GoneException("INVITATION_ALREADY_RESOLVED", "This invitation is no longer pending.");
        }

        log.info("Invitation cancelled: id={}, inviterUserId={}", invitationId, callerUserId);
    }

    /**
     * BE-021/AC-010. Owner-only. Effective immediately (SEC-002): once
     * revoked, reminder.application.ReminderService's owner-or-active-
     * collaborator check (BE-022) stops matching this share on the caller's
     * very next request — there is no separate expiry/grace step to run.
     * Idempotent: revoking an already-REVOKED share still returns success
     * (openapi.yaml documents only 204/404 for this endpoint, no conflict
     * status for "already revoked").
     */
    @Transactional
    public void revokeShare(UUID reminderId, UUID shareId, UUID callerUserId) {
        reminderService.getOwnedOrThrow(reminderId, callerUserId);
        ReminderShare share = reminderShareRepository.findById(shareId)
                .filter(s -> s.getReminderId().equals(reminderId))
                .orElseThrow(() -> new NotFoundException("REMINDER_SHARE_NOT_FOUND", "The requested share was not found."));

        if (share.getStatus() == ReminderShareStatus.ACTIVE) {
            share.revoke();
            reminderShareRepository.save(share);
            log.info("Reminder share revoked: id={}, reminderId={}, collaboratorUserId={}", shareId, reminderId, share.getCollaboratorUserId());
        }
    }

    private Invitation findInvitationVisibleTo(UUID invitationId, UUID callerUserId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("INVITATION_NOT_FOUND", "The requested invitation was not found."));
        if (!invitation.isInvitedUser(callerUserId)) {
            throw new NotFoundException("INVITATION_NOT_FOUND", "The requested invitation was not found.");
        }
        return invitation;
    }

    public record SharesAndInvitations(Page<ReminderShare> shares, Page<Invitation> invitations) {
    }
}
