-- V21__adr016_routines.sql
-- ADR-016 adenda Fase 3e2 (Documentacion/22-decision-log.md): Rutinas.
-- Ver Documentacion/09-data-model.md §"V4 candidato — Fase 3e", FR-032,
-- UC-25, AC-019.
--
-- DECISIÓN EXPLÍCITA DEL PRODUCT OWNER (FR-032): una Rutina NO genera
-- automáticamente REMINDER ni COMMITMENT. No hay job, worker ni trigger —
-- por eso esta tabla no tiene ninguna FK hacia reminders/commitments. Esto
-- mantiene 3e2 deliberadamente separada de 3d (Automatizaciones simples),
-- que sigue BLOCKED.
--
-- `completed` NO existe a propósito (FR-032): una misma Rutina se completa
-- repetidamente; su estado permanente es `active`, y la ejecución actual se
-- representa avanzando `next_execution_date`.

CREATE TABLE routines (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id       UUID NOT NULL REFERENCES users (id),
    title               VARCHAR(200) NOT NULL,
    description         VARCHAR(2000),
    frequency           VARCHAR(16) NOT NULL,
    next_execution_date TIMESTAMPTZ NOT NULL,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    version             INTEGER NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_routines_frequency CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY'))
);

CREATE INDEX ix_routines_owner_user_id ON routines (owner_user_id);
