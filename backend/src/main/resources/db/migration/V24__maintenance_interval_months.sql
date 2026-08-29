-- Pedido explícito del usuario (2026-08-28): "La frecuencia de '¿Cada cuándo?'
-- debe utilizarse para calcular las próximas fechas de mantenimiento".
--
-- Hasta ahora ese intervalo era un atajo de UI (CreateMaintenanceDialog.tsx):
-- calculaba `next_due_at` una vez y se perdía. Sin persistirlo, el calendario
-- no puede proyectar las siguientes ocurrencias — solo conoce la próxima.
--
-- Aditivo y nullable a propósito: los registros existentes se quedan sin
-- intervalo (no se inventa una periodicidad que el usuario nunca eligió) y
-- siguen comportándose exactamente igual, con una sola fecha.
ALTER TABLE maintenance_records
    ADD COLUMN interval_months INTEGER;

COMMENT ON COLUMN maintenance_records.interval_months IS
    'Periodicidad en meses elegida por el usuario ("¿Cada cuánto?"). NULL = mantenimiento de una sola fecha, sin repetición.';
