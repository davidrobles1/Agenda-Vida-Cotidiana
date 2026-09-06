package com.vidacotidiana.inventory.domain;

import com.vidacotidiana.shared.domain.ModuleContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/** Owner-only, mismo motivo que warranty.domain.WarrantyRepository — sin
    concepto de compartir para este módulo. */
public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    Page<InventoryItem> findByOwnerUserId(UUID ownerUserId, Pageable pageable);

    Page<InventoryItem> findByOwnerUserIdAndCategory(UUID ownerUserId, InventoryCategory category, Pageable pageable);

    /**
     * ADR-022: una sola consulta resuelve contexto + categoría + búsqueda,
     * todos opcionales.
     *
     * POR QUÉ EN LA CONSULTA Y NO EN EL CLIENTE: la pantalla filtraba en
     * memoria sobre la página ya cargada (20 artículos por defecto), así que
     * elegir "Vehículos" solo miraba esos 20 y ocultaba el resto **sin
     * decirlo**. Con más artículos que el tamaño de página, el filtro mentía.
     * Lo mismo vale para la búsqueda: buscar solo dentro de lo ya traído no
     * es buscar.
     *
     * El aislamiento por módulo baja aquí por el mismo motivo que en
     * WarrantyRepository (ADR-019): un recurso del otro módulo no debe salir
     * siquiera de la base de datos.
     */
    @Query("""
            SELECT i FROM InventoryItem i
            WHERE i.ownerUserId = :ownerUserId
              AND (:context IS NULL OR i.context = :context)
              AND (:category IS NULL OR i.category = :category)
              AND (:query IS NULL
                   OR LOWER(i.name) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%'))
                   OR LOWER(COALESCE(i.location, '')) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')))
            """)
    Page<InventoryItem> search(
            @Param("ownerUserId") UUID ownerUserId,
            @Param("context") ModuleContext context,
            @Param("category") InventoryCategory category,
            @Param("query") String query,
            Pageable pageable);
}
