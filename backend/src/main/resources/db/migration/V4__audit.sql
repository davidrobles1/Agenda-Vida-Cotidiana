-- V4__audit.sql
-- Alcance: AUDIT_EVENT (BE-029) — el schema mínimo para cumplir
-- literalmente 11-auth-security.md §Auditoría. Añadido como
-- RECOMMENDATION técnica explícita, ver Documentacion/09-data-model.md.
-- Deliberadamente sin columna de detalle libre/JSON.

CREATE TABLE audit_events (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type     VARCHAR(64) NOT NULL,
    actor_user_id  UUID REFERENCES users (id),
    target_type    VARCHAR(32) NOT NULL,
    target_id      UUID NOT NULL,
    occurred_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_audit_events_event_type CHECK (event_type IN (
        'INVITATION_CREATED', 'INVITATION_CANCELLED', 'INVITATION_ACCEPTED',
        'INVITATION_REJECTED', 'INVITATION_EXPIRED', 'SHARE_REVOKED'
    )),
    CONSTRAINT ck_audit_events_target_type CHECK (target_type IN ('REMINDER', 'INVITATION', 'REMINDER_SHARE'))
);

CREATE INDEX ix_audit_events_target ON audit_events (target_type, target_id);
CREATE INDEX ix_audit_events_occurred_at ON audit_events (occurred_at);
