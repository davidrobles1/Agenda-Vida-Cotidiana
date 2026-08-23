package com.vidacotidiana.person.api;

import com.vidacotidiana.identity.infrastructure.CurrentUser;
import com.vidacotidiana.person.api.dto.CreatePersonRequest;
import com.vidacotidiana.person.api.dto.PersonResponse;
import com.vidacotidiana.person.api.dto.UpdatePersonRequest;
import com.vidacotidiana.person.application.PersonService;
import com.vidacotidiana.person.domain.Person;
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
 * ADR-016/FR-021: Persona CRUD, owner-only — same shape as
 * warranty.api.WarrantyController, without a "complete" action (a Person
 * has no completion state).
 */
@RestController
@RequestMapping("/api/v1/people")
public class PersonController {

    private final PersonService personService;
    private final CurrentUser currentUser;

    public PersonController(PersonService personService, CurrentUser currentUser) {
        this.personService = personService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<PersonResponse> create(@Valid @RequestBody CreatePersonRequest request) {
        Person created = personService.create(currentUser.userId(), request.name(), request.role(), request.organization());
        return ResponseEntity.status(HttpStatus.CREATED).body(PersonResponse.from(created));
    }

    @GetMapping
    public PageResponse<PersonResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Person> people = personService.listOwnedBy(currentUser.userId(), pageable);
        return PageResponse.from(people.map(PersonResponse::from));
    }

    @GetMapping("/{id}")
    public PersonResponse get(@PathVariable UUID id) {
        Person person = personService.getOwnedOrThrow(id, currentUser.userId());
        return PersonResponse.from(person);
    }

    @PatchMapping("/{id}")
    public PersonResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePersonRequest request) {
        Person person = personService.edit(id, currentUser.userId(), request.name(), request.role(), request.organization(), request.version());
        return PersonResponse.from(person);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        personService.delete(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
