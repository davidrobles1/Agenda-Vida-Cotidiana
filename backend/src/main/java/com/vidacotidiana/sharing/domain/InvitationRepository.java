package com.vidacotidiana.sharing.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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
     *
     * BE-033: also requires expiresAt > now in the same atomic statement —
     * without this, an invitation whose expiresAt already passed but that
     * the expiration sweep (expireOverduePending, below) hasn't reached yet
     * could still be accepted/rejected/cancelled. This must stay a single
     * conditional UPDATE, not a separate Java-side expiresAt check, for the
     * same race-safety reason the PENDING check itself is atomic.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Invitation i SET i.status = :newStatus, i.resolvedAt = CURRENT_TIMESTAMP "
            + "WHERE i.id = :id AND i.status = com.vidacotidiana.sharing.domain.InvitationStatus.PENDING "
            + "AND i.expiresAt > CURRENT_TIMESTAMP")
    int resolveIfPending(@Param("id") UUID id, @Param("newStatus") InvitationStatus newStatus);

    /**
     * BE-033 — gap found while cross-checking 09-data-model.md's
     * `INVITATION(status, expires_at)` index comment ("para el job de
     * expiración") against the backlog: no job existed to actually flip
     * PENDING -> EXPIRED. A bulk atomic sweep, same WHERE-conditioned
     * UPDATE pattern as resolveIfPending.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Invitation i SET i.status = com.vidacotidiana.sharing.domain.InvitationStatus.EXPIRED, "
            + "i.resolvedAt = CURRENT_TIMESTAMP "
            + "WHERE i.status = com.vidacotidiana.sharing.domain.InvitationStatus.PENDING AND i.expiresAt <= CURRENT_TIMESTAMP")
    int expireOverduePending();

    /**
     * BE-028/AC-016/DEC-015(A'): invitations resolved (REJECTED/EXPIRED/
     * CANCELLED) more than 90 days ago (ASSUMPTION, 09-data-model.md) whose
     * recipient never created an account are purged outright — deleting the
     * whole row is the simplest of the two documented-as-equivalent options
     * ("purgar invited_email o la fila completa"), not a new decision.
     */
    @Modifying
    @Query("DELETE FROM Invitation i WHERE i.invitedUserId IS NULL AND i.resolvedAt < :cutoff AND i.status IN ("
            + "com.vidacotidiana.sharing.domain.InvitationStatus.REJECTED, "
            + "com.vidacotidiana.sharing.domain.InvitationStatus.EXPIRED, "
            + "com.vidacotidiana.sharing.domain.InvitationStatus.CANCELLED)")
    int purgeOrphaned(@Param("cutoff") Instant cutoff);
}
