import { apiFetch } from '../../core/api/httpClient'
import { SHAPE_CATALOG } from './visionBoardShapes'

export type VisionBoardElementType = 'TEXT' | 'IMAGE' | 'NOTE' | 'STICKER' | 'SHAPE' | 'TABLE' | 'CHART' | 'GRID'

export interface VisionBoardElement {
  id: string
  boardId: string
  type: VisionBoardElementType
  x: number
  y: number
  width: number
  height: number
  rotation: number
  zIndex: number
  locked: boolean
  visible: boolean
  data: Record<string, unknown>
  version: number
  createdAt: string
  updatedAt: string
}

/** FASE 16: Board Theme — independent of the app's own visual theme. Always
    present (the backend defaults a missing one to LIGHT at creation, see
    VisionBoardService#create), never optional here. */
export type VisionBoardThemeId = 'LIGHT' | 'DARK' | 'PAPER' | 'NATURAL' | 'CALM' | 'ENERGY'

export interface VisionBoard {
  id: string
  ownerUserId: string
  name: string
  description?: string
  width: number
  height: number
  theme: VisionBoardThemeId
  version: number
  createdAt: string
  updatedAt: string
  /** Only present on the single-board response (GET /vision-boards/{id}) —
      the list endpoint (GET /vision-boards) omits it, see
      VisionBoardResponse.java's own doc comment on the backend. */
  elements?: VisionBoardElement[]
}

