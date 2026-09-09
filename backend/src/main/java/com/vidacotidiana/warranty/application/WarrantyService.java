package com.vidacotidiana.warranty.application;

import com.vidacotidiana.inventory.application.InventoryItemService;
import com.vidacotidiana.inventory.domain.InventoryItem;
import com.vidacotidiana.shared.domain.ModuleContext;
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
    /** ADR-022: solo para validar que el artículo enlazado es del mismo
        dueño. Garantías no depende del agregado Inventario para nada más. */
    private final InventoryItemService inventoryItemService;

    public WarrantyService(WarrantyRepository warrantyRepository, InventoryItemService inventoryItemService) {
        this.warrantyRepository = warrantyRepository;
        this.inventoryItemService = inventoryItemService;
    }

    /**
     * DECISION del Product Owner (2026-09-06): una garantía SIEMPRE cubre un
     * artículo del inventario, y el enlace se exige aquí, en el alta.
     *
     * Antes el enlace solo existía en `edit` (ADR-022), así que toda garantía
     * nacía desconectada por obligación y ligarla era un segundo acto que
     * había que acordarse de hacer. La regla se aplica en los tres sitios
     * —servicio, Web y Android—, y esta es la única que la garantiza: la
     * validación de pantalla se salta con una llamada directa a la API.
     *
     * NO es retroactiva: las garantías anteriores sin artículo siguen siendo
     * válidas y `edit` no lo exige. Se restringe lo que entra, no lo que ya
     * está guardado.
     *
     * El archivo sigue siendo obligatorio también (pedido explícito del
     * usuario: "al registrar una garantía subir el archivo") — validado en la
     * capa de aplicación, no como NOT NULL en la tabla (ver
     * V14__warranty_documents.sql).
     */
    @Transactional
    public Warranty create(UUID ownerUserId, String item, Instant expiresAt, MultipartFile file,
                           UUID inventoryItemId) {
        return create(ownerUserId, item, expiresAt, file, ModuleContext.PERSONAL, inventoryItemId);
    }

    /** ADR-019: alta con el módulo desde el que se creó. */
    @Transactional
    public Warranty create(UUID ownerUserId, String item, Instant expiresAt, MultipartFile file, ModuleContext context,
                           UUID inventoryItemId) {
        // Real gap found in live testing: switching this endpoint from a
        // `@Valid @RequestBody` JSON DTO (which had `@NotBlank`) to plain
        // multipart `@RequestParam`s dropped that validation entirely —
        // `@RequestParam` alone never rejects an empty string. Restored
        // explicitly here since it's no longer enforced at the controller layer.
        if (item == null || item.isBlank()) {
            throw new ValidationException("item must not be blank.");
        }
        if (inventoryItemId == null) {
            throw new ValidationException("Una garantía debe indicar el artículo del inventario que cubre.");
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
        Warranty warranty = new Warranty(ownerUserId, item, expiresAt, context);
        requireLinkableItem(inventoryItemId, ownerUserId, warranty.getContext());
        warranty.linkInventoryItem(inventoryItemId);
        try {
            warranty.attachDocument(contentType, file.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read the uploaded warranty document.", e);
        }
        return warrantyRepository.save(warranty);
    }

    /**
     * Un artículo solo es enlazable si es del mismo dueño Y del mismo módulo.
     *
     * El dueño evita filtrar la existencia de artículos ajenos (misma regla de
     * no-enumeración que el resto del servicio: 404, nunca 403).
     *
     * El módulo cierra un hueco real de ADR-019/ADR-022: hasta ahora la única
     * defensa era que `WarrantyDetailDialog` cargaba el selector con
     * `listInventoryItems(activeMode)`, es decir, una decisión de CLIENTE. Una
     * llamada directa a la API podía enlazar una garantía Personal con un
     * artículo Laboral y hacer que un recurso de un módulo apareciera en el
     * otro, que es exactamente lo que la regla 2 del ADR-019 prohíbe.
     */
    private void requireLinkableItem(UUID inventoryItemId, UUID callerUserId, ModuleContext warrantyContext) {
        if (inventoryItemId == null) {
            return;
        }
        InventoryItem linked = inventoryItemService.getOwnedOrThrow(inventoryItemId, callerUserId);
        if (linked.getContext() != warrantyContext) {
            throw new ValidationException(
                    "El artículo pertenece al módulo " + linked.getContext()
                            + " y la garantía al módulo " + warrantyContext
                            + ". Solo se pueden enlazar recursos del mismo módulo.");
        }
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
        return listOwnedBy(ownerUserId, null, pageable);
    }

    /**
     * ADR-019: `context` nulo = sin filtrar, que es lo que necesita el
     * Calendario general (el modo que muestra Personal y Laboral juntos).
     * Con contexto, el filtro va en la consulta: los recursos del otro
     * módulo ni siquiera se leen.
     */
    @Transactional(readOnly = true)
    public Page<Warranty> listOwnedBy(UUID ownerUserId, ModuleContext context, Pageable pageable) {
        return (context == null)
                ? warrantyRepository.findByOwnerUserId(ownerUserId, pageable)
                : warrantyRepository.findByOwnerUserIdAndContext(ownerUserId, context, pageable);
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
        return edit(warrantyId, callerUserId, item, expiresAt, expectedVersion, null, false);
    }

    /**
     * ADR-022: edición con enlace al artículo del inventario.
     *
     * `linkInventoryItem` distingue "no tocar el enlace" (false) de
     * "cambiarlo" (true), igual que `clearInterval` en Mantenimiento
     * (ADR-021(i)): sin ese indicador, mandar `null` sería indistinguible de
     * no mandar nada y **desenlazar sería imposible**.
     *
     * El artículo se valida contra el mismo dueño: enlazar la garantía a un
     * artículo ajeno filtraría su existencia.
     */
    @Transactional
    public Warranty edit(UUID warrantyId, UUID callerUserId, String item, Instant expiresAt, int expectedVersion,
                         UUID inventoryItemId, boolean linkInventoryItem) {
        Warranty warranty = getOwnedOrThrow(warrantyId, callerUserId);

        if (expectedVersion != warranty.getVersion()) {
            throw new ConflictException("WARRANTY_VERSION_CONFLICT",
                    "Warranty " + warrantyId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + warranty.getVersion() + ").");
        }

        warranty.applyEdit(item, expiresAt);
        if (linkInventoryItem) {
            // Desenlazar (`null`) sigue permitido en la edición: la
            // obligatoriedad es del alta, no del ciclo de vida. Una garantía
            // anterior a esta regla puede corregirse sin quedar atrapada.
            requireLinkableItem(inventoryItemId, callerUserId, warranty.getContext());
            warranty.linkInventoryItem(inventoryItemId);
        }
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
