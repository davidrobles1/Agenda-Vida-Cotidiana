package com.vidacotidiana.sharing.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReminderShareRepository extends JpaRepository<ReminderShare, UUID> {

    Page<ReminderShare> findByReminderIdAndStatus(UUID reminderId, ReminderShareStatus status, Pageable pageable);

    // BE-022: does the caller hold ACTIVE collaborator access to this reminder?
    boolean existsByReminderIdAndCollaboratorUserIdAndStatus(UUID reminderId, UUID collaboratorUserId, ReminderShareStatus status);
}
