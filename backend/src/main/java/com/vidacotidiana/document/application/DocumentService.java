package com.vidacotidiana.document.application;

import com.vidacotidiana.document.domain.Document;
import com.vidacotidiana.document.domain.DocumentCategory;
import com.vidacotidiana.shared.domain.ModuleContext;
import com.vidacotidiana.document.domain.DocumentRepository;
import com.vidacotidiana.person.application.PersonService;
import com.vidacotidiana.project.application.ProjectService;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.ValidationException;
import com.vidacotidiana.sharing.application.ResourceSharingService;
import com.vidacotidiana.sharing.domain.SharedResourceType;
import com.vidacotidiana.user.domain.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
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
    private final ResourceSharingService resourceSharingService;
    private final AttachmentTargets attachmentTargets;

    public DocumentService(DocumentRepository documentRepository, UserRepository userRepository,
                            PersonService personService, ProjectService projectService,
                            ResourceSharingService resourceSharingService,
                            AttachmentTargets attachmentTargets) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.personService = personService;
        this.projectService = projectService;
        this.resourceSharingService = resourceSharingService;
        this.attachmentTargets = attachmentTargets;
    }

    @Transactional
    public Document upload(UUID ownerUserId, String name, DocumentCategory category, MultipartFile file) {
        return upload(ownerUserId, name, category, file, null, null);
    }

    /** ADR-016 Fase 3b/FR-030: personId/projectId, when present, validated the same way as note.application.NoteService.create. */
    @Transactional
    public Document upload(UUID ownerUserId, String name, DocumentCategory category, MultipartFile file,
                            UUID personId, UUID projectId) {
        return upload(ownerUserId, name, category, file, personId, projectId, ModuleContext.PERSONAL);
    }

    /** ADR-022: alta con el módulo desde el que se sube. */
    @Transactional
    public Document upload(UUID ownerUserId, String name, DocumentCategory category, MultipartFile file,
                            UUID personId, UUID projectId, ModuleContext context) {
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
            Document document = new Document(ownerUserId, name, category, contentType, file.getBytes(), personId,
                    projectId, context);
            return documentRepository.save(document);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the uploaded document.", e);
        }
    }

    /** ADR-022: contexto + categoría + búsqueda en la consulta, por el mismo
        motivo que en Inventario — filtrar sobre la página ya cargada oculta
        resultados sin avisar. */
    @Transactional(readOnly = true)
    public Page<Document> search(UUID userId, ModuleContext context, DocumentCategory category,
                                 String query, Pageable pageable) {
        String normalized = (query == null || query.isBlank()) ? null : query.trim();
        return documentRepository.search(userId, context, category, normalized, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Document> listVisibleTo(UUID userId, DocumentCategory category, Pageable pageable) {
        if (category != null) {
            return documentRepository.findVisibleToByCategory(userId, category, pageable);
        }
        return documentRepository.findVisibleTo(userId, pageable);
    }

    @Transactional(readOnly = true)
    /**
     * ADR-025 §6: un documento compartido con un integrante de la familia se
     * puede VER, y nada más.
     *
     * La comprobación se suma a {@code Document#isVisibleTo}, no la sustituye:
     * PRIVATE, SHARED por correo y FAMILY_PUBLIC siguen comportándose
     * exactamente igual que antes. Y se queda en la lectura a propósito —
     * {@code getOwnedOrThrow}, que es lo que usan editar, renombrar, cambiar
     * visibilidad y borrar, no la consulta: compartir no convierte el
     * documento en editable ni colaborativo (requisito §6).
     */
    public Document getVisibleOrThrow(UUID documentId, UUID callerUserId) {
        Document document = findOrThrow(documentId);
        if (!document.isVisibleTo(callerUserId)
                && !resourceSharingService.isSharedWith(SharedResourceType.DOCUMENT, documentId, callerUserId)) {
            throw new NotFoundException("DOCUMENT_NOT_FOUND", "The requested document was not found.");
        }
        return document;
    }

    /**
     * ADR-025 §7 — descarga en lote.
     *
     * REUTILIZA EL ALMACENAMIENTO QUE YA HAY: los bytes salen de
     * {@code Document#getData()}, la misma columna que sirve
     * {@code GET /documents/{id}/content} desde que existe el módulo. No se
     * crea una segunda infraestructura de archivos, ni un directorio, ni un
     * bucket, ni una copia temporal en disco — el ZIP se arma en memoria y se
     * devuelve.
     *
     * Sin {@code ids} descarga TODO lo del contexto indicado; con {@code ids},
     * solo esos. Cada documento pasa por {@code getVisibleOrThrow}, así que la
     * autorización es exactamente la misma que en la descarga de uno solo: por
     * aquí no se puede sacar nada que no se pudiera sacar ya de uno en uno.
     */
    @Transactional(readOnly = true)
    public byte[] zip(UUID callerUserId, ModuleContext context, List<UUID> ids) {
        List<Document> documents;
        if (ids == null || ids.isEmpty()) {
            documents = documentRepository
                    .search(callerUserId, context, null, null, Pageable.unpaged())
                    .getContent();
        } else {
            documents = ids.stream().distinct().map(id -> getVisibleOrThrow(id, callerUserId)).toList();
        }
        if (documents.isEmpty()) {
            throw new ValidationException("No hay documentos que descargar.");
        }

        // Dos documentos pueden llamarse igual; un ZIP no admite dos entradas
        // con el mismo nombre, así que el repetido lleva sufijo en vez de
        // sobrescribir silenciosamente al anterior.
        Set<String> used = new java.util.HashSet<>();
        var buffer = new java.io.ByteArrayOutputStream();
        try (var zip = new java.util.zip.ZipOutputStream(buffer)) {
            for (Document document : documents) {
                zip.putNextEntry(new java.util.zip.ZipEntry(uniqueEntryName(document, used)));
                zip.write(document.getData());
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return buffer.toByteArray();
    }

    /** Nombre de entrada seguro: sin rutas (evita el zip-slip al descomprimir) y sin repetir. */
    private static String uniqueEntryName(Document document, Set<String> used) {
        String base = document.getName() == null ? "documento" : document.getName();
        base = base.replaceAll("[/\\\\]", "-").trim();
        if (base.isEmpty()) {
            base = "documento";
        }
        String extension = EXTENSIONS.getOrDefault(document.getContentType(), "");
        String candidate = base.toLowerCase(java.util.Locale.ROOT).endsWith(extension)
                ? base
                : base + extension;
        String unique = candidate;
        int suffix = 2;
        while (!used.add(unique)) {
            int dot = candidate.lastIndexOf('.');
            unique = dot > 0
                    ? candidate.substring(0, dot) + " (" + suffix + ")" + candidate.substring(dot)
                    : candidate + " (" + suffix + ")";
            suffix++;
        }
        return unique;
    }

    /** Solo los tipos que ALLOWED_CONTENT_TYPES ya admite al subir. */
    private static final java.util.Map<String, String> EXTENSIONS = java.util.Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/webp", ".webp",
            "image/gif", ".gif",
            "application/pdf", ".pdf");

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

    /**
     * Colgar un documento de un recurso, o soltarlo (V37).
     *
     * ES LA MECÁNICA ÚNICA. Garantías, mantenimientos, tareas, artículos y
     * pagos usan esta misma ruta: no hay un almacén de archivos por módulo.
     * (El blob propio de `warranties` se conserva solo por compatibilidad
     * mientras los clientes migran — ver V38.)
     *
     * Con los dos campos nulos el documento se DESENGANCHA sin borrarse: sigue
     * en Documentos, deja de pertenecer a ese registro.
     */
    @Transactional
    public Document link(UUID documentId, UUID callerUserId, String resourceType, UUID resourceId) {
        Document document = getOwnedOrThrow(documentId, callerUserId);

        // SOLTAR sigue siendo válido y no valida nada: los dos campos nulos
        // significan «ya no cuelga de ningún sitio». No es un destino ausente,
        // es la ausencia de destino.
        boolean detaching = (resourceType == null || resourceType.isBlank()) && resourceId == null;
        if (!detaching) {
            // LA PROPIEDAD SE COMPRUEBA EN LOS DOS EXTREMOS.
            //
            // Antes solo se validaba el documento, así que un `resourceId`
            // arbitrario —incluido el de otro usuario— se aceptaba y se
            // guardaba. Que el cliente ofrezca únicamente destinos válidos no
            // sustituye a esto: la autorización vive donde está el dato.
            if (!attachmentTargets.isSupported(resourceType)) {
                throw new ValidationException(
                        "No se puede colgar un documento de un «" + resourceType + "». "
                                + "Tipos admitidos: " + String.join(", ", attachmentTargets.supportedTypes()) + ".");
            }
            if (!attachmentTargets.belongsTo(resourceType, resourceId, callerUserId)) {
                // 404 y no 403: que exista o no un recurso ajeno no se
                // confirma, misma regla de no-enumeración que el resto (AC-004).
                throw new NotFoundException("ATTACHMENT_TARGET_NOT_FOUND", "No encontramos ese destino.");
            }
        }

        document.linkTo(resourceType, resourceId);
        return document;
    }

    /** Los adjuntos de un recurso, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public List<Document> attachmentsOf(UUID callerUserId, String resourceType, UUID resourceId) {
        return documentRepository.findByOwnerUserIdAndResourceTypeAndResourceIdOrderByCreatedAtDesc(
                callerUserId, resourceType, resourceId);
    }
}
