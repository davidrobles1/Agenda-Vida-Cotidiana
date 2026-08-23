package com.vidacotidiana.user.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** POST /me/modes — Documentacion/openapi/openapi.yaml. FR-016/UC-15/ADR-015. */
public record EnableModeRequest(
        @NotNull @Pattern(regexp = "PERSONAL|LABORAL") String mode
) {
}
