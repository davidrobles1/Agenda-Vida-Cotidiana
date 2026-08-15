package com.vidacotidiana.sharing.application;

import com.vidacotidiana.audit.application.AuditEventService;
import com.vidacotidiana.audit.domain.AuditEventType;
import com.vidacotidiana.audit.domain.AuditTargetType;
import com.vidacotidiana.sharing.domain.Invitation;
import com.vidacotidiana.sharing.domain.InvitationRepository;
import com.vidacotidiana.sharing.domain.InvitationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Scheduled housekeeping for INVITATION, separate from SharingService
 * (which only handles request-driven business operations): BE-033
 * (expire overdue PENDING invitations) and BE-028/AC-016 (purge orphaned
 * resolved invitations after their retention window). Both run hourly via
 * Spring's @Scheduled (@EnableScheduling on BackendApplication) — no
 * external scheduler justified for V1's single instance.
 */
@Service
public class InvitationMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(InvitationMaintenanceService.class);
    private static final long JOB_INTERVAL_MS = 60 * 60 * 1000; // hourly
    private static final Duration ORPHAN_RETENTION = Duration.ofDays(90); // ASSUMPTION, 09-data-model.md

    private final InvitationRepository invitationRepository;
    private final AuditEventService auditEventService;

    public InvitationMaintenanceService(InvitationRepository invitationRepository, AuditEventService auditEventService) {
        this.invitationRepository = invitationRepository;
        this.auditEventService = auditEventService;
    }

    /**
     * BE-033: closes the gap where an overdue PENDING invitation was never
     * actually flipped to EXPIRED. BE-029: audits each row individually
     * (actorUserId null — this is a system job, not a user action) using
     * the same per-row atomic conditional UPDATE that guards against a
     * concurrent accept/reject/cancel resolving the same row first.
     */
    @Scheduled(fixedRate = JOB_INTERVAL_MS)
    @Transactional
    public void expireOverduePendingInvitations() {
        List<Invitation> candidates = invitationRepository.findByStatusAndExpiresAtLessThanEqual(InvitationStatus.PENDING, Instant.now());
        int expiredCount = 0;
        for (Invitation candidate : candidates) {
            int updated = invitationRepository.expireIfOverdue(candidate.getId());
            if (updated == 1) {
                auditEventService.record(AuditEventType.INVITATION_EXPIRED, null, AuditTargetType.INVITATION, candidate.getId());
                expiredCount++;
            }
        }
        if (expiredCount > 0) {
            log.info("Expired {} overdue pending invitation(s).", expiredCount);
        }
    }

    /** BE-028/AC-016. Offset from the expiration sweep so a newly-expired row isn't purged in the same pass. */
    @Scheduled(fixedRate = JOB_INTERVAL_MS, initialDelay = JOB_INTERVAL_MS / 2)
    @Transactional
    public void purgeOrphanedInvitations() {
        int purged = invitationRepository.purgeOrphaned(Instant.now().minus(ORPHAN_RETENTION));
        if (purged > 0) {
            log.info("Purged {} orphaned invitation(s) older than {} days.", purged, ORPHAN_RETENTION.toDays());
        }
    }
}
