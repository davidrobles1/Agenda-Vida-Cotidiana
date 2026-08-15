package com.vidacotidiana.reminder.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.reminder.api.dto.CompleteReminderRequest;
import com.vidacotidiana.reminder.api.dto.CreateReminderRequest;
import com.vidacotidiana.reminder.api.dto.ReminderResponse;
import com.vidacotidiana.reminder.application.ReminderService;
import com.vidacotidiana.reminder.domain.Reminder;
import com.vidacotidiana.shared.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Covers exactly the reminder endpoints in scope for the first vertical
 * slice (Documentacion/openapi/openapi.yaml): create, list (own reminders,
 * paginated), get by id, and toggle completion. PATCH (edit) and DELETE are
 * deliberately out of scope for this slice — see
 * docs/development/01-technical-backlog.md BE-014/BE-015.
 */
@RestController
@RequestMapping("/api/v1/reminders")
public class ReminderController {

    private final ReminderService reminderService;
    private final CurrentUser currentUser;

    public ReminderController(ReminderService reminderService, CurrentUser currentUser) {
        this.reminderService = reminderService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<ReminderResponse> create(@Valid @RequestBody CreateReminderRequest request) {
        Reminder created = reminderService.create(
                currentUser.userId(), request.title(), request.description(), request.dueAt());
        return ResponseEntity.status(HttpStatus.CREATED).body(ReminderResponse.from(created));
    }

    @GetMapping
    public PageResponse<ReminderResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Reminder> reminders = reminderService.listOwnedBy(currentUser.userId(), pageable);
        return PageResponse.from(reminders.map(ReminderResponse::from));
    }

    @GetMapping("/{id}")
    public ReminderResponse get(@PathVariable UUID id) {
        Reminder reminder = reminderService.getAccessible(id, currentUser.userId());
        return ReminderResponse.from(reminder);
    }

    @PostMapping("/{id}/complete")
    public ReminderResponse complete(@PathVariable UUID id,
                                      @RequestBody(required = false) CompleteReminderRequest request) {
        Integer expectedVersion = (request != null) ? request.version() : null;
        Reminder reminder = reminderService.toggleCompletion(id, currentUser.userId(), expectedVersion);
        return ReminderResponse.from(reminder);
    }
}
