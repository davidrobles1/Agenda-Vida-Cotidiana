package com.vidacotidiana;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @EnableScheduling backs the account-purge (BE-027), invitation-expiration
 * (BE-033), and orphaned-invitation-purge (BE-028) jobs — a single Spring
 * @Scheduled per concern is sufficient for V1's single-instance deployment;
 * no external scheduler (Quartz) is justified (CLAUDE.md "no sobrearquitecturar").
 */
@SpringBootApplication
@EnableScheduling
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
