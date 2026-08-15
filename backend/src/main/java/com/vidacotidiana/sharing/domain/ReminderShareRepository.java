package com.vidacotidiana.sharing.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReminderShareRepository extends JpaRepository<ReminderShare, UUID> {

    Page<ReminderShare> findByReminderIdAndStatus(UUID reminderId, ReminderShareStatus status, Pageable pageable);

    // BE-026: reminder.application.ReminderService.delete notifies every ACTIVE collaborator (AC-013) —
    // unpaginated, since a single reminder's collaborator list is small (V1 scope).
    List<ReminderShare> findByReminderIdAndStatus(UUID reminderId, ReminderShareStatus status);

    // BE-022: does the caller hold ACTIVE collaborator access to this reminder?
    boolean existsByReminderIdAndCollaboratorUserIdAndStatus(UUID reminderId, UUID collaboratorUserId, ReminderShareStatus status);
}
