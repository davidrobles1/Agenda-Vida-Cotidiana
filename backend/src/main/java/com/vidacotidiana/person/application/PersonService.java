package com.vidacotidiana.person.application;

import com.vidacotidiana.person.domain.Person;
import com.vidacotidiana.person.domain.PersonRepository;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service for the Person vertical slice (ADR-016, FR-021,
 * UC-18). Same authorization/locking pattern as warranty.application.
 * WarrantyService: owner-only throughout, 404 (never 403) on a resource
 * owned by someone else, mandatory version on edit.
 *
 * {@link #getOwnedOrThrow(UUID, UUID)} is exposed for reuse by
 * project.application.ProjectService (client_person_id) and
 * commitment.application.CommitmentService (person_id) — same pattern as
 * reminder.application.ReminderService's getOwnedOrThrow being reused by
 * sharing.application.SharingService.
 */
@Service
public class PersonService {

    private final PersonRepository personRepository;

    public PersonService(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    @Transactional
    public Person create(UUID ownerUserId, String name, String role, String organization) {
        Person person = new Person(ownerUserId, name, role, organization);
        return personRepository.save(person);
    }

    @Transactional(readOnly = true)
    public Page<Person> listOwnedBy(UUID ownerUserId, Pageable pageable) {
        return personRepository.findByOwnerUserId(ownerUserId, pageable);
    }

    @Transactional(readOnly = true)
    public Person getOwnedOrThrow(UUID personId, UUID callerUserId) {
        Person person = findOrThrow(personId);
        requireOwner(person, callerUserId);
        return person;
    }

    @Transactional
    public Person edit(UUID personId, UUID callerUserId, String name, String role, String organization, int expectedVersion) {
        Person person = getOwnedOrThrow(personId, callerUserId);

        if (expectedVersion != person.getVersion()) {
            throw new ConflictException("PERSON_VERSION_CONFLICT",
                    "Person " + personId + " was modified concurrently (expected version "
                            + expectedVersion + ", current version " + person.getVersion() + ").");
        }

        person.applyEdit(name, role, organization);
        try {
            return personRepository.save(person);
        } catch (ObjectOptimisticLockingFailureException raceLostToConcurrentUpdate) {
            throw new ConflictException("PERSON_VERSION_CONFLICT",
                    "Person " + personId + " was modified concurrently; refetch and retry.");
        }
    }

    @Transactional
    public void delete(UUID personId, UUID callerUserId) {
        Person person = getOwnedOrThrow(personId, callerUserId);
        personRepository.delete(person);
    }

    private Person findOrThrow(UUID personId) {
        return personRepository.findById(personId)
                .orElseThrow(() -> new NotFoundException("PERSON_NOT_FOUND", "The requested person was not found."));
    }

    private void requireOwner(Person person, UUID callerUserId) {
        if (!person.isOwnedBy(callerUserId)) {
            throw new NotFoundException("PERSON_NOT_FOUND", "The requested person was not found.");
        }
    }
}
