-- V11__adr016_laboral_module.sql
-- ADR-016 (Documentacion/22-decision-log.md): Módulo Laboral — Persona,
-- Proyecto, Compromiso, y extensión de reminders con person_id/project_id/
-- location. Ver Documentacion/09-data-model.md §"V3 — Módulo Laboral".
--
-- Nota de numeración: 34-laboral-module-proposal.md/09-data-model.md
-- mencionaban tentativamente "V7" como nombre de migración antes de que
-- este módulo se implementara — V7 a V10 ya estaban tomadas por trabajo
-- posterior (notes, vision boards) para cuando esto se ejecutó. Se usa V11,
-- el siguiente número real disponible.
--
-- Todas las columnas nuevas sobre `reminders` son nullable y aditivas — sin
-- backfill necesario, a diferencia de V6 (ADR-015), que sí requirió resolver
-- datos preexistentes.

CREATE TABLE people (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users (id),
    name          VARCHAR(200) NOT NULL,
    role          VARCHAR(200),
    organization  VARCHAR(200),
    version       INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_people_owner_user_id ON people (owner_user_id);

CREATE TABLE projects (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id    UUID NOT NULL REFERENCES users (id),
    name             VARCHAR(200) NOT NULL,
    client_person_id UUID REFERENCES people (id),
    status           VARCHAR(100),
    deadline         TIMESTAMPTZ,
    version          INTEGER NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_projects_owner_user_id ON projects (owner_user_id);
CREATE INDEX ix_projects_client_person_id ON projects (client_person_id);

-- ASSUMPTION (no decidida por el Product Owner — TBD explícito en ADR-016):
-- person_id se implementa NOT NULL para V3. ADR-016 dejó abierto si un
-- Compromiso puede existir sin una Persona asociada; se eligió NOT NULL por
-- ser el caso que cubren todos los ejemplos documentados (34-laboral-module-
-- proposal.md) y por ser el cambio más simple de relajar después (pasar a
-- nullable no rompe ningún consumidor existente) frente a lo contrario.
CREATE TABLE commitments (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id      UUID NOT NULL REFERENCES users (id),
    person_id          UUID NOT NULL REFERENCES people (id),
    project_id         UUID REFERENCES projects (id),
    description        VARCHAR(2000) NOT NULL,
    direction          VARCHAR(16) NOT NULL,
    due_at             TIMESTAMPTZ NOT NULL,
    status             VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    origin_reminder_id UUID REFERENCES reminders (id),
    version            INTEGER NOT NULL DEFAULT 0,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_commitments_direction CHECK (direction IN ('MINE', 'THEIRS')),
    CONSTRAINT ck_commitments_status CHECK (status IN ('OPEN', 'DONE'))
);

CREATE INDEX ix_commitments_owner_user_id ON commitments (owner_user_id);
CREATE INDEX ix_commitments_person_id ON commitments (person_id);
CREATE INDEX ix_commitments_project_id ON commitments (project_id);

-- FR-023/FR-024: vínculo opcional de Tarea (reminder) con Persona/Proyecto,
-- y ubicación para el caso de uso "reunión". Todas nullable — ningún
-- reminder preexistente cambia de valor.
ALTER TABLE reminders
    ADD COLUMN person_id UUID REFERENCES people (id),
    ADD COLUMN project_id UUID REFERENCES projects (id),
    ADD COLUMN location VARCHAR(500);

CREATE INDEX ix_reminders_person_id ON reminders (person_id);
CREATE INDEX ix_reminders_project_id ON reminders (project_id);
