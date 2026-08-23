package com.vidacotidiana.visionboard.api.dto;

import com.vidacotidiana.visionboard.domain.VisionBoardImage;

import java.util.UUID;

/**
 * BLOQUE B: the "internal reference" CLAUDE.md asks for — {@code id} is
 * what an IMAGE element's {@code data.imageId} stores; the client resolves
 * the actual bytes via {@code GET /vision-board-images/{id}}. Never echoes
 * the raw bytes back on upload.
 */
public record VisionBoardImageResponse(UUID id, String contentType, long sizeBytes) {

    public static VisionBoardImageResponse from(VisionBoardImage image) {
        return new VisionBoardImageResponse(image.getId(), image.getContentType(), image.getSizeBytes());
    }
}
