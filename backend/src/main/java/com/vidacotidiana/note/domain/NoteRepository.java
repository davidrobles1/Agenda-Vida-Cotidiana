package com.vidacotidiana.note.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Owner-only entity — mirrors warranty.domain.WarrantyRepository. No
 * sharing/collaborator concept for Note (no corresponding UI to share a
 * note), so a plain owner-scoped query is enough.
 */
public interface NoteRepository extends JpaRepository<Note, UUID> {

    Page<Note> findByOwnerUserId(UUID ownerUserId, Pageable pageable);
}
