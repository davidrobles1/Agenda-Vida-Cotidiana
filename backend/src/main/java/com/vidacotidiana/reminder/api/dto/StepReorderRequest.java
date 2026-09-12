package com.vidacotidiana.reminder.api.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * La lista COMPLETA de ids en su orden nuevo.
 *
 * No un «mueve este de aquí a allá»: con la lista entera el resultado no
 * depende del orden en que lleguen dos peticiones, y no quedan huecos ni
 * posiciones repetidas si se cruzan.
 */
public record StepReorderRequest(
        @NotEmpty List<UUID> orderedIds
) {
}
