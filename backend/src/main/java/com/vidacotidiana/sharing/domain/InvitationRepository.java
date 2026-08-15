package com.vidacotidiana.sharing.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    // AC-007: app-level pre-check for the friendly 409; the partial unique
    // index (uq_invitations_pending_reminder_email, V2__sharing.sql) is the
    // real guarantee against a genuine concurrent duplicate.
    Optional<Invitation> findByReminderIdAndInvitedEmailAndStatus(UUID reminderId, String invitedEmail, InvitationStatus status);

    // "Collaborators and pending invitations" for a reminder (BE-018) only ever shows PENDING ones.
    Page<Invitation> findByReminderIdAndStatus(UUID reminderId, InvitationStatus status, Pageable pageable);

    // GET /me/invitations (BE-018): pending invitations received by the caller.
    Page<Invitation> findByInvitedUserIdAndStatus(UUID invitedUserId, InvitationStatus status, Pageable pageable);

    /**
     * SEC-002/AC-008/AC-009/AC-017: the atomic conditional transition out of
     * PENDING required to avoid the race between two concurrent resolutions
     * of the same invitation (accept/reject/cancel/expiry). Returns the
     * number of rows updated — 0 means the invitation was no longer PENDING
     * when this statement ran, and the caller must respond 410 (Gone), never
     * apply the transition based on a value read earlier in Java.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Invitation i SET i.status = :newStatus, i.resolvedAt = CURRENT_TIMESTAMP "
            + "WHERE i.id = :id AND i.status = com.vidacotidiana.sharing.domain.InvitationStatus.PENDING")
    int resolveIfPending(@Param("id") UUID id, @Param("newStatus") InvitationStatus newStatus);
}
