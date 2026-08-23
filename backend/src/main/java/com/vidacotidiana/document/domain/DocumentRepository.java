package com.vidacotidiana.document.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /** Propios + compartidos directamente conmigo + públicos de cualquier otro
        dueño (FAMILY_PUBLIC) — la misma regla que Document#isVisibleTo, como
        query para poder paginar server-side en vez de traer todo y filtrar
        en memoria. */
    @Query("""
            SELECT d FROM Document d
            WHERE d.ownerUserId = :userId
               OR (d.visibility = 'SHARED' AND d.sharedWithUserId = :userId)
               OR d.visibility = 'FAMILY_PUBLIC'
            """)
    Page<Document> findVisibleTo(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            SELECT d FROM Document d
            WHERE d.category = :category
              AND (d.ownerUserId = :userId
                   OR (d.visibility = 'SHARED' AND d.sharedWithUserId = :userId)
                   OR d.visibility = 'FAMILY_PUBLIC')
            """)
    Page<Document> findVisibleToByCategory(@Param("userId") UUID userId, @Param("category") DocumentCategory category, Pageable pageable);
}
