package com.vidacotidiana.reminder.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta y renombrado comparten cuerpo: en los dos casos es solo el texto. */
public record StepTitleRequest(
        @NotBlank @Size(min = 1, max = 200) String title
) {
}
