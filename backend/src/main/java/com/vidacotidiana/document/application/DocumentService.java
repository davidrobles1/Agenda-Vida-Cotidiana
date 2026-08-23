package com.vidacotidiana.document.application;

import com.vidacotidiana.document.domain.Document;
import com.vidacotidiana.document.domain.DocumentCategory;
import com.vidacotidiana.document.domain.DocumentRepository;
import com.vidacotidiana.person.application.PersonService;
import com.vidacotidiana.project.application.ProjectService;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.ValidationException;
import com.vidacotidiana.user.domain.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.UUID;

/**
 * Módulo Documentos (pedido explícito del usuario, 2026-08-22). MIME/tamaño
 * validados aquí, mismo patrón que VisionBoardImageService — PDF se agrega
 * al set de tipos permitidos (identificación/comprobantes/seguros/contratos
 * son, en la práctica, casi siempre PDF, no solo imagen).
 */
@Service
public class DocumentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/png", "image/jpeg", "image/webp", "image/gif", "application/pdf");

    /** Mismo límite que Vision Board (23MB, ver VisionBoardImageService —
        aumentado +15MB sobre el original 8MB, pedido explícito del usuario). */
    private static final long MAX_SIZE_BYTES = 23L * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final PersonService personService;
    private final ProjectService projectService;

    public DocumentService(DocumentRepository documentRepository, UserRepository userRepository,
                            PersonService personService, ProjectService projectService) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.personService = personService;
        this.projectService = projectService;
    }

    @Transactional
    public Document upload(UUID ownerUserId, String name, DocumentCategory category, MultipartFile file) {
        return upload(ownerUserId, name, category, file, null, null);
    }

    /** ADR-016 Fase 3b/FR-030: personId/projectId, when present, validated the same way as note.application.NoteService.create. */
    @Transactional
    public Document upload(UUID ownerUserId, String name, DocumentCategory category, MultipartFile file,
                            UUID personId, UUID projectId) {
        // Same real gap found and fixed in warranty.application.WarrantyService#create
        // — plain multipart @RequestParams never reject a blank string on
        // their own, unlike a @Valid @RequestBody DTO would.
        if (name == null || name.isBlank()) {
            throw new ValidationException("name must not be blank.");
        }
        if (file.isEmpty()) {
            throw new ValidationException("The uploaded file is empty.");
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
        if (personId != null) {
            personService.getOwnedOrThrow(personId, ownerUserId);
        }
        if (projectId != null) {
            projectService.getOwnedOrThrow(projectId, ownerUserId);
        }
        try {
            Document document = new Document(ownerUserId, name, category, contentType, file.getBytes(), personId, projectId);
            return documentRepository.save(document);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the uploaded document.", e);
        }
    }

    @Transactional(readOnly = true)
    public Page<Document> listVisibleTo(UUID userId, DocumentCategory category, Pageable pageable) {
        if (category != null) {
            return documentRepository.findVisibleToByCategory(userId, category, pageable);
        }
        return documentRepository.findVisibleTo(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Document getVisibleOrThrow(UUID documentId, UUID callerUserId) {
        Document document = findOrThrow(documentId);
        if (!document.isVisibleTo(callerUserId)) {
            throw new NotFoundException("DOCUMENT_NOT_FOUND", "The requested document was not found.");
        }
        return document;
    }

    @Transactional
    public Document edit(UUID documentId, UUID callerUserId, String name, DocumentCategory category, int expectedVersion) {
        return edit(documentId, callerUserId, name, category, null, null, expectedVersion);
    }

    /** ADR-016 Fase 3b/FR-030: personId/projectId, when sent, validated the same way as upload(). */
    @Transactional
    public Document edit(UUID documentId, UUID callerUserId, String name, DocumentCategory category,
                          UUID personId, UUID projectId, int expectedVersion) {
        Document document = getOwnedOrThrow(documentId, callerUserId);
        if (personId != null) {
            personService.getOwnedOrThrow(personId, callerUserId);
        }
        if (projectId != null) {
            projectService.getOwnedOrThrow(projectId, callerUserId);
        }
        checkVersion(document, expectedVersion);
        document.applyEdit(name, category, personId, projectId);
        return saveOrConflict(document);
    }

    /** Comparte con un correo específico — siempre "tiene éxito" desde la
        perspectiva de la respuesta, coincida o no con una cuenta real (ver
        Document#shareWith y V12__documents.sql, SEC-001 no-enumeration). Si
        coincide, el destinatario lo ve de inmediato en su propio listado
        (findVisibleTo) — sin invitación pendiente ni correo, pedido
        explícito del usuario. */
    @Transactional
    public Document shareWithEmail(UUID documentId, UUID callerUserId, String email, int expectedVersion) {
        Document document = getOwnedOrThrow(documentId, callerUserId);
        checkVersion(document, expectedVersion);
        UUID resolvedUserId = userRepository.findByEmail(email).map(u -> u.getId()).orElse(null);
        document.shareWith(email, resolvedUserId);
        return saveOrConflict(document);
    }

    @Transactional
    public Document makePublic(UUID documentId, UUID callerUserId, int expectedVersion) {
        Document document = getOwnedOrThrow(documentId, callerUserId);
        checkVersion(document, expectedVersion);
        document.makePublic();
        return saveOrConflict(document);
    }

    @Transactional
    public Document makePrivate(UUID documentId, UUID callerUserId, int expectedVersion) {
        Document document = getOwnedOrThrow(documentId, callerUserId);
        checkVersion(document, expectedVersion);
        document.makePrivate();
        return saveOrConflict(document);
    }

    @Transactional
    public void delete(UUID documentId, UUID callerUserId) {
        Document document = getOwnedOrThrow(documentId, callerUserId);
        documentRepository.delete(document);
    }

    private Document getOwnedOrThrow(UUID documentId, UUID callerUserId) {
        Document document = findOrThrow(documentId);
        if (!document.isOwnedBy(callerUserId)) {
            throw new NotFoundException("DOCUMENT_NOT_FOUND", "The requested document was not found.");
        }
        return document;
    }

    private Document findOrThrow(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("DOCUMENT_NOT_FOUND", "The requested document was not found."));
    }

    private void checkVersion(Document document, int expectedVersion) {
        if (expectedVersion != document.getVersion()) {
            throw new ConflictException("DOCUMENT_VERSION_CONFLICT",
                    "Document " + document.getId() + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + document.getVersion() + ").");
        }
    }

    private Document saveOrConflict(Document document) {
        try {
            return documentRepository.save(document);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("DOCUMENT_VERSION_CONFLICT",
                    "Document " + document.getId() + " was modified concurrently; refetch and retry.");
        }
    }
}
