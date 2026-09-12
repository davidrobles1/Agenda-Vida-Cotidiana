-- V38 — El blob de garantías converge con `documents`.
--
-- EL PROBLEMA. V28 le dio a `warranties` su propio almacén de archivo
-- —`document_content_type`, `document_size_bytes`, `document_data`— con su
-- propio endpoint `/warranties/{id}/content`. Es una SEGUNDA mecánica de
-- ficheros en paralelo a `documents`, que ya tenía subida, descarga,
-- compartición, visibilidad y borrado. Dos formas de hacer lo mismo es
-- exactamente lo que el principio de no duplicación prohíbe, y el artefacto
-- pide adjuntos también en tareas y mantenimientos, que no tienen ninguna.
--
-- V37 abrió el camino (`resource_type` / `resource_id`). Esta migración mueve
-- los datos. Va APARTE de V37 a propósito: mezclar el cambio de esquema con el
-- movimiento de filas deja sin punto de retorno si la segunda mitad falla.
--
-- ═══ ESTA MIGRACIÓN NO BORRA NADA ═══
--
-- Copia, no mueve. Las tres columnas de `warranties` siguen ahí y siguen
-- sirviendo a `/warranties/{id}/content` mientras el cliente antiguo exista.
-- Retirarlas es una TERCERA migración, y solo cuando Android e iOS lean ya el
-- documento. Borrar bytes en la misma migración que los copia es la forma más
-- rápida de perderlos si algo sale mal a medio camino.

-- ---------------------------------------------------------------
-- 1. Cada garantía con archivo pasa a ser también un documento
-- ---------------------------------------------------------------
-- `category` = 'COMPROBANTES': el archivo de una garantía es, en la práctica,
-- la factura o el ticket de compra. Es uno de los cinco valores que admite el
-- CHECK de V12; no se inventa una categoría nueva.
--
-- `visibility` = 'PRIVATE': el blob de garantía nunca tuvo compartición, así
-- que el documento nace igual de cerrado. Conceder más de lo que había sería
-- ampliar el acceso a espaldas del usuario.
--
-- `context` se hereda de la garantía (ADR-019): una garantía laboral produce
-- un documento laboral.
INSERT INTO documents (
    id, owner_user_id, name, category, content_type, size_bytes, data,
    visibility, context, resource_type, resource_id, version, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    w.owner_user_id,
    -- El nombre visible: «Garantía — iPad de Ana». Sin él, el usuario vería
    -- una lista de documentos sin saber de dónde salió cada uno.
    left('Garantía — ' || w.item, 200),
    'COMPROBANTES',
    COALESCE(w.document_content_type, 'application/octet-stream'),
    COALESCE(w.document_size_bytes, length(w.document_data)),
    w.document_data,
    'PRIVATE',
    COALESCE(w.context, 'PERSONAL'),
    'WARRANTY',
    w.id,
    0,
    w.created_at,
    now()
FROM warranties w
WHERE w.document_data IS NOT NULL
  -- Idempotente: si la migración se repite sobre una base ya migrada, no
  -- duplica. Flyway no debería repetirla, pero una restauración parcial sí.
  AND NOT EXISTS (
      SELECT 1 FROM documents d
      WHERE d.resource_type = 'WARRANTY' AND d.resource_id = w.id
  );

-- ---------------------------------------------------------------
-- 2. Constancia de lo que queda pendiente
-- ---------------------------------------------------------------
COMMENT ON COLUMN warranties.document_data IS
    'OBSOLETO desde V38: los bytes viven ya en documents (resource_type=WARRANTY). '
    'Se conserva para /warranties/{id}/content hasta que los clientes migren. '
    'Su retirada es una migración aparte.';
