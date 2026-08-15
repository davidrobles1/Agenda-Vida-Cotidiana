package com.vidacotidiana.reminder.application;

import com.vidacotidiana.reminder.domain.Reminder;
import com.vidacotidiana.reminder.domain.ReminderRepository;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.VersionConflictException;
import com.vidacotidiana.sharing.domain.ReminderShareRepository;
import com.vidacotidiana.sharing.domain.ReminderShareStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Application service for the Reminder vertical slice (UC-03/UC-04/UC-05,
 * AC-003/AC-004/AC-004b/AC-005/AC-011/AC-013). Authorization and optimistic
 * locking are enforced here, not in the controller (15-coding-standards.md:
 * "controllers delgados; lógica en application/domain").
 *
 * Two authorization levels (AC-011/11-auth-security.md, BE-022):
 * - Owner-only (edit, delete, and — via getOwnedOrThrow, used by
 *   sharing.application.SharingService — invite/list-shares/revoke): only
 *   the owner. A COLLABORATOR here gets the same 404 as a stranger.
 * - Owner-or-active-collaborator (read, toggle completion): either the
 *   owner or a caller with an ACTIVE ReminderShare on this reminder.
 * Both cases: no access at all (including a REVOKED share, or none) gets
 * exactly the same NotFoundException as a truly missing reminder — never
 * 403, never distinguishing the two, to avoid leaking existence (AC-004,
 * consistent with the NotFound response description in openapi.yaml).
 */
@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final ReminderShareRepository reminderShareRepository;

    public ReminderService(ReminderRepository reminderRepository, ReminderShareRepository reminderShareRepository) {
        this.reminderRepository = reminderRepository;
        this.reminderShareRepository = reminderShareRepository;
    }

    @Transactional
    public Reminder create(UUID ownerUserId, String title, String description, Instant dueAt) {
        Reminder reminder = new Reminder(ownerUserId, title, description, dueAt);
        return reminderRepository.save(reminder);
    }

    /** FR-003/FR-004: owned reminders plus reminders shared with the caller as an ACTIVE collaborator. */
    @Transactional(readOnly = true)
    public Page<Reminder> listAccessibleTo(UUID callerUserId, Pageable pageable) {
        return reminderRepository.findAccessibleTo(callerUserId, pageable);
    }

    @Transactional(readOnly = true)
    public Reminder getAccessible(UUID reminderId, UUID callerUserId) {
        Reminder reminder = findOrThrow(reminderId);
        requireOwnerOrActiveCollaborator(reminder, callerUserId);
        return reminder;
    }

    /**
     * UC-04/AC-005/AC-011. Owner or active collaborator. If expectedVersion
     * is present, validates it against the stored version first (fast,
     * explicit 409 with the documented code) before applying the toggle; if
     * absent, applies the toggle without a concurrency check, as the
     * contract specifies.
     */
    @Transactional
    public Reminder toggleCompletion(UUID reminderId, UUID callerUserId, Integer expectedVersion) {
        Reminder reminder = findOrThrow(reminderId);
        requireOwnerOrActiveCollaborator(reminder, callerUserId);

        if (expectedVersion != null && expectedVersion != reminder.getVersion()) {
            throw new VersionConflictException(
                    "Reminder " + reminderId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + reminder.getVersion() + ").");
        }

        reminder.toggleCompletion();
        // The @Version column still protects against a true race that slipped
        // past the check above; Hibernate throws ObjectOptimisticLockingFailureException
        // on flush in that case, mapped to the same 409 by GlobalExceptionHandler.
        return reminderRepository.save(reminder);
    }

    /**
     * UC-05 (edit)/AC-004b. Owner-only (AC-011: a collaborator may never
     * edit). Unlike toggleCompletion, version is mandatory here: a mismatch
     * always rejects the edit with 409, never silently skips the check.
     */
    @Transactional
    public Reminder edit(UUID reminderId, UUID callerUserId, String title, String description,
                          Instant dueAt, int expectedVersion) {
        Reminder reminder = getOwnedOrThrow(reminderId, callerUserId);

        if (expectedVersion != reminder.getVersion()) {
            throw new VersionConflictException(
                    "Reminder " + reminderId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + reminder.getVersion() + ").");
        }

        reminder.applyEdit(title, description, dueAt);
        return reminderRepository.save(reminder);
    }

    /**
     * UC-05/AC-013/DEC-002. Owner-only (AC-011). Since BE-016 (V2__sharing.sql)
     * added INVITATION/REMINDER_SHARE with ON DELETE CASCADE on reminder_id,
     * deleting the REMINDER row now really does cascade at the database
     * level, satisfying that half of DEC-002 without any code change here.
     * What's still a real no-op is the push notification to active
     * collaborators (AC-013's second requirement) — notification/BE-025/026
     * don't exist yet. That gap is declared explicitly, not hidden: this
     * method extends to send it once BE-026 lands.
     */
    @Transactional
    public void delete(UUID reminderId, UUID callerUserId) {
        Reminder reminder = getOwnedOrThrow(reminderId, callerUserId);
        reminderRepository.delete(reminder);
    }

    /**
     * Owner-only authorization, exposed for sharing.application.SharingService
     * (BE-017/018/021: creating/listing invitations, revoking a share are all
     * owner-only operations on the underlying reminder) so that module reuses
     * this check instead of duplicating it against ReminderRepository directly.
     */
    @Transactional(readOnly = true)
    public Reminder getOwnedOrThrow(UUID reminderId, UUID callerUserId) {
        Reminder reminder = findOrThrow(reminderId);
        requireOwner(reminder, callerUserId);
        return reminder;
    }

    private Reminder findOrThrow(UUID reminderId) {
        return reminderRepository.findById(reminderId)
                .orElseThrow(() -> new NotFoundException("REMINDER_NOT_FOUND", "The requested reminder was not found."));
    }

    private void requireOwner(Reminder reminder, UUID callerUserId) {
        if (!reminder.isOwnedBy(callerUserId)) {
            throw new NotFoundException("REMINDER_NOT_FOUND", "The requested reminder was not found.");
        }
    }

    private void requireOwnerOrActiveCollaborator(Reminder reminder, UUID callerUserId) {
        if (reminder.isOwnedBy(callerUserId)) {
            return;
        }
        boolean isActiveCollaborator = reminderShareRepository.existsByReminderIdAndCollaboratorUserIdAndStatus(
                reminder.getId(), callerUserId, ReminderShareStatus.ACTIVE);
        if (!isActiveCollaborator) {
            throw new NotFoundException("REMINDER_NOT_FOUND", "The requested reminder was not found.");
        }
    }
}
