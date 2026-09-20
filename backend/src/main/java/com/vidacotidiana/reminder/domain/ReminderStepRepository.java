package com.vidacotidiana.reminder.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReminderStepRepository extends JpaRepository<ReminderStep, UUID> {

    List<ReminderStep> findByReminderIdOrderByPositionAsc(UUID reminderId);

    /**
     * El id del paso NO basta para autorizar: hay que saber de qué tarea es
     * antes de comprobar quién es su dueño. Pedir los dos en la firma impide
     * que una llamada se salte esa comprobación por descuido.
     */
    Optional<ReminderStep> findByIdAndReminderId(UUID id, UUID reminderId);

    long countByReminderId(UUID reminderId);

    long countByReminderIdAndDoneIsTrue(UUID reminderId);

    /**
     * EL AVANCE DE VARIAS TAREAS DE UNA VEZ.
     *
     * Existe para que la LISTA pueda enseñar el anillo de avance de cada tarea
     * —el artefacto aprobado lo pone en cada fila— sin preguntar una vez por
     * tarea. `countByReminderId` sirve para una; para veinte serían cuarenta
     * consultas, que es exactamente el motivo por el que el anillo no existía
     * en la lista. Esto es UNA agregación para toda la página.
     *
     * Devuelve solo las tareas que TIENEN pasos: las que no aparecen es que no
     * tienen ninguno, y eso no es lo mismo que tener cero hechos de cero.
     */
    @Query("""
            select s.reminderId as reminderId,
                   count(s) as total,
                   sum(case when s.done = true then 1 else 0 end) as done
            from ReminderStep s
            where s.reminderId in :reminderIds
            group by s.reminderId
            """)
    List<ReminderStepProgress> progressOf(@Param("reminderIds") Collection<UUID> reminderIds);

    /** La proyección de la agregación de arriba. */
    interface ReminderStepProgress {
        UUID getReminderId();
        long getTotal();
        long getDone();
    }
}
