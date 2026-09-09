package com.vidacotidiana.project.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * V32. Las dos consultas son las dos preguntas de negocio: "¿quién está en
 * este proyecto?" y "¿en qué proyectos participa esta persona?".
 *
 * Sin paginar: un proyecto tiene un puñado de participantes, no cientos. El
 * día que eso deje de ser cierto, se pagina — antes sería inventar un problema.
 */
public interface ProjectParticipantRepository extends JpaRepository<ProjectParticipant, UUID> {

    List<ProjectParticipant> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    List<ProjectParticipant> findByOwnerUserId(UUID ownerUserId);

    boolean existsByProjectIdAndPersonId(UUID projectId, UUID personId);
}
