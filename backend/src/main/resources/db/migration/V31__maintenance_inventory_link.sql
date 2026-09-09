-- El mantenimiento se enlaza con el artículo del inventario al que se le hace.
--
-- PROBLEMA QUE RESUELVE — el mismo que V28 resolvió para Garantías, y por
-- el mismo motivo, pero en la otra mitad del inventario. "Cambio de aceite"
-- vivía en Mantenimiento y "Auto Toyota" en Inventario, sin ninguna relación
-- entre ambos: el artículo podía responder "¿todavía tiene garantía?" pero no
-- "¿qué le toca y cuándo?", que es la otra mitad de saber qué tienes.
--
-- `maintenance_records.item` es texto libre y SE CONSERVA tal cual: es lo que
-- describe la tarea ("Cambio de aceite"), no el objeto. El artículo es un dato
-- distinto y va en su propia columna.
--
-- NULLABLE A PROPÓSITO, a diferencia del enlace obligatorio que la garantía
-- estrena en esta misma fase: una garantía siempre cubre un objeto, pero
-- también se mantiene lo que no lo es —el techo, el jardín, la caldera de un
-- edificio que no está inventariada—. Obligar aquí expulsaría casos reales.
--
-- ON DELETE SET NULL, mismo criterio que V28: borrar el artículo del
-- inventario no debe llevarse por delante su historial de mantenimiento, que
-- sigue siendo un registro válido de lo que se hizo.
ALTER TABLE maintenance_records
    ADD COLUMN inventory_item_id UUID NULL REFERENCES inventory_items (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_maintenance_records_inventory_item
    ON maintenance_records (inventory_item_id);

COMMENT ON COLUMN maintenance_records.inventory_item_id IS
    'Artículo del inventario al que se le hace este mantenimiento. NULL = sin enlazar (se mantiene algo que no es un artículo inventariado).';
