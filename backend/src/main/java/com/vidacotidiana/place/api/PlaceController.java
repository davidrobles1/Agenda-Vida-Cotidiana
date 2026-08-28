package com.vidacotidiana.place.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.place.api.dto.CreatePlaceRequest;
import com.vidacotidiana.place.api.dto.PlaceResponse;
import com.vidacotidiana.place.api.dto.UpdatePlaceRequest;
import com.vidacotidiana.place.application.PlaceService;
import com.vidacotidiana.place.domain.Place;
import com.vidacotidiana.shared.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * ADR-016 Fase 3e3/FR-033: Lugar CRUD, owner-only — same shape as
 * person.api.PersonController. A pure catalogue: no endpoint here links a
 * Place to a Reminder (FR-033 keeps reminders.place_id out of scope; the
 * client copies the address into the existing free-text location field).
 */
@RestController
@RequestMapping("/api/v1/places")
public class PlaceController {

    private final PlaceService placeService;
    private final CurrentUser currentUser;

    public PlaceController(PlaceService placeService, CurrentUser currentUser) {
        this.placeService = placeService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<PlaceResponse> create(@Valid @RequestBody CreatePlaceRequest request) {
        Place created = placeService.create(currentUser.userId(), request.name(), request.address(), request.personId());
        return ResponseEntity.status(HttpStatus.CREATED).body(PlaceResponse.from(created));
    }

    @GetMapping
    public PageResponse<PlaceResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Place> places = placeService.listOwnedBy(currentUser.userId(), pageable);
        return PageResponse.from(places.map(PlaceResponse::from));
    }

    @GetMapping("/{id}")
    public PlaceResponse get(@PathVariable UUID id) {
        Place place = placeService.getOwnedOrThrow(id, currentUser.userId());
        return PlaceResponse.from(place);
    }

    @PatchMapping("/{id}")
    public PlaceResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePlaceRequest request) {
        Place place = placeService.edit(id, currentUser.userId(), request.name(), request.address(),
                request.personId(), request.version());
        return PlaceResponse.from(place);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        placeService.delete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
