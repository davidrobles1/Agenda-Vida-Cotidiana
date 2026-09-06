package com.vidacotidiana.family.api;

import com.vidacotidiana.family.api.dto.CreateFamilyInvitationRequest;
import com.vidacotidiana.family.api.dto.FamilyInvitationResponse;
import com.vidacotidiana.family.api.dto.FamilyMemberResponse;
import com.vidacotidiana.family.api.dto.UserSearchResponse;
import com.vidacotidiana.family.application.FamilyService;
import com.vidacotidiana.identity.infrastructure.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Familia (ADR-025 §1): buscar personas, invitarlas y ver quién ya está dentro. */
@RestController
@RequestMapping("/api/v1")
public class FamilyController {

    private final FamilyService familyService;
    private final CurrentUser currentUser;

    public FamilyController(FamilyService familyService, CurrentUser currentUser) {
        this.familyService = familyService;
        this.currentUser = currentUser;
    }

    /**
     * Búsqueda de personas por nombre de usuario.
     *
     * El mínimo de {@link FamilyService#MIN_SEARCH_LENGTH} caracteres se
     * valida en el servicio y devuelve 400: la interfaz ya no llama por debajo
     * de ese umbral, pero la regla existe para proteger la base de datos y una
     * regla que solo vive en el cliente no protege nada.
     */
    @GetMapping("/users/search")
    public List<UserSearchResponse> searchUsers(@RequestParam("q") String query) {
        return familyService.searchUsers(query, currentUser.userId()).stream()
                .map(UserSearchResponse::from)
                .toList();
    }

    @GetMapping("/family/members")
    public List<FamilyMemberResponse> members() {
        return familyService.listMembers(currentUser.userId()).stream()
                .map(FamilyMemberResponse::from)
                .toList();
    }

    @DeleteMapping("/family/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID userId) {
        familyService.removeMember(currentUser.userId(), userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/family/invitations")
    public ResponseEntity<FamilyInvitationResponse> invite(@Valid @RequestBody CreateFamilyInvitationRequest request) {
        var invitation = familyService.invite(currentUser.userId(), request.userId());
        // Se relee por la vía normal para devolver la contraparte resuelta sin
        // duplicar aquí la lógica de nombres.
        FamilyInvitationResponse body = familyService.listSentInvitations(currentUser.userId()).stream()
                .filter(view -> view.invitation().getId().equals(invitation.getId()))
                .map(FamilyInvitationResponse::from)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "La invitación " + invitation.getId() + " desapareció justo después de crearse."));
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /** Las que YO envié y siguen pendientes — se ven en Familia, junto a los integrantes. */
    @GetMapping("/family/invitations/sent")
    public List<FamilyInvitationResponse> sentInvitations() {
        return familyService.listSentInvitations(currentUser.userId()).stream()
                .map(FamilyInvitationResponse::from)
                .toList();
    }

    /** Las que ME enviaron — el apartado de Configuración (requisito §1). */
    @GetMapping("/family/invitations/received")
    public List<FamilyInvitationResponse> receivedInvitations() {
        return familyService.listReceivedInvitations(currentUser.userId()).stream()
                .map(FamilyInvitationResponse::from)
                .toList();
    }

    @PostMapping("/family/invitations/{id}/accept")
    public ResponseEntity<Void> accept(@PathVariable UUID id) {
        familyService.acceptInvitation(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/family/invitations/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable UUID id) {
        familyService.rejectInvitation(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/family/invitations/{id}")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        familyService.cancelInvitation(id, currentUser.userId());
        return ResponseEntity.noContent().build();
    }
}
