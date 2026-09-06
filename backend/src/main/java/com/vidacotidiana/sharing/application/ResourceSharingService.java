package com.vidacotidiana.sharing.application;

import com.vidacotidiana.audit.application.AuditEventService;
import com.vidacotidiana.audit.domain.AuditEventType;
import com.vidacotidiana.audit.domain.AuditTargetType;
import com.vidacotidiana.family.application.FamilyService;
import com.vidacotidiana.notification.application.PushEvent;
import com.vidacotidiana.notification.application.PushEventType;
import com.vidacotidiana.notification.application.PushNotificationSender;
import com.vidacotidiana.shared.domain.ConflictException;
import com.vidacotidiana.shared.domain.NotFoundException;
import com.vidacotidiana.shared.domain.ValidationException;
import com.vidacotidiana.sharing.domain.ResourceShare;
import com.vidacotidiana.sharing.domain.ResourceShareRepository;
import com.vidacotidiana.sharing.domain.ResourceShareStatus;
import com.vidacotidiana.sharing.domain.SharedResourceType;
import com.vidacotidiana.user.domain.User;
import com.vidacotidiana.user.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Compartidos (ADR-025 §2 y §3): compartir un recurso existente con la
 * familia y seguir la responsabilidad sobre él.
 *
 * PRINCIPIO: no se duplica nada. Esta clase escribe únicamente en
 * {@code resource_shares}; los seis módulos de recursos se leen a través de
 * {@link SharedResourceCatalog} y ninguno cambia de comportamiento. Si mañana
 * se borrara esta tabla entera, cada módulo seguiría funcionando igual que
 * antes de ADR-025.
 */
@Service
public class ResourceSharingService {

    private static final Logger log = LoggerFactory.getLogger(ResourceSharingService.class);

    private final ResourceShareRepository shareRepository;
    private final SharedResourceCatalog catalog;
    private final FamilyService familyService;
    private final UserRepository userRepository;
    private final PushNotificationSender pushNotificationSender;
    private final AuditEventService auditEventService;

    public ResourceSharingService(ResourceShareRepository shareRepository, SharedResourceCatalog catalog,
                                   FamilyService familyService, UserRepository userRepository,
                                   PushNotificationSender pushNotificationSender, AuditEventService auditEventService) {
        this.shareRepository = shareRepository;
        this.catalog = catalog;
        this.familyService = familyService;
        this.userRepository = userRepository;
        this.pushNotificationSender = pushNotificationSender;
        this.auditEventService = auditEventService;
    }

    // ---------------------------------------------------------------------
    // Compartir
    // ---------------------------------------------------------------------

    /**
     * Comparte un recurso propio con un integrante de la familia.
     *
     * Tres comprobaciones, en este orden y por este motivo:
     *   1. el recurso existe y es de quien llama — nadie comparte lo ajeno;
     *   2. el destinatario está en su familia — Compartidos es colaboración
     *      familiar, no un canal abierto a cualquier cuenta de la instancia;
     *   3. la responsabilidad tiene sentido para ese tipo de recurso.
     */
    @Transactional
    public ResourceShare share(SharedResourceType type, UUID resourceId, UUID callerUserId,
                                UUID collaboratorUserId, boolean responsibility) {
        SharedResourceCatalog.ResourceRef ref = ownedOrThrow(type, resourceId, callerUserId);

        if (!familyService.familyUserIds(callerUserId).contains(collaboratorUserId)) {
            throw new ValidationException("Solo puedes compartir con integrantes de tu familia.");
        }
        if (responsibility && !type.supportsResponsibility()) {
            throw new ValidationException(
                    "Este tipo de recurso se comparte para consultarlo; no admite una parte que hacer.");
        }
        if (shareRepository.existsByResourceTypeAndResourceIdAndCollaboratorUserIdAndStatus(
                type, resourceId, collaboratorUserId, ResourceShareStatus.ACTIVE)) {
            throw new ConflictException("RESOURCE_ALREADY_SHARED", "Ya compartiste esto con esa persona.");
        }

        ResourceShare share = new ResourceShare(type, resourceId, callerUserId, collaboratorUserId, responsibility);
        try {
            share = shareRepository.save(share);
        } catch (DataIntegrityViolationException raceLostToConcurrentInsert) {
            // La garantía real es el índice parcial uq_resource_shares_active (V29);
            // esto solo convierte la carrera en el mismo 409 que el camino normal.
            throw new ConflictException("RESOURCE_ALREADY_SHARED", "Ya compartiste esto con esa persona.");
        }

        log.info("Resource shared: shareId={}, type={}, ownerUserId={}, responsibility={}",
                share.getId(), type, callerUserId, responsibility);
        auditEventService.record(AuditEventType.RESOURCE_SHARED, callerUserId,
                AuditTargetType.RESOURCE_SHARE, share.getId());
        pushNotificationSender.sendBestEffort(collaboratorUserId, new PushEvent(
                PushEventType.INVITATION_RECEIVED,
                responsibility
                        ? "Te comprometieron con \"" + ref.label() + "\"."
                        : "Compartieron \"" + ref.label() + "\" contigo."));
        return share;
    }

