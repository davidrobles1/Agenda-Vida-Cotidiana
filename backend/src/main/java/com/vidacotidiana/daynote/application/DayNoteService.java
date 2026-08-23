package com.vidacotidiana.daynote.application;

import com.vidacotidiana.daynote.domain.DayNoteElement;
import com.vidacotidiana.daynote.domain.DayNoteElementRepository;
import com.vidacotidiana.daynote.domain.DayNoteElementType;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.ValidationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Canvas de notas por día (pedido explícito del usuario, 2026-08-22).
 * Mismo patrón de autorización/bloqueo optimista que
 * visionboard.application.VisionBoardService, más una regla nueva sin
 * precedente en este código: "las formas no podrán superponerse" — ver
 * {@link #assertNoOverlap}.
 */
@Service
public class DayNoteService {

    private final DayNoteElementRepository repository;

    public DayNoteService(DayNoteElementRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DayNoteElement create(UUID ownerUserId, LocalDate noteDate, DayNoteElementType type,
                                  double x, double y, double width, double height, Map<String, Object> data) {
        List<DayNoteElement> siblings = repository.findByOwnerUserIdAndNoteDateOrderByZIndexAsc(ownerUserId, noteDate);
        assertNoOverlap(siblings, null, x, y, width, height);
        int nextZIndex = siblings.stream().mapToInt(DayNoteElement::getZIndex).max().orElse(-1) + 1;
        DayNoteElement element = new DayNoteElement(ownerUserId, noteDate, type, x, y, width, height, nextZIndex, data);
        return repository.save(element);
    }

    @Transactional(readOnly = true)
    public List<DayNoteElement> listForDay(UUID ownerUserId, LocalDate noteDate) {
        return repository.findByOwnerUserIdAndNoteDateOrderByZIndexAsc(ownerUserId, noteDate);
    }

    @Transactional(readOnly = true)
    public DayNoteElement getOwnedOrThrow(UUID elementId, UUID callerUserId) {
        DayNoteElement element = findOrThrow(elementId);
        requireOwner(element, callerUserId);
        return element;
    }

    /** Arrastrar/redimensionar — la validación de no-solapamiento es la
        misma que en creación, excluyendo el propio elemento (por eso el
        cliente SIEMPRE debe frenar el movimiento antes de soltar: esto es
        la red de seguridad del servidor, no el mecanismo principal —
        pedido explícito del usuario, aclarado: "se detiene en el borde
        más cercano sin encimar" es el comportamiento en el cliente). */
    @Transactional
    public DayNoteElement move(UUID elementId, UUID callerUserId, double x, double y, double width, double height, int expectedVersion) {
        DayNoteElement element = getOwnedOrThrow(elementId, callerUserId);
        checkVersion(element, expectedVersion);

        List<DayNoteElement> siblings = repository.findByOwnerUserIdAndNoteDateOrderByZIndexAsc(element.getOwnerUserId(), element.getNoteDate());
        assertNoOverlap(siblings, elementId, x, y, width, height);

        element.applyPosition(x, y, width, height);
        return saveOrConflict(element);
    }

    @Transactional
    public DayNoteElement editData(UUID elementId, UUID callerUserId, Map<String, Object> data, int expectedVersion) {
        DayNoteElement element = getOwnedOrThrow(elementId, callerUserId);
        checkVersion(element, expectedVersion);
        element.applyData(data);
        return saveOrConflict(element);
    }

    @Transactional
    public DayNoteElement bringToFront(UUID elementId, UUID callerUserId, int expectedVersion) {
        DayNoteElement element = getOwnedOrThrow(elementId, callerUserId);
        checkVersion(element, expectedVersion);
        List<DayNoteElement> siblings = repository.findByOwnerUserIdAndNoteDateOrderByZIndexAsc(element.getOwnerUserId(), element.getNoteDate());
        int maxZ = siblings.stream().mapToInt(DayNoteElement::getZIndex).max().orElse(0);
        element.applyZIndex(maxZ + 1);
        return saveOrConflict(element);
    }

    @Transactional
    public void delete(UUID elementId, UUID callerUserId) {
        DayNoteElement element = getOwnedOrThrow(elementId, callerUserId);
        repository.delete(element);
    }

    /** AABB (axis-aligned bounding box) — "no podrán superponerse", pedido
        explícito del usuario. No existe ningún chequeo de este tipo en
        ninguna otra parte del código (Vision Board solo alinea, nunca
        impide superposición) — esto es lógica genuinamente nueva. */
    private void assertNoOverlap(List<DayNoteElement> siblings, UUID excludeId, double x, double y, double width, double height) {
        for (DayNoteElement sibling : siblings) {
            if (excludeId != null && sibling.getId().equals(excludeId)) continue;
            if (sibling.overlaps(x, y, width, height)) {
                throw new ValidationException("Elements cannot overlap.");
            }
        }
    }

    private DayNoteElement findOrThrow(UUID elementId) {
        return repository.findById(elementId)
                .orElseThrow(() -> new NotFoundException("DAY_NOTE_ELEMENT_NOT_FOUND", "The requested element was not found."));
    }

    private void requireOwner(DayNoteElement element, UUID callerUserId) {
        if (!element.isOwnedBy(callerUserId)) {
            throw new NotFoundException("DAY_NOTE_ELEMENT_NOT_FOUND", "The requested element was not found.");
        }
    }

    private void checkVersion(DayNoteElement element, int expectedVersion) {
        if (expectedVersion != element.getVersion()) {
            throw new ConflictException("DAY_NOTE_ELEMENT_VERSION_CONFLICT",
                    "Element " + element.getId() + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + element.getVersion() + ").");
        }
    }

    private DayNoteElement saveOrConflict(DayNoteElement element) {
        try {
            return repository.save(element);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("DAY_NOTE_ELEMENT_VERSION_CONFLICT",
                    "Element " + element.getId() + " was modified concurrently; refetch and retry.");
        }
    }
}
