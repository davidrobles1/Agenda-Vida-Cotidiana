-- V33 — Prioridad de una tarea.
--
-- El artefacto maestro pide «¿Cuánto aprieta?» en la pantalla de alta y la
-- muestra como propiedad en el detalle. `reminders` no tenía dónde guardarlo:
-- solo título, descripción, fecha, estado, contexto, icono, pegatina, persona,
-- proyecto y lugar.
--
-- ADITIVA Y SIN PÉRDIDA: la columna entra con DEFAULT 'NORMAL', así que las
-- tareas que ya existen quedan exactamente como estaban y ninguna consulta
-- previa cambia de resultado.
--
-- Sin CHECK, mismo criterio que `kind` en subscriptions (V26) y que `type` en
-- vision_board_elements: la validación vive en el DTO, y así añadir un valor
-- no exige una migración.

ALTER TABLE reminders
    ADD COLUMN priority VARCHAR(8) NOT NULL DEFAULT 'NORMAL';

COMMENT ON COLUMN reminders.priority IS
    'LOW | NORMAL | URGENT — cuánto aprieta la tarea. Artefacto maestro, pantalla Crear.';

-- El filtro por prioridad se hace SIEMPRE dentro del ámbito de un usuario:
-- el índice compuesto es el que sirve a esa consulta, no uno solo sobre la
-- columna, que en una tabla con tres valores distintos no aportaría nada.
CREATE INDEX idx_reminders_owner_priority
    ON reminders (owner_user_id, priority);
