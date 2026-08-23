package com.vidacotidiana.visionboard.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.shared.api.PageResponse;
import com.vidacotidiana.visionboard.api.dto.CreateElementRequest;
import com.vidacotidiana.visionboard.api.dto.CreateVisionBoardRequest;
import com.vidacotidiana.visionboard.api.dto.ElementResponse;
import com.vidacotidiana.visionboard.api.dto.ReorderElementRequest;
import com.vidacotidiana.visionboard.api.dto.UpdateElementRequest;
import com.vidacotidiana.visionboard.api.dto.UpdateVisionBoardRequest;
import com.vidacotidiana.visionboard.api.dto.VisionBoardResponse;
import com.vidacotidiana.visionboard.application.VisionBoardService;
import com.vidacotidiana.visionboard.domain.VisionBoard;
import com.vidacotidiana.visionboard.domain.VisionBoardElement;
import com.vidacotidiana.visionboard.domain.VisionBoardElementType;
import com.vidacotidiana.visionboard.domain.VisionBoardTheme;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FASE 2: covers the Vision Board CRUD + element CRUD in
 * Documentacion/openapi/openapi.yaml — mirrors reminder.api.ReminderController/
 * note.api.NoteController's shape exactly. Owner-only throughout (no
 * sharing/collaborator concept for the MVP — same as Note/Warranty), same
 * non-enumeration rule (VisionBoardService never distinguishes "not found"
 * from "not yours", both are 404).
 *
 * GET /vision-boards/{id} returns the board WITH its elements
 * (VisionBoardResponse.from(board, elements)) — there is no separate GET
 * .../elements endpoint in this API; GET /vision-boards (the list) omits
 * elements to stay a lightweight summary (VisionBoardResponse.from(board)).
 */
@RestController
@RequestMapping("/api/v1/vision-boards")
public class VisionBoardController {

    private final VisionBoardService visionBoardService;
    private final CurrentUser currentUser;

    public VisionBoardController(VisionBoardService visionBoardService, CurrentUser currentUser) {
        this.visionBoardService = visionBoardService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<VisionBoardResponse> create(@Valid @RequestBody CreateVisionBoardRequest request) {
        VisionBoardTheme theme = request.theme() != null ? VisionBoardTheme.valueOf(request.theme()) : null;
        VisionBoard created = visionBoardService.create(
                currentUser.userId(), request.name(), request.description(), request.width(), request.height(),
                theme);
        return ResponseEntity.status(HttpStatus.CREATED).body(VisionBoardResponse.from(created));
    }

    @GetMapping
    public PageResponse<VisionBoardResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<VisionBoard> boards = visionBoardService.listOwnedBy(currentUser.userId(), pageable);
        return PageResponse.from(boards.map(VisionBoardResponse::from));
    }

    @GetMapping("/{id}")
    public VisionBoardResponse get(@PathVariable UUID id) {
        VisionBoard board = visionBoardService.getOwnedOrThrow(id, currentUser.userId());
        var elements = visionBoardService.listElements(id, currentUser.userId());
        return VisionBoardResponse.from(board, elements);
    }

    @PutMapping("/{id}")
    public VisionBoardResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateVisionBoardRequest request) {
        // FASE 16: this is also how a Board Theme change persists — the
        // client sends { theme, version }, nothing else; no dedicated
        // theme endpoint was added, this partial-update PUT already covers it.
        VisionBoardTheme theme = request.theme() != null ? VisionBoardTheme.valueOf(request.theme()) : null;
        VisionBoard board = visionBoardService.edit(
                id, currentUser.userId(), request.name(), request.description(), request.width(), request.height(),
                theme, request.version());
        return VisionBoardResponse.from(board);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        visionBoardService.delete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/elements")
    public ResponseEntity<ElementResponse> addElement(@PathVariable UUID id,
                                                        @Valid @RequestBody CreateElementRequest request) {
        VisionBoardElementType type = VisionBoardElementType.valueOf(request.type());
        VisionBoardElement element = visionBoardService.addElement(
                id, currentUser.userId(), type, request.x(), request.y(), request.width(), request.height(),
                request.data());
        return ResponseEntity.status(HttpStatus.CREATED).body(ElementResponse.from(element));
    }

    @PutMapping("/{id}/elements/{elementId}")
    public ElementResponse updateElement(@PathVariable UUID id, @PathVariable UUID elementId,
                                          @Valid @RequestBody UpdateElementRequest request) {
        VisionBoardElement element = visionBoardService.editElement(
                id, elementId, currentUser.userId(), request.x(), request.y(), request.width(), request.height(),
                request.rotation(), request.zIndex(), request.locked(), request.visible(), request.data(),
                request.version());
        return ElementResponse.from(element);
    }

    @DeleteMapping("/{id}/elements/{elementId}")
    public ResponseEntity<Void> deleteElement(@PathVariable UUID id, @PathVariable UUID elementId) {
        visionBoardService.deleteElement(id, elementId, currentUser.userId());
        return ResponseEntity.noContent().build();
    }

    /**
     * FASE 9 fix (layers): renumbers the whole board's zIndex sequence in
     * one atomic operation (VisionBoardService#reorderElement) and returns
     * every affected element so the caller can sync all of them without a
     * full board refetch.
     */
    @PostMapping("/{id}/elements/{elementId}/reorder")
    public List<ElementResponse> reorderElement(@PathVariable UUID id, @PathVariable UUID elementId,
                                                 @Valid @RequestBody ReorderElementRequest request) {
        List<VisionBoardElement> elements = visionBoardService.reorderElement(
                id, elementId, currentUser.userId(), request.direction(), request.version());
        return elements.stream().map(ElementResponse::from).collect(Collectors.toList());
    }
}
