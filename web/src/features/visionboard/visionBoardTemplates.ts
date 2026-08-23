import { MIN_SIZE } from './VisionBoardElementView'
import type { VisionBoardElementType } from './api'

/**
 * FASE 12: static, frontend-only template data — no backend storage, no new
 * element types. Every element uses only the types the model already
 * supports (TEXT/NOTE/STICKER/SHAPE); IMAGE is deliberately never used
 * here — a template pointing at some external URL would be one broken
 * image away from looking unfinished, and "no implementes almacenamiento
 * de imágenes" already rules out shipping real image assets of our own.
 *
 * Every template is authored against a fixed reference canvas
 * (TEMPLATE_REFERENCE_WIDTH/HEIGHT, matching the real default board size —
 * see CreateVisionBoardDialog.tsx's own DEFAULT_WIDTH/HEIGHT) and later fit
 * to whatever size the user's actual board is via `fitTemplateToSize`
 * (uniform scale + center, never a non-uniform stretch that would distort
 * a note's proportions).
 *
 * `zIndex` isn't part of this data at all: array order *is* paint order —
 * the applying code (VisionBoardCanvas.tsx) derives each element's zIndex
 * from its position in this array, offset above whatever's already on the
 * board (see its own doc comment on why: "no reemplazar, agregar encima").
 */
export interface VisionBoardTemplateElement {
  type: VisionBoardElementType
  x: number
  y: number
  width: number
  height: number
  /** FASE 13: small, deliberate tilts (a few degrees at most) — the
      "physical corkboard" look, not random noise. Every template's own
      gaps between elements were sized generously enough (50-80px) to
      absorb a rotated box's slightly larger bounding footprint (a WxH box
      rotated by θ grows by roughly H·θ/2 in width and W·θ/2 in height, θ in
      radians) without overlapping a neighbor. Defaults to 0 when omitted. */
  rotation?: number
  data: Record<string, unknown>
}

export interface VisionBoardTemplate {
  id: string
  name: string
  description: string
  elements: VisionBoardTemplateElement[]
}

export const TEMPLATE_REFERENCE_WIDTH = 1600
export const TEMPLATE_REFERENCE_HEIGHT = 1000

