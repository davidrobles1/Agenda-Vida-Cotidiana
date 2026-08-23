package com.vidacotidiana.visionboard.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * BLOQUE B (post-MVP): real PostgreSQL via Testcontainers, mocked JWT
 * principal, full stack — same pattern as VisionBoardControllerIntegrationTest.
 * Doesn't use the OpenAPI contract matcher that file's other tests do
 * (`openApi().isValid(VALIDATOR)`) — swagger-request-validator's multipart/
 * binary-response support doesn't fit this endpoint pair as cleanly as the
 * JSON ones; the paths/schemas are still documented in openapi.yaml for
 * humans/AI-context, just not contract-asserted here.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VisionBoardImageControllerIntegrationTest {

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
    private ObjectMapper objectMapper;

    private org.springframework.security.oauth2.jwt.Jwt.Builder jwtFor(UUID userId, String email) {
        return org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject(userId.toString())
                .claim("email", email)
                .claim("preferred_username", email)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
    }

    @Test
    void uploadThenGet_roundTripsTheSameBytesAndContentType() throws Exception {
        UUID userId = UUID.randomUUID();
        var principal = jwt().jwt(jwtFor(userId, "img-owner@example.com").build());
        byte[] pngBytes = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", pngBytes);

        String json = mockMvc.perform(multipart("/api/v1/vision-board-images").file(file).with(principal))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType", is("image/png")))
                .andExpect(jsonPath("$.sizeBytes", is(pngBytes.length)))
                .andReturn().getResponse().getContentAsString();
        String imageId = objectMapper.readTree(json).get("id").asText();

        mockMvc.perform(get("/api/v1/vision-board-images/" + imageId).with(principal))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(result -> {
                    byte[] returned = result.getResponse().getContentAsByteArray();
                    org.junit.jupiter.api.Assertions.assertArrayEquals(pngBytes, returned);
                });
    }

    @Test
    void upload_rejectsUnsupportedContentType() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "reject-type@example.com").build());
        MockMultipartFile file = new MockMultipartFile("file", "doc.svg", "image/svg+xml", "<svg></svg>".getBytes());

        mockMvc.perform(multipart("/api/v1/vision-board-images").file(file).with(principal))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void upload_rejectsOversizedImage() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "reject-size@example.com").build());
        // 24MB — above VisionBoardImageService.MAX_SIZE_BYTES (23MB, raised
        // from 8MB on 2026-08-21) but still under application.yml's servlet
        // max-request-size (25MB), so this exercises the service's own
        // typed VALIDATION_ERROR path, not a generic servlet-level rejection.
        byte[] tooLarge = new byte[24 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", tooLarge);

        mockMvc.perform(multipart("/api/v1/vision-board-images").file(file).with(principal))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    void get_anotherUsersImageReturnsNotFound() throws Exception {
        var owner = jwt().jwt(jwtFor(UUID.randomUUID(), "owner2@example.com").build());
        var stranger = jwt().jwt(jwtFor(UUID.randomUUID(), "stranger2@example.com").build());
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});

        String json = mockMvc.perform(multipart("/api/v1/vision-board-images").file(file).with(owner))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String imageId = objectMapper.readTree(json).get("id").asText();

        mockMvc.perform(get("/api/v1/vision-board-images/" + imageId).with(stranger))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_nonexistentReturnsNotFound() throws Exception {
        var principal = jwt().jwt(jwtFor(UUID.randomUUID(), "missing-img@example.com").build());
        mockMvc.perform(get("/api/v1/vision-board-images/" + UUID.randomUUID()).with(principal))
                .andExpect(status().isNotFound());
    }
}
