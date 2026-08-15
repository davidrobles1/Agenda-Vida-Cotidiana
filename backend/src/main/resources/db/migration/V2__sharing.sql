-- V2__sharing.sql
-- Alcance: entidades del módulo sharing (BE-016) — invitaciones y
-- colaboraciones sobre un recordatorio. Ver Documentacion/09-data-model.md
-- y docs/development/01-technical-backlog.md (BE-016..022).

CREATE TABLE invitations (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reminder_id      UUID NOT NULL REFERENCES reminders (id) ON DELETE CASCADE,
    inviter_user_id  UUID NOT NULL REFERENCES users (id),
    invited_email    VARCHAR(320) NOT NULL,
    invited_user_id  UUID REFERENCES users (id),
    status           VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    expires_at       TIMESTAMPTZ NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at      TIMESTAMPTZ,
    CONSTRAINT ck_invitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'CANCELLED'))
);

CREATE TABLE reminder_shares (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reminder_id           UUID NOT NULL REFERENCES reminders (id) ON DELETE CASCADE,
    collaborator_user_id  UUID NOT NULL REFERENCES users (id),
    invitation_id         UUID NOT NULL REFERENCES invitations (id),
    status                VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at            TIMESTAMPTZ,
    CONSTRAINT ck_reminder_shares_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT uq_reminder_shares_reminder_collaborator UNIQUE (reminder_id, collaborator_user_id)
);

-- AC-007/09-data-model.md: respalda el 409 de invitación duplicada — solo
-- puede existir una invitación PENDING por (reminder_id, invited_email).
CREATE UNIQUE INDEX uq_invitations_pending_reminder_email
    ON invitations (reminder_id, invited_email)
    WHERE status = 'PENDING';

CREATE INDEX ix_invitations_invited_email ON invitations (invited_email);
CREATE INDEX ix_invitations_status_expires_at ON invitations (status, expires_at);
CREATE INDEX ix_reminder_shares_collaborator_user_id ON reminder_shares (collaborator_user_id);
