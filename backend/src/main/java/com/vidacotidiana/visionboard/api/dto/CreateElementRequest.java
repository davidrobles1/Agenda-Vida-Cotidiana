package com.vidacotidiana.visionboard.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.Map;

/**
 * {@code type} is validated as a string here (same pattern as
 * reminder.api.dto.CreateReminderRequest's {@code context}) so an invalid
 * value fails request validation with a clear message before it ever
 * reaches VisionBoardElementType.valueOf(...). {@code data} is optional —
 * an element can be created with an empty payload and filled in later.
 */
public record CreateElementRequest(
        @NotBlank @Pattern(regexp = "TEXT|IMAGE|NOTE|STICKER|SHAPE|TABLE|CHART|GRID") String type,
        @NotNull Double x,
        @NotNull Double y,
        @NotNull Double width,
        @NotNull Double height,
        Map<String, Object> data
) {
}