interface VisionBoardsPage {
  items: VisionBoard[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

/**
 * BLOQUE A finding: this used to call `GET /vision-boards` with no query
 * params, which silently defaults to `page=0&size=20` on the backend
 * (VisionBoardController#list) — any board beyond the first 20 was never
 * loaded into `VisionBoardPage.tsx`'s `boards` state, so it never appeared
 * in the Switcher and, worse, could make the new "delete the last board"
 * flow show the empty state while boards the UI never fetched still
 * existed server-side. Confirmed for real: this account had accumulated
 * 40+ boards across every previous phase's e2e runs, and the Switcher only
 * ever listed 20 of them. Fixed by requesting the backend's own max page
 * size (100, `Math.min(size, 100)`) and, in the rare case that's still not
 * everything, walking the remaining pages — not a new pagination UI (no
 * realistic personal Vision Board usage needs one), just making "the
 * board list" actually mean all of it.
 */
export async function listVisionBoards(): Promise<VisionBoardsPage> {
  const first = await fetchVisionBoardsPage(0, 100)
  if (first.totalPages <= 1) return first

  const items = [...first.items]
  for (let page = 1; page < first.totalPages; page += 1) {
    const next = await fetchVisionBoardsPage(page, 100)
    items.push(...next.items)
  }
  return { items, page: 0, size: items.length, totalElements: first.totalElements, totalPages: 1 }
}

async function fetchVisionBoardsPage(page: number, size: number): Promise<VisionBoardsPage> {
  const response = await apiFetch(`/vision-boards?page=${page}&size=${size}`)
  if (!response.ok) throw new Error(`GET /vision-boards failed: ${response.status}`)
  return response.json()
}

export async function getVisionBoard(id: string): Promise<VisionBoard> {
  const response = await apiFetch(`/vision-boards/${id}`)
  if (!response.ok) throw new Error(`GET /vision-boards/${id} failed: ${response.status}`)
  return response.json()
}

export interface CreateVisionBoardInput {
  name: string
  description?: string
  width: number
  height: number
  /** Optional — the backend defaults to LIGHT when omitted. */
  theme?: VisionBoardThemeId
}

export async function createVisionBoard(input: CreateVisionBoardInput): Promise<VisionBoard> {
  const response = await apiFetch('/vision-boards', {
    method: 'POST',
    body: JSON.stringify(input),
  })
  if (!response.ok) throw new Error(`POST /vision-boards failed: ${response.status}`)
  return response.json()
}

/**
 * FASE 16: partial update — every field except `version` is optional, a
 * null/omitted one leaves the stored value unchanged (VisionBoard#applyEdit
 * on the backend), same convention as UpdateVisionBoardElementInput. This
 * is also how a Board Theme change persists — no dedicated theme endpoint,
 * callers send just `{ theme, version }`.
 */
export interface UpdateVisionBoardInput {
  name?: string
  description?: string
  width?: number
  height?: number
  theme?: VisionBoardThemeId
  version: number
}

export async function updateVisionBoard(id: string, input: UpdateVisionBoardInput): Promise<VisionBoard> {
  const response = await apiFetch(`/vision-boards/${id}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  })
  if (!response.ok) {
    throw new Error(
      response.status === 409
        ? 'El Vision Board fue modificado en otro lugar; recarga la página.'
        : `PUT /vision-boards/${id} failed: ${response.status}`,
    )
  }
  return response.json()
}

/**
 * BLOQUE A: the backend has exposed `DELETE /vision-boards/{id}` since it
 * was first built (used only by its own integration test until now — see
 * VisionBoardControllerIntegrationTest's `deleteVisionBoard_nonexistentReturnsNotFound`).
 * No new endpoint needed, only this client wrapper, same shape as
 * `deleteVisionBoardElement` above.
 */
export async function deleteVisionBoard(id: string): Promise<void> {
  const response = await apiFetch(`/vision-boards/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /vision-boards/${id} failed: ${response.status}`)
}

/**
 * BLOQUE B (post-MVP): the "internal reference" an uploaded/pasted/dropped
 * image resolves to — an IMAGE element's `data.imageId` stores `id`, never
 * the original file/URL. See VisionBoardImageController's own doc comment
 * on the backend for the storage decision.
 */
export interface VisionBoardImage {
  id: string
  contentType: string
  sizeBytes: number
}

export async function uploadVisionBoardImage(file: File): Promise<VisionBoardImage> {
  const formData = new FormData()
  formData.append('file', file)
  const response = await apiFetch('/vision-board-images', { method: 'POST', body: formData })
  if (!response.ok) {
    if (response.status === 400) {
      const body = await response.json().catch(() => null)
      throw new Error(body?.message ?? 'La imagen no es válida.')
    }
    if (response.status === 413) throw new Error('La imagen es demasiado grande.')
    throw new Error(`POST /vision-board-images failed: ${response.status}`)
  }
  return response.json()
}

/**
 * FASE 7 fix (was a FASE 6 pendiente): SHAPE now supports 3 variants,
 * stored as `data.shape` — no new column, no ElementType change, same
 * free-form JSONB `data` every other type already uses. Absent/unknown
 * `data.shape` (every SHAPE created before this phase existed) falls back
 * to 'rectangle', its original and only look.
 *
 * BLOQUE C (post-MVP): widened from the original 3-value union to a plain
 * `string` — the catalog (visionBoardShapes.ts) grew to ~30 ids, and a
 * literal union of all of them here would just be that same list
 * duplicated. `shapeVariantOf` is still the one place that validates a
 * stored `data.shape` against the real catalog.
 */
export type VisionBoardShapeVariant = string

export function shapeVariantOf(data: Record<string, unknown>): VisionBoardShapeVariant {
  return typeof data.shape === 'string' && SHAPE_CATALOG.some((s) => s.id === data.shape) ? data.shape : 'rectangle'
}

/** FASE 6: `data`'s shape is per-type, matching what VisionBoardElementView.tsx
    already reads back out for each type (see its own doc comment) — nothing
    invented here that the renderer doesn't already expect:
    - TEXT/NOTE: `{ text: string }`
    - STICKER: `{ stickerId: string }` — same catalog id contract as
      Reminder/Note's own stickerId, resolved via
      core/ui/pickers/pickerCatalog.ts's findStickerOption.
    - IMAGE: `{ url: string }`
    - SHAPE: `{ shape?: VisionBoardShapeVariant }` — FASE 7, see
      shapeVariantOf above. */
export interface CreateVisionBoardElementInput {
  type: VisionBoardElementType
  x: number
  y: number
  width: number
  height: number
  data?: Record<string, unknown>
}

export async function createVisionBoardElement(
  boardId: string,
  input: CreateVisionBoardElementInput,
): Promise<VisionBoardElement> {
  const response = await apiFetch(`/vision-boards/${boardId}/elements`, {
    method: 'POST',
    body: JSON.stringify(input),
  })
  if (!response.ok) throw new Error(`POST /vision-boards/${boardId}/elements failed: ${response.status}`)
  return response.json()
}

/**
 * FASE 4: partial update — every field except `version` is optional, a null/
 * omitted one leaves the stored value unchanged (VisionBoardElement#applyEdit
 * on the backend). Dragging sends only `{ x, y, version }`, leaving width/
 * height/rotation/zIndex/data untouched.
 */
export interface UpdateVisionBoardElementInput {
  x?: number
  y?: number
  width?: number
  height?: number
  rotation?: number
  zIndex?: number
  locked?: boolean
  visible?: boolean
  data?: Record<string, unknown>
  version: number
}

export async function updateVisionBoardElement(
  boardId: string,
  elementId: string,
  input: UpdateVisionBoardElementInput,
): Promise<VisionBoardElement> {
  const response = await apiFetch(`/vision-boards/${boardId}/elements/${elementId}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  })
  if (!response.ok) {
    throw new Error(
      response.status === 409
        ? 'El elemento fue modificado en otro lugar; recarga el Vision Board.'
        : `PUT /vision-boards/${boardId}/elements/${elementId} failed: ${response.status}`,
    )
  }
  return response.json()
}

/**
 * FASE 9: undoing a 'create' has no partial-update equivalent — the element
 * simply must stop existing, same real DELETE the backend has exposed
 * since FASE 2. No delete *button* exists in the toolbar (out of scope for
 * this cycle — see VisionBoardCanvas.tsx's history doc comment), but
 * undoing a create is still a real, user-reachable action that needs it.
 */
export async function deleteVisionBoardElement(boardId: string, elementId: string): Promise<void> {
  const response = await apiFetch(`/vision-boards/${boardId}/elements/${elementId}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /vision-boards/${boardId}/elements/${elementId} failed: ${response.status}`)
}

/**
 * FASE 9 fix (layers): "subir/bajar una capa" swaps the element with
 * whichever one is immediately above/below it in paint order — not a
 * client-side zIndex+-1, which is meaningless once elements share a zIndex
 * (every element does, at creation — see the backend's VisionBoardElement
 * constructor). The backend renumbers the whole board atomically
 * (VisionBoardService#reorderElement) and returns every affected element,
 * so the caller can sync all of their zIndex values without a full board
 * refetch.
 */
export type VisionBoardReorderDirection = 'FRONT' | 'BACK' | 'RAISE' | 'LOWER'

export async function reorderVisionBoardElement(
  boardId: string,
  elementId: string,
  direction: VisionBoardReorderDirection,
  version: number,
): Promise<VisionBoardElement[]> {
  const response = await apiFetch(`/vision-boards/${boardId}/elements/${elementId}/reorder`, {
    method: 'POST',
    body: JSON.stringify({ direction, version }),
  })
  if (!response.ok) {
    throw new Error(
      response.status === 409
        ? 'El elemento fue modificado en otro lugar; recarga el Vision Board.'
        : `POST /vision-boards/${boardId}/elements/${elementId}/reorder failed: ${response.status}`,
    )
  }
  return response.json()
}
