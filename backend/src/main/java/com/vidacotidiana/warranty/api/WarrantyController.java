package com.vidacotidiana.warranty.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.shared.api.PageResponse;
import com.vidacotidiana.shared.domain.ModuleContext;
import com.vidacotidiana.warranty.api.dto.CompleteWarrantyRequest;
import com.vidacotidiana.warranty.api.dto.UpdateWarrantyRequest;
import com.vidacotidiana.warranty.api.dto.WarrantyResponse;
import com.vidacotidiana.warranty.application.WarrantyService;
import com.vidacotidiana.warranty.domain.Warranty;
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

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * BE-037/WEB-009: full Warranty CRUD, mirroring
 * reminder.api.ReminderController's shape exactly (create, list own items
 * paginated, get by id, toggle completion, edit, delete) but owner-only
 * throughout — see WarrantyRepository for why there is no
 * "owner or active collaborator" distinction here.
 */
@RestController
@RequestMapping("/api/v1/warranties")
public class WarrantyController {

    private final WarrantyService warrantyService;
    private final CurrentUser currentUser;

    public WarrantyController(WarrantyService warrantyService, CurrentUser currentUser) {
        this.warrantyService = warrantyService;
        this.currentUser = currentUser;
    }

    /** Pedido explícito del usuario (2026-08-21): "al registrar una
        garantía subir el archivo de la garantía en formato imagen o pdf en
        un modal central será el registro" — multipart en vez de JSON,
        mismo shape que DocumentController#upload. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WarrantyResponse> create(
            @RequestPart("file") MultipartFile file,
            @RequestParam("item") String item,
            @RequestParam("expiresAt") Instant expiresAt,
            // ADR-019: módulo desde el que se crea. Ausente ⇒ PERSONAL.
            @RequestParam(value = "context", required = false) String context) {
        Warranty created = warrantyService.create(currentUser.userId(), item, expiresAt, file,
                ModuleContext.fromNullable(context));
        return ResponseEntity.status(HttpStatus.CREATED).body(WarrantyResponse.from(created));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> getContent(@PathVariable UUID id) {
        Warranty warranty = warrantyService.getWithDocumentOrThrow(id, currentUser.userId());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(warranty.getDocumentContentType()))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePrivate())
                .body(warranty.getDocumentData());
    }

    @GetMapping
    public PageResponse<WarrantyResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            // ADR-019: sin `context` se devuelve todo (Calendario general);
            // con contexto, el filtro baja hasta la consulta SQL.
            @RequestParam(value = "context", required = false) String context) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Warranty> warranties = warrantyService.listOwnedBy(
                currentUser.userId(), ModuleContext.filterFromNullable(context), pageable);
        return PageResponse.from(warranties.map(WarrantyResponse::from));
    }

    @GetMapping("/{id}")
    public WarrantyResponse get(@PathVariable UUID id) {
        Warranty warranty = warrantyService.getOwnedOrThrow(id, currentUser.userId());
        return WarrantyResponse.from(warranty);
    }

    @PostMapping("/{id}/complete")
    public WarrantyResponse complete(@PathVariable UUID id,
                                      @RequestBody(required = false) CompleteWarrantyRequest request) {
        Integer expectedVersion = (request != null) ? request.version() : null;
        Warranty warranty = warrantyService.toggleCompletion(id, currentUser.userId(), expectedVersion);
        return WarrantyResponse.from(warranty);
    }

    @PatchMapping("/{id}")
    public WarrantyResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateWarrantyRequest request) {
        Warranty warranty = warrantyService.edit(id, currentUser.userId(), request.item(), request.expiresAt(), request.version());
        return WarrantyResponse.from(warranty);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        warrantyService.delete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
