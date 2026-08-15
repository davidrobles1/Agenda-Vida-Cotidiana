package com.vidacotidiana.reminder.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    // Sharing (REMINDER_SHARE) is not implemented yet in this vertical slice
    // (see docs/development/01-technical-backlog.md, BE-016..022) — until
    // then, "accessible reminders" is exactly "owned reminders".
    Page<Reminder> findByOwnerUserId(UUID ownerUserId, Pageable pageable);
}
