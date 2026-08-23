package com.vidacotidiana.warranty.application;

import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.ValidationException;
import com.vidacotidiana.warranty.domain.Warranty;
import com.vidacotidiana.warranty.domain.WarrantyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Application service for the Warranty vertical slice (BE-037). Mirrors
 * reminder.application.ReminderService's authorization/locking pattern, but
 * owner-only throughout (no collaborator concept — see WarrantyRepository).
 * Same non-enumeration rule as Reminder (AC-004/SEC-001, 11-auth-security.md):
 * a caller with no access gets exactly the same 404 as a truly missing
 * warranty, never 403.
 */
@Service
public class WarrantyService {

    /** Mismo set que document.application.DocumentService — imagen o PDF,
        pedido explícito del usuario ("subir el archivo... en formato
        imagen o pdf"). */
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/png", "image/jpeg", "image/webp", "image/gif", "application/pdf");

    private static final long MAX_SIZE_BYTES = 23L * 1024 * 1024;

    private final WarrantyRepository warrantyRepository;

    public WarrantyService(WarrantyRepository warrantyRepository) {
        this.warrantyRepository = warrantyRepository;
    }

    /** El archivo es obligatorio al registrar (pedido explícito del
        usuario: "al registrar una garantía subir el archivo") — validado
        aquí en la capa de aplicación, no como NOT NULL en la tabla (ver
        V14__warranty_documents.sql). */
    @Transactional
    public Warranty create(UUID ownerUserId, String item, Instant expiresAt, MultipartFile file) {
        // Real gap found in live testing: switching this endpoint from a
        // `@Valid @RequestBody` JSON DTO (which had `@NotBlank`) to plain
        // multipart `@RequestParam`s dropped that validation entirely —
        // `@RequestParam` alone never rejects an empty string. Restored
        // explicitly here since it's no longer enforced at the controller layer.
        if (item == null || item.isBlank()) {
            throw new ValidationException("item must not be blank.");
        }
        if (file == null || file.isEmpty()) {
            throw new ValidationException("A warranty document (image or PDF) is required.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ValidationException(
                    "Unsupported document type" + (contentType != null ? ": " + contentType : "")
                            + ". Allowed: " + String.join(", ", ALLOWED_CONTENT_TYPES) + ".");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ValidationException("Document exceeds the " + (MAX_SIZE_BYTES / (1024 * 1024)) + "MB limit.");
        }
        Warranty warranty = new Warranty(ownerUserId, item, expiresAt);
        try {
            warranty.attachDocument(contentType, file.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the uploaded warranty document.", e);
        }
        return warrantyRepository.save(warranty);
    }

    /** Mismo split metadata/bytes que document.application.DocumentService
        — WarrantyResponse nunca incluye los bytes, solo esto. */
    @Transactional(readOnly = true)
    public Warranty getWithDocumentOrThrow(UUID warrantyId, UUID callerUserId) {
        Warranty warranty = getOwnedOrThrow(warrantyId, callerUserId);
        if (!warranty.hasDocument()) {
            throw new NotFoundException("WARRANTY_DOCUMENT_NOT_FOUND", "This warranty has no attached document.");
        }
        return warranty;
    }

    @Transactional(readOnly = true)
    public Page<Warranty> listOwnedBy(UUID ownerUserId, Pageable pageable) {
        return warrantyRepository.findByOwnerUserId(ownerUserId, pageable);
    }

    @Transactional(readOnly = true)
    public Warranty getOwnedOrThrow(UUID warrantyId, UUID callerUserId) {
        Warranty warranty = findOrThrow(warrantyId);
        requireOwner(warranty, callerUserId);
        return warranty;
    }

    /**
     * Toggles ACTIVE<->COMPLETED. If expectedVersion is present, validates it
     * against the stored version first (fast, explicit 409) before applying
     * the toggle; if absent, applies the toggle without a concurrency check —
     * same contract as ReminderService#toggleCompletion.
     */
    @Transactional
    public Warranty toggleCompletion(UUID warrantyId, UUID callerUserId, Integer expectedVersion) {
        Warranty warranty = getOwnedOrThrow(warrantyId, callerUserId);

        if (expectedVersion != null && expectedVersion != warranty.getVersion()) {
            throw new ConflictException("WARRANTY_VERSION_CONFLICT",
                    "Warranty " + warrantyId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + warranty.getVersion() + ").");
        }

        warranty.toggleCompletion();
        try {
            return warrantyRepository.save(warranty);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            // Safety net for a genuine race that slipped past the pre-check above,
            // same pattern as sharing.application.SharingService's DataIntegrityViolationException catch.
            throw new ConflictException("WARRANTY_VERSION_CONFLICT",
                    "Warranty " + warrantyId + " was modified concurrently; refetch and retry.");
        }
    }

    /** Owner-only. version is mandatory — a mismatch always rejects the edit with 409. */
    @Transactional
    public Warranty edit(UUID warrantyId, UUID callerUserId, String item, Instant expiresAt, int expectedVersion) {
        Warranty warranty = getOwnedOrThrow(warrantyId, callerUserId);

        if (expectedVersion != warranty.getVersion()) {
            throw new ConflictException("WARRANTY_VERSION_CONFLICT",
                    "Warranty " + warrantyId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + warranty.getVersion() + ").");
        }

        warranty.applyEdit(item, expiresAt);
        try {
            return warrantyRepository.save(warranty);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("WARRANTY_VERSION_CONFLICT",
                    "Warranty " + warrantyId + " was modified concurrently; refetch and retry.");
        }
    }

    @Transactional
    public void delete(UUID warrantyId, UUID callerUserId) {
        Warranty warranty = getOwnedOrThrow(warrantyId, callerUserId);
        warrantyRepository.delete(warranty);
    }

    private Warranty findOrThrow(UUID warrantyId) {
        return warrantyRepository.findById(warrantyId)
                .orElseThrow(() -> new NotFoundException("WARRANTY_NOT_FOUND", "The requested warranty was not found."));
    }

    private void requireOwner(Warranty warranty, UUID callerUserId) {
        if (!warranty.isOwnedBy(callerUserId)) {
            throw new NotFoundException("WARRANTY_NOT_FOUND", "The requested warranty was not found.");
        }
    }
}
