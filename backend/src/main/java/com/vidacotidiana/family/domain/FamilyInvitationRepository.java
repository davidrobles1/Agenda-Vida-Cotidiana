package com.vidacotidiana.family.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamilyInvitationRepository extends JpaRepository<FamilyInvitation, UUID> {

    /** Precomprobación de nivel aplicación para el 409 amable; la garantía real
        frente a una carrera es el índice parcial uq_family_invitations_pending (V29). */
    Optional<FamilyInvitation> findByInviterUserIdAndInvitedUserIdAndStatus(
            UUID inviterUserId, UUID invitedUserId, FamilyInvitationStatus status);

    /** Invitaciones recibidas por quien llama — el apartado de Configuración. */
    List<FamilyInvitation> findByInvitedUserIdAndStatusOrderByCreatedAtDesc(
            UUID invitedUserId, FamilyInvitationStatus status);

    /** Invitaciones que quien llama envió y siguen sin respuesta — la sección Familia
        muestra su estado junto a los integrantes ya aceptados. */
    List<FamilyInvitation> findByInviterUserIdAndStatusOrderByCreatedAtDesc(
            UUID inviterUserId, FamilyInvitationStatus status);

    /**
     * Misma transición condicional atómica que
     * {@code InvitationRepository.resolveIfPending}, y por el mismo motivo: dos
     * peticiones concurrentes sobre la misma invitación no pueden resolverla
     * las dos. 0 filas actualizadas significa que ya no estaba PENDING cuando
     * se ejecutó la sentencia, y quien llama debe responder 410 — nunca aplicar
     * la transición a partir de un estado leído antes en Java.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FamilyInvitation i SET i.status = :newStatus, i.resolvedAt = CURRENT_TIMESTAMP "
            + "WHERE i.id = :id AND i.status = com.vidacotidiana.family.domain.FamilyInvitationStatus.PENDING")
    int resolveIfPending(@Param("id") UUID id, @Param("newStatus") FamilyInvitationStatus newStatus);
}
