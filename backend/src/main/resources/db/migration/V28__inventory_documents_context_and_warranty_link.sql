-- ADR-022: Inventario y Documentos entran al aislamiento por módulo, y la
-- garantía se enlaza con el artículo que cubre.
--
-- PROBLEMA QUE RESUELVE (1) — aislamiento incompleto. La migración V25
-- añadió `context` a Garantías, Mantenimiento, Suscripciones y Notas del
-- día, pero dejó fuera `inventory_items` y `documents`. El resultado es que
-- esas dos secciones **comparten datos entre Personal y Laboral**, en
-- contra de la regla 2 del ADR-019 ("un recurso de un módulo no debe
-- aparecer ni afectar al otro"). No era una decisión: los dos módulos se
-- construyeron el 2026-08-22, seis días antes de que existiera la regla, y
-- nadie volvió sobre ellos.
--
-- REGLA 4 DEL ADR-019, aplicada igual que en V25 y aprobada de nuevo por el
-- Product Owner el 2026-09-01: todo lo que ya existe es PERSONAL. La
-- columna entra con DEFAULT 'PERSONAL' y NOT NULL, así que las filas
-- actuales quedan asignadas sin ambigüedad y sin adivinar nada.

ALTER TABLE inventory_items
    ADD COLUMN context VARCHAR(16) NOT NULL DEFAULT 'PERSONAL';

ALTER TABLE documents
    ADD COLUMN context VARCHAR(16) NOT NULL DEFAULT 'PERSONAL';

-- Mismos índices que V25: toda consulta de listado filtra por dueño y
-- contexto a la vez.
CREATE INDEX IF NOT EXISTS idx_inventory_items_owner_context
    ON inventory_items (owner_user_id, context);
CREATE INDEX IF NOT EXISTS idx_documents_owner_context
    ON documents (owner_user_id, context);

-- PROBLEMA QUE RESUELVE (2) — el mismo objeto vivía en dos listas que se
-- ignoraban. "Refrigerador Mabe" era un artículo del inventario Y una
-- garantía, sin ninguna relación entre ambos: no se podía responder "¿este
-- artículo todavía tiene garantía?", que es justo la pregunta que hace útil
-- tener las dos secciones.
--
-- El vínculo vive en `warranties` y no en `inventory_items` porque la
-- garantía es la que se refiere a un artículo, y porque así un artículo
-- puede acumular más de una a lo largo del tiempo (garantía de fábrica y
-- extensión contratada aparte) sin que el modelo lo impida.
--
-- ON DELETE SET NULL a propósito: borrar el artículo del inventario no debe
-- llevarse por delante el comprobante de su garantía, que puede seguir
-- haciendo falta para una reclamación.
ALTER TABLE warranties
    ADD COLUMN inventory_item_id UUID NULL REFERENCES inventory_items (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_warranties_inventory_item
    ON warranties (inventory_item_id);

COMMENT ON COLUMN inventory_items.context IS
    'ADR-019/ADR-022: módulo propietario (PERSONAL/LABORAL). Se fija al crear.';
COMMENT ON COLUMN documents.context IS
    'ADR-019/ADR-022: módulo propietario (PERSONAL/LABORAL). Se fija al crear.';
COMMENT ON COLUMN warranties.inventory_item_id IS
    'ADR-022: artículo del inventario que cubre esta garantía. NULL = sin enlazar.';
