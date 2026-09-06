-- ADR-021: el mantenimiento recurrente pasa a repetirse de verdad.
--
-- PROBLEMA QUE RESUELVE: `intervalMonths` existe desde la migración V24, se
-- guarda y NUNCA programaba la siguiente ocurrencia. `toggleCompletion()`
-- solo invertía un booleano, así que "Cambio de aceite · cada 3 meses" se
-- completaba una vez y quedaba COMPLETADO para siempre. Peor: `dateAlerts`
-- salta los COMPLETADO, así que completarlo borraba TODAS las ocurrencias
-- proyectadas en el calendario — el usuario perdía la recurrencia justo por
-- hacer lo correcto.
--
-- ADITIVA: no toca ninguna columna existente. La tabla `maintenance_records`
-- se queda exactamente como está; lo que cambia es el comportamiento del
-- dominio, que ahora avanza la fecha en vez de cerrar el registro.

-- Historial de ejecuciones. Equivalente a `payment_records` (ADR-020(f)) y
-- por el mismo motivo: sin él, avanzar la fecha borraría el rastro de que
-- la ocurrencia anterior se hizo, y "¿cuándo cambié el filtro?" —la
-- pregunta que más se le hace a un módulo de mantenimiento— no tendría
-- respuesta.
CREATE TABLE maintenance_log (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    maintenance_record_id UUID NOT NULL REFERENCES maintenance_records (id) ON DELETE CASCADE,
    owner_user_id         UUID NOT NULL REFERENCES users (id),
    -- La fecha que ESTABA programada cuando se completó. Es la clave del
    -- deshacer: permite devolver el registro exactamente a donde estaba.
    scheduled_date        DATE NOT NULL,
    -- Cuándo se hizo de verdad. Puede ser muy posterior a la programada:
    -- es justo lo que hace falta para saber si se va con retraso.
    completed_date        DATE NOT NULL,
    note                  VARCHAR(500),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_maintenance_log_record ON maintenance_log (maintenance_record_id, scheduled_date DESC);
CREATE INDEX ix_maintenance_log_owner ON maintenance_log (owner_user_id);

-- Una misma ocurrencia no puede completarse dos veces: es el equivalente en
-- datos de que "Hecho" sea idempotente. Sin esto, un doble clic o un
-- reintento de red saltarían dos ciclos y la próxima revisión aparecería un
-- intervalo entero más tarde de lo real.
CREATE UNIQUE INDEX ux_maintenance_log_occurrence
    ON maintenance_log (maintenance_record_id, scheduled_date);

COMMENT ON TABLE maintenance_log IS
    'ADR-021: ejecuciones de un mantenimiento. Una fila por ocurrencia completada.';
COMMENT ON COLUMN maintenance_log.scheduled_date IS
    'Fecha que estaba programada al completar. Sostiene el deshacer.';
COMMENT ON COLUMN maintenance_log.completed_date IS
    'Fecha real de ejecución. Comparada con scheduled_date da el retraso.';
