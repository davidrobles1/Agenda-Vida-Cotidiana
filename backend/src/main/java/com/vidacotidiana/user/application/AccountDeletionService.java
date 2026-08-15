package com.vidacotidiana.user.application;

import com.vidacotidiana.user.domain.User;
import com.vidacotidiana.user.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * UC-13/AC-015/DEC-015 (BE-027). No cancellation/reversal endpoint exists —
 * 09-data-model.md marks that behavior explicitly TBD (UX, non-blocking)
 * and openapi.yaml documents no such endpoint, so none is invented here.
 * No login-blocking during PENDING_DELETION either: UC-13 step 3 marks the
 * exact behavior TBD, and this backend doesn't manage sessions at all
 * (DEC-004/ADR-008) — that would live in Keycloak, not here, if ever built.
 *
 * The purge job runs hourly via Spring's @Scheduled (@EnableScheduling on
 * BackendApplication) — a single-instance-appropriate choice; V1 has no
 * justification for Quartz or another external scheduler (CLAUDE.md "no
 * sobrearquitecturar").
 */
@Service
public class AccountDeletionService {

    private static final Logger log = LoggerFactory.getLogger(AccountDeletionService.class);
    private static final long PURGE_JOB_INTERVAL_MS = 60 * 60 * 1000; // hourly

    private final UserRepository userRepository;

    public AccountDeletionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void requestDeletion(UUID userId) {
        // UserSyncFilter guarantees this row exists for any authenticated caller.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user row missing after sync filter."));
        user.requestDeletion();
        log.info("Account deletion requested: userId={}, purgeAt={}", userId, user.getPurgeAt());
    }

    @Scheduled(fixedRate = PURGE_JOB_INTERVAL_MS)
    @Transactional
    public void purgeAccountsPastGracePeriod() {
        List<User> due = userRepository.findByDeletionStatusAndPurgeAtBefore("PENDING_DELETION", Instant.now());
        for (User user : due) {
            user.purge();
        }
        if (!due.isEmpty()) {
            log.info("Purged {} account(s) past their 30-day grace period.", due.size());
        }
    }
}
