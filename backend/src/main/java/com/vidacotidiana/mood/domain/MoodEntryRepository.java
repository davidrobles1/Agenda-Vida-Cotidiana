package com.vidacotidiana.mood.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * TODAS las consultas llevan `ownerUserId`. No hay ni un método que permita
 * leer el ánimo de otra persona, ni siquiera por id: siendo dato de salud, la
 * pertenencia no es un filtro que el servicio deba recordar aplicar, es parte
 * de la firma.
 */
public interface MoodEntryRepository extends JpaRepository<MoodEntry, UUID> {

    Optional<MoodEntry> findByOwnerUserIdAndEntryDate(UUID ownerUserId, LocalDate entryDate);

    List<MoodEntry> findByOwnerUserIdAndEntryDateBetweenOrderByEntryDateDesc(
            UUID ownerUserId, LocalDate from, LocalDate to);

    /**
     * Borrado DURO de todo el historial (Ajustes → Privacidad).
     *
     * Las etiquetas caen solas por el ON DELETE CASCADE de `mood_entry_tag`.
     */
    @Modifying
    @Query("DELETE FROM MoodEntry m WHERE m.ownerUserId = :ownerUserId")
    int deleteAllByOwnerUserId(@Param("ownerUserId") UUID ownerUserId);

    long countByOwnerUserId(UUID ownerUserId);
}
