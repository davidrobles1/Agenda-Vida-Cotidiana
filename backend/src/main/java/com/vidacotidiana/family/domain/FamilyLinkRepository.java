package com.vidacotidiana.family.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FamilyLinkRepository extends JpaRepository<FamilyLink, UUID> {

    List<FamilyLink> findByUserIdOrderByCreatedAtAsc(UUID userId);

    boolean existsByUserIdAndRelativeUserId(UUID userId, UUID relativeUserId);

    /** Al deshacer un vínculo hay que borrar las DOS filas, no solo la del que pulsa. */
    void deleteByUserIdAndRelativeUserId(UUID userId, UUID relativeUserId);
}
