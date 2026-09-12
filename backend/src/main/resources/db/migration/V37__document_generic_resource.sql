-- V37 — Adjuntos en cualquier recurso.
--
-- EL PROBLEMA QUE RESUELVE. Hoy hay DOS mecánicas de archivo en la aplicación:
-- `documents`, que enlaza a persona y proyecto, y `warranties`, que guarda su
-- propio blob en `document_content_type` / `document_size_bytes`. Dos maneras
-- de hacer lo mismo es exactamente lo que el principio de no duplicación
-- prohíbe, y el artefacto pide adjuntos también en tareas y mantenimientos —
-- que no tienen ninguna de las dos.
--
-- Se amplía `documents` en vez de crear una tercera: un adjunto es un
-- documento, y ya existe todo su ciclo (subida, descarga, compartir, borrado).
--
-- ADITIVA: ambas columnas admiten NULL. Los documentos que ya existen —los que
-- cuelgan de persona o proyecto, y los sueltos— siguen igual.
--
-- `person_id` y `project_id` SE CONSERVAN: son enlaces de dominio con su
-- propia semántica («el documento de esta persona»), no adjuntos. Migrarlos
-- aquí no aportaría nada y rompería consultas existentes.

ALTER TABLE documents
    ADD COLUMN resource_type VARCHAR(30),
    ADD COLUMN resource_id   UUID;

COMMENT ON COLUMN documents.resource_type IS
    'REMINDER | MAINTENANCE | WARRANTY | INVENTORY_ITEM | SUBSCRIPTION. NULL = documento suelto.';
COMMENT ON COLUMN documents.resource_id IS
    'Id del recurso del que cuelga. Sin FK: apunta a cinco tablas distintas.';

-- Parcial: solo las filas que de verdad cuelgan de algo. Un índice sobre toda
-- la tabla indexaría sobre todo NULLs, que es donde está la mayoría hoy.
CREATE INDEX idx_documents_resource
    ON documents (resource_type, resource_id)
    WHERE resource_type IS NOT NULL;

-- La convergencia del blob de `warranties` hacia `documents` va en una
-- migración APARTE y posterior (V38), con su propia transformación de datos.
-- Mezclar el cambio de esquema con el movimiento de filas en una sola
-- migración deja sin punto de retorno si la segunda mitad falla.
