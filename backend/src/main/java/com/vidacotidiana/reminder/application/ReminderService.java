package com.vidacotidiana.reminder.application;

import com.vidacotidiana.notification.application.PushEvent;
import com.vidacotidiana.notification.application.PushEventType;
import com.vidacotidiana.notification.application.PushNotificationSender;
import com.vidacotidiana.person.application.PersonService;
import com.vidacotidiana.project.application.ProjectService;
import com.vidacotidiana.reminder.domain.Reminder;
import com.vidacotidiana.reminder.domain.ReminderContext;
import com.vidacotidiana.reminder.domain.ReminderRepository;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.VersionConflictException;
import com.vidacotidiana.sharing.domain.ReminderShare;
import com.vidacotidiana.sharing.domain.ReminderShareRepository;
import com.vidacotidiana.sharing.domain.ReminderShareStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
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
    private final PushNotificationSender pushNotificationSender;
    private final PersonService personService;
    private final ProjectService projectService;

    public ReminderService(ReminderRepository reminderRepository, ReminderShareRepository reminderShareRepository,
                            PushNotificationSender pushNotificationSender, PersonService personService,
                            ProjectService projectService) {
        this.reminderRepository = reminderRepository;
        this.reminderShareRepository = reminderShareRepository;
        this.pushNotificationSender = pushNotificationSender;
        this.personService = personService;
        this.projectService = projectService;
    }

    /**
     * BE-038/ADR-015: {@code context} is nullable — defaults to PERSONAL,
     * same reasoning as CreateReminderRequest's own doc comment (every
     * pre-ADR-015 call site doesn't send one yet). iconId/stickerId are
     * likewise optional — a task created before this feature, or created
     * without picking either, simply has none.
     */
    @Transactional
    public Reminder create(UUID ownerUserId, String title, String description, Instant dueAt, String context,
                            String iconId, String stickerId) {
        return create(ownerUserId, title, description, dueAt, context, iconId, stickerId, null, null, null);
    }

    /**
     * ADR-016/FR-023/FR-024: personId/projectId/location, all optional. When
     * present, personId/projectId must reference a Persona/Proyecto owned by
     * the same caller — getOwnedOrThrow throws the same 404
     * (PERSON_NOT_FOUND/PROJECT_NOT_FOUND) it would on direct access,
     * never leaking whether the id exists under another user (SEC-001
     * principle applied here too, not just to Reminder itself).
     */
    @Transactional
    public Reminder create(UUID ownerUserId, String title, String description, Instant dueAt, String context,
                            String iconId, String stickerId, UUID personId, UUID projectId, String location) {
        if (personId != null) {
            personService.getOwnedOrThrow(personId, ownerUserId);
        }
        if (projectId != null) {
            projectService.getOwnedOrThrow(projectId, ownerUserId);
        }
        ReminderContext resolvedContext = (context != null) ? ReminderContext.valueOf(context) : ReminderContext.PERSONAL;
        Reminder reminder = new Reminder(ownerUserId, title, description, dueAt, resolvedContext, iconId, stickerId,
                personId, projectId, location);
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
     * iconId/stickerId: see Reminder#applyEdit's own doc — always applied
     * as sent, not null-guarded like title/description/dueAt.
     */
    @Transactional
    public Reminder edit(UUID reminderId, UUID callerUserId, String title, String description,
                          Instant dueAt, String iconId, String stickerId, int expectedVersion) {
        return edit(reminderId, callerUserId, title, description, dueAt, iconId, stickerId, null, null, null, expectedVersion);
    }

    /** ADR-016/FR-023/FR-024: personId/projectId, when sent, are validated the same way as in create(). */
    @Transactional
    public Reminder edit(UUID reminderId, UUID callerUserId, String title, String description,
                          Instant dueAt, String iconId, String stickerId, UUID personId, UUID projectId,
                          String location, int expectedVersion) {
        Reminder reminder = getOwnedOrThrow(reminderId, callerUserId);

        if (personId != null) {
            personService.getOwnedOrThrow(personId, callerUserId);
        }
        if (projectId != null) {
            projectService.getOwnedOrThrow(projectId, callerUserId);
        }

        if (expectedVersion != reminder.getVersion()) {
            throw new VersionConflictException(
                    "Reminder " + reminderId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + reminder.getVersion() + ").");
        }

        reminder.applyEdit(title, description, dueAt, iconId, stickerId, personId, projectId, location);
        return reminderRepository.save(reminder);
    }

    /**
     * UC-05/AC-013/DEC-002. Owner-only (AC-011). Since BE-016 (V2__sharing.sql)
     * added INVITATION/REMINDER_SHARE with ON DELETE CASCADE on reminder_id,
     * deleting the REMINDER row cascades at the database level, satisfying
     * the first half of DEC-002 without any extra code here. BE-026 closes
     * the second half: every ACTIVE collaborator gets a best-effort push
     * before the row is gone (AC-013).
     */
    @Transactional
    public void delete(UUID reminderId, UUID callerUserId) {
        Reminder reminder = getOwnedOrThrow(reminderId, callerUserId);

        List<ReminderShare> activeShares = reminderShareRepository.findByReminderIdAndStatus(reminderId, ReminderShareStatus.ACTIVE);
        for (ReminderShare share : activeShares) {
            pushNotificationSender.sendBestEffort(share.getCollaboratorUserId(),
                    new PushEvent(PushEventType.REMINDER_DELETED, "A reminder shared with you was deleted: " + reminder.getTitle()));
        }

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
