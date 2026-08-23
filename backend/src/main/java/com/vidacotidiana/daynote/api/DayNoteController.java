package com.vidacotidiana.daynote.api;

import com.vidacotidiana.daynote.api.dto.BringToFrontRequest;
import com.vidacotidiana.daynote.api.dto.CreateDayNoteElementRequest;
import com.vidacotidiana.daynote.api.dto.DayNoteElementResponse;
import com.vidacotidiana.daynote.api.dto.MoveDayNoteElementRequest;
import com.vidacotidiana.daynote.api.dto.UpdateDayNoteElementDataRequest;
import com.vidacotidiana.daynote.application.DayNoteService;
import com.vidacotidiana.daynote.domain.DayNoteElement;
import com.vidacotidiana.daynote.domain.DayNoteElementType;
import com.vidacotidiana.identity.infrastructure.CurrentUser;
import jakarta.validation.Valid;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Pedido explícito del usuario (2026-08-22): "Reemplazar la vista actual de
 * notas por un único Canvas, similar al de Visión Board." Sin endpoint de
 * listado paginado global — a diferencia del resto de módulos de este
 * proyecto, aquí siempre se listan TODOS los elementos de un día concreto
 * a la vez (un canvas completo), nunca por id individual salvo mutación.
 */
@RestController
@RequestMapping("/api/v1/day-notes")
public class DayNoteController {

    private final DayNoteService dayNoteService;
    private final CurrentUser currentUser;

    public DayNoteController(DayNoteService dayNoteService, CurrentUser currentUser) {
        this.dayNoteService = dayNoteService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<DayNoteElementResponse> create(@Valid @RequestBody CreateDayNoteElementRequest request) {
        DayNoteElement created = dayNoteService.create(
                currentUser.userId(),
                request.noteDate(),
                DayNoteElementType.valueOf(request.type()),
                request.x(), request.y(), request.width(), request.height(),
                request.data());
        return ResponseEntity.status(HttpStatus.CREATED).body(DayNoteElementResponse.from(created));
    }

    @GetMapping
    public List<DayNoteElementResponse> listForDay(@RequestParam LocalDate date) {
        return dayNoteService.listForDay(currentUser.userId(), date).stream()
                .map(DayNoteElementResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public DayNoteElementResponse get(@PathVariable UUID id) {
        return DayNoteElementResponse.from(dayNoteService.getOwnedOrThrow(id, currentUser.userId()));
    }

    @PutMapping("/{id}/position")
    public DayNoteElementResponse move(@PathVariable UUID id, @Valid @RequestBody MoveDayNoteElementRequest request) {
        DayNoteElement moved = dayNoteService.move(id, currentUser.userId(),
                request.x(), request.y(), request.width(), request.height(), request.version());
        return DayNoteElementResponse.from(moved);
    }

    @PutMapping("/{id}/data")
    public DayNoteElementResponse editData(@PathVariable UUID id, @Valid @RequestBody UpdateDayNoteElementDataRequest request) {
        DayNoteElement edited = dayNoteService.editData(id, currentUser.userId(), request.data(), request.version());
        return DayNoteElementResponse.from(edited);
    }

    @PostMapping("/{id}/bring-to-front")
    public DayNoteElementResponse bringToFront(@PathVariable UUID id, @Valid @RequestBody BringToFrontRequest request) {
        DayNoteElement element = dayNoteService.bringToFront(id, currentUser.userId(), request.version());
        return DayNoteElementResponse.from(element);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        dayNoteService.delete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
