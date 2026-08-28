package com.vidacotidiana.note.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.note.api.dto.CreateNoteRequest;
import com.vidacotidiana.note.api.dto.NoteResponse;
import com.vidacotidiana.note.api.dto.ResolveTaskSuggestionRequest;
import com.vidacotidiana.note.api.dto.UpdateNoteRequest;
import com.vidacotidiana.note.application.NoteService;
import com.vidacotidiana.note.domain.Note;
import com.vidacotidiana.shared.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Full Note CRUD, mirroring warranty.api.WarrantyController's shape
 * exactly (create, list own items paginated, get by id, edit, delete) but
 * without a `/complete` action — a Note has no completion/status concept,
 * unlike Warranty/Reminder.
 */
@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

    private final NoteService noteService;
    private final CurrentUser currentUser;

    public NoteController(NoteService noteService, CurrentUser currentUser) {
        this.noteService = noteService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<NoteResponse> create(@Valid @RequestBody CreateNoteRequest request) {
        Note created = noteService.create(
                currentUser.userId(), request.title(), request.description(), request.iconId(), request.stickerId(),
                request.personId(), request.projectId());
        return ResponseEntity.status(HttpStatus.CREATED).body(NoteResponse.from(created));
    }

    @GetMapping
    public PageResponse<NoteResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Note> notes = noteService.listOwnedBy(currentUser.userId(), pageable);
        return PageResponse.from(notes.map(NoteResponse::from));
    }

    @GetMapping("/{id}")
    public NoteResponse get(@PathVariable UUID id) {
        Note note = noteService.getOwnedOrThrow(id, currentUser.userId());
        return NoteResponse.from(note);
    }

    @PatchMapping("/{id}")
    public NoteResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateNoteRequest request) {
        Note note = noteService.edit(
                id, currentUser.userId(), request.title(), request.description(),
                request.iconId(), request.stickerId(), request.personId(), request.projectId(), request.version());
        return NoteResponse.from(note);
    }

    /**
     * ADR-016 Fase 3d/FR-035, UC-28: marca la sugerencia de tarea de esta
     * nota como resuelta (convertida o descartada). Nada aquí crea la Tarea
     * — si el usuario convierte, el cliente ya llamó a `POST /reminders`
     * antes; este endpoint solo registra que la nota deja de ofrecer la
     * sugerencia.
     */
    @PostMapping("/{id}/resolve-task-suggestion")
    public NoteResponse resolveTaskSuggestion(@PathVariable UUID id,
                                              @RequestBody(required = false) ResolveTaskSuggestionRequest request) {
        Integer expectedVersion = (request != null) ? request.version() : null;
        Note note = noteService.resolveTaskSuggestion(id, currentUser.userId(), expectedVersion);
        return NoteResponse.from(note);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        noteService.delete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
