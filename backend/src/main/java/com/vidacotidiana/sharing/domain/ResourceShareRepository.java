package com.vidacotidiana.sharing.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceShareRepository extends JpaRepository<ResourceShare, UUID> {

    /** "Yo compartí" — la mitad emisora de la sección Compartidos. */
    List<ResourceShare> findByOwnerUserIdAndStatusOrderByCreatedAtDesc(UUID ownerUserId, ResourceShareStatus status);

    /** "Me compartieron" — la mitad receptora. */
    List<ResourceShare> findByCollaboratorUserIdAndStatusOrderByCreatedAtDesc(UUID collaboratorUserId, ResourceShareStatus status);

    /** Con quién está compartido un recurso concreto: lo usa su propia ficha al editarlo. */
    List<ResourceShare> findByResourceTypeAndResourceIdAndStatus(
            SharedResourceType resourceType, UUID resourceId, ResourceShareStatus status);

    Optional<ResourceShare> findByResourceTypeAndResourceIdAndCollaboratorUserIdAndStatus(
            SharedResourceType resourceType, UUID resourceId, UUID collaboratorUserId, ResourceShareStatus status);

    /** Comprobación de acceso: ¿este recurso está compartido conmigo ahora mismo? */
    boolean existsByResourceTypeAndResourceIdAndCollaboratorUserIdAndStatus(
            SharedResourceType resourceType, UUID resourceId, UUID collaboratorUserId, ResourceShareStatus status);
}
