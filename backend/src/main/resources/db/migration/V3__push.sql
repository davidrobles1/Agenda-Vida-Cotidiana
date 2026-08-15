-- V3__push.sql
-- Alcance: DEVICE_PUSH_TOKEN (BE-023) — registro de dispositivos para
-- notificaciones push. Ver Documentacion/09-data-model.md.

CREATE TABLE device_push_tokens (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES users (id),
    platform      VARCHAR(32) NOT NULL,
    token         VARCHAR NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_device_push_tokens_platform CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    CONSTRAINT uq_device_push_tokens_token UNIQUE (token)
);

CREATE INDEX ix_device_push_tokens_user_id ON device_push_tokens (user_id);