    /** Cambia si el colaborador queda comprometido, sin volver a compartir. */
    @Transactional
    public ResourceShare updateResponsibility(UUID shareId, UUID callerUserId, boolean responsibility) {
        ResourceShare share = ownedShareOrThrow(shareId, callerUserId);
        if (responsibility && !share.getResourceType().supportsResponsibility()) {
            throw new ValidationException(
                    "Este tipo de recurso se comparte para consultarlo; no admite una parte que hacer.");
        }
        share.setResponsibility(responsibility);
        return shareRepository.save(share);
    }

    /**
     * Revoca el acceso. Inmediato, sin ventana de gracia, e idempotente:
     * revocar algo ya revocado devuelve éxito igual (mismo criterio que
     * SharingService#revokeShare).
     */
    @Transactional
    public void revoke(UUID shareId, UUID callerUserId) {
        ResourceShare share = ownedShareOrThrow(shareId, callerUserId);
        if (share.isActive()) {
            share.revoke();
            shareRepository.save(share);
            log.info("Resource share revoked: shareId={}, ownerUserId={}", shareId, callerUserId);
            auditEventService.record(AuditEventType.RESOURCE_SHARE_REVOKED, callerUserId,
                    AuditTargetType.RESOURCE_SHARE, shareId);
            pushNotificationSender.sendBestEffort(share.getCollaboratorUserId(),
                    new PushEvent(PushEventType.REMINDER_SHARE_REVOKED, "Se retiró tu acceso a un recurso compartido."));
        }
    }

    // ---------------------------------------------------------------------
    // Responsabilidad
    // ---------------------------------------------------------------------

    /**
     * "Ya hice mi parte" — solo la persona comprometida puede marcarlo.
     *
     * NO toca el recurso original. El estado de un recordatorio sigue siendo
     * único y global (DEC-001); lo que se registra aquí es que ESTA persona
     * cumplió lo suyo, que es exactamente lo que el dueño necesita ver.
     */
    @Transactional
    public ResourceShare markPartDone(UUID shareId, UUID callerUserId) {
        ResourceShare share = collaboratorShareOrThrow(shareId, callerUserId);
        if (!share.hasResponsibility()) {
            throw new ValidationException("Este recurso se compartió contigo para consultarlo, sin una parte que hacer.");
        }
        share.markPartDone();
        share = shareRepository.save(share);

        auditEventService.record(AuditEventType.RESOURCE_PART_DONE, callerUserId,
                AuditTargetType.RESOURCE_SHARE, shareId);
        String label = catalog.find(share.getResourceType(), share.getResourceId())
                .map(SharedResourceCatalog.ResourceRef::label)
                .orElse("un recurso compartido");
        pushNotificationSender.sendBestEffort(share.getOwnerUserId(),
                new PushEvent(PushEventType.INVITATION_ACCEPTED, "Hicieron su parte de \"" + label + "\"."));
        return share;
    }

    /** Deshace lo anterior. Misma persona, misma comprobación. */
    @Transactional
    public ResourceShare reopenPart(UUID shareId, UUID callerUserId) {
        ResourceShare share = collaboratorShareOrThrow(shareId, callerUserId);
        share.reopenPart();
        share = shareRepository.save(share);
        auditEventService.record(AuditEventType.RESOURCE_PART_REOPENED, callerUserId,
                AuditTargetType.RESOURCE_SHARE, shareId);
        return share;
    }

    // ---------------------------------------------------------------------
    // Consultas
    // ---------------------------------------------------------------------

    /** "Me compartieron": recursos de otros a los que tengo acceso. */
    @Transactional(readOnly = true)
    public List<ShareView> listReceived(UUID callerUserId) {
        return decorate(shareRepository.findByCollaboratorUserIdAndStatusOrderByCreatedAtDesc(
                callerUserId, ResourceShareStatus.ACTIVE), true);
    }

