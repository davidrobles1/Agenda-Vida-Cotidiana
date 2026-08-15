package com.vidacotidiana.reminder.application;

import com.vidacotidiana.reminder.domain.Reminder;
import com.vidacotidiana.reminder.domain.ReminderRepository;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.VersionConflictException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Application service for the Reminder vertical slice (UC-03/UC-04,
 * AC-003/AC-004/AC-005). Authorization (ownership) and optimistic locking
 * are enforced here, not in the controller (15-coding-standards.md:
 * "controllers delgados; lógica en application/domain").
 *
 * Sharing (REMINDER_SHARE / COLLABORATOR access) is not implemented yet —
 * see docs/development/01-technical-backlog.md BE-016..022. Until then,
 * "accessible" is exactly "owned".
 */
@Service
public class ReminderService {

    private final ReminderRepository reminderRepository;

    public ReminderService(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    @Transactional
    public Reminder create(UUID ownerUserId, String title, String description, Instant dueAt) {
        Reminder reminder = new Reminder(ownerUserId, title, description, dueAt);
        return reminderRepository.save(reminder);
    }

    @Transactional(readOnly = true)
    public Page<Reminder> listOwnedBy(UUID ownerUserId, Pageable pageable) {
        return reminderRepository.findByOwnerUserId(ownerUserId, pageable);
    }

    @Transactional(readOnly = true)
    public Reminder getAccessible(UUID reminderId, UUID callerUserId) {
        Reminder reminder = findOrThrow(reminderId);
        requireAccess(reminder, callerUserId);
        return reminder;
    }

    /**
     * UC-04/AC-005. If expectedVersion is present, validates it against the
     * stored version first (fast, explicit 409 with the documented code)
     * before applying the toggle; if absent, applies the toggle without a
     * concurrency check, as the contract specifies.
     */
    @Transactional
    public Reminder toggleCompletion(UUID reminderId, UUID callerUserId, Integer expectedVersion) {
        Reminder reminder = findOrThrow(reminderId);
        requireAccess(reminder, callerUserId);

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
     * UC-05 (edit)/AC-004b. Unlike toggleCompletion, version is mandatory
     * here: a mismatch always rejects the edit with 409, never silently
     * skips the check.
     */
    @Transactional
    public Reminder edit(UUID reminderId, UUID callerUserId, String title, String description,
                          Instant dueAt, int expectedVersion) {
        Reminder reminder = findOrThrow(reminderId);
        requireAccess(reminder, callerUserId);

        if (expectedVersion != reminder.getVersion()) {
            throw new VersionConflictException(
                    "Reminder " + reminderId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + reminder.getVersion() + ").");
        }

        reminder.applyEdit(title, description, dueAt);
        return reminderRepository.save(reminder);
    }

    /**
     * UC-05/AC-013/DEC-002. openapi.yaml describes this endpoint as
     * cascading to INVITATION/REMINDER_SHARE and notifying active
     * collaborators before deletion — but REMINDER_SHARE/INVITATION and
     * push notifications don't exist yet in the code (BE-016..021, BE-025
     * are still TODO). There is nothing to cascade and no one to notify
     * today, so this deletes exactly the REMINDER row: a real no-op on the
     * still-unimplemented parts, not a contract violation. When sharing and
     * push land, this method extends to cover the cascade and the
     * notification (BE-022/BE-026 in 01-technical-backlog.md); no new
     * backlog id is needed.
     */
    @Transactional
    public void delete(UUID reminderId, UUID callerUserId) {
        Reminder reminder = findOrThrow(reminderId);
        requireAccess(reminder, callerUserId);
        reminderRepository.delete(reminder);
    }

    private Reminder findOrThrow(UUID reminderId) {
        return reminderRepository.findById(reminderId)
                .orElseThrow(() -> new NotFoundException("REMINDER_NOT_FOUND", "The requested reminder was not found."));
    }

    /**
     * AC-004: "otro usuario recibe 404 ... sin revelar existencia" — a
     * caller without access gets the same NotFoundException as a truly
     * missing reminder, never 403, to avoid leaking existence (consistent
     * with the NotFound response description in openapi.yaml).
     */
    private void requireAccess(Reminder reminder, UUID callerUserId) {
        if (!reminder.isOwnedBy(callerUserId)) {
            throw new NotFoundException("REMINDER_NOT_FOUND", "The requested reminder was not found.");
        }
    }
}
