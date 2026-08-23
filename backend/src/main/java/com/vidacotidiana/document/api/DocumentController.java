package com.vidacotidiana.document.api;

import com.vidacotidiana.document.api.dto.DocumentResponse;
import com.vidacotidiana.document.api.dto.ShareDocumentRequest;
import com.vidacotidiana.document.api.dto.UpdateDocumentRequest;
import com.vidacotidiana.document.api.dto.VersionRequest;
import com.vidacotidiana.document.application.DocumentService;
import com.vidacotidiana.document.domain.Document;
import com.vidacotidiana.document.domain.DocumentCategory;
import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.shared.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Módulo Documentos (pedido explícito del usuario, 2026-08-22). Mismo split
 * metadata/bytes que VisionBoardImageController (GET vs GET/{id}/content),
 * mismo shape de CRUD que WarrantyController (create/list/get/edit/delete)
 * más los 3 endpoints de compartir (share/make-public/make-private) — ver
 * DocumentService's own doc comment para el porqué de "siempre éxito" en
 * share.
 */
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final CurrentUser currentUser;

    public DocumentController(DocumentService documentService, CurrentUser currentUser) {
        this.documentService = documentService;
        this.currentUser = currentUser;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("category") DocumentCategory category,
            @RequestParam(value = "personId", required = false) UUID personId,
            @RequestParam(value = "projectId", required = false) UUID projectId) {
        Document document = documentService.upload(currentUser.userId(), name, category, file, personId, projectId);
        return ResponseEntity.status(HttpStatus.CREATED).body(DocumentResponse.from(document));
    }

    @GetMapping
    public PageResponse<DocumentResponse> list(
            @RequestParam(required = false) DocumentCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Document> documents = documentService.listVisibleTo(currentUser.userId(), category, pageable);
        return PageResponse.from(documents.map(DocumentResponse::from));
    }

    @GetMapping("/{id}")
    public DocumentResponse get(@PathVariable UUID id) {
        Document document = documentService.getVisibleOrThrow(id, currentUser.userId());
        return DocumentResponse.from(document);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> getContent(@PathVariable UUID id) {
        Document document = documentService.getVisibleOrThrow(id, currentUser.userId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePrivate())
                .body(document.getData());
    }

    @PatchMapping("/{id}")
    public DocumentResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateDocumentRequest request) {
        Document document = documentService.edit(
                id, currentUser.userId(), request.name(), request.category(),
                request.personId(), request.projectId(), request.version());
        return DocumentResponse.from(document);
    }

    @PostMapping("/{id}/share")
    public DocumentResponse share(@PathVariable UUID id, @Valid @RequestBody ShareDocumentRequest request) {
        Document document = documentService.shareWithEmail(id, currentUser.userId(), request.email(), request.version());
        return DocumentResponse.from(document);
    }

    @PostMapping("/{id}/make-public")
    public DocumentResponse makePublic(@PathVariable UUID id, @Valid @RequestBody VersionRequest request) {
        Document document = documentService.makePublic(id, currentUser.userId(), request.version());
        return DocumentResponse.from(document);
    }

    @PostMapping("/{id}/make-private")
    public DocumentResponse makePrivate(@PathVariable UUID id, @Valid @RequestBody VersionRequest request) {
        Document document = documentService.makePrivate(id, currentUser.userId(), request.version());
        return DocumentResponse.from(document);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        documentService.delete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
