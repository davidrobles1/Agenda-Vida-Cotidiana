-- V15__subscriptions.sql
-- Módulo Suscripciones (pedido explícito del usuario, 2026-08-22): "el
-- modal registrará Servicio, compañía, plan contratado, qué día se tiene
-- que pagar, si los pagos son mensuales/semanales/anuales." Nota
-- deliberada: el usuario NO pidió un campo de precio/monto en esta
-- especificación (a diferencia del mock anterior, MockSubscription#price)
-- — no se agrega uno; CLAUDE.md excluye explícitamente Finanzas de V1, y
-- un campo de monto ahí empujaría justo hacia ese territorio sin haber
-- sido pedido.
--
-- "Qué día se tiene que pagar" se modela como una fecha concreta
-- (next_payment_date), no un número de día abstracto (1-31 no tiene
-- sentido para "semanal", y un nombre de día de semana no lo tiene para
-- "anual") — mismo patrón que warranties.expires_at /
-- maintenance_records.next_due_at ya usan para "la próxima vez que esto
-- importa." billing_cycle es el campo separado que sí guarda
-- mensual/semanal/anual.

CREATE TABLE subscriptions (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id      UUID NOT NULL REFERENCES users (id),
    service            VARCHAR(200) NOT NULL,
    company            VARCHAR(200),
    plan               VARCHAR(200),
    next_payment_date  TIMESTAMPTZ NOT NULL,
    billing_cycle      VARCHAR(16) NOT NULL,
    version            INTEGER NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_subscriptions_billing_cycle CHECK (billing_cycle IN ('WEEKLY', 'MONTHLY', 'YEARLY'))
);

CREATE INDEX ix_subscriptions_owner_user_id ON subscriptions (owner_user_id);
