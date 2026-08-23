-- V18__adr016_documents_person_project_links.sql
-- ADR-016 Fase 3b (Documentacion/22-decision-log.md, docs/development/
-- 08-laboral-module-plan.md Fase 3b): vínculo opcional de DOCUMENT con
-- Persona/Proyecto — candidato V4 ("Documentos vinculados"), FR-030.
--
-- Mismo patrón aditivo que V16 sobre notes: columnas nullable, sin
-- backfill — ningún documento preexistente cambia de valor. No toca la
-- tabla `day_note_elements` (V17, trabajo concurrente no relacionado).

ALTER TABLE documents
    ADD COLUMN person_id UUID REFERENCES people (id),
    ADD COLUMN project_id UUID REFERENCES projects (id);

CREATE INDEX ix_documents_person_id ON documents (person_id);
CREATE INDEX ix_documents_project_id ON documents (project_id);
