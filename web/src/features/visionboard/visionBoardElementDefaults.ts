import type { CreateVisionBoardElementInput, VisionBoardElementType } from './api'

/** FASE 6: fixed defaults — no "click on canvas to place" interaction was
    asked for, so every new element lands at the same spot/size for its
    type; the user repositions it with the drag/resize already built in
    Fase 4/5. Sizes are just reasonable starting points per type.

    FASE 13: pulled out of VisionBoardToolbar.tsx into its own module —
    VisionBoardElementLibrary.tsx needs `buildInput` too, and having both
    files import it from here instead of one importing it from the other
    avoids a circular import between them. */
const DEFAULT_POSITION = { x: 40, y: 40 }
const DEFAULT_SIZE: Record<VisionBoardElementType, { width: number; height: number }> = {
  TEXT: { width: 220, height: 60 },
  NOTE: { width: 200, height: 150 },
  STICKER: { width: 80, height: 80 },
  IMAGE: { width: 240, height: 160 },
  SHAPE: { width: 120, height: 120 },
  TABLE: { width: 260, height: 130 },
  CHART: { width: 260, height: 170 },
  GRID: { width: 260, height: 180 },
}

export function buildInput(type: VisionBoardElementType, data?: Record<string, unknown>): CreateVisionBoardElementInput {
  return { type, ...DEFAULT_POSITION, ...DEFAULT_SIZE[type], data }
}

/** BLOQUE B (post-MVP): paste (Ctrl/Cmd+V) and drag & drop from the OS
    file explorer both land the new image centered on a real screen point
    (the pointer) instead of the fixed `DEFAULT_POSITION` every other
    creation path uses — same size table, just not the same position.
    Exported separately rather than adding an optional position parameter
    to `buildInput` itself, so every *other* call site (unaffected by this
    change) keeps reading as "fixed defaults" at a glance. */
export function buildInputAt(
  type: VisionBoardElementType,
  center: { x: number; y: number },
  data?: Record<string, unknown>,
  size: { width: number; height: number } = DEFAULT_SIZE[type],
): CreateVisionBoardElementInput {
  return { type, x: center.x - size.width / 2, y: center.y - size.height / 2, ...size, data }
}

/** 2026-08-23 (pedido explícito del usuario): "la forma se acomode en base
    a la imagen" — mismo `DEFAULT_POSITION` fijo que `buildInput`, pero con
    un tamaño real (la proporción de la imagen recién elegida,
    visionBoardImages.ts's `fitImageElementSize`) en vez del tamaño fijo
    por tipo. Solo tiene sentido para IMAGE hoy — los demás tipos no tienen
    un "tamaño natural" propio que medir. */
export function buildInputWithSize(
  type: VisionBoardElementType,
  size: { width: number; height: number },
  data?: Record<string, unknown>,
): CreateVisionBoardElementInput {
  return { type, ...DEFAULT_POSITION, ...size, data }
}
