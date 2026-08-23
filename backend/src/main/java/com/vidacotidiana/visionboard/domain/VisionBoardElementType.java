package com.vidacotidiana.visionboard.domain;

/**
 * MVP element types (Vision Board FASE 1/6). New types are added here only
 * — the rest of the model (position/size/rotation/layer/lock/visibility) is
 * shared by every type, and type-specific data lives entirely in
 * {@link VisionBoardElement#getData()}, so adding a type never requires a
 * schema change. Confirmed again for real when TABLE/CHART were added
 * (2026-08-22, pedido explícito del usuario, catálogo estilo Canva): the
 * `type` column is a plain `VARCHAR(16)` with no DB-level CHECK constraint
 * (see V8__vision_boards.sql) — validation lives entirely in
 * CreateElementRequest's own {@code @Pattern}, so this really was additive,
 * no migration needed. "Marcos" (frames) deliberately did NOT get a new
 * type here — implemented as an optional {@code data.frameStyle} on the
 * existing IMAGE type instead, reusing its whole upload/URL pipeline.
 * GRID (2026-08-23) is a decorative multi-cell placeholder for now (each
 * cell is a color swatch, not yet a real per-cell photo upload) — same
 * "no cell editing yet" scope limit already applied to TABLE, kept
 * consistent rather than making this one type disproportionately more
 * complete than the others in the same batch.
 */
public enum VisionBoardElementType {
    TEXT,
    IMAGE,
    NOTE,
    STICKER,
    SHAPE,
    TABLE,
    CHART,
    GRID
}
