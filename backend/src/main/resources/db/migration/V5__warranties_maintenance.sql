-- V5__warranties_maintenance.sql
-- Alcance: BE-037/WEB-009 (docs/development/01-technical-backlog.md,
-- docs/development/05-v2-plan.md §3/§4) — backend real para los dos módulos
-- de Calendario que hasta ahora corrían enteramente sobre
-- web/src/core/mock/mockData.ts (UX-006). Ver Documentacion/09-data-model.md
-- para el razonamiento completo de status calculado vs. almacenado.
--
-- Mismo patrón que "reminders" (V1__init_schema.sql): dueño por FK a users,
-- sin ON DELETE CASCADE en owner_user_id (mismo comportamiento que reminders),
-- bloqueo optimista con columna version.
--
-- DECISION técnica (documentada en 09-data-model.md): la columna `status`
-- almacena ÚNICAMENTE si el registro fue completado por el usuario
-- (ACTIVE/COMPLETED) — no los 4 valores temporales que expone la API
-- (VIGENTE/POR_VENCER/VENCIDA/COMPLETADO para warranties;
-- AL_DIA/PROXIMO/VENCIDO/COMPLETADO para mantenimiento). El bucket temporal
-- se deriva en el momento de la respuesta a partir de expires_at/next_due_at
-- comparado con "ahora" (mismo patrón que CalendarPage.tsx#reminderTone en
-- el frontend, que ya deriva "Vencida" de dueAt en vez de almacenarlo). Esto
-- evita que un status temporal almacenado quede obsoleto sin un job
-- periódico que lo recalcule — infraestructura no justificada para V2
-- (CLAUDE.md "no sobrearquitecturar").

CREATE TABLE warranties (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users (id),
    item          VARCHAR(200) NOT NULL,
    expires_at    TIMESTAMPTZ NOT NULL,
    status        VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    version       INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_warranties_status CHECK (status IN ('ACTIVE', 'COMPLETED'))
);

CREATE INDEX ix_warranties_owner_user_id ON warranties (owner_user_id);

CREATE TABLE maintenance_records (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users (id),
    item          VARCHAR(200) NOT NULL,
    next_due_at   TIMESTAMPTZ NOT NULL,
    status        VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    version       INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_maintenance_records_status CHECK (status IN ('ACTIVE', 'COMPLETED'))
);

CREATE INDEX ix_maintenance_records_owner_user_id ON maintenance_records (owner_user_id);
