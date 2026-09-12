package com.vidacotidiana.document.domain;

import com.vidacotidiana.shared.domain.ModuleContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /**
     * ADR-022: contexto + categoría + búsqueda en una sola consulta, todos
     * opcionales, conservando la regla de visibilidad de `findVisibleTo`
     * (propios + compartidos conmigo + públicos de la familia).
     *
     * POR QUÉ EN LA CONSULTA: la pantalla filtraba por categoría en memoria
     * sobre la página ya cargada (20 documentos), aunque el endpoint ya
     * aceptaba `category`. Con más documentos que el tamaño de página, el
     * filtro ocultaba resultados sin avisar.
     *
     * El filtro de contexto se aplica sobre `d.context`, que es el módulo
     * del DUEÑO: un documento compartido conmigo sigue perteneciendo al
     * módulo desde el que se creó (ADR-019), no al mío.
     */
    @Query("""
            SELECT d FROM Document d
            WHERE (d.ownerUserId = :userId
                   OR (d.visibility = 'SHARED' AND d.sharedWithUserId = :userId)
                   OR d.visibility = 'FAMILY_PUBLIC')
              AND (:context IS NULL OR d.context = :context)
              AND (:category IS NULL OR d.category = :category)
              AND (:query IS NULL
                   OR LOWER(d.name) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')))
            """)
    Page<Document> search(
            @Param("userId") UUID userId,
            @Param("context") ModuleContext context,
            @Param("category") DocumentCategory category,
            @Param("query") String query,
            Pageable pageable);

    /**
     * Los adjuntos de UN recurso (V37).
     *
     * Lleva `ownerUserId` en la firma y no solo el recurso: sin él, conocer un
     * id bastaría para leer los adjuntos de otra persona. La pertenencia es
     * parte de la consulta, no una comprobación que alguien deba recordar.
     */
    List<Document> findByOwnerUserIdAndResourceTypeAndResourceIdOrderByCreatedAtDesc(
            UUID ownerUserId, String resourceType, UUID resourceId);
}
