package com.vidacotidiana.sharing.application;

import com.vidacotidiana.audit.application.AuditEventService;
import com.vidacotidiana.audit.domain.AuditEventType;
import com.vidacotidiana.audit.domain.AuditTargetType;
import com.vidacotidiana.sharing.domain.Invitation;
import com.vidacotidiana.sharing.domain.InvitationRepository;
import com.vidacotidiana.sharing.domain.InvitationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for BE-033 (expiration sweep) and BE-028/AC-016 (orphan purge). */
class InvitationMaintenanceServiceTest {

    private InvitationRepository invitationRepository;
    private AuditEventService auditEventService;
    private InvitationMaintenanceService service;

    @BeforeEach
    void setUp() {
        invitationRepository = Mockito.mock(InvitationRepository.class);
        auditEventService = Mockito.mock(AuditEventService.class);
        service = new InvitationMaintenanceService(invitationRepository, auditEventService);
    }

    @Test
    void expireOverduePendingInvitations_expiresEachCandidateAndAuditsIt() {
        Invitation overdue = new Invitation(UUID.randomUUID(), UUID.randomUUID(), "overdue@example.com", null);
        UUID invitationId = fakeInvitationId(overdue);
        when(invitationRepository.findByStatusAndExpiresAtLessThanEqual(org.mockito.ArgumentMatchers.eq(InvitationStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(overdue));
        when(invitationRepository.expireIfOverdue(invitationId)).thenReturn(1);

        service.expireOverduePendingInvitations();

        verify(invitationRepository).expireIfOverdue(invitationId);
        // BE-029: actorUserId is null — this is a system job, not a user action.
        verify(auditEventService).record(AuditEventType.INVITATION_EXPIRED, null, AuditTargetType.INVITATION, invitationId);
    }

    @Test
    void expireOverduePendingInvitations_raceLostToConcurrentResolution_doesNotAudit() {
        // BE-033/BE-029: if a concurrent accept/reject/cancel resolved the row first, expireIfOverdue
        // returns 0 (its own atomic WHERE no longer matches) and no audit event must be recorded for it.
        Invitation overdue = new Invitation(UUID.randomUUID(), UUID.randomUUID(), "raced@example.com", null);
        UUID invitationId = fakeInvitationId(overdue);
        when(invitationRepository.findByStatusAndExpiresAtLessThanEqual(org.mockito.ArgumentMatchers.eq(InvitationStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(overdue));
        when(invitationRepository.expireIfOverdue(invitationId)).thenReturn(0);

        service.expireOverduePendingInvitations();

        verify(auditEventService, never()).record(any(), any(), any(), any());
    }

    @Test
    void purgeOrphanedInvitations_usesA90DayCutoff() {
        service.purgeOrphanedInvitations();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(invitationRepository).purgeOrphaned(cutoffCaptor.capture());
        Instant expectedCutoff = Instant.now().minus(90, ChronoUnit.DAYS);
        assertThat(cutoffCaptor.getValue()).isCloseTo(expectedCutoff, within(5, ChronoUnit.SECONDS));
    }

    private UUID fakeInvitationId(Invitation invitation) {
        UUID id = UUID.randomUUID();
        try {
            Field idField = Invitation.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(invitation, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return id;
    }
}
