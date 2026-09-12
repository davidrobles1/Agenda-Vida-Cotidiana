-- V36 — Ánimo del día (Bienestar).
--
-- Implementa la especificación aprobada en
-- Documentacion/35-artefacto-maestro-matriz-capacidades.md §4.1, sin añadir ni
-- un campo más. El artefacto maestro sitúa «¿Cómo te sientes hoy?» en Inicio y
-- le da sección propia; no existía nada equivalente en el backend.
--
-- ═══ ESTO ES DATO DE SALUD ═══
--
-- No es una preferencia ni una etiqueta: es información sobre el estado
-- emocional de una persona. Tres consecuencias, y las tres son estructurales,
-- no de interfaz:
--
--   1. NO SE COMPARTE NUNCA. `mood_entry` no entra en `resource_shares` ni en
--      `family_links`. No hay ruta por la que otro usuario pueda leerla — ni
--      siquiera quien comparte tareas contigo.
--   2. EL BORRADO ES DURO. `DELETE /api/v1/moods` elimina las filas, no marca
--      un `deleted_at`. Un borrado suave sobre esto sería conservar justo lo
--      que el usuario pidió que desapareciera.
--   3. NO ENTRA EN EXPORTACIONES DE FAMILIA. Ver
--      Documentacion/05-data/data-classification.md.
--
-- Ninguna de estas tres reglas se inventa aquí: vienen de §4.1.

CREATE TABLE mood_entry (
    id            UUID PRIMARY KEY,
    owner_user_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- DATE y no TIMESTAMPTZ: el ánimo es de un DÍA, no de un instante. Guardar
    -- la hora invitaría a preguntas que el producto no hace («¿a qué hora
    -- estabas mal?») y son exactamente las que no queremos poder responder.
    entry_date    DATE        NOT NULL,
    value         SMALLINT    NOT NULL,
    note          TEXT,
    version       INTEGER     NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Un día, un ánimo. Volver a marcar sustituye; no acumula historial dentro
    -- del mismo día, que sería un registro de altibajos que nadie pidió.
    CONSTRAINT uq_mood_entry_user_date UNIQUE (owner_user_id, entry_date),
    -- 0 genial · 1 bien · 2 normal · 3 regular · 4 bajo. Aquí SÍ hay CHECK,
    -- al contrario que en `priority`: la escala es cerrada por diseño y un
    -- valor fuera de rango no sería una extensión, sería un dato roto.
    CONSTRAINT ck_mood_entry_value CHECK (value BETWEEN 0 AND 4)
);

COMMENT ON TABLE mood_entry IS
    'DATO DE SALUD. Privado por definición: nunca se comparte, nunca se exporta, se borra en duro.';

CREATE INDEX idx_mood_entry_owner_date
    ON mood_entry (owner_user_id, entry_date DESC);

-- Las etiquetas del «¿Qué lo hizo así?». Opcionales, y tan sensibles como el
-- propio ánimo: caen con él por CASCADE.
CREATE TABLE mood_entry_tag (
    mood_entry_id UUID        NOT NULL REFERENCES mood_entry (id) ON DELETE CASCADE,
    tag           VARCHAR(40) NOT NULL,
    PRIMARY KEY (mood_entry_id, tag)
);

COMMENT ON TABLE mood_entry_tag IS
    'Etiquetas opcionales del ánimo. Mismo nivel de sensibilidad que mood_entry.';
