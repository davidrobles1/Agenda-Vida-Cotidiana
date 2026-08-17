package com.vidacotidiana.user.api;

import com.vidacotidiana.user.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * BE-036 (05-v2-plan.md §3, 33-security-cross-audit.md §1.2): real
 * reproduction of the race condition found on-device verifying AND-008/
 * WEB-008 self-registration — two authenticated requests for a brand-new
 * user, fired as close to simultaneously as the JVM allows (a CountDownLatch
 * releasing both threads together, not a fixed delay), against a real
 * Postgres (Testcontainers, not mocked/inferred). Before the fix this
 * reproduced a real 500 (duplicate key value violates unique constraint
 * "users_pkey") on the losing thread; this test would have failed against
 * the pre-fix UserSyncService.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserSyncServiceConcurrencyIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("vidacotidiana_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private Jwt jwtFor(UUID userId, String email, String username) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .claim("email", email)
                .claim("preferred_username", username)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    @Test
    void twoConcurrentFirstRequestsForABrandNewUser_bothSucceed_onlyOneRowIsCreated() throws Exception {
        UUID userId = UUID.randomUUID();
        Jwt token = jwtFor(userId, "concurrent@example.com", "concurrentuser");

        int requestCount = 8;
        ExecutorService pool = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger serverErrors = new AtomicInteger();

        List<Future<Integer>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < requestCount; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await();
                var result = mockMvc.perform(get("/api/v1/me").with(jwt().jwt(token))).andReturn();
                int status = result.getResponse().getStatus();
                if (status >= 500) serverErrors.incrementAndGet();
                return status;
            }));
        }

        ready.await(10, TimeUnit.SECONDS);
        start.countDown();

        for (Future<Integer> future : futures) {
            assertThat(future.get(20, TimeUnit.SECONDS))
                    .as("every concurrent request for a brand-new user must succeed, none may 500")
                    .isEqualTo(200);
        }
        pool.shutdown();

        assertThat(serverErrors.get()).isZero();

        long rowCount = userRepository.findAll().stream().filter(u -> u.getId().equals(userId)).count();
        assertThat(rowCount)
                .as("exactly one USER row must exist after the race, never zero or duplicated")
                .isEqualTo(1);

        var row = userRepository.findById(userId).orElseThrow();
        assertThat(row.getEmail()).isEqualTo("concurrent@example.com");
        assertThat(row.getUsername()).isEqualTo("concurrentuser");
    }
}
