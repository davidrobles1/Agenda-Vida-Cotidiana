/**
 * 2026-08-23 (pedido explícito del usuario, catálogo estilo Canva):
 * "Marcos" — implementado como un `data.frameStyle` opcional sobre el tipo
 * IMAGE ya existente (ver VisionBoardElementType.java's propio doc
 * comment sobre por qué no es un tipo nuevo) — reutiliza por completo el
 * pipeline de subida/URL que IMAGE ya tiene, así que a diferencia de
 * TABLE/GRID esto SÍ es contenido real desde el primer momento, no un
 * placeholder decorativo.
 */
export type VisionBoardFrameStyleId = 'circle' | 'hexagon' | 'rounded' | 'polaroid' | 'film'

export interface VisionBoardFrameStyle {
  id: VisionBoardFrameStyleId
  label: string
  /** `clip-path` cuando la forma se resuelve solo con recorte; `kind`
      cuando necesita chrome adicional (borde/franja) que un simple
      clip-path no puede dar — ver VisionBoardCanvas.module.css's
      `.elementImageFrame*`. */
  clipPath?: string
  kind?: 'rounded' | 'polaroid' | 'film'
}

export const FRAME_STYLE_OPTIONS: VisionBoardFrameStyle[] = [
  { id: 'circle', label: 'Círculo', clipPath: 'circle(50% at 50% 50%)' },
  {
    id: 'hexagon',
    label: 'Hexágono',
    clipPath: 'polygon(50% 2%, 93% 26%, 93% 74%, 50% 98%, 7% 74%, 7% 26%)',
  },
  { id: 'rounded', label: 'Redondeado', kind: 'rounded' },
  { id: 'polaroid', label: 'Polaroid', kind: 'polaroid' },
  { id: 'film', label: 'Película', kind: 'film' },
]

export function frameStyleOf(id: string | undefined): VisionBoardFrameStyle | undefined {
  return FRAME_STYLE_OPTIONS.find((style) => style.id === id)
}
