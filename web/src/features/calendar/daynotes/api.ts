import { apiFetch } from '../../../core/api/httpClient'

/**
 * Canvas de notas por día (pedido explícito del usuario, 2026-08-22).
 * Mirrors web/src/features/visionboard/api.ts's element CRUD shape, pero
 * agrupado por `noteDate` (no por `boardId`) — GET siempre devuelve todos
 * los elementos de un día concreto, nunca paginado (un canvas completo).
 */
export type DayNoteElementType = 'BANNER' | 'TEXT'

export interface DayNoteElementData {
  text?: string
  bold?: boolean
  italic?: boolean
  /** 2026-08-22 (pedido explícito del usuario, ampliar con el catálogo de
      Vision Board): solo para BANNER — un id de visionBoardShapes.ts's
      familia "banners" (label/ribbon/flag/scroll/pennant). `undefined`
      se trata como 'ribbon' (el valor fijo que tenía antes). */
  shape?: string
  /** Solo para TEXT — uno de los 4 roles tipográficos ya definidos en
      index.css (--font-serif/--font-sans/--font-script/--font-mono).
      `undefined` se trata como 'sans' (el estilo que tenía antes). */
  font?:
  | 'serif'
  | 'sans'
  | 'script'
  | 'mono'
  | 'modern'
  | 'elegant'
  | 'editorial'
  | 'classic'
  | 'fashion'
  | 'handwritten'
  | 'parisienne'
  | 'extravagant'
  | 'playful'
  | 'typewriter'
}

export interface DayNoteElement {
  id: string
  ownerUserId: string
  noteDate: string
  type: DayNoteElementType
  x: number
  y: number
  width: number
  height: number
  zIndex: number
  data: DayNoteElementData
  version: number
  createdAt: string
  updatedAt: string
}

export interface CreateDayNoteElementInput {
  noteDate: string
  type: DayNoteElementType
  x: number
  y: number
  width: number
  height: number
  data?: DayNoteElementData
}

export async function listDayNoteElements(dateKey: string): Promise<DayNoteElement[]> {
  const response = await apiFetch(`/day-notes?date=${dateKey}`)
  if (!response.ok) throw new Error(`GET /day-notes failed: ${response.status}`)
  return response.json()
}

export async function createDayNoteElement(input: CreateDayNoteElementInput): Promise<DayNoteElement> {
  const response = await apiFetch('/day-notes', {
    method: 'POST',
    body: JSON.stringify(input),
  })
  if (!response.ok) throw new Error(`POST /day-notes failed: ${response.status}`)
  return response.json()
}

export async function moveDayNoteElement(
  id: string,
  position: { x: number; y: number; width: number; height: number; version: number },
): Promise<DayNoteElement> {
  const response = await apiFetch(`/day-notes/${id}/position`, {
    method: 'PUT',
    body: JSON.stringify(position),
  })
  if (!response.ok) throw new Error(`PUT /day-notes/${id}/position failed: ${response.status}`)
  return response.json()
}

export async function editDayNoteElementData(
  id: string,
  data: DayNoteElementData,
  version: number,
): Promise<DayNoteElement> {
  const response = await apiFetch(`/day-notes/${id}/data`, {
    method: 'PUT',
    body: JSON.stringify({ data, version }),
  })
  if (!response.ok) throw new Error(`PUT /day-notes/${id}/data failed: ${response.status}`)
  return response.json()
}

export async function deleteDayNoteElement(id: string): Promise<void> {
  const response = await apiFetch(`/day-notes/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /day-notes/${id} failed: ${response.status}`)
}
