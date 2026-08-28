package com.vidacotidiana.place.application;

import com.vidacotidiana.person.application.PersonService;
import com.vidacotidiana.place.domain.Place;
import com.vidacotidiana.place.domain.PlaceRepository;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for the Place vertical slice (ADR-016 adenda Fase 3e3,
 * FR-033, UC-26, AC-020). Owner-only, same locking pattern as
 * person.application.PersonService.
 *
 * Depends on PersonService (not PersonRepository directly) to reuse its
 * ownership check for {@code personId} — exactly the same cross-module reuse
 * pattern as project.application.ProjectService with clientPersonId.
 */
@Service
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final PersonService personService;

    public PlaceService(PlaceRepository placeRepository, PersonService personService) {
        this.placeRepository = placeRepository;
        this.personService = personService;
    }

    @Transactional
    public Place create(UUID ownerUserId, String name, String address, UUID personId) {
        if (personId != null) {
            personService.getOwnedOrThrow(personId, ownerUserId);
        }
        Place place = new Place(ownerUserId, name, address, personId);
        return placeRepository.save(place);
    }

    @Transactional(readOnly = true)
    public Page<Place> listOwnedBy(UUID ownerUserId, Pageable pageable) {
        return placeRepository.findByOwnerUserId(ownerUserId, pageable);
    }

    @Transactional(readOnly = true)
    public Place getOwnedOrThrow(UUID placeId, UUID callerUserId) {
        Place place = findOrThrow(placeId);
        requireOwner(place, callerUserId);
        return place;
    }

    @Transactional
    public Place edit(UUID placeId, UUID callerUserId, String name, String address, UUID personId, int expectedVersion) {
        Place place = getOwnedOrThrow(placeId, callerUserId);

        if (personId != null) {
            personService.getOwnedOrThrow(personId, callerUserId);
        }

        if (expectedVersion != place.getVersion()) {
            throw new ConflictException("PLACE_VERSION_CONFLICT",
                    "Place " + placeId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + place.getVersion() + ").");
        }

        place.applyEdit(name, address, personId);
        try {
            return placeRepository.save(place);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("PLACE_VERSION_CONFLICT",
                    "Place " + placeId + " was modified concurrently; refetch and retry.");
        }
    }

    @Transactional
    public void delete(UUID placeId, UUID callerUserId) {
        Place place = getOwnedOrThrow(placeId, callerUserId);
        placeRepository.delete(place);
    }

    private Place findOrThrow(UUID placeId) {
        return placeRepository.findById(placeId)
                .orElseThrow(() -> new NotFoundException("PLACE_NOT_FOUND", "The requested place was not found."));
    }

    private void requireOwner(Place place, UUID callerUserId) {
        if (!place.isOwnedBy(callerUserId)) {
            throw new NotFoundException("PLACE_NOT_FOUND", "The requested place was not found.");
        }
    }
}
