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
            """)
    Page<Reminder> findAccessibleTo(@Param("userId") UUID userId, Pageable pageable);
}
