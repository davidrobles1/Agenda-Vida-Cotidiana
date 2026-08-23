-- V7__notes_and_reminder_icon_sticker.sql
-- Alcance: llevar Agenda (Reminder) y Notas a funcionalidad completa
-- (icono/sticker en Reminder; entidad Note real persistida, antes 100%
-- local vía useState en el frontend).
--
-- REMINDER.icon_id / REMINDER.sticker_id: nullable, sin backfill — a
-- diferencia de V6 (REMINDER.context, que se volvió NOT NULL y necesitó
-- backfill), estos campos son opcionales desde el día uno: una tarea
-- existente simplemente no tiene icono/sticker todavía, no hay un valor
-- "correcto" que asignarle retroactivamente. Guardan el `id` estable del
-- catálogo (ver web/src/core/ui/pickers/pickerCatalog.ts), nunca una ruta
-- de asset ni el propio emoji — la resolución id -> asset vive enteramente
-- en el frontend, igual que ya se documentó para el catálogo de stickers.
--
-- NOTES: mismo patrón exacto que "warranties"/"maintenance_records"
-- (V5__warranties_maintenance.sql) — dueño por FK a users, sin ON DELETE
-- CASCADE en owner_user_id (mismo comportamiento que el resto de las
-- entidades owner-scoped), bloqueo optimista con columna version. Sin
-- concepto de "status" (una Nota no tiene estados temporales como
-- Warranty/Reminder) y sin concepto de compartición (owner-only, mismo
-- criterio que Warranty — no existe UI de compartir notas).

ALTER TABLE reminders
    ADD COLUMN icon_id VARCHAR(32),
    ADD COLUMN sticker_id VARCHAR(32);

CREATE TABLE notes (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users (id),
    title         VARCHAR(200) NOT NULL,
    description   VARCHAR(2000) NOT NULL DEFAULT '',
    icon_id       VARCHAR(32),
    sticker_id    VARCHAR(32),
    version       INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_notes_owner_user_id ON notes (owner_user_id);
