-- V8__vision_boards.sql
-- Alcance: FASE 1 del Vision Board — modelo de datos únicamente (sin API,
-- sin UI). Mismo patrón exacto que V5__warranties_maintenance.sql/
-- V7__notes_and_reminder_icon_sticker.sql: dueño por FK a users, sin ON
-- DELETE CASCADE en owner_user_id (mismo comportamiento que el resto de las
-- entidades owner-scoped), bloqueo optimista con columna version. Sin
-- concepto de compartición (owner-only, mismo criterio que Warranty/Note —
-- no existe UI de compartir boards).
--
-- vision_board_elements es una tabla real (no un JSON embebido en
-- vision_boards) — ver el doc comment de VisionBoardElement.java para el
-- razonamiento completo (API que direcciona elementos individualmente,
-- bloqueo optimista independiente por elemento, capas/undo-redo futuros).
-- board_id SÍ usa ON DELETE CASCADE: a diferencia de owner_user_id (una
-- referencia a un dueño, no una relación de composición), un elemento
-- pertenece enteramente a su board — mismo criterio ya usado en
-- V2__sharing.sql para reminder_id en invitations/reminder_shares.

CREATE TABLE vision_boards (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users (id),
    name          VARCHAR(200) NOT NULL,
    description   VARCHAR(2000),
    width         INTEGER NOT NULL,
    height        INTEGER NOT NULL,
    background    VARCHAR(64),
    version       INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_vision_boards_owner_user_id ON vision_boards (owner_user_id);

CREATE TABLE vision_board_elements (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    board_id   UUID NOT NULL REFERENCES vision_boards (id) ON DELETE CASCADE,
    type       VARCHAR(16) NOT NULL,
    x          DOUBLE PRECISION NOT NULL,
    y          DOUBLE PRECISION NOT NULL,
    width      DOUBLE PRECISION NOT NULL,
    height     DOUBLE PRECISION NOT NULL,
    rotation   DOUBLE PRECISION NOT NULL DEFAULT 0,
    z_index    INTEGER NOT NULL DEFAULT 0,
    locked     BOOLEAN NOT NULL DEFAULT false,
    visible    BOOLEAN NOT NULL DEFAULT true,
    data       JSONB NOT NULL DEFAULT '{}'::jsonb,
    version    INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_vision_board_elements_board_id ON vision_board_elements (board_id);
