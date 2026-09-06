package com.vidacotidiana.family.application;

import com.vidacotidiana.audit.application.AuditEventService;
import com.vidacotidiana.audit.domain.AuditEventType;
import com.vidacotidiana.audit.domain.AuditTargetType;
import com.vidacotidiana.family.domain.FamilyInvitation;
import com.vidacotidiana.family.domain.FamilyInvitationRepository;
import com.vidacotidiana.family.domain.FamilyInvitationStatus;
import com.vidacotidiana.family.domain.FamilyLink;
import com.vidacotidiana.family.domain.FamilyLinkRepository;
import com.vidacotidiana.notification.application.PushEvent;
import com.vidacotidiana.notification.application.PushEventType;
import com.vidacotidiana.notification.application.PushNotificationSender;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.GoneException;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.ValidationException;
import com.vidacotidiana.user.domain.User;
import com.vidacotidiana.user.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Familia (ADR-025).
 *
 * Reutiliza la infraestructura que ya existe en vez de duplicarla:
 * {@link UserRepository} para resolver personas, {@link AuditEventService}
 * para el registro y {@link PushNotificationSender} para avisar — el mismo
 * trío que usa {@code sharing.application.SharingService}. Ninguna cuenta se
 * crea aquí: Keycloak sigue siendo la única fuente de identidad (DEC-004).
 */
@Service
public class FamilyService {

    private static final Logger log = LoggerFactory.getLogger(FamilyService.class);

    /**
     * Mínimo de caracteres para buscar personas (requisito §1).
     *
     * Se valida AQUÍ además de en la interfaz: el límite existe para no lanzar
     * consultas caras sobre el padrón de usuarios, y una regla que solo vive en
     * el cliente no protege la base de datos de nadie que llame al endpoint
     * directamente.
     */
    public static final int MIN_SEARCH_LENGTH = 5;

    /** Tope duro de resultados: buscar personas no es paginar un listado propio. */
    private static final int SEARCH_LIMIT = 10;

    private final FamilyInvitationRepository invitationRepository;
    private final FamilyLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final PushNotificationSender pushNotificationSender;
    private final AuditEventService auditEventService;

    public FamilyService(FamilyInvitationRepository invitationRepository, FamilyLinkRepository linkRepository,
                          UserRepository userRepository, PushNotificationSender pushNotificationSender,
                          AuditEventService auditEventService) {
        this.invitationRepository = invitationRepository;
        this.linkRepository = linkRepository;
        this.userRepository = userRepository;
        this.pushNotificationSender = pushNotificationSender;
        this.auditEventService = auditEventService;
    }

    // ---------------------------------------------------------------------
    // Búsqueda
    // ---------------------------------------------------------------------

    /**
     * Busca personas por nombre de usuario (requisito §1).
     *
     * Solo por USERNAME, nunca por correo: el correo es un vector de
     * enumeración y SEC-001 prohíbe exponerlo. El nombre de usuario es el
     * identificador que la persona a la que se busca ya compartió.
     *
     * Devuelve el estado de la relación con quien busca —ninguna, invitación
     * enviada, invitación recibida o ya en la familia— para que la interfaz no
     * ofrezca invitar a alguien a quien ya invitó.
     */
    @Transactional(readOnly = true)
    public List<UserSearchResult> searchUsers(String query, UUID callerUserId) {
        String term = query == null ? "" : query.trim();
        if (term.length() < MIN_SEARCH_LENGTH) {
            throw new ValidationException(
                    "La búsqueda necesita al menos " + MIN_SEARCH_LENGTH + " caracteres.");
        }

        Set<UUID> family = linkRepository.findByUserIdOrderByCreatedAtAsc(callerUserId).stream()
                .map(FamilyLink::getRelativeUserId)
                .collect(Collectors.toSet());
        Set<UUID> invited = invitationRepository
                .findByInviterUserIdAndStatusOrderByCreatedAtDesc(callerUserId, FamilyInvitationStatus.PENDING)
                .stream()
                .map(FamilyInvitation::getInvitedUserId)
                .collect(Collectors.toSet());
        Set<UUID> invitedMe = invitationRepository
                .findByInvitedUserIdAndStatusOrderByCreatedAtDesc(callerUserId, FamilyInvitationStatus.PENDING)
                .stream()
                .map(FamilyInvitation::getInviterUserId)
                .collect(Collectors.toSet());

        return userRepository.searchByUsername(term, SEARCH_LIMIT).stream()
                // Nadie se busca a sí mismo, y una cuenta en proceso de borrado
                // no debería poder recibir invitaciones nuevas.
                .filter(user -> !user.getId().equals(callerUserId))
                .filter(user -> "ACTIVE".equals(user.getDeletionStatus()))
                .map(user -> new UserSearchResult(
                        user.getId(),
                        user.getUsername(),
                        relationOf(user.getId(), family, invited, invitedMe)))
                .toList();
    }

