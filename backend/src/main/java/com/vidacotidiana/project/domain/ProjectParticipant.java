package com.vidacotidiana.project.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Una persona participando en un proyecto, con su rol (V32).
 *
 * Sin {@code @Version}: una participación no se edita, se crea o se quita.
 * Cambiar el rol de alguien es borrar y volver a añadir, así que no hay
 * ninguna ventana de edición concurrente que proteger — al contrario que en
 * Proyecto o Persona, donde dos pantallas pueden estar editando el mismo
 * registro a la vez.
 *
 * Guarda ids y no entidades, igual que el enlace garantía↔artículo: Proyecto
 * y Persona son agregados distintos y esto es una referencia entre ellos, no
 * una composición.
 */
@Entity
@Table(name = "project_participants")
public class ProjectParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Redundante con el dueño del proyecto, y a propósito: permite responder
        "¿en qué proyectos participa esta persona?" sin unir con `projects`. */
    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "person_id", nullable = false)
    private UUID personId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ProjectParticipant() {
        // JPA
    }

    public ProjectParticipant(UUID ownerUserId, UUID projectId, UUID personId, ParticipantRole role) {
        this.ownerUserId = ownerUserId;
        this.projectId = projectId;
        this.personId = personId;
        this.role = (role != null) ? role : ParticipantRole.OTRO;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerUserId() {
        return ownerUserId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public ParticipantRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isOwnedBy(UUID userId) {
        return this.ownerUserId.equals(userId);
    }
}
