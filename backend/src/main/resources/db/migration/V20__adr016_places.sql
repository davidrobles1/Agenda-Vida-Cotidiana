-- V20__adr016_places.sql
-- ADR-016 adenda Fase 3e3 (Documentacion/22-decision-log.md): Lugares.
-- Ver Documentacion/09-data-model.md §"V4 candidato — Fase 3e", FR-033,
-- UC-26, AC-020.
--
-- Catálogo de ubicaciones reutilizables. Deliberadamente NO se agrega
-- reminders.place_id (FUERA DE ALCANCE explícito de FR-033): elegir un Lugar
-- al crear una Tarea solo copia su texto al campo `reminders.location` ya
-- existente (FR-024). Ninguna tabla existente cambia — sin backfill.

CREATE TABLE places (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users (id),
    name          VARCHAR(200) NOT NULL,
    address       VARCHAR(500),
    -- Opcional: un Lugar puede pertenecer a una Persona (p. ej. "Oficina de
    -- ACME"). Mismo patrón que projects.client_person_id.
    person_id     UUID REFERENCES people (id),
    version       INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_places_owner_user_id ON places (owner_user_id);
CREATE INDEX ix_places_person_id ON places (person_id);
