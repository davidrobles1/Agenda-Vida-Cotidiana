package com.vidacotidiana.routine.application;

import com.vidacotidiana.routine.domain.Routine;
import com.vidacotidiana.routine.domain.RoutineProgress;
import com.vidacotidiana.routine.domain.RoutineProgressRepository;
import com.vidacotidiana.shared.domain.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * El contador diario de un hábito (V35).
 *
 * Es lo que llena los anillos de Rutinas del artefacto: «4 de 8» vasos, «15 de
 * 20» minutos. Hasta V35 una rutina solo sabía decir «toca hoy» o «no toca».
 *
 * Toda operación comprueba el dueño a través de `routineService`: el progreso
 * no guarda `ownerUserId`, lo hereda de su rutina, igual que un paso lo hereda
 * de su tarea.
 */
@Service
public class RoutineProgressService {

    /** Ventana máxima de consulta: un año, como en `mood`. */
    private static final int MAX_RANGE_DAYS = 366;

    private final RoutineProgressRepository progressRepository;
    private final RoutineService routineService;

    public RoutineProgressService(RoutineProgressRepository progressRepository, RoutineService routineService) {
        this.progressRepository = progressRepository;
        this.routineService = routineService;
    }

    /**
     * Sumar al día. Es lo que hace tocar el anillo en el artefacto.
     *
     * UPSERT sobre (rutina, día): la primera vez crea la fila, las siguientes
     * suman sobre ella. La restricción única de la tabla impide que dos toques
     * simultáneos creen dos filas del mismo día.
     *
     * Al llegar a la meta NO se reinicia solo: quien quiera volver a cero lo
     * dice explícitamente con `set(0)`. Un contador que se vacía al completarse
     * borra la prueba de que el hábito se cumplió.
     */
    @Transactional
    public RoutineProgress add(UUID routineId, UUID callerUserId, LocalDate date, int delta) {
        Routine routine = routineService.getOwnedOrThrow(routineId, callerUserId);
        if (!routine.isCounted()) {
            throw new ValidationException(
                    "Esa rutina no lleva contador. Ponle una meta diaria antes de sumarle progreso.");
        }
        LocalDate day = date == null ? LocalDate.now() : date;
        if (day.isAfter(LocalDate.now())) {
            throw new ValidationException("No se puede registrar progreso de un día que no ha llegado.");
        }
        return progressRepository.findByRoutineIdAndProgressDate(routineId, day)
                .map(existing -> {
                    existing.add(delta);
                    return existing;
                })
                .orElseGet(() -> progressRepository.save(
                        new RoutineProgress(routineId, day, Math.max(0, delta))));
    }

    /** Fijar el valor exacto de un día, para corregir sin ir de uno en uno. */
    @Transactional
    public RoutineProgress set(UUID routineId, UUID callerUserId, LocalDate date, int value) {
        Routine routine = routineService.getOwnedOrThrow(routineId, callerUserId);
        if (!routine.isCounted()) {
            throw new ValidationException(
                    "Esa rutina no lleva contador. Ponle una meta diaria antes de fijarle progreso.");
        }
        LocalDate day = date == null ? LocalDate.now() : date;
        return progressRepository.findByRoutineIdAndProgressDate(routineId, day)
                .map(existing -> {
                    existing.set(value);
                    return existing;
                })
                .orElseGet(() -> progressRepository.save(new RoutineProgress(routineId, day, value)));
    }

    @Transactional(readOnly = true)
    public List<RoutineProgress> list(UUID routineId, UUID callerUserId, LocalDate from, LocalDate to) {
        routineService.getOwnedOrThrow(routineId, callerUserId);
        LocalDate end = to == null ? LocalDate.now() : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (start.isAfter(end)) {
            throw new ValidationException("La fecha inicial es posterior a la final.");
        }
        if (start.plusDays(MAX_RANGE_DAYS).isBefore(end)) {
            throw new ValidationException("El rango no puede superar " + MAX_RANGE_DAYS + " días.");
        }
        return progressRepository.findByRoutineIdAndProgressDateBetweenOrderByProgressDateDesc(routineId, start, end);
    }

    /** Declarar la meta diaria de una rutina — o retirarla con `null`. */
    @Transactional
    public Routine setTarget(UUID routineId, UUID callerUserId, Integer targetCount, String unit) {
        Routine routine = routineService.getOwnedOrThrow(routineId, callerUserId);
        routine.setTarget(targetCount, unit);
        return routine;
    }
}
