-- V6__adr015_context_modes.sql
-- ADR-015: USER.personal_enabled/laboral_enabled, REMINDER.context.
-- Nota de numeración: V5 está reservada para otro trabajo en curso en
-- paralelo (Garantías/Mantenimiento, BE-037) — esta migración usa V6
-- deliberadamente para no colisionar al integrar ambas ramas.
--
-- DECISION (Product Owner, 2026-08-18, ver docs/development/01-technical-backlog.md
-- BE-038): la migración de backfill de REMINDER.context sobre datos ya
-- existentes fue un TBD explícito, resuelto por el Product Owner, no
-- asumido — recordatorios existentes pasan a 'PERSONAL', y en la misma
-- migración se hace "grandfather" de personal_enabled = true para todo
-- USER preexistente (antes de este ADR la app era de un solo contexto,
-- implícitamente personal — se reconoce eso explícitamente en el dato en
-- vez de dejar una inconsistencia: un usuario con recordatorios PERSONAL
-- pero sin "Personal" habilitado en su propio selector de navegación).

ALTER TABLE users
    ADD COLUMN personal_enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN laboral_enabled BOOLEAN NOT NULL DEFAULT false;

UPDATE users SET personal_enabled = true;

ALTER TABLE reminders
    ADD COLUMN context VARCHAR(16);

UPDATE reminders SET context = 'PERSONAL';

ALTER TABLE reminders
    ALTER COLUMN context SET NOT NULL,
    ADD CONSTRAINT ck_reminders_context CHECK (context IN ('PERSONAL', 'LABORAL'));
