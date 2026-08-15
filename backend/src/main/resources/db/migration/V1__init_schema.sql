-- V1__init_schema.sql
-- Alcance: únicamente lo necesario para el primer vertical slice
-- (Auth -> User -> Create/List/Complete Reminder). Las tablas de
-- sharing (invitations, reminder_shares) y push (device_push_tokens)
-- se añaden en migraciones posteriores (V2/V3), cuando esos módulos
-- se implementen — ver Documentacion/09-data-model.md y
-- docs/development/01-technical-backlog.md.

-- gen_random_uuid() is a PostgreSQL 13+ built-in (core, no extension
-- required); the target engine for V1 is PostgreSQL 16 (docker-compose.yml).

CREATE TABLE users (
    id                    UUID PRIMARY KEY,
    email                 VARCHAR(320) NOT NULL,
    username              VARCHAR(64),
    status                VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    deletion_status       VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    deletion_requested_at TIMESTAMPTZ,
    purge_at              TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ck_users_deletion_status CHECK (deletion_status IN ('ACTIVE', 'PENDING_DELETION', 'DELETED'))
);

CREATE TABLE reminders (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users (id),
    title         VARCHAR(200) NOT NULL,
    description   VARCHAR(2000),
    due_at        TIMESTAMPTZ,
    status        VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    version       INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_reminders_status CHECK (status IN ('PENDING', 'COMPLETED'))
);

CREATE INDEX ix_reminders_owner_user_id ON reminders (owner_user_id);
