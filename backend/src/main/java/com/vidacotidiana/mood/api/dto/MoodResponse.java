package com.vidacotidiana.mood.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vidacotidiana.mood.domain.MoodEntry;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * NO EXPONE `ownerUserId`, al contrario que el resto de respuestas del sistema.
 *
 * No es un descuido de simetría: es dato de salud y el único que puede leerlo
 * es su dueño, así que el identificador no aporta nada al cliente y sí amplía
 * lo que viaja por la red y acaba en un registro. Lo que no se envía no se
 * filtra.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MoodResponse(
        UUID id,
        LocalDate date,
        int value,
        String note,
        List<String> tags,
        int version,
        Instant createdAt,
        Instant updatedAt
) {
    public static MoodResponse from(MoodEntry entry) {
        return new MoodResponse(
                entry.getId(),
                entry.getEntryDate(),
                entry.getValue(),
                entry.getNote(),
                List.copyOf(entry.getTags()),
                entry.getVersion(),
                entry.getCreatedAt(),
                entry.getUpdatedAt()
        );
    }
}