    private static RelationState relationOf(UUID userId, Set<UUID> family, Set<UUID> invited, Set<UUID> invitedMe) {
        if (family.contains(userId)) {
            return RelationState.FAMILY;
        }
        if (invited.contains(userId)) {
            return RelationState.INVITATION_SENT;
        }
        if (invitedMe.contains(userId)) {
            return RelationState.INVITATION_RECEIVED;
        }
        return RelationState.NONE;
    }

    // ---------------------------------------------------------------------
    // Invitaciones
    // ---------------------------------------------------------------------

    /** Invita a alguien a formar parte de la familia de quien llama. */
    @Transactional
    public FamilyInvitation invite(UUID callerUserId, UUID invitedUserId) {
        if (callerUserId.equals(invitedUserId)) {
            throw new ValidationException("No puedes invitarte a ti mismo.");
        }
        User invited = userRepository.findById(invitedUserId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "No se encontró a esa persona."));

        if (linkRepository.existsByUserIdAndRelativeUserId(callerUserId, invitedUserId)) {
            throw new ConflictException("ALREADY_FAMILY", "Esa persona ya forma parte de tu familia.");
        }
        if (invitationRepository.findByInviterUserIdAndInvitedUserIdAndStatus(
                callerUserId, invitedUserId, FamilyInvitationStatus.PENDING).isPresent()) {
            throw new ConflictException("FAMILY_INVITATION_ALREADY_PENDING",
                    "Ya enviaste una invitación a esa persona.");
        }
        // Si ya me invitaron, invitar de vuelta crearía dos invitaciones cruzadas
        // que se aceptarían por separado para acabar en el mismo vínculo. Se
        // corta aquí y se le dice a quien llama dónde responder.
        if (invitationRepository.findByInviterUserIdAndInvitedUserIdAndStatus(
                invitedUserId, callerUserId, FamilyInvitationStatus.PENDING).isPresent()) {
            throw new ConflictException("FAMILY_INVITATION_INCOMING",
                    "Esa persona ya te invitó: responde su invitación desde Ajustes.");
        }

        FamilyInvitation invitation = new FamilyInvitation(callerUserId, invitedUserId);
        try {
            invitation = invitationRepository.save(invitation);
        } catch (DataIntegrityViolationException raceLostToConcurrentInsert) {
            // Red de seguridad de la carrera real que cubre el índice parcial
            // uq_family_invitations_pending (V29) — mismo patrón que SharingService.
            throw new ConflictException("FAMILY_INVITATION_ALREADY_PENDING",
                    "Ya enviaste una invitación a esa persona.");
        }

