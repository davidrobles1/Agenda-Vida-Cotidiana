package com.vidacotidiana.daynote.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DayNoteElementRepository extends JpaRepository<DayNoteElement, UUID> {

    /**
     * Explicit JPQL, not a derived query name — same parser gap already
     * documented in visionboard.domain.VisionBoardElementRepository's own
     * findByBoardIdOrderByZIndexAsc (Spring Data's derived-query parser
     * cannot resolve "zIndex" from a single-letter-then-capital property
     * name in a method name; confirmed again here the same way, at context
     * startup). createdAt/id break ties deterministically, same reason as
     * Vision Board's own query.
     */
    @Query("SELECT e FROM DayNoteElement e WHERE e.ownerUserId = :ownerUserId AND e.noteDate = :noteDate "
            + "ORDER BY e.zIndex ASC, e.createdAt ASC, e.id ASC")
    List<DayNoteElement> findByOwnerUserIdAndNoteDateOrderByZIndexAsc(@Param("ownerUserId") UUID ownerUserId, @Param("noteDate") LocalDate noteDate);
}
