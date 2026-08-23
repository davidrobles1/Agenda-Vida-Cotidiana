package com.vidacotidiana.visionboard.application;

import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.visionboard.domain.VisionBoard;
import com.vidacotidiana.visionboard.domain.VisionBoardElement;
import com.vidacotidiana.visionboard.domain.VisionBoardElementRepository;
import com.vidacotidiana.visionboard.domain.VisionBoardElementType;
import com.vidacotidiana.visionboard.domain.VisionBoardRepository;
import com.vidacotidiana.visionboard.domain.VisionBoardTheme;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Application service for the Vision Board vertical slice (FASE 1). Mirrors
 * note.application.NoteService's authorization/locking pattern exactly —
 * owner-only throughout, same non-enumeration rule as
 * Reminder/Note/Warranty (AC-004/SEC-001, 11-auth-security.md): a caller
 * with no access gets exactly the same 404 as a truly missing board/
 * element, never 403. No collaboration/sharing here (MVP rule — see
 * VisionBoard's own doc comment).
 *
 * Element mutations always re-verify board ownership first (getOwnedOrThrow
 * on the board), then operate on the element itself with its own,
 * independent optimistic-lock check — see VisionBoardElement's doc comment
 * for why elements carry their own version instead of sharing the board's.
 */
@Service
public class VisionBoardService {

    private final VisionBoardRepository visionBoardRepository;
    private final VisionBoardElementRepository visionBoardElementRepository;

    public VisionBoardService(VisionBoardRepository visionBoardRepository,
                               VisionBoardElementRepository visionBoardElementRepository) {
        this.visionBoardRepository = visionBoardRepository;
        this.visionBoardElementRepository = visionBoardElementRepository;
    }

    /** FASE 16: {@code theme} defaults to LIGHT when the caller omits it —
        the one place that default is applied, so every other reader (the
        entity, the response DTO) can assume a board's theme is always
        concretely set, never null. */
    @Transactional
    public VisionBoard create(UUID ownerUserId, String name, String description, int width, int height,
                               VisionBoardTheme theme) {
        VisionBoard board = new VisionBoard(ownerUserId, name, description, width, height,
                theme != null ? theme : VisionBoardTheme.LIGHT);
        return visionBoardRepository.save(board);
    }

    @Transactional(readOnly = true)
    public Page<VisionBoard> listOwnedBy(UUID ownerUserId, Pageable pageable) {
        return visionBoardRepository.findByOwnerUserId(ownerUserId, pageable);
    }

    @Transactional(readOnly = true)
    public VisionBoard getOwnedOrThrow(UUID boardId, UUID callerUserId) {
        VisionBoard board = findBoardOrThrow(boardId);
        requireOwner(board, callerUserId);
        return board;
    }

    /** Owner-only. version is mandatory — a mismatch always rejects the edit with 409. */
    @Transactional
    public VisionBoard edit(UUID boardId, UUID callerUserId, String name, String description, Integer width,
                             Integer height, VisionBoardTheme theme, int expectedVersion) {
        VisionBoard board = getOwnedOrThrow(boardId, callerUserId);

        if (expectedVersion != board.getVersion()) {
            throw new ConflictException("VISION_BOARD_VERSION_CONFLICT",
                    "VisionBoard " + boardId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + board.getVersion() + ").");
        }

        board.applyEdit(name, description, width, height, theme);
        try {
            return visionBoardRepository.save(board);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("VISION_BOARD_VERSION_CONFLICT",
                    "VisionBoard " + boardId + " was modified concurrently; refetch and retry.");
        }
    }

    /** Elements cascade-delete at the database level (board_id FK, ON DELETE CASCADE — V8 migration). */
    @Transactional
    public void delete(UUID boardId, UUID callerUserId) {
        VisionBoard board = getOwnedOrThrow(boardId, callerUserId);
        visionBoardRepository.delete(board);
    }

    /** FASE 8: paint order (ascending zIndex). Ownership of the board is verified first. */
    @Transactional(readOnly = true)
    public List<VisionBoardElement> listElements(UUID boardId, UUID callerUserId) {
        getOwnedOrThrow(boardId, callerUserId);
        return visionBoardElementRepository.findByBoardIdOrderByZIndexAsc(boardId);
    }

    @Transactional
    public VisionBoardElement addElement(UUID boardId, UUID callerUserId, VisionBoardElementType type, double x,
                                          double y, double width, double height, Map<String, Object> data) {
        getOwnedOrThrow(boardId, callerUserId);
        VisionBoardElement element = new VisionBoardElement(boardId, type, x, y, width, height, data);
        return visionBoardElementRepository.save(element);
    }

    /** Owner-only (via the board). version is mandatory — a mismatch always rejects the edit with 409. */
    @Transactional
    public VisionBoardElement editElement(UUID boardId, UUID elementId, UUID callerUserId, Double x, Double y,
                                           Double width, Double height, Double rotation, Integer zIndex,
                                           Boolean locked, Boolean visible, Map<String, Object> data,
                                           int expectedVersion) {
        getOwnedOrThrow(boardId, callerUserId);
        VisionBoardElement element = getElementOnBoardOrThrow(boardId, elementId);

        if (expectedVersion != element.getVersion()) {
            throw new ConflictException("VISION_BOARD_ELEMENT_VERSION_CONFLICT",
                    "VisionBoardElement " + elementId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + element.getVersion() + ").");
        }

        element.applyEdit(x, y, width, height, rotation, zIndex, locked, visible, data);
        try {
            return visionBoardElementRepository.save(element);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("VISION_BOARD_ELEMENT_VERSION_CONFLICT",
                    "VisionBoardElement " + elementId + " was modified concurrently; refetch and retry.");
        }
    }

    /**
     * FASE 9 fix (layers redo): "subir/bajar una capa" now means exactly
     * what the user asked for — move the element exactly one position
     * relative to whichever element is immediately above/below it in
     * paint order, not an unconditional zIndex+-1 (meaningless once
     * elements share a zIndex, which every element does at creation —
     * see VisionBoardElement's constructor). Every element on the board
     * is renumbered to a dense 0..N-1 sequence matching the requested
     * order in this one transaction, so ties can never recur and
     * "immediately above/below" stays well-defined for the next call.
     * Only the target element's version is checked: the siblings'
     * zIndex changes are a system-driven side effect of the caller's
     * one intended action, not something the caller is expected to
     * have pre-fetched/re-sent a version for.
     */
    @Transactional
    public List<VisionBoardElement> reorderElement(UUID boardId, UUID elementId, UUID callerUserId, String direction,
                                                     int expectedVersion) {
        getOwnedOrThrow(boardId, callerUserId);
        List<VisionBoardElement> ordered = new ArrayList<>(visionBoardElementRepository.findByBoardIdOrderByZIndexAsc(boardId));

        int currentIndex = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(elementId)) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex < 0) {
            throw new NotFoundException("VISION_BOARD_ELEMENT_NOT_FOUND",
                    "The requested vision board element was not found.");
        }

        VisionBoardElement target = ordered.get(currentIndex);
        if (expectedVersion != target.getVersion()) {
            throw new ConflictException("VISION_BOARD_ELEMENT_VERSION_CONFLICT",
                    "VisionBoardElement " + elementId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + target.getVersion() + ").");
        }

        int newIndex = switch (direction) {
            case "FRONT" -> ordered.size() - 1;
            case "BACK" -> 0;
            case "RAISE" -> Math.min(currentIndex + 1, ordered.size() - 1);
            case "LOWER" -> Math.max(currentIndex - 1, 0);
            default -> currentIndex;
        };

        if (newIndex != currentIndex) {
            ordered.remove(currentIndex);
            ordered.add(newIndex, target);
        }

        List<VisionBoardElement> renumbered = new ArrayList<>(ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            VisionBoardElement element = ordered.get(i);
            if (element.getZIndex() != i) {
                element.applyEdit(null, null, null, null, null, i, null, null, null);
            }
            renumbered.add(element);
        }
        try {
            return visionBoardElementRepository.saveAll(renumbered);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("VISION_BOARD_ELEMENT_VERSION_CONFLICT",
                    "VisionBoardElement " + elementId + " was modified concurrently; refetch and retry.");
        }
    }

    @Transactional
    public void deleteElement(UUID boardId, UUID elementId, UUID callerUserId) {
        getOwnedOrThrow(boardId, callerUserId);
        VisionBoardElement element = getElementOnBoardOrThrow(boardId, elementId);
        visionBoardElementRepository.delete(element);
    }

    private VisionBoard findBoardOrThrow(UUID boardId) {
        return visionBoardRepository.findById(boardId)
                .orElseThrow(() -> new NotFoundException("VISION_BOARD_NOT_FOUND", "The requested vision board was not found."));
    }

    private void requireOwner(VisionBoard board, UUID callerUserId) {
        if (!board.isOwnedBy(callerUserId)) {
            throw new NotFoundException("VISION_BOARD_NOT_FOUND", "The requested vision board was not found.");
        }
    }

    /** Never leaks whether the element exists on a *different* board — same non-enumeration rule as the board itself. */
    private VisionBoardElement getElementOnBoardOrThrow(UUID boardId, UUID elementId) {
        VisionBoardElement element = visionBoardElementRepository.findById(elementId)
                .filter(candidate -> candidate.getBoardId().equals(boardId))
                .orElseThrow(() -> new NotFoundException("VISION_BOARD_ELEMENT_NOT_FOUND",
                        "The requested vision board element was not found."));
        return element;
    }
}
