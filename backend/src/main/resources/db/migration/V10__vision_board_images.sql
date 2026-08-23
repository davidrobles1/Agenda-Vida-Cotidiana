-- V10__vision_board_images.sql
-- BLOQUE B (post-MVP) — real image storage for the Vision Board's IMAGE
-- element type. Until now `data.url` (vision_board_elements) only ever
-- held an external URL typed in by the user; this adds a place to store
-- uploaded bytes.
--
-- No object storage (S3-compatible) is provisioned in this environment yet
-- (AI-CONTEXT.md: "Object Storage S3-compatible: preparado conceptualmente,
-- no implementado en V1") and standing up real cloud infra from here is out
-- of proportion to "ampliación mínima" — Postgres BYTEA is the zero-new-
-- infra option that still satisfies "almacenarse físicamente en el
-- servidor" literally (this data directory *is* server-side physical
-- storage) and rides on the exact same backup/DR story every other table
-- here already has. Migrating to real S3-compatible object storage later
-- is a drop-in swap behind VisionBoardImageService (see its own doc
-- comment) — flagged as a Mejora Futura, not implemented here.
--
-- Owned directly by owner_user_id (not board_id) — same reasoning
-- VisionBoardImageService's doc comment gives: an uploaded image isn't
-- exclusively "of" one board (duplicating an IMAGE element, or reusing the
-- same upload across boards, should both just reference the same row), so
-- there's no FK to vision_boards, and no ON DELETE CASCADE tied to a board
-- being deleted — same "owner reference, not a composition relationship"
-- distinction V8's own doc comment already draws for owner_user_id there.

CREATE TABLE vision_board_images (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES users (id),
    content_type  VARCHAR(64) NOT NULL,
    size_bytes    BIGINT NOT NULL,
    data          BYTEA NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_vision_board_images_owner_user_id ON vision_board_images (owner_user_id);
