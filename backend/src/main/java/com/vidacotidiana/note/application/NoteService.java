package com.vidacotidiana.note.application;

import com.vidacotidiana.note.domain.Note;
import com.vidacotidiana.note.domain.NoteRepository;
import com.vidacotidiana.person.application.PersonService;
import com.vidacotidiana.project.application.ProjectService;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for the Note vertical slice. Mirrors
 * warranty.application.WarrantyService's authorization/locking pattern
 * exactly — owner-only throughout, same non-enumeration rule as
 * Reminder/Warranty (AC-004/SEC-001, 11-auth-security.md): a caller with
 * no access gets exactly the same 404 as a truly missing note, never 403.
 *
 * ADR-016 Fase 3a/FR-029 (candidato V4): depends on PersonService/
 * ProjectService only to reuse their ownership check for the optional
 * personId/projectId link — same reuse pattern as ReminderService.
 */
@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final PersonService personService;
    private final ProjectService projectService;

    public NoteService(NoteRepository noteRepository, PersonService personService, ProjectService projectService) {
        this.noteRepository = noteRepository;
        this.personService = personService;
        this.projectService = projectService;
    }

    @Transactional
    public Note create(UUID ownerUserId, String title, String description, String iconId, String stickerId) {
        return create(ownerUserId, title, description, iconId, stickerId, null, null);
    }

    /** ADR-016 Fase 3a/FR-029: personId/projectId, when present, validated the same way as ReminderService.create. */
    @Transactional
    public Note create(UUID ownerUserId, String title, String description, String iconId, String stickerId,
                        UUID personId, UUID projectId) {
        if (personId != null) {
            personService.getOwnedOrThrow(personId, ownerUserId);
        }
        if (projectId != null) {
            projectService.getOwnedOrThrow(projectId, ownerUserId);
        }
        Note note = new Note(ownerUserId, title, description, iconId, stickerId, personId, projectId);
        return noteRepository.save(note);
    }

    @Transactional(readOnly = true)
    public Page<Note> listOwnedBy(UUID ownerUserId, Pageable pageable) {
        return noteRepository.findByOwnerUserId(ownerUserId, pageable);
    }

    @Transactional(readOnly = true)
    public Note getOwnedOrThrow(UUID noteId, UUID callerUserId) {
        Note note = findOrThrow(noteId);
        requireOwner(note, callerUserId);
        return note;
    }

    /** Owner-only. version is mandatory — a mismatch always rejects the edit with 409. */
    @Transactional
    public Note edit(UUID noteId, UUID callerUserId, String title, String description, String iconId,
                      String stickerId, int expectedVersion) {
        return edit(noteId, callerUserId, title, description, iconId, stickerId, null, null, expectedVersion);
    }

    /** ADR-016 Fase 3a/FR-029: personId/projectId, when sent, validated the same way as create(). */
    @Transactional
    public Note edit(UUID noteId, UUID callerUserId, String title, String description, String iconId,
                      String stickerId, UUID personId, UUID projectId, int expectedVersion) {
        Note note = getOwnedOrThrow(noteId, callerUserId);

        if (personId != null) {
            personService.getOwnedOrThrow(personId, callerUserId);
        }
        if (projectId != null) {
            projectService.getOwnedOrThrow(projectId, callerUserId);
        }

        if (expectedVersion != note.getVersion()) {
            throw new ConflictException("NOTE_VERSION_CONFLICT",
                    "Note " + noteId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + note.getVersion() + ").");
        }

        note.applyEdit(title, description, iconId, stickerId, personId, projectId);
        try {
            return noteRepository.save(note);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("NOTE_VERSION_CONFLICT",
                    "Note " + noteId + " was modified concurrently; refetch and retry.");
        }
    }

    @Transactional
    public void delete(UUID noteId, UUID callerUserId) {
        Note note = getOwnedOrThrow(noteId, callerUserId);
        noteRepository.delete(note);
    }

    private Note findOrThrow(UUID noteId) {
        return noteRepository.findById(noteId)
                .orElseThrow(() -> new NotFoundException("NOTE_NOT_FOUND", "The requested note was not found."));
    }

    private void requireOwner(Note note, UUID callerUserId) {
        if (!note.isOwnedBy(callerUserId)) {
            throw new NotFoundException("NOTE_NOT_FOUND", "The requested note was not found.");
        }
    }
}
