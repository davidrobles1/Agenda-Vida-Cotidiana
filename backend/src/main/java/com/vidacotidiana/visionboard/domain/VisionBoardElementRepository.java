package com.vidacotidiana.visionboard.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VisionBoardElementRepository extends JpaRepository<VisionBoardElement, UUID> {

    /**
     * FASE 8 (layers): callers always want elements in paint order.
     * Explicit JPQL, not a derived query name (findByBoardIdOrderByZIndexAsc)
     * — verified for real at context startup that Spring Data's derived-query
     * parser cannot resolve the "zIndex" property from that method name
     * (single-letter-then-capital property names are a known parser gap),
     * throwing PathElementException before the app ever boots. Same
     * @Query escape hatch ReminderRepository already uses for its own
     * non-trivial query.
     *
     * FASE 9 fix (real gap found while implementing "subir/bajar una capa"
     * correctly): every element created by FASE 6's toolbar starts at
     * zIndex 0, so "ORDER BY zIndex" alone leaves ties in whatever order
     * the database happens to return them — not a real, stable paint
     * order, and not enough to know who's "immediately above/below" a
     * given element for reordering. createdAt (then id, for the
     * same-millisecond case) makes the order deterministic.
     */
    @Query("SELECT e FROM VisionBoardElement e WHERE e.boardId = :boardId ORDER BY e.zIndex ASC, e.createdAt ASC, e.id ASC")
    List<VisionBoardElement> findByBoardIdOrderByZIndexAsc(@Param("boardId") UUID boardId);
}