export const VISION_BOARD_TEMPLATES: VisionBoardTemplate[] = [
  {
    id: 'suenos',
    name: 'Sueños',
    description: 'Un punto de partida para visualizar tus grandes aspiraciones.',
    elements: [
      { type: 'SHAPE', x: 60, y: 40, width: 760, height: 180, rotation: -1, data: { shape: 'rectangle' } },
      { type: 'TEXT', x: 100, y: 72, width: 680, height: 100, rotation: -1.5, data: { text: 'Mis sueños' } },
      { type: 'STICKER', x: 900, y: 50, width: 100, height: 100, rotation: 8, data: { stickerId: 'celebration' } },
      { type: 'NOTE', x: 100, y: 280, width: 330, height: 190, rotation: -3, data: { text: 'Viajar por el mundo y conocer nuevas culturas.' } },
      { type: 'NOTE', x: 490, y: 280, width: 330, height: 190, rotation: 2.5, data: { text: 'Aprender algo nuevo cada año.' } },
      { type: 'STICKER', x: 880, y: 280, width: 90, height: 90, rotation: -7, data: { stickerId: 'wellness' } },
      { type: 'SHAPE', x: 1000, y: 430, width: 140, height: 140, data: { shape: 'circle' } },
      { type: 'NOTE', x: 100, y: 530, width: 330, height: 190, rotation: 3, data: { text: 'Construir la vida que imagino.' } },
    ],
  },
  {
    id: 'metas',
    name: 'Metas',
    description: 'Tres metas principales del año, listas para revisar y ajustar.',
    elements: [
      { type: 'SHAPE', x: 60, y: 40, width: 900, height: 160, rotation: -0.8, data: { shape: 'rectangle' } },
      { type: 'TEXT', x: 100, y: 68, width: 820, height: 100, rotation: -1.5, data: { text: 'Metas del año' } },
      { type: 'NOTE', x: 100, y: 260, width: 250, height: 160, rotation: -3, data: { text: 'Meta 1: salud y bienestar.' } },
      { type: 'NOTE', x: 410, y: 260, width: 250, height: 160, rotation: 2, data: { text: 'Meta 2: finanzas personales.' } },
      { type: 'NOTE', x: 720, y: 260, width: 250, height: 160, rotation: -2.5, data: { text: 'Meta 3: desarrollo profesional.' } },
      { type: 'STICKER', x: 1030, y: 260, width: 90, height: 90, rotation: 6, data: { stickerId: 'work' } },
      { type: 'SHAPE', x: 100, y: 470, width: 800, height: 6, rotation: -1, data: { shape: 'line' } },
      { type: 'TEXT', x: 100, y: 520, width: 500, height: 70, rotation: 1, data: { text: 'Revisar cada trimestre.' } },
    ],
  },
  {
    id: 'viajes',
    name: 'Viajes',
    description: 'Tres próximos destinos y espacio para el presupuesto y fechas.',
    elements: [
      { type: 'SHAPE', x: 60, y: 40, width: 700, height: 160, rotation: -1, data: { shape: 'rectangle' } },
      { type: 'TEXT', x: 100, y: 68, width: 600, height: 100, rotation: -1.5, data: { text: 'Próximos viajes' } },
      { type: 'STICKER', x: 820, y: 50, width: 110, height: 110, rotation: -8, data: { stickerId: 'travel' } },
      { type: 'NOTE', x: 100, y: 270, width: 300, height: 180, rotation: -3, data: { text: 'Destino 1: Japón, en primavera.' } },
      { type: 'NOTE', x: 460, y: 270, width: 300, height: 180, rotation: 2.5, data: { text: 'Destino 2: Perú, Machu Picchu.' } },
      { type: 'NOTE', x: 820, y: 270, width: 300, height: 180, rotation: -2, data: { text: 'Destino 3: Islandia, auroras boreales.' } },
      { type: 'SHAPE', x: 100, y: 510, width: 120, height: 120, data: { shape: 'circle' } },
      { type: 'TEXT', x: 280, y: 530, width: 500, height: 80, rotation: 1.5, data: { text: 'Presupuesto estimado y fechas.' } },
    ],
  },
  {
    id: 'personal',
    name: 'Personal',
    description: 'Hábitos y recordatorios simples para el bienestar diario.',
    elements: [
      { type: 'SHAPE', x: 60, y: 40, width: 650, height: 160, rotation: 1, data: { shape: 'rectangle' } },
      { type: 'TEXT', x: 100, y: 68, width: 560, height: 100, rotation: 1.5, data: { text: 'Bienestar personal' } },
      { type: 'STICKER', x: 770, y: 50, width: 100, height: 100, rotation: 7, data: { stickerId: 'emotion' } },
      { type: 'NOTE', x: 100, y: 270, width: 290, height: 170, rotation: -3, data: { text: 'Meditar 10 minutos al día.' } },
      { type: 'NOTE', x: 450, y: 270, width: 290, height: 170, rotation: 2.5, data: { text: 'Dormir 8 horas.' } },
      { type: 'STICKER', x: 800, y: 270, width: 90, height: 90, rotation: -6, data: { stickerId: 'wellness' } },
      { type: 'SHAPE', x: 100, y: 490, width: 680, height: 6, rotation: 1, data: { shape: 'line' } },
      { type: 'NOTE', x: 100, y: 540, width: 290, height: 170, rotation: -2.5, data: { text: 'Agradecer algo cada noche.' } },
    ],
  },
  {
    id: 'productividad',
    name: 'Productividad',
    description: 'Rutinas de planificación y enfoque para la semana.',
    elements: [
      { type: 'SHAPE', x: 60, y: 40, width: 900, height: 160, rotation: -0.8, data: { shape: 'rectangle' } },
      { type: 'TEXT', x: 100, y: 68, width: 820, height: 100, rotation: -1.5, data: { text: 'Productividad y enfoque' } },
      { type: 'STICKER', x: 1030, y: 50, width: 90, height: 90, rotation: 8, data: { stickerId: 'reminder' } },
      { type: 'NOTE', x: 100, y: 270, width: 270, height: 170, rotation: -3, data: { text: 'Planificar la semana los domingos.' } },
      { type: 'NOTE', x: 420, y: 270, width: 270, height: 170, rotation: 2, data: { text: 'Bloques de trabajo profundo.' } },
      { type: 'NOTE', x: 740, y: 270, width: 270, height: 170, rotation: -2.5, data: { text: 'Revisar pendientes cada tarde.' } },
      { type: 'STICKER', x: 1080, y: 270, width: 90, height: 90, rotation: -7, data: { stickerId: 'study' } },
      { type: 'SHAPE', x: 100, y: 490, width: 1000, height: 6, rotation: -1, data: { shape: 'line' } },
      { type: 'TEXT', x: 100, y: 540, width: 400, height: 70, rotation: 1.5, data: { text: 'Una tarea a la vez.' } },
    ],
  },
]

export interface FittedTemplateElement extends VisionBoardTemplateElement {
  rotation: number
}

/**
 * Uniform "fit + center" scale — never a non-uniform per-axis stretch,
 * which would distort a note or shape's proportions. The same function
 * serves two callers at very different target sizes: the small card/
 * preview boxes in VisionBoardTemplatesAction.tsx, and the real board size
 * when actually applying (VisionBoardCanvas.tsx's handleApplyTemplate) —
 * both are just "fit this composition into a WxH area," only the numbers
 * differ. Widths/heights are floored at MIN_SIZE so a drastically small
 * target (a tiny preview thumbnail, or a board far smaller than the
 * reference canvas) never collapses an element to nothing.
 */
export function fitTemplateToSize(
  template: VisionBoardTemplate,
  targetWidth: number,
  targetHeight: number,
): FittedTemplateElement[] {
  const scale = Math.min(targetWidth / TEMPLATE_REFERENCE_WIDTH, targetHeight / TEMPLATE_REFERENCE_HEIGHT)
  const scaledWidth = TEMPLATE_REFERENCE_WIDTH * scale
  const scaledHeight = TEMPLATE_REFERENCE_HEIGHT * scale
  const offsetX = (targetWidth - scaledWidth) / 2
  const offsetY = (targetHeight - scaledHeight) / 2

  return template.elements.map((element) => ({
    ...element,
    x: offsetX + element.x * scale,
    y: offsetY + element.y * scale,
    width: Math.max(MIN_SIZE, element.width * scale),
    height: Math.max(MIN_SIZE, element.height * scale),
    rotation: element.rotation ?? 0,
  }))
}
