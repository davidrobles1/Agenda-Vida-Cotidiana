-- V17__day_note_elements.sql
-- Pedido explícito del usuario (2026-08-22): "Reemplazar la vista actual
-- de notas por un único Canvas, similar al de Visión Board" — reemplaza
-- la superficie de UI de `notes` (title/description/icon/sticker, sin
-- fecha, ver V7/V16), no la tabla en sí (que se conserva intacta, sin
-- tocar, para no perder datos reales ya creados por el usuario durante
-- esta misma sesión — ver NoteService/NoteController's propio doc comment
-- nuevo sobre esta decisión). Esta es una entidad completamente nueva,
-- independiente.
--
-- Mismo patrón de posición/tamaño/capa que vision_board_elements (V8):
-- x/y/width/height/z_index/version, `data` JSONB para el contenido real
-- (texto/negrita/cursiva — igual para ambos `type`, ver
-- DayNoteElement.java). `note_date` (no un board_id) es lo que agrupa los
-- elementos de "un canvas" — pedido explícito: "las notas pertenecen al
-- día seleccionado."
--
-- type: exactamente 2 valores pedidos explícitamente ("1 tipo de forma:
-- sticker/banner. 1 tipo de texto") — BANNER (la forma; también puede
-- llevar texto dentro, aclarado por el usuario) y TEXT (bloque de texto
-- suelto, negrita/cursiva).
--
-- Sin índice UNIQUE de no-solapamiento aquí a propósito — "no podrán
-- superponerse" se garantiza a nivel de aplicación (DayNoteService,
-- chequeo de intersección de rectángulos contra los demás elementos del
-- mismo owner+día antes de guardar), no como una restricción de base de
-- datos declarativa (Postgres no tiene un constraint nativo simple para
-- "ningún rectángulo se solapa con otro" sin extensiones adicionales como
-- btree_gist, que sería sobre-ingeniería para esta necesidad puntual).

CREATE TABLE day_note_elements (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users (id),
    note_date     DATE NOT NULL,
    type          VARCHAR(16) NOT NULL,
    x             DOUBLE PRECISION NOT NULL,
    y             DOUBLE PRECISION NOT NULL,
    width         DOUBLE PRECISION NOT NULL,
    height        DOUBLE PRECISION NOT NULL,
    z_index       INTEGER NOT NULL DEFAULT 0,
    data          JSONB NOT NULL DEFAULT '{}'::jsonb,
    version       INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_day_note_elements_type CHECK (type IN ('BANNER', 'TEXT'))
);

CREATE INDEX ix_day_note_elements_owner_date ON day_note_elements (owner_user_id, note_date);
