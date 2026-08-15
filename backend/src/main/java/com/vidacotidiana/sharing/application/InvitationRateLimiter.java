package com.vidacotidiana.sharing.application;

import com.vidacotidiana.shared.domain.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * DEVOPS-001/SEC-001/21-security.md: "rate limiting por usuario/IP sobre
 * creación de invitaciones", algorithm/threshold left to technical
 * judgment. In-memory sliding window per authenticated caller — V1 runs a
 * single backend instance (INFRA-004 is still TBD on the exact hosting
 * setup, but nothing in this cycle introduces multi-instance deployment),
 * so an in-memory limiter is sufficient and avoids adding an external
 * dependency (Redis, Bucket4j) not otherwise justified for V1.
 *
 * MAX_INVITATIONS_PER_WINDOW/WINDOW are technical parameters (not a
 * business decision) chosen to be generous enough for legitimate use
 * (inviting a household/small group) while still bounding abuse.
 */
@Component
public class InvitationRateLimiter {

    private static final int MAX_INVITATIONS_PER_WINDOW = 10;
    private static final Duration WINDOW = Duration.ofHours(1);

    private final ConcurrentHashMap<UUID, Deque<Instant>> recentInvitationsByUser = new ConcurrentHashMap<>();

    void checkAndRecord(UUID userId) {
        Instant now = Instant.now();
        Deque<Instant> timestamps = recentInvitationsByUser.computeIfAbsent(userId, id -> new ConcurrentLinkedDeque<>());
        synchronized (timestamps) {
            Instant windowStart = now.minus(WINDOW);
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_INVITATIONS_PER_WINDOW) {
                throw new RateLimitExceededException(
                        "Too many invitations created recently; try again later.");
            }
            timestamps.addLast(now);
        }
    }
}
