package com.vidacotidiana.visionboard.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Mirrors reminder.api.dto.CreateReminderRequest/note.api.dto.CreateNoteRequest's shape.
 * {@code theme} is optional — a missing/omitted value defaults to LIGHT
 * (VisionBoardService#create), same "sensible default when unspecified"
 * rule already used elsewhere in this API.
 */
public record CreateVisionBoardRequest(
        @NotBlank @Size(min = 1, max = 200) String name,
        @Size(max = 2000) String description,
        @NotNull @Min(1) Integer width,
        @NotNull @Min(1) Integer height,
        @Pattern(regexp = "LIGHT|DARK|PAPER|NATURAL|CALM|ENERGY") String theme
) {
}
