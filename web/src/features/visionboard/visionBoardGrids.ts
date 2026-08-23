/**
 * 2026-08-23 (pedido explícito del usuario, catálogo estilo Canva):
 * Cuadrículas — igual que TABLE, decorativo por ahora (celdas de color, no
 * fotos reales cargadas por celda todavía — eso es composición real de
 * varias imágenes, una función más grande por sí sola; ver
 * VisionBoardElementType.java's propio doc comment sobre esta misma
 * decisión). Cinco diseños, no los "docenas" que tendría un catálogo real
 * — mismo criterio de alcance que ya se aplicó al resto de este bloque.
 */
export type VisionBoardGridLayoutId = 'twoH' | 'twoV' | 'three' | 'four' | 'mosaic'

export interface VisionBoardGridLayout {
  id: VisionBoardGridLayoutId
  label: string
  cells: number
  gridTemplateRows: string
  gridTemplateColumns: string
  /** Índice de celda (0-based) que ocupa 2 filas — solo 'mosaic'. */
  spanCell?: number
}

export const GRID_LAYOUT_OPTIONS: VisionBoardGridLayout[] = [
  { id: 'twoH', label: '2 horizontal', cells: 2, gridTemplateRows: 'repeat(2, 1fr)', gridTemplateColumns: '1fr' },
  { id: 'twoV', label: '2 vertical', cells: 2, gridTemplateRows: '1fr', gridTemplateColumns: 'repeat(2, 1fr)' },
  { id: 'three', label: '3 en fila', cells: 3, gridTemplateRows: '1fr', gridTemplateColumns: 'repeat(3, 1fr)' },
  { id: 'four', label: 'Cuadrícula 2×2', cells: 4, gridTemplateRows: 'repeat(2, 1fr)', gridTemplateColumns: 'repeat(2, 1fr)' },
  { id: 'mosaic', label: 'Mosaico', cells: 5, gridTemplateRows: 'repeat(2, 1fr)', gridTemplateColumns: 'repeat(3, 1fr)', spanCell: 0 },
]

/** Mismos 4 tonos que ya usa el resto de este bloque estilo Canva
    (visionBoardCharts.ts's `SLICE_COLORS`), reciclados en vez de inventar
    una tercera paleta pequeña. */
export const GRID_CELL_COLORS = ['#2c5f8c', '#b47b48', '#4d6b46', '#8a5a12', '#2f5f66']

export function gridLayoutOf(id: string | undefined): VisionBoardGridLayout {
  return GRID_LAYOUT_OPTIONS.find((layout) => layout.id === id) ?? GRID_LAYOUT_OPTIONS[3]
}
