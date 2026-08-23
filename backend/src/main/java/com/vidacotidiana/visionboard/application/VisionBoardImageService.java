package com.vidacotidiana.visionboard.application;

import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.ValidationException;
import com.vidacotidiana.visionboard.domain.VisionBoardImage;
import com.vidacotidiana.visionboard.domain.VisionBoardImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.UUID;

/**
 * BLOQUE B (post-MVP): validates and stores an uploaded/pasted/dropped
 * Vision Board image, and serves it back out — see V10 migration's doc
 * comment for why the storage itself is Postgres BYTEA and not real
 * object storage. Deliberately a thin, self-contained service (not folded
 * into VisionBoardService) so a later swap to S3-compatible storage only
 * ever touches this one class — `upload`/`getOwnedOrThrow` are the whole
 * surface any caller depends on.
 *
 * MIME/size validation happens here, not only via Spring's
 * `spring.servlet.multipart.max-file-size` (application.yml) — that config
 * rejects oversized *requests* before a controller method even runs (a
 * generic failure), this also gives a clear, typed VALIDATION_ERROR for
 * "not an image at all" and restates the size limit as a real domain rule,
 * not just a servlet-container ceiling (CLAUDE.md, "MIME validation", "file
 * size limits").
 */
@Service
public class VisionBoardImageService {

    /** CLAUDE.md asks V1 to only store what's actually needed — no SVG (XML,
        can carry embedded scripts) and no AVIF/HEIC (rarer, inconsistent
        browser `<img>` support) for a first pass; the four formats every
        Vision Board sticker/export path already assumes cover the realistic
        "photo I want to drop on my board" case. */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

    /** Pedido explícito del usuario (2026-08-21): "aumenta el peso de
        imágenes unos 15 megas más para cargar" — el límite original (8MB,
        ver ASSUMPTION previa) resultaba insuficiente para fotos de cámaras
        modernas/alta resolución que los usuarios querían subir. 23MB =
        8MB + 15MB, aplicando el aumento solicitado directamente sobre el
        límite anterior. `application.yml`'s
        `spring.servlet.multipart.max-file-size`/`max-request-size` (25MB)
        se subieron en la misma proporción, manteniendo el mismo margen
        servlet-vs-dominio que ya existía (10MB vs 8MB). */
    private static final long MAX_SIZE_BYTES = 23L * 1024 * 1024;

    private final VisionBoardImageRepository repository;

    public VisionBoardImageService(VisionBoardImageRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public VisionBoardImage upload(UUID ownerUserId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ValidationException("The uploaded file is empty.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ValidationException(
                    "Unsupported image type" + (contentType != null ? ": " + contentType : "")
                            + ". Allowed: " + String.join(", ", ALLOWED_CONTENT_TYPES) + ".");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ValidationException("Image exceeds the " + (MAX_SIZE_BYTES / (1024 * 1024)) + "MB limit.");
        }
        try {
            VisionBoardImage image = new VisionBoardImage(ownerUserId, contentType, file.getBytes());
            return repository.save(image);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the uploaded image.", e);
        }
    }

    /** Same non-enumeration principle as VisionBoardService#getOwnedOrThrow
        — a missing id and an id owned by someone else both 404, never
        distinguished in the response. */
    @Transactional(readOnly = true)
    public VisionBoardImage getOwnedOrThrow(UUID imageId, UUID callerUserId) {
        VisionBoardImage image = repository.findById(imageId)
                .orElseThrow(() -> new NotFoundException("VISION_BOARD_IMAGE_NOT_FOUND",
                        "Vision Board image " + imageId + " was not found."));
        if (!image.isOwnedBy(callerUserId)) {
            throw new NotFoundException("VISION_BOARD_IMAGE_NOT_FOUND",
                    "Vision Board image " + imageId + " was not found.");
        }
        return image;
    }
}
