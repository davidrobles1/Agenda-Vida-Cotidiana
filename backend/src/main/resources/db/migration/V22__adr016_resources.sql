-- V22__adr016_resources.sql
-- ADR-016 adenda Fase 3e4 (Documentacion/22-decision-log.md): Recursos.
-- Ver Documentacion/09-data-model.md §"V4 candidato — Fase 3e", FR-034,
-- UC-27, AC-021.
--
-- DECISION del Product Owner (2026-08-28, opción A): `reference` es un ÚNICO
-- campo de texto libre — no se separa en `url` + `reference`. Razón: varios
-- de los tipos aprobados (MANUAL, PLANTILLA, HERRAMIENTA) no siempre tienen
-- una URL, y separarlos los dejaría con un campo vacío permanente.
--
-- Un RESOURCE NO almacena archivos: solo metadatos y una referencia de
-- texto. `DOCUMENT` (FR-030) sigue siendo la única entidad responsable de
-- documentos reales — sin versionado, sin permisos compartidos, sin storage
-- nuevo (todo eso fuera de alcance explícito de FR-034).

CREATE TABLE resources (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users (id),
    name          VARCHAR(200) NOT NULL,
    type          VARCHAR(16) NOT NULL,
    reference     VARCHAR(2000),
    description   VARCHAR(2000),
    -- Vínculos opcionales, mismo patrón que notes/documents (V16/V18).
    person_id     UUID REFERENCES people (id),
    project_id    UUID REFERENCES projects (id),
    version       INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_resources_type CHECK (type IN ('DOCUMENTO', 'ENLACE', 'PLANTILLA', 'MANUAL', 'HERRAMIENTA', 'OTRO'))
);

CREATE INDEX ix_resources_owner_user_id ON resources (owner_user_id);
CREATE INDEX ix_resources_person_id ON resources (person_id);
CREATE INDEX ix_resources_project_id ON resources (project_id);
