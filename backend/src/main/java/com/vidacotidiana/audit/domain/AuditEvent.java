package com.vidacotidiana.audit.domain;

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
 * Maps to the AUDIT_EVENT entity added in Documentacion/09-data-model.md
 * (RECOMMENDATION técnica, BE-029) — the minimal schema to satisfy
 * 11-auth-security.md §Auditoría literally. Deliberately has no free-text/
 * JSON detail column (never a place to accidentally store an email or a
 * token). Written in the same transaction as the business operation it
 * audits, never best-effort like push.
 */
@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private AuditEventType eventType;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private AuditTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditEvent() {
        // JPA
    }

    /** actorUserId may be null — a system job (e.g. invitation expiration) has no human actor. */
    public AuditEvent(AuditEventType eventType, UUID actorUserId, AuditTargetType targetType, UUID targetId) {
        this.eventType = eventType;
        this.actorUserId = actorUserId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.occurredAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public AuditTargetType getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
