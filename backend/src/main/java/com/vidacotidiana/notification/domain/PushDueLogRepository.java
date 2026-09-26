package com.vidacotidiana.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface PushDueLogRepository extends JpaRepository<PushDueLog, PushDueLog.Key> {

    boolean existsByUserIdAndResourceTypeAndResourceIdAndNotifiedOn(
            UUID userId, String resourceType, UUID resourceId, LocalDate notifiedOn);

    /** Limpieza: lo de hace más de un mes ya no evita ningún duplicado. */
    @Modifying
    @Query("DELETE FROM PushDueLog l WHERE l.notifiedOn < :before")
    int deleteOlderThan(@Param("before") LocalDate before);
}