    /** "Yo compartí": recursos míos a los que otros tienen acceso. */
    @Transactional(readOnly = true)
    public List<ShareView> listSent(UUID callerUserId) {
        return decorate(shareRepository.findByOwnerUserIdAndStatusOrderByCreatedAtDesc(
                callerUserId, ResourceShareStatus.ACTIVE), false);
    }

    /** Con quién está compartido un recurso concreto — lo pide su propia ficha. */
    @Transactional(readOnly = true)
    public List<ShareView> listForResource(SharedResourceType type, UUID resourceId, UUID callerUserId) {
        ownedOrThrow(type, resourceId, callerUserId);
        return decorate(shareRepository.findByResourceTypeAndResourceIdAndStatus(
                type, resourceId, ResourceShareStatus.ACTIVE), false);
    }

    /**
     * ¿Puede esta persona ver este recurso por compartición?
     *
     * Pensado para que los módulos lo consulten sin conocer esta tabla. Hoy no
     * lo llama ninguno: ADR-025 no cambia la autorización de ningún módulo
     * —cada uno sigue siendo de su dueño— y la sección Compartidos resuelve la
     * lectura por su propia vía. Se deja porque es la pregunta que cualquier
     * módulo hará el día que quiera abrir su recurso al colaborador, y así la
     * regla vive en un solo sitio en vez de replicarse seis veces.
     */
    @Transactional(readOnly = true)
    public boolean isSharedWith(SharedResourceType type, UUID resourceId, UUID userId) {
        return shareRepository.existsByResourceTypeAndResourceIdAndCollaboratorUserIdAndStatus(
                type, resourceId, userId, ResourceShareStatus.ACTIVE);
    }

    // ---------------------------------------------------------------------

    private List<ShareView> decorate(List<ResourceShare> shares, boolean counterpartIsOwner) {
        if (shares.isEmpty()) {
            return List.of();
        }
        Set<UUID> counterpartIds = new LinkedHashSet<>();
        for (ResourceShare share : shares) {
            counterpartIds.add(counterpartIsOwner ? share.getOwnerUserId() : share.getCollaboratorUserId());
        }
        Map<UUID, User> users = new LinkedHashMap<>();
        for (User user : userRepository.findAllById(new ArrayList<>(counterpartIds))) {
            users.put(user.getId(), user);
        }

        List<ShareView> views = new ArrayList<>(shares.size());
        for (ResourceShare share : shares) {
            UUID counterpartId = counterpartIsOwner ? share.getOwnerUserId() : share.getCollaboratorUserId();
            // Un recurso borrado deja su fila de compartición huérfana: se
            // omite en vez de pintar una fila sin nombre. No se borra aquí —
            // limpiar datos no es trabajo de una consulta de lectura.
            catalog.find(share.getResourceType(), share.getResourceId())
                    .ifPresent(ref -> views.add(new ShareView(share, ref, users.get(counterpartId))));
        }
        return views;
    }

    private SharedResourceCatalog.ResourceRef ownedOrThrow(SharedResourceType type, UUID resourceId, UUID callerUserId) {
        SharedResourceCatalog.ResourceRef ref = catalog.find(type, resourceId)
                .orElseThrow(() -> new NotFoundException("RESOURCE_NOT_FOUND", "No se encontró ese recurso."));
        if (!ref.ownerUserId().equals(callerUserId)) {
            // 404 y no 403: el mismo criterio que el resto del dominio — a quien
            // no es dueño no se le confirma que el recurso existe.
            throw new NotFoundException("RESOURCE_NOT_FOUND", "No se encontró ese recurso.");
        }
        return ref;
    }

    private ResourceShare ownedShareOrThrow(UUID shareId, UUID callerUserId) {
        return shareRepository.findById(shareId)
                .filter(share -> share.getOwnerUserId().equals(callerUserId))
                .orElseThrow(() -> new NotFoundException("RESOURCE_SHARE_NOT_FOUND", "No se encontró esa compartición."));
    }

    private ResourceShare collaboratorShareOrThrow(UUID shareId, UUID callerUserId) {
        return shareRepository.findById(shareId)
                .filter(share -> share.getCollaboratorUserId().equals(callerUserId) && share.isActive())
                .orElseThrow(() -> new NotFoundException("RESOURCE_SHARE_NOT_FOUND", "No se encontró esa compartición."));
    }

    /** Una fila de la sección Compartidos: la relación, el recurso y la otra persona. */
    public record ShareView(ResourceShare share, SharedResourceCatalog.ResourceRef resource, User counterpart) {
    }
}
