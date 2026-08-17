package com.vidacotidiana.user.application;

import com.vidacotidiana.user.domain.User;
import com.vidacotidiana.user.domain.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Ensures a USER row exists for every authenticated caller, upserted from
 * the validated Keycloak JWT (UC-01/UC-02: the backend never creates
 * accounts itself, it only mirrors the identity Keycloak already
 * authenticated). Runs once per authenticated request via UserSyncFilter.
 */
@Service
public class UserSyncService {

    private final UserRepository userRepository;

    // BE-036 (real race condition found verifying AND-008/WEB-008 self-registration
    // on-device, 05-v2-plan.md §3): self-injected proxy so the retry in
    // syncFromToken below goes through Spring's transactional interceptor and opens
    // a genuinely new transaction/connection — required because a Postgres unique
    // violation aborts the whole transaction (every later statement on that same
    // connection fails with "current transaction is aborted" until rollback), so
    // catching the exception and re-querying inside the same @Transactional method
    // would not work. @Lazy avoids a circular-construction error from injecting the
    // bean into itself.
    private final UserSyncService self;

    public UserSyncService(UserRepository userRepository, @Lazy UserSyncService self) {
        this.userRepository = userRepository;
        // Spring always supplies a non-null proxy here (that's the point of @Lazy
        // self-injection) — the null fallback only matters for plain `new
        // UserSyncService(repo, null)` construction in unit tests, where a direct
        // self-reference is correct since there's no proxy/transaction to route
        // through anyway.
        this.self = self != null ? self : this;
    }

    /**
     * BE-036: two authenticated requests for the same brand-new user can race —
     * both see findById() as empty (no row committed yet) and both attempt
     * save(), the loser failing with a real unique-constraint violation on the
     * primary key. Reproduced for real registering a new account and
     * immediately firing two concurrent authenticated requests (RemindersPage's
     * own GET /reminders + GET /me on first load does exactly this). Rather
     * than propagate the 500, retry once in a fresh transaction: by the time
     * Postgres can even raise the constraint error, the winning transaction's
     * insert is guaranteed visible (concurrent inserts on the same unique key
     * block and recheck rather than erroring past a still-uncommitted row), so
     * the retry's findById() is guaranteed to find the row the other request
     * created.
     */
    public User syncFromToken(UUID userId, String email, String username) {
        try {
            return self.doSync(userId, email, username);
        } catch (DataIntegrityViolationException raceLost) {
            return self.doSync(userId, email, username);
        }
    }

    /**
     * BE-027 correctness fix (found while validating the purge job, same
     * pattern as BE-030..032): a purged (DELETED) account must never be
     * re-synced from the JWT. Without this guard, any authenticated request
     * after purge (the Keycloak account itself isn't deleted by this
     * backend, DEC-004/ADR-008 — that's out of scope here) would silently
     * overwrite the anonymized email/username back to the real values,
     * defeating AC-015's "los datos personales... se purgan
     * definitivamente".
     */
    @Transactional
    public User doSync(UUID userId, String email, String username) {
        return userRepository.findById(userId)
                .map(existing -> {
                    if (!"DELETED".equals(existing.getDeletionStatus())) {
                        existing.refreshFromIdentityProvider(email, username);
                    }
                    return existing;
                })
                .orElseGet(() -> userRepository.save(new User(userId, email, username)));
    }
}
