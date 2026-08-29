-- ADR-019: aislamiento de recursos por módulo (Personal / Laboral).
--
-- Pedido explícito del usuario (2026-08-28): un recurso pertenece al módulo
-- desde el que se creó y no debe aparecer ni afectar al otro, "también en la
-- lógica de negocio y en las consultas", no solo en la vista.
--
-- REGLA 4 DEL PEDIDO, literal: "todos los recursos que actualmente existen
-- deben considerarse pertenecientes al módulo Personal. No deben migrarse ni
-- reinterpretarse como recursos de Laboral". Por eso la columna entra con
-- DEFAULT 'PERSONAL' y NOT NULL: las filas existentes quedan en Personal sin
-- ambigüedad y sin que haya que adivinar nada.
--
-- Solo se tocan los cuatro recursos que alimentan el Calendario. NOTE
-- (features/calendar/notes) queda fuera a propósito: pese al nombre de su
-- carpeta, no lo consume el calendario sino Personas/Proyectos.

ALTER TABLE warranties
    ADD COLUMN context VARCHAR(16) NOT NULL DEFAULT 'PERSONAL';

ALTER TABLE maintenance_records
    ADD COLUMN context VARCHAR(16) NOT NULL DEFAULT 'PERSONAL';

ALTER TABLE subscriptions
    ADD COLUMN context VARCHAR(16) NOT NULL DEFAULT 'PERSONAL';

ALTER TABLE day_note_elements
    ADD COLUMN context VARCHAR(16) NOT NULL DEFAULT 'PERSONAL';

-- `reminders.context` ya existía (ADR-015) pero admitía NULL, y un NULL se
-- colaba en los DOS módulos: el filtro del cliente dejaba pasar los
-- recordatorios sin contexto en Personal y en Laboral. Eso es exactamente la
-- fuga que la regla 2 prohíbe, así que se cierra aquí.
UPDATE reminders SET context = 'PERSONAL' WHERE context IS NULL;

-- Índices por (dueño, contexto): toda consulta del calendario filtra ahora
-- por ambas columnas a la vez.
CREATE INDEX IF NOT EXISTS idx_warranties_owner_context ON warranties (owner_user_id, context);
CREATE INDEX IF NOT EXISTS idx_maintenance_records_owner_context ON maintenance_records (owner_user_id, context);
CREATE INDEX IF NOT EXISTS idx_subscriptions_owner_context ON subscriptions (owner_user_id, context);
CREATE INDEX IF NOT EXISTS idx_day_note_elements_owner_context ON day_note_elements (owner_user_id, context);
CREATE INDEX IF NOT EXISTS idx_reminders_owner_context ON reminders (owner_user_id, context);

COMMENT ON COLUMN warranties.context IS 'ADR-019: módulo propietario (PERSONAL/LABORAL). Fijado al crear, inmutable.';
COMMENT ON COLUMN maintenance_records.context IS 'ADR-019: módulo propietario (PERSONAL/LABORAL). Fijado al crear, inmutable.';
COMMENT ON COLUMN subscriptions.context IS 'ADR-019: módulo propietario (PERSONAL/LABORAL). Fijado al crear, inmutable.';
COMMENT ON COLUMN day_note_elements.context IS 'ADR-019: módulo propietario (PERSONAL/LABORAL). Fijado al crear, inmutable.';
