package com.vidacotidiana.sharing.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.sharing.api.dto.CreateResourceShareRequest;
import com.vidacotidiana.sharing.api.dto.ResourceShareResponse;
import com.vidacotidiana.sharing.api.dto.UpdateResourceShareRequest;
import com.vidacotidiana.sharing.application.ResourceSharingService;
import com.vidacotidiana.sharing.domain.SharedResourceType;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Compartidos (ADR-025 §2/§3/§4).
 *
 * Un solo controlador para los seis tipos de recurso, con el tipo en la ruta.
 * La alternativa —un endpoint de compartición dentro de cada módulo— habría
 * repetido seis veces la misma lógica de propiedad, familia y responsabilidad.
 */
@RestController
@RequestMapping("/api/v1")
public class ResourceShareController {

    private final ResourceSharingService sharingService;
    private final CurrentUser currentUser;

    public ResourceShareController(ResourceSharingService sharingService, CurrentUser currentUser) {
        this.sharingService = sharingService;
        this.currentUser = currentUser;
    }

    /** Comparte un recurso propio. El tipo va en la ruta, ya validado por el enum. */
    @PostMapping("/shared-resources/{type}/{resourceId}")
    public ResponseEntity<ResourceShareResponse> share(
            @PathVariable SharedResourceType type,
            @PathVariable UUID resourceId,
            @Valid @RequestBody CreateResourceShareRequest request) {
        sharingService.share(type, resourceId, currentUser.userId(),
                request.collaboratorUserId(), request.responsibility());
        // Se devuelve la lista del recurso, ya decorada con nombres, en lugar de
        // recomponer aquí la vista a mano.
        ResourceShareResponse body = sharingService
                .listForResource(type, resourceId, currentUser.userId()).stream()
                .filter(view -> view.share().getCollaboratorUserId().equals(request.collaboratorUserId()))
                .map(ResourceShareResponse::from)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "La compartición desapareció justo después de crearse."));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /** Con quién está compartido un recurso — lo consulta su propia ficha al editarlo. */
    @GetMapping("/shared-resources/{type}/{resourceId}")
    public List<ResourceShareResponse> forResource(
            @PathVariable SharedResourceType type,
            @PathVariable UUID resourceId) {
        return sharingService.listForResource(type, resourceId, currentUser.userId()).stream()
                .map(ResourceShareResponse::from)
                .toList();
    }

    /** "Me compartieron". */
    @GetMapping("/shared-resources/received")
    public List<ResourceShareResponse> received() {
        return sharingService.listReceived(currentUser.userId()).stream()
                .map(ResourceShareResponse::from)
                .toList();
    }

    /** "Yo compartí". */
    @GetMapping("/shared-resources/sent")
    public List<ResourceShareResponse> sent() {
        return sharingService.listSent(currentUser.userId()).stream()
                .map(ResourceShareResponse::from)
                .toList();
    }

    @PatchMapping("/shared-resources/shares/{shareId}")
    public ResourceShareResponse updateResponsibility(
            @PathVariable UUID shareId,
            @RequestBody UpdateResourceShareRequest request) {
        sharingService.updateResponsibility(shareId, currentUser.userId(), request.responsibility());
        return sharingService.listSent(currentUser.userId()).stream()
                .filter(view -> view.share().getId().equals(shareId))
                .map(ResourceShareResponse::from)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("La compartición " + shareId + " no se pudo releer."));
    }

    @DeleteMapping("/shared-resources/shares/{shareId}")
    public ResponseEntity<Void> revoke(@PathVariable UUID shareId) {
        sharingService.revoke(shareId, currentUser.userId());
        return ResponseEntity.noContent().build();
    }

    /** "Ya hice mi parte" — solo quien está comprometido. */
    @PostMapping("/shared-resources/shares/{shareId}/part-done")
    public ResourceShareResponse markPartDone(@PathVariable UUID shareId) {
        sharingService.markPartDone(shareId, currentUser.userId());
        return reread(shareId);
    }

    @DeleteMapping("/shared-resources/shares/{shareId}/part-done")
    public ResourceShareResponse reopenPart(@PathVariable UUID shareId) {
        sharingService.reopenPart(shareId, currentUser.userId());
        return reread(shareId);
    }

    private ResourceShareResponse reread(UUID shareId) {
        return sharingService.listReceived(currentUser.userId()).stream()
                .filter(view -> view.share().getId().equals(shareId))
                .map(ResourceShareResponse::from)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("La compartición " + shareId + " no se pudo releer."));
    }
}
