-- V23__adr016_note_task_suggestion.sql
-- ADR-016 Fase 3d (Documentacion/22-decision-log.md): automatizaciones
-- simples — sugerencia de tarea a partir de una nota. FR-035, UC-28, AC-022.
--
-- REGLA APROBADA POR EL PRODUCT OWNER (2026-08-28), la que estuvo BLOCKED
-- hasta hoy:
--   1. El disparador es MANUAL: un botón "Sugerir tarea" en la nota. No hay
--      detección automática por palabras clave, ni job, ni heurística —
--      nada se dispara solo (por eso esta columna no tiene ningún proceso
--      detrás, solo registra qué decidió el usuario).
--   2. La sugerencia puede convertirse en tarea o descartarse.
--   3. Una vez convertida O descartada, no vuelve a ofrecerse para esa nota.
--
-- Un único booleano basta para (3): ambos desenlaces producen el mismo
-- efecto observable (la nota deja de ofrecer la sugerencia). No se modela
-- "convertida" vs. "descartada" por separado porque ninguna regla aprobada
-- las distingue — la Tarea creada, cuando la hay, ya queda registrada como
-- REMINDER real y vinculada a la misma Persona/Proyecto que la nota.
--
-- Aditiva y nullable-safe: DEFAULT FALSE deja toda NOTE preexistente
-- ofreciendo la sugerencia, que es el estado correcto (nadie la ha resuelto
-- todavía). Sin backfill.

ALTER TABLE notes
    ADD COLUMN task_suggestion_resolved BOOLEAN NOT NULL DEFAULT FALSE;
