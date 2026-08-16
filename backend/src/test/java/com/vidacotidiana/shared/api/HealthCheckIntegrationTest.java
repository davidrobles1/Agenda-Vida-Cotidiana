package com.vidacotidiana.shared.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * INFRA-006: /actuator/health must be reachable without a bearer token (so a
 * docker-compose healthcheck or a load balancer can call it) and must not
 * leak component details (DataSourceHealthIndicator's DB/pool info) to an
 * unauthenticated caller — see SecurityConfig's explicit permitAll() for
 * this path and application.yml's management.endpoint.health.show-details:
 * when-authorized.
 *
 * The DOWN case (Postgres actually unreachable) is NOT exercised here —
 * stopping/starting the class-level Testcontainers Postgres mid-test would
 * affect every other test class sharing the JVM's container reuse, so it was
 * verified for real instead against a standalone dev stack (docker stop/start
 * vc-dev-postgres + curl), documented with the actual request/response pairs
 * in 01-technical-backlog.md (INFRA-006).
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthCheckIntegrationTest {

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

    @Test
    void healthEndpoint_isReachableWithoutAuthenticationAndReportsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                // show-details: when-authorized — an unauthenticated caller gets
                // the top-level status only, never the db/diskSpace/ping breakdown.
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void healthEndpoint_respondsWithActuatorContentTypeNotHtml() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/vnd.spring-boot.actuator.v3+json"));
    }
}
