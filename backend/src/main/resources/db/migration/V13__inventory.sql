-- V13__inventory.sql
-- Módulo Inventario (pedido explícito del usuario, 2026-08-22): antes
-- "scaffolding-only" (web/src/features/inventory/InventoryPage.tsx, UX-006,
-- mock data — web/src/core/mock/mockData.ts's MockInventoryItem: name,
-- category, location). Mismo patrón que warranties (V5__warranties_maintenance.sql):
-- dueño por FK a users, sin ON DELETE CASCADE, bloqueo optimista via version.
--
-- category: las 3 categorías que ya mostraba el mock (Electrónicos, Hogar,
-- Vehículos) — "Todos" (pedido por el usuario) es un filtro del lado del
-- cliente sobre estas 3, no una cuarta categoría real.
--
-- location: campo libre ya presente en el mock (siempre "En uso" en los
-- datos de ejemplo) — se mantiene como texto libre, no un enum, porque el
-- mock nunca definió un conjunto cerrado de valores para él.

CREATE TABLE inventory_items (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users (id),
    name          VARCHAR(200) NOT NULL,
    category      VARCHAR(32) NOT NULL,
    location      VARCHAR(200),
    version       INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_inventory_items_category CHECK (category IN ('ELECTRONICOS', 'HOGAR', 'VEHICULOS'))
);

CREATE INDEX ix_inventory_items_owner_user_id ON inventory_items (owner_user_id);
