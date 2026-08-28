-- V19__adr016_objectives.sql
-- ADR-016 adenda Fase 3e1 (Documentacion/22-decision-log.md): Objetivos.
-- Ver Documentacion/09-data-model.md §"V4 candidato — Fase 3e", FR-031,
-- UC-24, AC-018.
--
-- Tabla nueva e independiente: sin FK a projects/people en este incremento
-- (FUERA DE ALCANCE explícito de FR-031). Ninguna tabla existente cambia —
-- sin backfill, sin migración de datos.

CREATE TABLE objectives (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users (id),
    title         VARCHAR(200) NOT NULL,
    -- Progreso 100% manual (FR-031): el usuario actualiza current_value a
    -- mano. No hay cálculo automático ni derivación desde Proyectos/Tareas.
    target_value  INTEGER,
    current_value INTEGER NOT NULL DEFAULT 0,
    deadline      TIMESTAMPTZ,
    -- Marcado explícitamente por el usuario (AC-018): nunca derivado de
    -- current_value >= target_value.
    completed     BOOLEAN NOT NULL DEFAULT FALSE,
    version       INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_objectives_owner_user_id ON objectives (owner_user_id);
