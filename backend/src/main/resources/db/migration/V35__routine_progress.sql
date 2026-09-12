-- V35 — Contador diario de un hábito.
--
-- Los anillos de Rutinas del artefacto dicen «4 de 8» vasos de agua, «15 de 20»
-- minutos. `routines` solo sabía frecuencia, próxima ejecución y si está
-- activa: podía decir «toca hoy» o «no toca», nunca «llevas cuatro».
--
-- COMPATIBILIDAD TOTAL: con `target_count` NULL la rutina se comporta
-- exactamente como hasta hoy —sí o no— y ninguna de las que ya existen cambia
-- de aspecto ni de comportamiento. El contador es opt-in por rutina.

ALTER TABLE routines
    ADD COLUMN target_count INTEGER,
    ADD COLUMN unit         VARCHAR(20);

COMMENT ON COLUMN routines.target_count IS
    'Meta diaria. NULL = rutina de sí/no, el comportamiento anterior a V35.';
COMMENT ON COLUMN routines.unit IS
    'Unidad que acompaña a la cifra: vasos, min, km, cap. Solo rotula.';

-- El progreso es POR DÍA y no se acumula en la rutina: así el histórico queda
-- disponible para la racha sin tener que reconstruirlo, y reabrir un día
-- anterior no obliga a recalcular nada.
CREATE TABLE routine_progress (
    id            UUID PRIMARY KEY,
    routine_id    UUID    NOT NULL REFERENCES routines (id) ON DELETE CASCADE,
    progress_date DATE    NOT NULL,
    count         INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_routine_progress_day UNIQUE (routine_id, progress_date)
);

COMMENT ON TABLE routine_progress IS
    'Cuánto se llevó hecho de una rutina en un día. Una fila por rutina y día.';

-- La lectura típica es «el progreso de esta rutina en este rango de fechas»,
-- que es justo lo que cubre la restricción única; el índice descendente sirve
-- a la consulta de «los últimos días» sin ordenar en memoria.
CREATE INDEX idx_routine_progress_routine_date
    ON routine_progress (routine_id, progress_date DESC);
