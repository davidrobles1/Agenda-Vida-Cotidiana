package com.vidacotidiana.user.api;

import com.vidacotidiana.user.application.AccountDeletionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BE-027 — UC-13/AC-015/DEC-015, real PostgreSQL via Testcontainers. Forces
 * purge_at into the past directly in the database (as the task explicitly
 * allows) rather than waiting 30 real days, then invokes the scheduled
 * purge job's method directly instead of waiting for @Scheduled to fire.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountDeletionIntegrationTest {

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
    private DataSource dataSource;

    @Autowired
    private AccountDeletionService accountDeletionService;

    private RequestPostProcessor principalFor(UUID userId, String email, String username) {
        var jwtToken = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .claim("email", email)
                .claim("preferred_username", username)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        return jwt().jwt(jwtToken);
    }

    @Test
    void deleteMe_thenPurgeJob_anonymizesAccountPastGracePeriod() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = principalFor(userId, "todelete@example.com", "todelete");

        // Sync the USER row (UserSyncFilter runs on any authenticated request).
        mockMvc.perform(get("/api/v1/me").with(principal)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/me").with(principal)).andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/me").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletionStatus", is("PENDING_DELETION")));

        // Force purge_at into the past — real 30-day wait is not practical in a test, exactly as the task allows.
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE users SET purge_at = ? WHERE id = ?")) {
            statement.setObject(1, OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
            statement.setObject(2, userId);
            statement.executeUpdate();
        }

        // Invoke the scheduled job's method directly instead of waiting for @Scheduled to fire.
        accountDeletionService.purgeAccountsPastGracePeriod();

        mockMvc.perform(get("/api/v1/me").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletionStatus", is("DELETED")))
                .andExpect(jsonPath("$.email", is("deleted-" + userId + "@purged.invalid")));
    }

    @Test
    void purgeJob_leavesAccountsStillWithinGracePeriodUntouched() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = principalFor(userId, "stillwaiting@example.com", "stillwaiting");
        mockMvc.perform(get("/api/v1/me").with(principal)).andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/me").with(principal)).andExpect(status().isAccepted());

        // purge_at is 30 days out by default — the job must not touch this account yet.
        accountDeletionService.purgeAccountsPastGracePeriod();

        mockMvc.perform(get("/api/v1/me").with(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletionStatus", is("PENDING_DELETION")))
                .andExpect(jsonPath("$.email", is("stillwaiting@example.com")));
    }
}