        log.info("Family invitation created: id={}, inviterUserId={}", invitation.getId(), callerUserId);
        auditEventService.record(AuditEventType.FAMILY_INVITATION_CREATED, callerUserId,
                AuditTargetType.FAMILY_INVITATION, invitation.getId());
        pushNotificationSender.sendBestEffort(invited.getId(),
                new PushEvent(PushEventType.INVITATION_RECEIVED, "Te invitaron a formar parte de una familia."));
        return invitation;
    }

    /** Invitaciones pendientes recibidas por quien llama (apartado de Configuración). */
    @Transactional(readOnly = true)
    public List<InvitationView> listReceivedInvitations(UUID callerUserId) {
        List<FamilyInvitation> invitations = invitationRepository
                .findByInvitedUserIdAndStatusOrderByCreatedAtDesc(callerUserId, FamilyInvitationStatus.PENDING);
        Map<UUID, User> byId = usersById(invitations.stream().map(FamilyInvitation::getInviterUserId).toList());
        return invitations.stream()
                .map(invitation -> new InvitationView(invitation,
                        byId.get(invitation.getInviterUserId())))
                .toList();
    }

    /**
     * Acepta una invitación. La transición PENDING -> ACCEPTED es el UPDATE
     * condicional atómico del repositorio: 0 filas significa que ya no estaba
     * pendiente y hay que responder 410, nunca aplicar la aceptación a partir
     * de un estado leído antes.
     */
    @Transactional
    public void acceptInvitation(UUID invitationId, UUID callerUserId) {
        FamilyInvitation invitation = findReceivedOrThrow(invitationId, callerUserId);

        if (invitationRepository.resolveIfPending(invitationId, FamilyInvitationStatus.ACCEPTED) == 0) {
            throw new GoneException("FAMILY_INVITATION_ALREADY_RESOLVED", "Esa invitación ya no está pendiente.");
        }

        // Las DOS filas: la relación es simétrica y "mi familia" se consulta por
        // un solo lado. Insertar una sola dejaría la familia visible para uno y
        // no para el otro.
        linkRepository.save(new FamilyLink(invitation.getInviterUserId(), invitation.getInvitedUserId(), invitationId));
        linkRepository.save(new FamilyLink(invitation.getInvitedUserId(), invitation.getInviterUserId(), invitationId));

        log.info("Family invitation accepted: id={}, invitedUserId={}", invitationId, callerUserId);
        auditEventService.record(AuditEventType.FAMILY_INVITATION_ACCEPTED, callerUserId,
                AuditTargetType.FAMILY_INVITATION, invitationId);
        pushNotificationSender.sendBestEffort(invitation.getInviterUserId(),
                new PushEvent(PushEventType.INVITATION_ACCEPTED, "Aceptaron tu invitación familiar."));
    }

    /** Rechaza una invitación. Nunca crea vínculo. */
    @Transactional
    public void rejectInvitation(UUID invitationId, UUID callerUserId) {
        FamilyInvitation invitation = findReceivedOrThrow(invitationId, callerUserId);

        if (invitationRepository.resolveIfPending(invitationId, FamilyInvitationStatus.REJECTED) == 0) {
            throw new GoneException("FAMILY_INVITATION_ALREADY_RESOLVED", "Esa invitación ya no está pendiente.");
        }

        log.info("Family invitation rejected: id={}, invitedUserId={}", invitationId, callerUserId);
        auditEventService.record(AuditEventType.FAMILY_INVITATION_REJECTED, callerUserId,
                AuditTargetType.FAMILY_INVITATION, invitationId);
        pushNotificationSender.sendBestEffort(invitation.getInviterUserId(),
                new PushEvent(PushEventType.INVITATION_REJECTED, "Rechazaron tu invitación familiar."));
    }

    /** Cancela una invitación que quien llama envió. */
    @Transactional
    public void cancelInvitation(UUID invitationId, UUID callerUserId) {
        FamilyInvitation invitation = invitationRepository.findById(invitationId)
                .filter(i -> i.isInviter(callerUserId))
                .orElseThrow(() -> new NotFoundException("FAMILY_INVITATION_NOT_FOUND", "No se encontró esa invitación."));

        if (invitationRepository.resolveIfPending(invitationId, FamilyInvitationStatus.CANCELLED) == 0) {
            throw new GoneException("FAMILY_INVITATION_ALREADY_RESOLVED", "Esa invitación ya no está pendiente.");
        }

        log.info("Family invitation cancelled: id={}, inviterUserId={}", invitationId, callerUserId);
        auditEventService.record(AuditEventType.FAMILY_INVITATION_CANCELLED, callerUserId,
                AuditTargetType.FAMILY_INVITATION, invitationId);
        pushNotificationSender.sendBestEffort(invitation.getInvitedUserId(),
                new PushEvent(PushEventType.INVITATION_CANCELLED, "Se canceló una invitación familiar."));
    }

    // ---------------------------------------------------------------------
    // Integrantes
    // ---------------------------------------------------------------------

    /** Los integrantes ya aceptados de la familia de quien llama. */
    @Transactional(readOnly = true)
    public List<MemberView> listMembers(UUID callerUserId) {
        List<FamilyLink> links = linkRepository.findByUserIdOrderByCreatedAtAsc(callerUserId);
        Map<UUID, User> byId = usersById(links.stream().map(FamilyLink::getRelativeUserId).toList());
        return links.stream()
                .map(link -> new MemberView(link, byId.get(link.getRelativeUserId())))
                .filter(view -> view.user() != null)
                .toList();
    }

    /** Invitaciones que quien llama envió y siguen sin respuesta. */
    @Transactional(readOnly = true)
    public List<InvitationView> listSentInvitations(UUID callerUserId) {
        List<FamilyInvitation> invitations = invitationRepository
                .findByInviterUserIdAndStatusOrderByCreatedAtDesc(callerUserId, FamilyInvitationStatus.PENDING);
        Map<UUID, User> byId = usersById(invitations.stream().map(FamilyInvitation::getInvitedUserId).toList());
        return invitations.stream()
                .map(invitation -> new InvitationView(invitation, byId.get(invitation.getInvitedUserId())))
                .toList();
    }

    /**
     * Deshace un vínculo familiar. Borra las dos filas: la relación es
     * simétrica y dejar media relación viva sería un estado imposible de leer.
     *
     * NO toca los recursos ya compartidos con esa persona. Quitarlos sería una
     * decisión de producto que nadie ha tomado, y borrar en cascada accesos que
     * el dueño concedió a propósito es justo el tipo de efecto colateral que no
     * debe ocurrir sin pedirlo. El dueño los revoca desde Compartidos.
     */
    @Transactional
    public void removeMember(UUID callerUserId, UUID relativeUserId) {
        if (!linkRepository.existsByUserIdAndRelativeUserId(callerUserId, relativeUserId)) {
            throw new NotFoundException("FAMILY_LINK_NOT_FOUND", "Esa persona no forma parte de tu familia.");
        }
        linkRepository.deleteByUserIdAndRelativeUserId(callerUserId, relativeUserId);
        linkRepository.deleteByUserIdAndRelativeUserId(relativeUserId, callerUserId);

        log.info("Family link removed: userId={}, relativeUserId={}", callerUserId, relativeUserId);
        auditEventService.record(AuditEventType.FAMILY_LINK_REMOVED, callerUserId,
                AuditTargetType.FAMILY_LINK, relativeUserId);
    }

    /**
     * Los ids de la familia de una persona. Lo consume
     * {@code sharing.application.ResourceSharingService} para comprobar que
     * solo se comparte con familia — la regla vive aquí, en un solo sitio.
     */
    @Transactional(readOnly = true)
    public Set<UUID> familyUserIds(UUID userId) {
        return linkRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(FamilyLink::getRelativeUserId)
                .collect(Collectors.toSet());
    }

    // ---------------------------------------------------------------------

    private FamilyInvitation findReceivedOrThrow(UUID invitationId, UUID callerUserId) {
        return invitationRepository.findById(invitationId)
                .filter(i -> i.isInvitedUser(callerUserId))
                .orElseThrow(() -> new NotFoundException("FAMILY_INVITATION_NOT_FOUND", "No se encontró esa invitación."));
    }

    /** Una sola consulta para resolver todos los nombres — evita el N+1 del listado. */
    private Map<UUID, User> usersById(List<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        List<UUID> distinct = new ArrayList<>(new java.util.LinkedHashSet<>(ids));
        Map<UUID, User> byId = new LinkedHashMap<>();
        for (User user : userRepository.findAllById(distinct)) {
            byId.put(user.getId(), user);
        }
        return byId;
    }

    /** Estado de la relación entre quien busca y el resultado encontrado. */
    public enum RelationState {
        NONE,
        INVITATION_SENT,
        INVITATION_RECEIVED,
        FAMILY
    }

    public record UserSearchResult(UUID userId, String username, RelationState relation) {
    }

    public record MemberView(FamilyLink link, User user) {
    }

    public record InvitationView(FamilyInvitation invitation, User counterpart) {
    }

    /** Orden estable por nombre para las vistas que lo necesiten. */
    public static final Comparator<MemberView> BY_USERNAME =
            Comparator.comparing(view -> view.user().getUsername(), Comparator.nullsLast(String::compareToIgnoreCase));
}
