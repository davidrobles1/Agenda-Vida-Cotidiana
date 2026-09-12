-- V34 — Pasos dentro de una tarea.
--
-- El anillo de avance del artefacto (`C.ring`) se deriva de los pasos: «4 de 5
-- pasos» es lo que hace que el 80 % signifique algo. Sin esta tabla, el anillo
-- no tendría de dónde salir.
--
-- EL PORCENTAJE NO SE GUARDA. Se calcula al leer, desde `done` sobre el total.
-- Guardarlo crearía dos fuentes de verdad para el mismo número, y la segunda
-- se desincroniza el día que alguien marque un paso por otra vía.
--
-- ON DELETE CASCADE: un paso no existe fuera de su tarea. Borrar la tarea y
-- dejar pasos huérfanos sería dejar basura que nadie volvería a mirar.

CREATE TABLE reminder_steps (
    id           UUID PRIMARY KEY,
    reminder_id  UUID         NOT NULL REFERENCES reminders (id) ON DELETE CASCADE,
    title        VARCHAR(200) NOT NULL,
    done         BOOLEAN      NOT NULL DEFAULT false,
    position     INTEGER      NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE reminder_steps IS
    'Pasos de una tarea. El % del anillo se DERIVA de done/total, nunca se almacena.';

-- Los pasos se leen siempre completos y en orden para una tarea concreta:
-- este índice cubre exactamente esa consulta.
CREATE INDEX idx_reminder_steps_reminder
    ON reminder_steps (reminder_id, position);
