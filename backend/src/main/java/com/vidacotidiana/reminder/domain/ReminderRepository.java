package com.vidacotidiana.reminder.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    /**
     * FR-003/FR-004: "propios y compartidos" — reminders owned by the caller
     * plus reminders where the caller holds an ACTIVE ReminderShare (BE-022).
     * References the sharing module's ReminderShare entity by JPQL entity
     * name (not a Java import): the two modules share a schema-level
     * relationship inherent to "a share belongs to a reminder" in this
     * modular monolith (ADR-001) — there is no separate deployable boundary
     * to preserve here.
     */
    @Query("""
            SELECT r FROM Reminder r
            WHERE r.ownerUserId = :userId
               OR r.id IN (
                   SELECT rs.reminderId FROM ReminderShare rs
                   WHERE rs.collaboratorUserId = :userId AND rs.status = com.vidacotidiana.sharing.domain.ReminderShareStatus.ACTIVE
               )
            ORDER BY r.createdAt DESC
            """)
    Page<Reminder> findAccessibleTo(@Param("userId") UUID userId, Pageable pageable);

    /**
     * ADR-019: la misma consulta acotada al módulo activo.
     *
     * Hasta ahora el aislamiento de recordatorios era SOLO visual — el
     * servidor devolvía todo y `CalendarPage` filtraba en el cliente, que es
     * justo lo que la regla 5 del pedido prohíbe. Ahora el contexto entra en
     * la consulta.
     *
     * Los recordatorios COMPARTIDOS con el usuario también se filtran por
     * contexto: el contexto es del recurso, no de quien lo mira, así que una
     * tarea laboral que alguien te comparte sigue siendo laboral y no debe
     * aparecer en Personal.
     */
    @Query("""
            SELECT r FROM Reminder r
            WHERE r.context = :context
              AND (
                   r.ownerUserId = :userId
                OR r.id IN (
                       SELECT rs.reminderId FROM ReminderShare rs
                       WHERE rs.collaboratorUserId = :userId AND rs.status = com.vidacotidiana.sharing.domain.ReminderShareStatus.ACTIVE
                   )
              )
            ORDER BY r.createdAt DESC
            """)
    Page<Reminder> findAccessibleToByContext(@Param("userId") UUID userId,
                                             @Param("context") ReminderContext context,
                                             Pageable pageable);
}
