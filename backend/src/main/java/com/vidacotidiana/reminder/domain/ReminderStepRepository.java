package com.vidacotidiana.reminder.domain;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
