-- V16__adr016_notes_person_project_links.sql
-- ADR-016 Fase 3a (Documentacion/22-decision-log.md, docs/development/
-- 08-laboral-module-plan.md Fase 3): vínculo opcional de NOTE con
-- Persona/Proyecto — candidato V4 ("Notas vinculadas"), FR-029.
--
-- Mismo patrón aditivo que V11 sobre reminders: columnas nullable, sin
-- backfill — ninguna nota preexistente cambia de valor.

ALTER TABLE notes
    ADD COLUMN person_id UUID REFERENCES people (id),
    ADD COLUMN project_id UUID REFERENCES projects (id);

CREATE INDEX ix_notes_person_id ON notes (person_id);
CREATE INDEX ix_notes_project_id ON notes (project_id);
