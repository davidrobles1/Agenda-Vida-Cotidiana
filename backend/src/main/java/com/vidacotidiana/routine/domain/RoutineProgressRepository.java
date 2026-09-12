package com.vidacotidiana.routine.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoutineProgressRepository extends JpaRepository<RoutineProgress, UUID> {

    Optional<RoutineProgress> findByRoutineIdAndProgressDate(UUID routineId, LocalDate progressDate);

    List<RoutineProgress> findByRoutineIdAndProgressDateBetweenOrderByProgressDateDesc(
            UUID routineId, LocalDate from, LocalDate to);

    /** Para el «2 de 4 hoy» de la cabecera de Rutinas, en una sola consulta. */
    List<RoutineProgress> findByRoutineIdInAndProgressDate(List<UUID> routineIds, LocalDate progressDate);
}
