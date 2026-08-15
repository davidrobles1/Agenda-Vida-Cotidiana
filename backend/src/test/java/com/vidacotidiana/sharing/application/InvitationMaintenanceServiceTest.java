package com.vidacotidiana.sharing.application;

import com.vidacotidiana.sharing.domain.InvitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for BE-033 (expiration sweep) and BE-028/AC-016 (orphan purge). */
class InvitationMaintenanceServiceTest {

    private InvitationRepository invitationRepository;
    private InvitationMaintenanceService service;

    @BeforeEach
    void setUp() {
        invitationRepository = Mockito.mock(InvitationRepository.class);
        service = new InvitationMaintenanceService(invitationRepository);
    }

    @Test
    void expireOverduePendingInvitations_delegatesToAtomicBulkUpdate() {
        when(invitationRepository.expireOverduePending()).thenReturn(3);

        service.expireOverduePendingInvitations();

        verify(invitationRepository).expireOverduePending();
    }

    @Test
    void purgeOrphanedInvitations_usesA90DayCutoff() {
        service.purgeOrphanedInvitations();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(invitationRepository).purgeOrphaned(cutoffCaptor.capture());
        Instant expectedCutoff = Instant.now().minus(90, ChronoUnit.DAYS);
        assertThat(cutoffCaptor.getValue()).isCloseTo(expectedCutoff, within(5, ChronoUnit.SECONDS));
    }
}
