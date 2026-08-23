import { useCallback, useEffect, useRef, useState, type CSSProperties, type MouseEvent as ReactMouseEvent } from 'react'
import { VisuallyHidden } from 'react-aria-components'
import { Sparkles } from 'lucide-react'
import type { EntranceAnimationId } from './VisionBoardAnimationAction'
import { VisionBoardContextMenu } from './VisionBoardContextMenu'
import {
  createVisionBoardElement,
  deleteVisionBoard,
  deleteVisionBoardElement,
  reorderVisionBoardElement,
  updateVisionBoard,
  updateVisionBoardElement,
  uploadVisionBoardImage,
  type CreateVisionBoardElementInput,
  type VisionBoard,
  type VisionBoardElement,
  type VisionBoardElementType,
  type VisionBoardReorderDirection,
  type VisionBoardThemeId,
} from './api'
import { computeDragSnap, computeResizeSnap, type Guide } from './snapGuides'
import { buildInputAt } from './visionBoardElementDefaults'
import { MIN_SIZE, VisionBoardElementView } from './VisionBoardElementView'
import { fitImageElementSize, loadImageNaturalSize } from './visionBoardImages'
import { VisionBoardToolbar } from './VisionBoardToolbar'
import { fitTemplateToSize, type VisionBoardTemplate } from './visionBoardTemplates'
import { visionBoardThemeStyleVars } from './visionBoardThemes'
import styles from './VisionBoardCanvas.module.css'

interface VisionBoardCanvasProps {
  board: VisionBoard
  /** FASE 16: VisionBoardCanvas doesn't own board-level state (only
      `elements`, an optimistic copy of `board.elements`) — a Board Theme
      change is a board-level field, so the updated board (new `theme` +
      bumped `version`) is handed back up to VisionBoardPage, which does
      own it, same "onCreated" callback shape that page already uses. */
  onBoardChange: (board: VisionBoard) => void
  /** FASE 17: the caller's full board list + switch handler, both owned by
      VisionBoardPage.tsx (same reasoning as `onBoardChange` above) — this
      component only forwards them to VisionBoardToolbar/VisionBoardSwitcher.
      VisionBoardPage remounts this whole component (`key={board.id}`) on
      every switch, which is what actually resets selection/history/zoom —
      nothing here needs to react to `boards` changing. */
  boards: VisionBoard[]
  switchingBoard: boolean
  onSwitchBoard: (id: string) => void
  /** FASE 24: forwarded to VisionBoardToolbar's new "Nuevo Vision Board"
      entry — same "this component doesn't own board-level state, only
      forwards" reasoning as `onBoardChange`/`boards` above. */
  onBoardCreated: (board: VisionBoard) => void
  /** BLOQUE A: called *after* the board is actually deleted on the backend
      — VisionBoardPage owns removing it from `boards` and picking what to
      show next (another board, or the empty state), same "this component
      doesn't own board-level state" split as `onBoardChange` above. */
  onBoardDeleted: (deletedBoardId: string) => void
}

type ElementPatch = Partial<
  Pick<VisionBoardElement, 'x' | 'y' | 'width' | 'height' | 'rotation' | 'zIndex' | 'data' | 'locked'>
>

/**
 * FASE 18: the one source of truth for the autosave indicator —
 * deliberately a single tagged union, not `isSaving`/`hasChanges`/
 * `saveError` as separate booleans/strings that could drift out of sync
 * with each other. `'error'` carries its own message; retrying is a plain
 * function call (`retryPendingAutosaves`), not more status.
 */
export type SaveStatus = { kind: 'saved' } | { kind: 'dirty' } | { kind: 'saving' } | { kind: 'error'; message: string }

/** FASE 18: within the spec's own suggested 500–1000ms range. Applies only
    to *when the network call for a drag/resize/rotate fires* — the local
    state update and the Undo/Redo history entry both still happen the
    instant the gesture ends, never delayed by this. */
const AUTOSAVE_DEBOUNCE_MS = 700

/** FASE 10: 25%–200%, 10-point steps — reasonable range/granularity, no
    requirement pinned an exact one down. */
const ZOOM_MIN = 0.25
const ZOOM_MAX = 2
const ZOOM_STEP = 0.1
const ZOOM_DEFAULT = 1

/** BLOQUE G (post-MVP): Arrow-key nudge amounts, canvas-space px (never
    divided by zoom — unlike a screen-space pointer delta, a keypress has
    no natural screen distance to convert from, so this is a fixed logical
    step regardless of zoom, same "1px is 1px of the board" a numeric
    x/y input would be). */
const KEYBOARD_MOVE_FINE = 1
const KEYBOARD_MOVE_LARGE = 10

/** BLOQUE G (post-MVP): zoom/pan persisted per board — a per-viewer UI
    preference, not board content, so `localStorage` (never a new backend
    field/endpoint) is the right, minimal place for it, same "this belongs
    to the browser, not the server" reasoning the rest of this app already
    applies elsewhere. Keyed by board id so switching boards restores each
    one's own last zoom/scroll independently. Every read/write is
    try/catch-guarded — private browsing, a full quota, or a disabled
    storage API must never break opening a board, only silently skip
    remembering its viewport. */
interface PersistedViewport {
  zoom: number
  scrollLeft: number
  scrollTop: number
}

function viewportStorageKey(boardId: string): string {
  return `vision-board:viewport:${boardId}`
}

function loadPersistedViewport(boardId: string): PersistedViewport | null {
  try {
    const raw = localStorage.getItem(viewportStorageKey(boardId))
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (typeof parsed.zoom !== 'number' || typeof parsed.scrollLeft !== 'number' || typeof parsed.scrollTop !== 'number') {
      return null
    }
    return parsed
  } catch {
    return null
  }
}

function savePersistedViewport(boardId: string, viewport: PersistedViewport): void {
  try {
    localStorage.setItem(viewportStorageKey(boardId), JSON.stringify(viewport))
  } catch {
    // Storage unavailable/full/disabled — the zoom/pan just won't be
    // remembered next time; never worth surfacing an error for this.
  }
}

/** FASE 11: visual offset (canvas-space px, unaffected by zoom) applied to
    a duplicate so it never lands exactly on top of its original — small
    enough to stay obviously "the same place," big enough to grab without
    first nudging it aside by hand. */
const DUPLICATE_OFFSET = 16

/**
 * FASE 9: one entry per logical user action (never per pointermove).
 * FASE 18 fix: for drag/resize/rotate specifically, an entry is now pushed
 * the instant the gesture ends, independent of whether the autosave PUT
 * for it has even been sent yet (it's debounced — see
 * `scheduleElementAutosave`) let alone succeeded — Undo/Redo is a history
 * of *local* state transitions, autosave is a separate concern for
 * getting that local state to the server eventually; conflating the two
 * would mean a slow save could delay an entry appearing in the stack, or a
 * failed one could make the entry vanish even though the user's on-screen
 * change is still sitting right there. create/edit/lock/layer keep the
 * older "only on success" rule for now (see each handler's own comment for
 * why). Undoing a 'create' (or a
 * 'duplicate') deletes the element(s); redoing creates *new* ones with new
 * ids, so every entry referencing an old id (in either stack) has to be
 * found and rewritten in one pass (see `remapElementId`) — every variant
 * that can reference an element id does so as either a single `elementId`
 * field or an array of items/changes each carrying their own `elementId`,
 * which is what lets `remapElementId` stay one small switch instead of
 * reimplementing this per call site.
 *
 * 'layer' stores positions (index in ascending-zIndex order), not zIndex
 * values or a whole-board snapshot: RAISE/LOWER are adjacent swaps, and an
 * adjacent swap is its own exact inverse (swap forward, swap back — every
 * *other* element's relative order is untouched either way), so undoing any
 * layer change, including FRONT/BACK, is just walking the element back from
 * its after-index to its before-index one adjacent swap at a time (see
 * `stepLayer`). No new persistent state, no second source of truth — every
 * step still goes through the one real reorder endpoint FASE 9 added.
 *
 * FASE 11 adds three variants:
 * - 'moveGroup': a drag that moved more than one selected element at once —
 *   one entry for the whole group, never one per element, so undo/redo
 *   moves everyone back together in a single logical step.
 * - 'duplicate': CreateElementRequest has no rotation field (see
 *   VisionBoardService/openapi.yaml), so redoing a duplicate that had a
 *   non-zero rotation needs a POST *and* a follow-up PUT — `rotation` is
 *   carried alongside `input` so redo can reproduce exactly what the
 *   original duplicate action did.
 * - 'delete': the fullest snapshot of any entry (everything
 *   CreateElementRequest can't carry — rotation, zIndex — has to travel
 *   with it too), because undoing a delete is a real recreation, not a
 *   partial update to an element that's still there.
 *
 * FASE 12 adds 'template': applying a template creates several elements at
 * once but is still ONE logical action — one entry lists every element it
 * created (each with its own `input`/`rotation`, same shape as
 * 'duplicate', plus the `zIndex` handleApplyTemplate assigned it, since a
 * template's whole point is to land in a coherent stacking order above
 * whatever was already on the board). Undo removes all of them; redo
 * recreates all of them and remaps every new id, exactly like 'duplicate'.
 */
type HistoryEntry =
  | { type: 'move'; elementId: string; before: { x: number; y: number }; after: { x: number; y: number } }
  | {
      type: 'resize'
      elementId: string
      before: { width: number; height: number }
      after: { width: number; height: number }
    }
  | { type: 'rotate'; elementId: string; before: { rotation: number }; after: { rotation: number } }
  | {
      type: 'edit'
      elementId: string
      before: { data: Record<string, unknown> }
      after: { data: Record<string, unknown> }
    }
  | { type: 'layer'; elementId: string; beforeIndex: number; afterIndex: number }
  | { type: 'create'; elementId: string; input: CreateVisionBoardElementInput }
  | {
      type: 'moveGroup'
      changes: Array<{ elementId: string; before: { x: number; y: number }; after: { x: number; y: number } }>
    }
  | { type: 'duplicate'; items: Array<{ elementId: string; input: CreateVisionBoardElementInput; rotation: number }> }
  | {
      type: 'delete'
      items: Array<{
        elementId: string
        type: VisionBoardElementType
        x: number
        y: number
        width: number
        height: number
        rotation: number
        zIndex: number
        data: Record<string, unknown>
      }>
    }
  | {
      type: 'template'
      items: Array<{ elementId: string; input: CreateVisionBoardElementInput; rotation: number; zIndex: number }>
    }
  | { type: 'lock'; changes: Array<{ elementId: string; before: boolean; after: boolean }> }

/**
 * FASE 3: loads and displays a board's real elements at their real
 * x/y/width/height/rotation/zIndex, with click-to-select.
 *
 * FASE 4/5: dragging, resizing, rotating. `elements` is a local,
 * optimistically-updatable copy of `board.elements` — not a new data
 * source (still the same Vision Board elements the API returned), just
 * given local mutability so an interaction updates the screen instantly
 * and persists once, on release, instead of re-fetching the whole board or
 * hitting the network per pixel moved. `persistElementChange` is the one
 * place all single-element field changes actually talk to the network —
 * same optimistic-update-then-persist-then-revert-on-failure shape for
 * each, just a different field patch, so this stays one function instead
 * of several near-duplicates.
 *
 * FASE 6: `VisionBoardToolbar` creates TEXT/IMAGE/NOTE/STICKER/SHAPE
 * elements via the real POST. `handleCreateElement` deliberately doesn't
 * follow the optimistic-then-revert shape above — there's no element yet
 * to optimistically show (no id/version exist before the server assigns
 * them), so it only ever adds one after the real response comes back, then
 * selects it.
 *
 * FASE 8/9 fix: layers — "subir/bajar una capa" now moves the element
 * exactly one position relative to whichever element is immediately above/
 * below it in paint order, via the real reorder endpoint
 * (VisionBoardService#reorderElement on the backend), not a client-side
 * zIndex±1 (meaningless once elements share a zIndex, which every element
 * does at creation).
 *
 * FASE 9: Undo/Redo (`undoStack`/`redoStack`, see HistoryEntry above) and
 * Smart Guides (`activeGuides`, see snapGuides.ts) — both live entirely in
 * this component's own state, no new global store, nothing sent to or
 * stored by the backend beyond the same real persist calls every other
 * change already makes.
 *
 * FASE 10: zoom + pan. Zoom is `transform: scale()` on `.canvas` alone —
 * a pure view transform, never touches any element's stored x/y/width/
 * height/rotation. Pan reuses `.viewport`'s pre-existing `overflow: auto`
 * (native scroll) rather than a new custom drag-to-pan gesture — panning
 * only makes sense once the (possibly zoomed) canvas exceeds the visible
 * area, which is exactly when a scrollable container already shows
 * scrollbars/responds to trackpad-drag/wheel on its own. See
 * VisionBoardElementView.tsx's own doc comment for how drag/resize
 * convert screen-space pointer deltas into real canvas-space ones under
 * zoom (rotate needs no such conversion — see there for why; the same
 * zoom-awareness is why snapGuides.ts's threshold is divided by zoom too).
 *
 * FASE 11: multi-selection (`selectedIds`, replacing the old single
 * `selectedId`), group drag, duplicate, delete. `groupDragStartRef`
 * snapshots every selected element's position the instant selection is
 * decided (click/shift-click), so a drag that starts on any one of them
 * can compute one shared delta and apply it to the whole group without
 * re-deriving "who's in the group" on every pointermove.
 *
 * FASE 18: Autosave — `saveStatus` is the single source of truth for the
 * toolbar's indicator. Every mutation already talked to the network
 * immediately at gesture-end (never per pointermove, since FASE 4/5); this
 * phase adds three things on top of that, not a second persistence
 * architecture: (1) drag/resize/rotate now debounce+coalesce their PUT via
 * `scheduleElementAutosave` instead of calling `persistElementChange`
 * directly, so a rapid drag→resize→rotate on the same element sends fewer
 * requests; (2) a failed autosave keeps the local optimistic change and
 * offers Reintentar, instead of silently reverting; (3) Undo/Redo history
 * is now pushed synchronously at gesture-end, never delayed by or
 * conditioned on how the (debounced) save resolves — see HistoryEntry's
 * own doc comment for why that decoupling matters.
 */

/** FASE 20 audit fix: selection was communicated purely visually (each
    element's own `aria-pressed`/border, discoverable only by tabbing to
    that exact element) — nothing summarized "how many are selected" for a
    screen reader user. This builds that one summary line; it's rendered
    inside a `VisuallyHidden` `role="status"` region below, never inline
    per-element text (each element already announces its own name/locked
    state via VisionBoardElementView's own aria-label). */
function selectionAnnouncement(selectedElements: VisionBoardElement[]): string {
  if (selectedElements.length === 0) return ''
  const lockedCount = selectedElements.filter((el) => el.locked).length
  const base =
    selectedElements.length === 1 ? '1 elemento seleccionado' : `${selectedElements.length} elementos seleccionados`
  if (lockedCount === 0) return base
  if (lockedCount === selectedElements.length) return `${base}, bloqueado${selectedElements.length > 1 ? 's' : ''}`
  return `${base}, ${lockedCount} bloqueado${lockedCount > 1 ? 's' : ''}`
}
export function VisionBoardCanvas({
  board,
  onBoardChange,
  boards,
  switchingBoard,
  onSwitchBoard,
  onBoardCreated,
  onBoardDeleted,
}: VisionBoardCanvasProps) {
  const [elements, setElements] = useState<VisionBoardElement[]>(() => board.elements ?? [])
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [savingElementId, setSavingElementId] = useState<string | null>(null)
  /** BLOQUE G (post-MVP): "microanimación física al soltar" — which
      element(s) (a `moveGroup` drop can settle several at once) should
      show the brief settle animation right now. Set for ~260ms right when
      a real drag/resize/rotate *ends* (never during the live gesture —
      VisionBoardElementView.tsx keeps applying position/size/rotation via
      plain inline style the whole time it's moving, unanimated, exactly
      as before, so the drag itself always stays perfectly real-time),
      then cleared. `prefers-reduced-motion` is a pure CSS media query on
      the animation itself (VisionBoardCanvas.module.css's `.settling`),
      not handled here. */
  const [settlingElementIds, setSettlingElementIds] = useState<Set<string>>(new Set())
  const settleTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  function triggerSettle(elementIds: string[]) {
    if (settleTimerRef.current) clearTimeout(settleTimerRef.current)
    setSettlingElementIds(new Set(elementIds))
    settleTimerRef.current = setTimeout(() => setSettlingElementIds(new Set()), 260)
  }

  /** 2026-08-22 (pedido explícito del usuario, catálogo estilo Canva):
      mismo patrón que `settling` arriba — un flag transitorio, nunca
      derivado directamente de `data.entrance` en cada render (eso haría
      que el elemento "tiemble" cada vez que el tablero se vuelve a
      renderizar por cualquier motivo, no solo al elegir la animación).
      Se limpia sola tras 650ms (cubre la más larga de las 6, `bounceIn`
      a 620ms). Sin "modo presentación" todavía — deliberado, ver
      VisionBoardAnimationAction.tsx. */
  const [playingAnimation, setPlayingAnimation] = useState<{ elementId: string; animationId: EntranceAnimationId } | null>(null)
  const animationTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  async function handleApplyEntranceAnimation(animationId: EntranceAnimationId) {
    if (selectedIds.length !== 1) return
    const elementId = selectedIds[0]
    const element = elements.find((el) => el.id === elementId)
    if (!element || element.locked) return
    await handleEditElement({ ...element.data, entrance: animationId })
    if (animationTimerRef.current) clearTimeout(animationTimerRef.current)
    setPlayingAnimation({ elementId, animationId })
    animationTimerRef.current = setTimeout(() => setPlayingAnimation(null), 650)
  }
  const [dragError, setDragError] = useState<string | null>(null)
  // BLOQUE G: restores this board's own last zoom immediately on mount —
  // `board.id` is stable for this component's whole lifetime (a board
  // switch remounts it entirely via `key={board.id}`), so reading once
  // here (not in an effect) is enough.
  const [zoom, setZoom] = useState(() => loadPersistedViewport(board.id)?.zoom ?? ZOOM_DEFAULT)
  /** BLOQUE G: the scrollable `.viewport` node — imperative scroll restore
      (mount) and read (persisting on change) both need a real ref, there's
      no declarative way to set/read scroll position through JSX props. */
  const viewportRef = useRef<HTMLDivElement | null>(null)
  const [activeGuides, setActiveGuides] = useState<Guide[]>([])
  const [undoStack, setUndoStack] = useState<HistoryEntry[]>([])
  const [redoStack, setRedoStack] = useState<HistoryEntry[]>([])
  const [historyBusy, setHistoryBusy] = useState(false)
  const groupDragStartRef = useRef<Map<string, { x: number; y: number }> | null>(null)
  /** BLOQUE G (post-MVP): keyboard nudge (Arrow keys) — a burst of rapid
      presses coalesces into *one* undo entry (same "one logical action,
      one entry" rule drag already follows), same shape as
      `groupDragStartRef` above: the position each moved element had right
      before the *first* key press of the current burst, cleared once the
      debounce below settles. */
  const keyboardMoveSessionRef = useRef<Map<string, { x: number; y: number }> | null>(null)
  const keyboardMoveTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  /** BLOQUE B (post-MVP): the `.canvas` element itself — no drag/resize/
      rotate handler needed one before (they only ever compute pointer
      *deltas*, never an absolute screen→canvas point), but paste/drop
      placement genuinely needs "where is this screen point in canvas
      space," which needs a real DOM node to measure from. */
  const canvasRef = useRef<HTMLDivElement | null>(null)
  /** BLOQUE B: paste (Ctrl/Cmd+V) has no coordinates of its own — it's a
      keyboard event — so the last real pointer position over the canvas is
      tracked separately (via .canvas's own onPointerMove below) and reused
      when a paste actually happens. */
  const lastPointerScreenPos = useRef({ x: 0, y: 0 })
  /** FASE 16: local, optimistically-updatable copy of `board.theme` — same
      "instant feedback, revert on failure" shape `elements` already uses
      for element fields. Diverges from `board.theme` only for the brief
      window between picking a new theme and the PUT resolving. */
  const [theme, setTheme] = useState<VisionBoardThemeId>(board.theme)
  const [themeSaving, setThemeSaving] = useState(false)
  /** FASE 17: lifted out of VisionBoardDeleteConfirm so the Delete/
      Backspace keyboard shortcut can open the exact same confirm dialog
      the toolbar's own Eliminar button opens — never a second, bypassed
      deletion path. */
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false)
  /** BLOQUE C (post-MVP): inline text editing on a SHAPE — which element
      (if any) currently shows the overlay textarea instead of its normal
      rendered text (see VisionBoardShapeTextEditor below, and
      ElementContent's SHAPE case in VisionBoardElementView.tsx for the
      "at rest" text overlay this replaces while active). At most one at a
      time, same as Editar. */
  const [editingTextElementId, setEditingTextElementId] = useState<string | null>(null)
  /** BLOQUE E (post-MVP): lifted open-state for every popover the context
      menu (right-click / two-finger tap) can trigger — same
      "deleteConfirmOpen"-shaped lift FASE 17 already established, applied
      to Templates/Elementos/Editar/Cambiar tema/Nuevo Vision Board so
      VisionBoardContextMenu can open the *exact* existing popover each of
      those toolbar buttons already opens, never a second implementation
      of any of them. */
  const [templatesOpen, setTemplatesOpen] = useState(false)
  const [elementLibraryOpen, setElementLibraryOpen] = useState(false)
  const [themeSwitcherOpen, setThemeSwitcherOpen] = useState(false)
  const [createBoardOpen, setCreateBoardOpen] = useState(false)
  const [elementEditorOpen, setElementEditorOpen] = useState(false)
  /** BLOQUE E: the context menu's own open/position state — `null` when
      closed, otherwise the canvas-relative point it should render at
      (native `contextmenu`'s own clientX/clientY, same screenToCanvasPoint
      story item paste/drop already needed isn't relevant here — this is
      screen-space, matching where a real OS context menu appears). */
  const [contextMenuPosition, setContextMenuPosition] = useState<{ x: number; y: number } | null>(null)
  /** FASE 18 */
  const [saveStatus, setSaveStatus] = useState<SaveStatus>({ kind: 'saved' })
  /** FASE 18: always-fresh mirror of `elements` — read inside
      `flushPendingAutosaves`, which can fire many renders after
      `scheduleElementAutosave` scheduled it (the debounce timer's own
      closure was captured at schedule-time); reading straight off
      `elements` there would risk using a stale `element.version` and
      drawing an avoidable 409 from the backend's own optimistic lock. */
  const elementsRef = useRef(elements)
  useEffect(() => {
    elementsRef.current = elements
  }, [elements])
  /** FASE 21 perf fix: same mirror pattern as `elementsRef`, added so
      `handleSelectElement` below can be `useCallback`'d with a stable
      identity (see that callback's own doc comment for why stability here
      matters — without it, every `VisionBoardElementView` would re-render
      on every render of this component, defeating `React.memo` on that
      component even when dragging one element with hundreds of others on
      the board). */
  const selectedIdsRef = useRef(selectedIds)
  useEffect(() => {
    selectedIdsRef.current = selectedIds
  }, [selectedIds])
  /** FASE 18: elementId -> merged patch still waiting out the debounce
      window, or (after a failed flush) waiting for retry — only
      drag/resize/rotate route through this (see `scheduleElementAutosave`'s
      own doc comment for why just these three). Refs, not state: this is
      internal scheduling bookkeeping, not something that should trigger a
      render on its own — `saveStatus` is what the UI actually reads. */
  const pendingAutosavePatches = useRef<Map<string, ElementPatch>>(new Map())
  const autosaveTimer = useRef<ReturnType<typeof setTimeout> | null>(null)

  // Only re-syncs when the board itself changes (not on every render) — an
  // in-progress interaction's own optimistic updates must not be clobbered
  // by this.
  useEffect(() => {
    setElements(board.elements ?? [])
  }, [board.id, board.elements])

  useEffect(() => {
    setTheme(board.theme)
  }, [board.id, board.theme])

  /** FASE 16: optimistic-update-then-persist-then-revert-on-failure, same
      shape persistElementChange already uses for element fields — this is
      the board-level equivalent, the one place a Board Theme change
      actually talks to the network. `themeSaving` gates the switcher so a
      second pick can't fire mid-flight and race the first one's `version`. */
  async function handleThemeChange(nextTheme: VisionBoardThemeId) {
    if (nextTheme === theme || themeSaving) return
    const previousTheme = theme
    setTheme(nextTheme)
    setThemeSaving(true)
    setDragError(null)
    setSaveStatus({ kind: 'saving' })
    try {
      const updated = await updateVisionBoard(board.id, { theme: nextTheme, version: board.version })
      // PUT /vision-boards/{id} returns the board WITHOUT `elements`
      // (VisionBoardResponse.from(board), the single-arg overload — only
      // GET /vision-boards/{id} includes them). Passing `updated` straight
      // through would set `board.elements` to `undefined` on the parent,
      // which re-fires the `setElements(board.elements ?? [])` sync effect
      // below and wipes every element from the canvas locally (confirmed
      // for real — a genuine app bug, not a test artifact). `elements`
      // here is this component's own current, authoritative copy (see the
      // FASE 16 prop doc comment above), so it's what should survive.
      onBoardChange({ ...updated, elements })
      maybeMarkSaved()
    } catch (e) {
      setTheme(previousTheme)
      const message = e instanceof Error ? e.message : 'No se pudo cambiar el tema del Vision Board.'
      setDragError(message)
      setSaveStatus({ kind: 'error', message })
    } finally {
      setThemeSaving(false)
    }
  }

  /** FASE 18: only flips the indicator to 'saved' when nothing else is
      still outstanding — a debounced drag/resize/rotate save can be
      pending/in-flight at the same moment some *other* mutation (e.g. a
      lock toggle) just finished successfully; that other success
      shouldn't claim "todo guardado" while something is still on its way. */
  function maybeMarkSaved() {
    if (pendingAutosavePatches.current.size === 0 && !autosaveTimer.current) {
      setSaveStatus({ kind: 'saved' })
    }
  }

  /**
   * FASE 18: drag/resize/rotate end route their persistence through here
   * instead of calling `persistElementChange` directly. "No hagas una
   * petición HTTP por cada pixel" was already true (this only ever fires
   * once per gesture, at pointerup, same as before) — what this adds is
   * "agrupación de cambios": a user routinely drags, then immediately
   * resizes, then immediately rotates the *same* element within a couple
   * seconds, and debouncing+merging those into fewer PUTs is what that
   * requirement actually buys here. The caller pushes its own Undo/Redo
   * entry immediately, synchronously, *before* calling this — see
   * HistoryEntry's own doc comment for why persistence and history are
   * deliberately decoupled now.
   */
  function scheduleElementAutosave(elementId: string, patch: ElementPatch) {
    const existingPatch = pendingAutosavePatches.current.get(elementId)
    pendingAutosavePatches.current.set(elementId, existingPatch ? { ...existingPatch, ...patch } : patch)
    // Functional form so back-to-back schedules within the same debounce
    // window (e.g. drag-then-resize on the same element) don't each force
    // a render with a brand-new object when the status is already 'dirty'.
    setSaveStatus((current) => (current.kind === 'dirty' ? current : { kind: 'dirty' }))
    if (autosaveTimer.current) clearTimeout(autosaveTimer.current)
    autosaveTimer.current = setTimeout(() => {
      autosaveTimer.current = null
      void flushPendingAutosaves()
    }, AUTOSAVE_DEBOUNCE_MS)
  }

  /** Sends every still-pending patch at once — one PUT per element (no
      bulk endpoint exists, and none was added for this). A patch that
      fails stays in the pending map and is never reverted locally ("no
      perder los cambios locales"); one that succeeds is reconciled from
      the response exactly like `persistElementChange` already does for
      its own callers. */
  async function flushPendingAutosaves(): Promise<void> {
    const entries = Array.from(pendingAutosavePatches.current.entries())
    if (entries.length === 0) return
    pendingAutosavePatches.current.clear()
    setSaveStatus({ kind: 'saving' })

    let firstError: string | null = null
    await Promise.all(
      entries.map(async ([elementId, patch]) => {
        const element = elementsRef.current.find((el) => el.id === elementId)
        if (!element) return // deleted meanwhile — nothing left to save
        try {
          const updated = await updateVisionBoardElement(board.id, elementId, { ...patch, version: element.version })
          setElements((current) =>
            current.map((el) =>
              el.id === elementId
                ? {
                    ...el,
                    x: updated.x,
                    y: updated.y,
                    width: updated.width,
                    height: updated.height,
                    rotation: updated.rotation,
                    zIndex: updated.zIndex,
                    locked: updated.locked,
                    data: updated.data,
                    version: updated.version,
                  }
                : el,
            ),
          )
        } catch (e) {
          pendingAutosavePatches.current.set(elementId, patch)
          firstError = e instanceof Error ? e.message : 'No se pudo guardar el cambio.'
        }
      }),
    )

    if (firstError) {
      setSaveStatus({ kind: 'error', message: firstError })
    } else {
      maybeMarkSaved()
    }
  }

  /** "Reintentar" — a single explicit re-attempt of whatever's still
      pending, never a loop/backoff ("no generes loops de requests"). A
      failed flush only ever *adds* to `pendingAutosavePatches`, never
      clears it, so this always targets the latest known-unsaved state.
      An error from anything *other* than the debounced drag/resize/rotate
      path (edit/lock/theme/create/delete/…) already reverted its own
      local state and shows its own contextual error (the editor's own
      popover, VisionBoardDeleteConfirm's dialog, etc.) — there's nothing
      left in `pendingAutosavePatches` to retry for those, so this instead
      clears the now-stale global indicator rather than leaving a
      "Reintentar" that visibly does nothing. */
  function retryPendingAutosaves() {
    if (pendingAutosavePatches.current.size === 0) {
      maybeMarkSaved()
      return
    }
    if (autosaveTimer.current) {
      clearTimeout(autosaveTimer.current)
      autosaveTimer.current = null
    }
    void flushPendingAutosaves()
  }

  /** FASE 18: forces a pending autosave to send *now* instead of waiting
      out the debounce — used before switching boards (see
      `handleSwitchBoardRequest`) and before the tab closes/reloads (see
      the `beforeunload` handler), so a pending change is never silently
      left behind. */
  async function flushPendingAutosavesNow(): Promise<void> {
    if (autosaveTimer.current) {
      clearTimeout(autosaveTimer.current)
      autosaveTimer.current = null
    }
    await flushPendingAutosaves()
  }

  /** FASE 18: reflects any of the board's *other* network-touching
      mutations (create/duplicate/delete/template/layer — everything that
      isn't already covered by `persistElementChange`/
      `scheduleElementAutosave`) in the same shared save-status indicator,
      without changing any of their existing error-propagation contracts
      (some re-throw for a popover to show inline, some use `dragError`,
      delete lets VisionBoardDeleteConfirm catch it) — this only ever
      *adds* a status transition around whatever each one already does. */
  async function trackSaveStatus<T>(operation: () => Promise<T>): Promise<T> {
    setSaveStatus({ kind: 'saving' })
    try {
      const result = await operation()
      maybeMarkSaved()
      return result
    } catch (e) {
      setSaveStatus({ kind: 'error', message: e instanceof Error ? e.message : 'No se pudo guardar el cambio.' })
      throw e
    }
  }

  /**
   * Returns the error message on failure, `null` on success — never
   * throws. handleToggleLock/handleEditElement/undo-redo await this and
   * only push a history entry when it resolves to `null` (drag/resize/
   * rotate moved to `scheduleElementAutosave` above, FASE 18 — see that
   * function's own doc comment for why just those three).
   * handleEditElement is the one caller that re-throws on failure, so
   * VisionBoardElementEditor's popover stays open showing the error instead
   * of closing on a failed save.
   */
  async function persistElementChange(
    elementId: string,
    patch: ElementPatch,
    revertPatch: ElementPatch,
  ): Promise<string | null> {
    const element = elements.find((el) => el.id === elementId)
    if (!element) return null

    // Applied immediately: for drag/resize/rotate this is a harmless
    // no-op (onDragMove/onResizeMove/onRotateMove already applied the same
    // patch continuously while the gesture was in progress), but it's what
    // actually makes FASE 7's edit — which has no equivalent "live" step —
    // show up on the canvas at all, right when Guardar is pressed rather
    // than only after the network round-trip.
    setElements((current) => current.map((el) => (el.id === elementId ? { ...el, ...patch } : el)))
    setSavingElementId(elementId)
    setDragError(null)
    setSaveStatus({ kind: 'saving' })
    try {
      const updated = await updateVisionBoardElement(board.id, elementId, { ...patch, version: element.version })
      setElements((current) =>
        current.map((el) =>
          el.id === elementId
            ? {
                ...el,
                x: updated.x,
                y: updated.y,
                width: updated.width,
                height: updated.height,
                rotation: updated.rotation,
                zIndex: updated.zIndex,
                locked: updated.locked,
                data: updated.data,
                version: updated.version,
              }
            : el,
        ),
      )
      maybeMarkSaved()
      return null
    } catch (e) {
      // The PUT failed — the server never applied this change, so the
      // screen must go back to matching what's actually persisted rather
      // than showing a value that only ever existed locally. (Only this
      // function still reverts on failure — scheduleElementAutosave's own
      // flush deliberately doesn't, see its own doc comment.)
      setElements((current) => current.map((el) => (el.id === elementId ? { ...el, ...revertPatch } : el)))
      const message = e instanceof Error ? e.message : 'No se pudo guardar el cambio.'
      setDragError(message)
      setSaveStatus({ kind: 'error', message })
      return message
    } finally {
      setSavingElementId(null)
    }
  }

  function pushHistory(entry: HistoryEntry) {
    setUndoStack((current) => [...current, entry])
    setRedoStack([])
  }

  /** FASE 9/11: after undoing a create/duplicate/delete and then redoing
      it, the recreated element(s) get brand-new ids — every entry in
      *either* stack still holding an old one (e.g. a move made after the
      original create) has to be rewritten, or a later undo/redo would
      PUT/reorder/delete an id that no longer exists. */
  function remapElementId(oldId: string, newId: string) {
    const remapOne = (entry: HistoryEntry): HistoryEntry => {
      switch (entry.type) {
        case 'move':
        case 'resize':
        case 'rotate':
        case 'edit':
        case 'layer':
        case 'create':
          return entry.elementId === oldId ? { ...entry, elementId: newId } : entry
        case 'moveGroup':
          return entry.changes.some((change) => change.elementId === oldId)
            ? {
                ...entry,
                changes: entry.changes.map((change) =>
                  change.elementId === oldId ? { ...change, elementId: newId } : change,
                ),
              }
            : entry
        case 'duplicate':
          return entry.items.some((item) => item.elementId === oldId)
            ? {
                ...entry,
                items: entry.items.map((item) => (item.elementId === oldId ? { ...item, elementId: newId } : item)),
              }
            : entry
        case 'delete':
          return entry.items.some((item) => item.elementId === oldId)
            ? {
                ...entry,
                items: entry.items.map((item) => (item.elementId === oldId ? { ...item, elementId: newId } : item)),
              }
            : entry
        case 'template':
          return entry.items.some((item) => item.elementId === oldId)
            ? {
                ...entry,
                items: entry.items.map((item) => (item.elementId === oldId ? { ...item, elementId: newId } : item)),
              }
            : entry
        case 'lock':
          return entry.changes.some((change) => change.elementId === oldId)
            ? {
                ...entry,
                changes: entry.changes.map((change) =>
                  change.elementId === oldId ? { ...change, elementId: newId } : change,
                ),
              }
            : entry
      }
    }
    setUndoStack((current) => current.map(remapOne))
    setRedoStack((current) => current.map(remapOne))
  }

  function syncElementsFromReorder(updated: VisionBoardElement[]) {
    setElements((current) =>
      current.map((el) => {
        const match = updated.find((candidate) => candidate.id === el.id)
        return match ? { ...el, zIndex: match.zIndex, version: match.version } : el
      }),
    )
  }

  /** FASE 9: walks one element from `fromIndex` to `toIndex` (positions in
      ascending-zIndex order) via single-neighbor RAISE/LOWER reorder calls
      — see HistoryEntry's own doc comment for why this alone is enough to
      undo/redo any layer change, FRONT/BACK included. `guard` only exists
      to stop a runaway loop if the board's state ever stops matching
      expectations (e.g. concurrent edits mid-replay); it should never
      actually bind in normal use. */
  async function stepLayer(elementId: string, fromIndex: number, toIndex: number): Promise<void> {
    const startElement = elements.find((el) => el.id === elementId)
    if (!startElement) return
    let currentIndex = fromIndex
    let version = startElement.version
    let guard = elements.length + 2
    while (currentIndex !== toIndex && guard > 0) {
      guard -= 1
      const direction: VisionBoardReorderDirection = toIndex > currentIndex ? 'RAISE' : 'LOWER'
      const updated = await reorderVisionBoardElement(board.id, elementId, direction, version)
      syncElementsFromReorder(updated)
      const sorted = [...updated].sort((a, b) => a.zIndex - b.zIndex)
      const target = sorted.find((el) => el.id === elementId)
      if (!target) return
      version = target.version
      const newIndex = sorted.findIndex((el) => el.id === elementId)
      if (newIndex === currentIndex) break
      currentIndex = newIndex
    }
  }

  /** Creates one element and, only if it needs a rotation and/or zIndex
      CreateElementRequest can't express, follows up with a single PUT to
      set them — shared by duplicate, delete-undo, and (FASE 12) template
      application, every place that recreates an element from a snapshot
      or wants it to land at a specific stacking position. */
  async function createWithOverrides(
    input: CreateVisionBoardElementInput,
    rotation: number,
    zIndex: number,
  ): Promise<VisionBoardElement> {
    const created = await createVisionBoardElement(board.id, input)
    if (rotation === 0 && zIndex === 0) return created
    return updateVisionBoardElement(board.id, created.id, { rotation, zIndex, version: created.version })
  }

  /** FASE 9: applies one history entry in the given direction, returning
      the entry to push onto the *other* stack — identical to the one
      passed in for every type except redoing a 'create'/'duplicate' or
      undoing a 'delete', whose recreated element(s) get new ids (see
      remapElementId above). */
  async function applyHistoryEntry(entry: HistoryEntry, direction: 'undo' | 'redo'): Promise<HistoryEntry> {
    switch (entry.type) {
      case 'move':
      case 'resize':
      case 'rotate':
      case 'edit': {
        const target = direction === 'undo' ? entry.before : entry.after
        const revert = direction === 'undo' ? entry.after : entry.before
        await persistElementChange(entry.elementId, target, revert)
        return entry
      }
      case 'layer': {
        const fromIndex = direction === 'undo' ? entry.afterIndex : entry.beforeIndex
        const toIndex = direction === 'undo' ? entry.beforeIndex : entry.afterIndex
        await stepLayer(entry.elementId, fromIndex, toIndex)
        return entry
      }
      case 'create': {
        if (direction === 'undo') {
          await deleteVisionBoardElement(board.id, entry.elementId)
          setElements((current) => current.filter((el) => el.id !== entry.elementId))
          setSelectedIds((current) => current.filter((id) => id !== entry.elementId))
          return entry
        }
        const created = await createVisionBoardElement(board.id, entry.input)
        remapElementId(entry.elementId, created.id)
        setElements((current) => [...current, created])
        setSelectedIds([created.id])
        return { ...entry, elementId: created.id }
      }
      case 'moveGroup': {
        await Promise.all(
          entry.changes.map((change) => {
            const target = direction === 'undo' ? change.before : change.after
            const revert = direction === 'undo' ? change.after : change.before
            return persistElementChange(change.elementId, target, revert)
          }),
        )
        return entry
      }
      case 'duplicate': {
        if (direction === 'undo') {
          await Promise.all(entry.items.map((item) => deleteVisionBoardElement(board.id, item.elementId)))
          const removedIds = new Set(entry.items.map((item) => item.elementId))
          setElements((current) => current.filter((el) => !removedIds.has(el.id)))
          setSelectedIds((current) => current.filter((id) => !removedIds.has(id)))
          return entry
        }
        const recreated = await Promise.all(
          entry.items.map(async (item) => ({
            oldId: item.elementId,
            element: await createWithOverrides(item.input, item.rotation, 0),
          })),
        )
        recreated.forEach(({ oldId, element }) => remapElementId(oldId, element.id))
        setElements((current) => [...current, ...recreated.map((r) => r.element)])
        setSelectedIds(recreated.map((r) => r.element.id))
        return {
          ...entry,
          items: entry.items.map((item, index) => ({ ...item, elementId: recreated[index].element.id })),
        }
      }
      case 'delete': {
        if (direction === 'undo') {
          const recreated = await Promise.all(
            entry.items.map(async (item) => ({
              oldId: item.elementId,
              element: await createWithOverrides(
                { type: item.type, x: item.x, y: item.y, width: item.width, height: item.height, data: item.data },
                item.rotation,
                item.zIndex,
              ),
            })),
          )
          recreated.forEach(({ oldId, element }) => remapElementId(oldId, element.id))
          setElements((current) => [...current, ...recreated.map((r) => r.element)])
          setSelectedIds(recreated.map((r) => r.element.id))
          return {
            ...entry,
            items: entry.items.map((item, index) => ({ ...item, elementId: recreated[index].element.id })),
          }
        }
        await Promise.all(entry.items.map((item) => deleteVisionBoardElement(board.id, item.elementId)))
        const removedIds = new Set(entry.items.map((item) => item.elementId))
        setElements((current) => current.filter((el) => !removedIds.has(el.id)))
        setSelectedIds((current) => current.filter((id) => !removedIds.has(id)))
        return entry
      }
      case 'template': {
        if (direction === 'undo') {
          await Promise.all(entry.items.map((item) => deleteVisionBoardElement(board.id, item.elementId)))
          const removedIds = new Set(entry.items.map((item) => item.elementId))
          setElements((current) => current.filter((el) => !removedIds.has(el.id)))
          setSelectedIds((current) => current.filter((id) => !removedIds.has(id)))
          return entry
        }
        const recreated = await Promise.all(
          entry.items.map(async (item) => ({
            oldId: item.elementId,
            element: await createWithOverrides(item.input, item.rotation, item.zIndex),
          })),
        )
        recreated.forEach(({ oldId, element }) => remapElementId(oldId, element.id))
        setElements((current) => [...current, ...recreated.map((r) => r.element)])
        setSelectedIds(recreated.map((r) => r.element.id))
        return {
          ...entry,
          items: entry.items.map((item, index) => ({ ...item, elementId: recreated[index].element.id })),
        }
      }
      case 'lock': {
        await Promise.all(
          entry.changes.map((change) => {
            const target = direction === 'undo' ? change.before : change.after
            const revert = direction === 'undo' ? change.after : change.before
            return persistElementChange(change.elementId, { locked: target }, { locked: revert })
          }),
        )
        return entry
      }
    }
  }

  async function handleUndo() {
    if (historyBusy) return
    const entry = undoStack[undoStack.length - 1]
    if (!entry) return
    setHistoryBusy(true)
    setUndoStack((current) => current.slice(0, -1))
    try {
      const applied = await applyHistoryEntry(entry, 'undo')
      setRedoStack((current) => [...current, applied])
    } catch (e) {
      setUndoStack((current) => [...current, entry])
      setDragError(e instanceof Error ? e.message : 'No se pudo deshacer el cambio.')
    } finally {
      setHistoryBusy(false)
    }
  }

  async function handleRedo() {
    if (historyBusy) return
    const entry = redoStack[redoStack.length - 1]
    if (!entry) return
    setHistoryBusy(true)
    setRedoStack((current) => current.slice(0, -1))
    try {
      const applied = await applyHistoryEntry(entry, 'redo')
      setUndoStack((current) => [...current, applied])
    } catch (e) {
      setRedoStack((current) => [...current, entry])
      setDragError(e instanceof Error ? e.message : 'No se pudo rehacer el cambio.')
    } finally {
      setHistoryBusy(false)
    }
  }

  // FASE 9/17: Ctrl/Cmd+Z, Ctrl/Cmd+Shift+Z, Delete/Backspace, Escape — all
  // ignored while focus is in a text field (INPUT/TEXTAREA/contentEditable)
  // so native text editing/undo (inside VisionBoardElementEditor's
  // textarea/inputs, or a "URL de la imagen" input) keeps working
  // untouched.
  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      const target = event.target as HTMLElement | null
      const inTextField =
        !!target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable)

      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'z') {
        if (inTextField) return
        event.preventDefault()
        if (event.shiftKey) {
          void handleRedo()
        } else {
          void handleUndo()
        }
        return
      }

      // FASE 17: "Delete/Backspace → eliminar después de confirmación" —
      // opens the exact same VisionBoardDeleteConfirm dialog the toolbar's
      // own Eliminar button opens (lifted open state, see
      // deleteConfirmOpen), never a shortcut that bypasses it. No-ops when
      // nothing selected is actually deletable (e.g. the whole selection
      // is locked) — same "no puede eliminarse accidentalmente" rule
      // handleDeleteSelection itself already enforces.
      if ((event.key === 'Delete' || event.key === 'Backspace') && !inTextField) {
        if (deleteConfirmOpen || selectedIds.length === 0) return
        const hasDeletable = elements.some((el) => selectedIds.includes(el.id) && !el.locked)
        if (!hasDeletable) return
        event.preventDefault()
        setDeleteConfirmOpen(true)
        return
      }

      // BLOQUE C (post-MVP): Enter opens inline text editing for a single
      // selected SHAPE — double-click on the element itself does the same
      // (VisionBoardElementView.tsx's onDoubleClick). No-ops for every
      // other type/selection shape, so Enter keeps doing nothing anywhere
      // else it didn't before.
      if (event.key === 'Enter' && !inTextField && !editingTextElementId) {
        if (selectedIds.length !== 1) return
        const only = elements.find((el) => el.id === selectedIds[0])
        if (!only || only.type !== 'SHAPE' || only.locked) return
        event.preventDefault()
        handleStartEditText(only.id)
        return
      }

      // BLOQUE G (post-MVP): Arrow keys nudge the current selection — plain
      // press = KEYBOARD_MOVE_FINE, Shift+press = KEYBOARD_MOVE_LARGE. The
      // `[role="dialog"], [role="alertdialog"], [role="menu"]` guard
      // (same landmark check Escape's own handler below already uses) is
      // what keeps this from ALSO moving a canvas element while the user
      // is actually pressing arrows to navigate a radiogroup (Board
      // Theme, Forma, Emojis…) inside an open popover — those events
      // reach this native `window` listener too (React's own
      // `preventDefault` on the radiogroup's handler doesn't stop native
      // propagation), so without this guard both things would happen at
      // once.
      if (
        (event.key === 'ArrowUp' || event.key === 'ArrowDown' || event.key === 'ArrowLeft' || event.key === 'ArrowRight') &&
        !inTextField &&
        !target?.closest('[role="dialog"], [role="alertdialog"], [role="menu"]')
      ) {
        if (selectedIds.length === 0) return
        event.preventDefault()
        const step = event.shiftKey ? KEYBOARD_MOVE_LARGE : KEYBOARD_MOVE_FINE
        const dx = event.key === 'ArrowLeft' ? -step : event.key === 'ArrowRight' ? step : 0
        const dy = event.key === 'ArrowUp' ? -step : event.key === 'ArrowDown' ? step : 0
        handleKeyboardMove(dx, dy)
        return
      }

      // "Escape → cancelar interacción / cerrar overlay" — every popover/
      // dialog in this feature (Templates, Elementos, Theme, Export,
      // Eliminar, Editar) is a real React Aria Popover/Modal and already
      // closes on Escape natively; this only handles the one case React
      // Aria doesn't own — clearing the canvas's own multi-selection. The
      // `[role="dialog"], [role="alertdialog"]` check is what keeps this
      // from double-handling Escape while one of those is open (so it
      // never fights React Aria's own close-and-restore-focus behavior).
      if (event.key === 'Escape' && !inTextField) {
        if (target?.closest('[role="dialog"], [role="alertdialog"]')) return
        if (selectedIds.length > 0) {
          setSelectedIds([])
          groupDragStartRef.current = null
        }
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  // handleStartEditText intentionally omitted — it's a stable useCallback([])
  // (declared further below), so its identity never changes; including it
  // here would also be a temporal-dead-zone reference at this point in the
  // component body (it isn't declared yet when this effect first runs).
  }, [undoStack, redoStack, elements, historyBusy, handleUndo, handleRedo, selectedIds, deleteConfirmOpen, editingTextElementId])

  /**
   * FASE 11: decides the next selection for a click/shift-click on
   * `elementId`, and — in the same step — snapshots the resulting
   * selection's current positions for a possible group drag
   * (`groupDragStartRef`). Doing both here, once per real user gesture (see
   * VisionBoardElementView.tsx's `pointerHandledSelectionRef` for how
   * double-firing is avoided), is what lets a click-and-immediately-drag on
   * an unselected element both select it and start moving it in one
   * continuous gesture, exactly like the pre-FASE-11 single-selection flow
   * did.
   *
   * Plain click: replaces the selection with just this element — *unless*
   * it's already part of a multi-selection, in which case the group stays
   * intact (so dragging any member of an existing selection moves the
   * whole group, the standard "click without shift on an already-selected
   * item" convention in visual editors). Shift+click: toggles membership.
   *
   * FASE 21 perf fix: `useCallback` with an empty dependency array (reads
   * `selectedIdsRef`/`elementsRef` instead of the `selectedIds`/`elements`
   * closures) so this function keeps the exact same identity across every
   * render of VisionBoardCanvas. Passed directly as `VisionBoardElementView`'s
   * `onSelect` prop (no per-element inline arrow wrapper) — combined with
   * `React.memo` on that component, this is what lets 499 unrelated elements
   * skip re-rendering while one is being dragged; without a stable identity
   * here, every element's `onSelect` prop would look "changed" on every
   * render and defeat the memoization for all of them, not just the ones
   * that need selecting.
   */
  const handleSelectElement = useCallback((elementId: string, shiftKey: boolean) => {
    const currentSelection = selectedIdsRef.current
    const next = shiftKey
      ? currentSelection.includes(elementId)
        ? currentSelection.filter((id) => id !== elementId)
        : [...currentSelection, elementId]
      : currentSelection.includes(elementId) && currentSelection.length > 1
        ? currentSelection
        : [elementId]
    setSelectedIds(next)
    // FASE 17: locked elements are excluded from the group-drag snapshot —
    // "no puede moverse" has to hold even when a locked element merely
    // rides along in someone else's multi-selection; a locked element
    // itself never reaches here as the dragged one (VisionBoardElementView
    // already refuses to start a drag for it), but without this filter it
    // would still get dragged *along with* an unlocked group member.
    groupDragStartRef.current = next.includes(elementId)
      ? new Map(
          elementsRef.current
            .filter((el) => next.includes(el.id) && !el.locked)
            .map((el) => [el.id, { x: el.x, y: el.y }]),
        )
      : null
  }, [])

  /** FASE 9/11: Smart Guides during a drag — compares the moving box's
      edges/center against every other element's and the canvas's own,
      snapping within a zoom-aware threshold; see snapGuides.ts. Applied to
      local state immediately, same "instant visual feedback" as before.
      Group drags (see groupDragStartRef) snap only the actively-dragged
      element against elements *outside* the group, then apply the same
      resulting delta to every member — this keeps relative positions
      exactly intact, which snapping each member independently would not.
      Reads `elements` via `elementsRef` instead of the `elements` state
      closure — the *implementation* of the O(N) find/filter work here is
      unchanged (see the Fase 21 audit report for why that O(N) itself
      isn't the bottleneck even at 500 elements), only the *source* of the
      array is now ref-based, which is what lets this whole function be
      `useCallback`'d with a stable, empty-deps identity.
      FASE 21 perf fix: this is what actually matters for drag performance
      — a stable identity means `VisionBoardElementView.tsx`'s `onDragMove`
      prop no longer changes on every pointermove, so `React.memo` there
      can skip re-rendering the hundreds of elements NOT being dragged
      instead of every element re-rendering on every single pointermove. */
  const handleDragMove = useCallback(
    (elementId: string, x: number, y: number) => {
      const currentElements = elementsRef.current
      const current = currentElements.find((el) => el.id === elementId)
      if (!current) return

      const groupStart = groupDragStartRef.current
      if (groupStart && groupStart.size > 1 && groupStart.has(elementId)) {
        const others = currentElements.filter((el) => !groupStart.has(el.id))
        const snap = computeDragSnap(x, y, current.width, current.height, others, board.width, board.height, zoom)
        setActiveGuides(snap.guides)
        const start = groupStart.get(elementId)!
        const deltaX = snap.x - start.x
        const deltaY = snap.y - start.y
        setElements((currentList) =>
          currentList.map((el) => {
            const memberStart = groupStart.get(el.id)
            return memberStart ? { ...el, x: memberStart.x + deltaX, y: memberStart.y + deltaY } : el
          }),
        )
        return
      }

      const others = currentElements.filter((el) => el.id !== elementId)
      const snap = computeDragSnap(x, y, current.width, current.height, others, board.width, board.height, zoom)
      setActiveGuides(snap.guides)
      setElements((currentList) =>
        currentList.map((el) => (el.id === elementId ? { ...el, x: snap.x, y: snap.y } : el)),
      )
    },
    [board.width, board.height, zoom],
  )

  /** FASE 18 fix: persistence now goes through `scheduleElementAutosave`
      (debounced, coalescing) instead of an immediately-awaited
      `persistElementChange` — but the `moveGroup`/`move` history entry is
      still pushed synchronously, right here, regardless of how that
      autosave eventually resolves (see HistoryEntry's own doc comment).
      FASE 21 perf fix: same `elementsRef` + `useCallback` treatment as
      `handleDragMove` above, for the same reason (stable `onDragEnd`
      identity). `scheduleElementAutosave`/`pushHistory` are intentionally
      not in the dependency array — both only ever touch refs and
      functional `setState` updaters internally (never a stale closure over
      `elements`/`undoStack`/etc.), so calling whichever instance of them
      was captured when this callback was created is behaviorally identical
      to calling the latest one; oxlint's exhaustive-deps warning for this
      shape is already an accepted, commented pattern elsewhere in this
      same file (see the Ctrl/Cmd+Z `useEffect` above). */
  const handleDragEnd = useCallback(
    (elementId: string, x: number, y: number, startX: number, startY: number) => {
      const currentElements = elementsRef.current
      const groupStart = groupDragStartRef.current
      const isGroupDrag = !!groupStart && groupStart.size > 1 && groupStart.has(elementId)
      groupDragStartRef.current = null
      setActiveGuides([])

      const current = currentElements.find((el) => el.id === elementId)
      if (!current) return

      if (isGroupDrag) {
        const others = currentElements.filter((el) => !groupStart!.has(el.id))
        const snap = computeDragSnap(x, y, current.width, current.height, others, board.width, board.height, zoom)
        const start = groupStart!.get(elementId)!
        const deltaX = snap.x - start.x
        const deltaY = snap.y - start.y
        if (deltaX === 0 && deltaY === 0) return

        const changes = Array.from(groupStart!.entries()).map(([id, startPos]) => ({
          elementId: id,
          before: startPos,
          after: { x: startPos.x + deltaX, y: startPos.y + deltaY },
        }))
        changes.forEach((change) => scheduleElementAutosave(change.elementId, change.after))
        pushHistory({ type: 'moveGroup', changes })
        triggerSettle(changes.map((change) => change.elementId))
        return
      }

      // Recomputed independently from ElementView's own raw (unsnapped)
      // release value, so what gets persisted matches what was last shown
      // snapped on screen, not a slightly-different raw pointer position.
      const others = currentElements.filter((el) => el.id !== elementId)
      const snap = computeDragSnap(x, y, current.width, current.height, others, board.width, board.height, zoom)
      if (snap.x === startX && snap.y === startY) return
      scheduleElementAutosave(elementId, { x: snap.x, y: snap.y })
      pushHistory({ type: 'move', elementId, before: { x: startX, y: startY }, after: { x: snap.x, y: snap.y } })
      triggerSettle([elementId])
    },
    [board.width, board.height, zoom],
  )

  /** BLOQUE G (post-MVP): Arrow keys nudge the current selection —
      `KEYBOARD_MOVE_FINE` per press, `KEYBOARD_MOVE_LARGE` with Shift held
      (see the keydown handler below for the actual key→delta mapping).
      Reuses every existing moving-an-element mechanism instead of a
      parallel implementation: `computeDragSnap` (Smart Guides — single-
      element nudges only, same as handleDragEnd's own split between
      single and group), `scheduleElementAutosave` (debounced PUT, same as
      drag), and `pushHistory` — the only genuinely new piece is
      `keyboardMoveSessionRef`, which coalesces a whole burst of rapid
      presses into one undo entry instead of one per keystroke, mirroring
      what a single continuous drag gesture already produces. Locked
      elements are excluded from what actually moves, same rule
      `handleSelectElement`'s own group-drag snapshot already applies. */
  function handleKeyboardMove(dx: number, dy: number) {
    const currentElements = elementsRef.current
    const movableIds = selectedIdsRef.current.filter((id) => {
      const el = currentElements.find((candidate) => candidate.id === id)
      return el && !el.locked
    })
    if (movableIds.length === 0) return

    if (!keyboardMoveSessionRef.current) {
      keyboardMoveSessionRef.current = new Map(
        movableIds.map((id) => {
          const el = currentElements.find((candidate) => candidate.id === id)!
          return [id, { x: el.x, y: el.y }]
        }),
      )
    }

    const updates = new Map<string, { x: number; y: number }>()
    if (movableIds.length === 1) {
      const id = movableIds[0]
      const el = currentElements.find((candidate) => candidate.id === id)!
      const others = currentElements.filter((candidate) => candidate.id !== id)
      const snap = computeDragSnap(el.x + dx, el.y + dy, el.width, el.height, others, board.width, board.height, zoom)
      updates.set(id, { x: snap.x, y: snap.y })
      setActiveGuides(snap.guides)
    } else {
      for (const id of movableIds) {
        const el = currentElements.find((candidate) => candidate.id === id)!
        updates.set(id, { x: el.x + dx, y: el.y + dy })
      }
    }

    setElements((current) =>
      current.map((el) => {
        const update = updates.get(el.id)
        return update ? { ...el, x: update.x, y: update.y } : el
      }),
    )
    updates.forEach((position, id) => scheduleElementAutosave(id, position))

    if (keyboardMoveTimerRef.current) clearTimeout(keyboardMoveTimerRef.current)
    keyboardMoveTimerRef.current = setTimeout(() => {
      const session = keyboardMoveSessionRef.current
      keyboardMoveSessionRef.current = null
      setActiveGuides([])
      if (!session) return
      const latest = elementsRef.current
      if (session.size === 1) {
        const [id, before] = Array.from(session.entries())[0]
        const el = latest.find((candidate) => candidate.id === id)
        if (el && (el.x !== before.x || el.y !== before.y)) {
          pushHistory({ type: 'move', elementId: id, before, after: { x: el.x, y: el.y } })
        }
        return
      }
      const changes = Array.from(session.entries())
        .map(([id, before]) => {
          const el = latest.find((candidate) => candidate.id === id)
          return { elementId: id, before, after: el ? { x: el.x, y: el.y } : before }
        })
        .filter((change) => change.before.x !== change.after.x || change.before.y !== change.after.y)
      if (changes.length > 0) pushHistory({ type: 'moveGroup', changes })
    }, 500)
  }

  /** FASE 9: same snap treatment as drag, but only the axis the active
      resize handle actually touches — an E-only resize never snaps height,
      an S-only resize never snaps width (see snapGuides.ts's own doc
      comment on why). Which axis is active is inferred from whether the
      raw incoming value differs from what's currently rendered: the E
      handle always reports the untouched axis as exactly its start value
      (VisionBoardElementView's computeResize), so an unchanged value means
      "not this gesture's axis," not "coincidentally snapped already."
      FASE 11: resize stays single-element only — no group-resize was
      asked for, and this is unchanged from FASE 9/10.
      FASE 21 perf fix: same `elementsRef` + stable `useCallback` treatment
      as `handleDragMove`. */
  const handleResizeMove = useCallback(
    (elementId: string, width: number, height: number) => {
      const currentElements = elementsRef.current
      const current = currentElements.find((el) => el.id === elementId)
      if (!current) return
      const others = currentElements.filter((el) => el.id !== elementId)
      const snapWidth = width !== current.width
      const snapHeight = height !== current.height
      const snap = computeResizeSnap(
        current.x,
        current.y,
        width,
        height,
        snapWidth,
        snapHeight,
        others,
        board.width,
        board.height,
        zoom,
        MIN_SIZE,
      )
      setActiveGuides(snap.guides)
      setElements((currentList) =>
        currentList.map((el) => (el.id === elementId ? { ...el, width: snap.width, height: snap.height } : el)),
      )
    },
    [board.width, board.height, zoom],
  )

  /** FASE 18 fix: same shift as handleDragEnd — debounced/coalesced
      autosave via `scheduleElementAutosave`, `resize` history pushed
      synchronously regardless of that autosave's own outcome.
      FASE 21 perf fix: same `elementsRef` + stable `useCallback` treatment
      as `handleDragEnd`. */
  const handleResizeEnd = useCallback(
    (elementId: string, width: number, height: number, startWidth: number, startHeight: number) => {
      const currentElements = elementsRef.current
      const current = currentElements.find((el) => el.id === elementId)
      setActiveGuides([])
      if (!current) return
      const others = currentElements.filter((el) => el.id !== elementId)
      const snapWidth = width !== startWidth
      const snapHeight = height !== startHeight
      const snap = computeResizeSnap(
        current.x,
        current.y,
        width,
        height,
        snapWidth,
        snapHeight,
        others,
        board.width,
        board.height,
        zoom,
        MIN_SIZE,
      )
      if (snap.width === startWidth && snap.height === startHeight) return
      scheduleElementAutosave(elementId, { width: snap.width, height: snap.height })
      pushHistory({
        type: 'resize',
        elementId,
        before: { width: startWidth, height: startHeight },
        after: { width: snap.width, height: snap.height },
      })
      triggerSettle([elementId])
    },
    [board.width, board.height, zoom],
  )

  // FASE 21 perf fix: stable identity, same reasoning as handleDragMove —
  // this one never read `elements` at all (rotation is out of scope for
  // Smart Guides, see snapGuides.ts), so it needs no ref, just useCallback.
  const handleRotateMove = useCallback((elementId: string, rotation: number) => {
    setElements((current) => current.map((el) => (el.id === elementId ? { ...el, rotation } : el)))
  }, [])

  // FASE 9: rotation is explicitly out of scope for Smart Guides (see
  // snapGuides.ts's own doc comment) — this stays exactly what it was
  // before FASE 9, other than pushing a history entry. FASE 11: stays
  // single-element only, same as resize. FASE 18 fix: same debounced-
  // autosave-plus-synchronous-history shift as handleDragEnd/handleResizeEnd.
  // FASE 21 perf fix: stable identity, same reasoning as handleDragEnd.
  const handleRotateEnd = useCallback((elementId: string, rotation: number, startRotation: number) => {
    if (rotation === startRotation) return
    scheduleElementAutosave(elementId, { rotation })
    pushHistory({ type: 'rotate', elementId, before: { rotation: startRotation }, after: { rotation } })
    triggerSettle([elementId])
  }, [])

  /**
   * FASE 6: unlike move/resize/rotate, there's nothing to show optimistically
   * before the server responds — a fabricated element would be lying about
   * an id/version that don't exist yet, so this only ever adds the element
   * once the real POST response comes back. Selecting it immediately after
   * lets Fase 4/5's drag/resize/rotate apply to it right away, as asked.
   */
  async function handleCreateElement(input: CreateVisionBoardElementInput) {
    setDragError(null)
    try {
      await trackSaveStatus(async () => {
        const created = await createVisionBoardElement(board.id, input)
        setElements((current) => [...current, created])
        setSelectedIds([created.id])
        pushHistory({ type: 'create', elementId: created.id, input })
      })
    } catch (e) {
      setDragError(e instanceof Error ? e.message : 'No se pudo crear el elemento.')
    }
  }

  /** BLOQUE B (post-MVP): `.canvas` has `transform: scale(zoom)` with
      `transformOrigin: 'top left'` (see `canvasStyle` below) — a live
      `getBoundingClientRect()` on it already reflects both that scale and
      `.viewport`'s current scroll/pan, so converting a raw screen point
      into real canvas-space x/y needs nothing beyond this one division. */
  function screenToCanvasPoint(clientX: number, clientY: number): { x: number; y: number } {
    const rect = canvasRef.current?.getBoundingClientRect()
    if (!rect) return { x: clientX, y: clientY }
    return { x: (clientX - rect.left) / zoom, y: (clientY - rect.top) / zoom }
  }

  const PASTEABLE_IMAGE_TYPES = new Set(['image/png', 'image/jpeg', 'image/webp', 'image/gif'])

  /** BLOQUE B: paste (Ctrl/Cmd+V) and drag & drop from the OS file
      explorer both funnel through here — upload, then create exactly like
      any other element (`handleCreateElement`, same autosave/undo/
      selection behavior as clicking "Agregar" in the Elementos popover),
      just centered on a real screen point instead of the fixed
      DEFAULT_POSITION every popover-driven creation uses. */
  async function uploadAndPlaceImage(file: File, screenPoint: { x: number; y: number }) {
    if (!PASTEABLE_IMAGE_TYPES.has(file.type)) return
    setDragError(null)
    try {
      const uploaded = await uploadVisionBoardImage(file)
      const canvasPoint = screenToCanvasPoint(screenPoint.x, screenPoint.y)
      // 2026-08-23 (pedido explícito del usuario): "la forma se acomode en
      // base a la imagen" — mismo criterio que ImageEntryFields.tsx (el
      // popover "Elementos"), aplicado también aquí para que pegar/soltar
      // una imagen no se comporte distinto a subirla desde el popover.
      const objectUrl = URL.createObjectURL(file)
      const natural = await loadImageNaturalSize(objectUrl)
      URL.revokeObjectURL(objectUrl)
      const size = natural ? fitImageElementSize(natural) : undefined
      await handleCreateElement(buildInputAt('IMAGE', canvasPoint, { imageId: uploaded.id }, size))
    } catch (e) {
      setDragError(e instanceof Error ? e.message : 'No se pudo subir la imagen.')
    }
  }

  // BLOQUE B: same `inTextField` guard the Delete/Backspace/Ctrl+Z keydown
  // handler below already uses — pasting into the Editar popover's "URL de
  // la imagen" field (plain text) or any other input must stay native,
  // never intercepted into a new board element.
  useEffect(() => {
    function handlePaste(event: ClipboardEvent) {
      const target = event.target as HTMLElement | null
      const inTextField =
        !!target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable)
      if (inTextField) return
      const file = Array.from(event.clipboardData?.items ?? [])
        .filter((item) => item.kind === 'file')
        .map((item) => item.getAsFile())
        .find((candidate): candidate is File => !!candidate && candidate.type.startsWith('image/'))
      if (!file) return
      event.preventDefault()
      void uploadAndPlaceImage(file, lastPointerScreenPos.current)
    }
    window.addEventListener('paste', handlePaste)
    return () => window.removeEventListener('paste', handlePaste)
  }, [zoom])

  /**
   * FASE 7: reuses persistElementChange exactly like move/resize/rotate —
   * only `data` changes, x/y/width/height/rotation/zIndex are never sent
   * (the backend leaves omitted fields untouched, same as every other
   * partial update here) and selection is never touched, so it just stays
   * on the same element after a save, as asked. The thrown error (instead
   * of the silent revert drag/resize/rotate get away with) is what keeps
   * VisionBoardElementEditor's popover open with the message visible
   * instead of closing on a failed save. FASE 11: Editar is hidden
   * entirely for a multi-selection (VisionBoardToolbar), so this only ever
   * runs against a single selected element, same as before.
   */
  async function handleEditElement(data: Record<string, unknown>) {
    if (selectedIds.length !== 1) return
    const elementId = selectedIds[0]
    const element = elements.find((el) => el.id === elementId)
    // FASE 17: defense in depth — the toolbar's own Editar trigger is
    // already disabled for a locked element, this just guarantees "no
    // puede editarse" holds even if something else ever calls this.
    if (!element || element.locked) return
    const beforeData = element.data
    const errorMessage = await persistElementChange(elementId, { data }, { data: beforeData })
    if (errorMessage) throw new Error(errorMessage)
    pushHistory({ type: 'edit', elementId, before: { data: beforeData }, after: { data } })
  }

  /** BLOQUE C (post-MVP): double-click or Enter (see the keydown handler
      below) on a SHAPE opens this inline editor instead of the toolbar's
      Editar popover — no external editor, per the phase's own "no abrir un
      editor externo." Guards mirror handleEditElement's own (never for a
      locked element), but isn't gated on `selectedIds` the way
      handleEditElement is — a double-click can start editing an element
      that wasn't already the selection. `useCallback` + `elementsRef` (the
      FASE 21 stable-callback pattern every other per-element handler
      already uses) since this is passed straight through to the memoized
      VisionBoardElementView. */
  const handleStartEditText = useCallback((elementId: string) => {
    const element = elementsRef.current.find((el) => el.id === elementId)
    if (!element || element.locked || element.type !== 'SHAPE') return
    setEditingTextElementId(elementId)
  }, [])

  function handleCancelEditText() {
    setEditingTextElementId(null)
  }

  /** Same persist-then-push-history shape as handleEditElement (not routed
      through it directly — that function's own `selectedIds.length !== 1`
      guard reads `selectedIds` from this render's closure, which could
      still be stale immediately after a double-click that hadn't also
      selected the element) — merges only `text` into the existing `data`
      (preserving `data.shape`), and no-ops if nothing actually changed, so
      opening the editor and closing it without typing leaves no autosave/
      undo entry behind. */
  async function handleCommitEditText(elementId: string, text: string) {
    setEditingTextElementId(null)
    const element = elements.find((el) => el.id === elementId)
    if (!element || element.locked) return
    const currentText = typeof element.data.text === 'string' ? element.data.text : ''
    if (text === currentText) return
    const beforeData = element.data
    const afterData = { ...element.data, text }
    const errorMessage = await persistElementChange(elementId, { data: afterData }, { data: beforeData })
    if (errorMessage) {
      setDragError(errorMessage)
      return
    }
    pushHistory({ type: 'edit', elementId, before: { data: beforeData }, after: { data: afterData } })
  }

  /**
   * FASE 9 fix: "subir/bajar una capa" now means exactly what it says —
   * move the element one position relative to whichever one is immediately
   * above/below it in paint order, via the real reorder endpoint
   * (VisionBoardService#reorderElement), which atomically renumbers every
   * element on the board to a dense zIndex sequence and returns all of
   * them. FRONT/BACK go through the same one call — the backend, not the
   * client, decides how far that moves the element. FASE 11: single-
   * element only (LayerActions is hidden for a multi-selection).
   */
  async function handleLayerChange(kind: 'front' | 'back' | 'raise' | 'lower') {
    if (selectedIds.length !== 1 || historyBusy) return
    const elementId = selectedIds[0]
    const element = elements.find((el) => el.id === elementId)
    // FASE 17: defense in depth — LayerActions is already disabled for a
    // locked element in the toolbar.
    if (!element || element.locked) return

    const sortedBefore = [...elements].sort((a, b) => a.zIndex - b.zIndex)
    const beforeIndex = sortedBefore.findIndex((el) => el.id === elementId)
    if (beforeIndex < 0) return

    const direction: VisionBoardReorderDirection =
      kind === 'front' ? 'FRONT' : kind === 'back' ? 'BACK' : kind === 'raise' ? 'RAISE' : 'LOWER'

    setDragError(null)
    try {
      await trackSaveStatus(async () => {
        const updated = await reorderVisionBoardElement(board.id, elementId, direction, element.version)
        syncElementsFromReorder(updated)
        const sortedAfter = [...updated].sort((a, b) => a.zIndex - b.zIndex)
        const afterIndex = sortedAfter.findIndex((el) => el.id === elementId)
        if (afterIndex !== beforeIndex) {
          pushHistory({ type: 'layer', elementId, beforeIndex, afterIndex })
        }
      })
    } catch (e) {
      setDragError(e instanceof Error ? e.message : 'No se pudo cambiar el orden.')
    }
  }

  /**
   * FASE 11: duplicates every selected element — same type/dimensions/
   * data, offset by DUPLICATE_OFFSET, rotation restored via a follow-up PUT
   * when non-zero (CreateElementRequest has no rotation field). All copies
   * persist before selection moves to them, so a failed duplicate can't
   * leave the selection pointing at an element that was never created.
   */
  async function handleDuplicateSelection() {
    if (selectedIds.length === 0) return
    const targets = elements.filter((el) => selectedIds.includes(el.id))
    if (targets.length === 0) return

    setDragError(null)
    try {
      await trackSaveStatus(async () => {
        const duplicated = await Promise.all(
          targets.map(async (original) => {
            const input: CreateVisionBoardElementInput = {
              type: original.type,
              x: original.x + DUPLICATE_OFFSET,
              y: original.y + DUPLICATE_OFFSET,
              width: original.width,
              height: original.height,
              data: original.data,
            }
            const element = await createWithOverrides(input, original.rotation, 0)
            return { element, input, rotation: original.rotation }
          }),
        )
        setElements((current) => [...current, ...duplicated.map((d) => d.element)])
        setSelectedIds(duplicated.map((d) => d.element.id))
        pushHistory({
          type: 'duplicate',
          items: duplicated.map((d) => ({ elementId: d.element.id, input: d.input, rotation: d.rotation })),
        })
      })
    } catch (e) {
      setDragError(e instanceof Error ? e.message : 'No se pudo duplicar el elemento.')
    }
  }

  /**
   * FASE 11: deletes every selected element via the real DELETE endpoint
   * (present since FASE 2). Called as VisionBoardDeleteConfirm's
   * `onConfirm` — that component owns the alertdialog UI/its own
   * deleting/error state, this only owns persistence + local state + the
   * one history entry covering every deleted element.
   *
   * FASE 17: a locked element is excluded entirely — "no puede eliminarse
   * accidentalmente" — same filtering VisionBoardToolbar already does to
   * compute the count shown in the confirm dialog (`deletableElements`),
   * so what the dialog promises to delete and what this actually deletes
   * always match.
   */
  async function handleDeleteSelection() {
    if (selectedIds.length === 0) return
    const targets = elements.filter((el) => selectedIds.includes(el.id) && !el.locked)
    if (targets.length === 0) return

    // Re-thrown by trackSaveStatus on failure — VisionBoardDeleteConfirm's
    // own `onConfirm` awaits this and shows the error inline in its
    // dialog, same as before FASE 18; this only adds the shared
    // save-status reflection around it.
    await trackSaveStatus(async () => {
      await Promise.all(targets.map((el) => deleteVisionBoardElement(board.id, el.id)))
      const deletedIds = new Set(targets.map((el) => el.id))
      setElements((current) => current.filter((el) => !deletedIds.has(el.id)))
      setSelectedIds([])
      pushHistory({
        type: 'delete',
        items: targets.map((el) => ({
          elementId: el.id,
          type: el.type,
          x: el.x,
          y: el.y,
          width: el.width,
          height: el.height,
          rotation: el.rotation,
          zIndex: el.zIndex,
          data: el.data,
        })),
      })
    })
  }

  /**
   * FASE 17: bloquear/desbloquear — reuses `persistElementChange` exactly
   * like every other single-field patch (drag/resize/rotate/edit already
   * do this; `locked` was already part of the PUT contract, just never
   * exercised by any UI — see api.ts's `UpdateVisionBoardElementInput`).
   *
   * One toggle, not two separate "Bloquear"/"Desbloquear" actions: if every
   * selected element is already locked, the action unlocks all of them;
   * otherwise (all unlocked, or a mix) it locks all of them — a mixed
   * selection converging to "now everyone's locked" is the one outcome
   * that's unambiguous to explain and needs no extra UI for the mixed
   * case, matching "no inventar una UI compleja si no es necesaria."
   * Elements already in the target state are skipped (no-op PUT, no
   * history noise) — only real changes count toward the one history entry.
   */
  async function handleToggleLock() {
    if (selectedIds.length === 0) return
    const selected = elements.filter((el) => selectedIds.includes(el.id))
    if (selected.length === 0) return
    const nextLocked = !selected.every((el) => el.locked)
    const targets = selected.filter((el) => el.locked !== nextLocked)
    if (targets.length === 0) return

    setDragError(null)
    const results = await Promise.all(
      targets.map((el) => persistElementChange(el.id, { locked: nextLocked }, { locked: el.locked })),
    )
    const succeededChanges = targets
      .filter((_, index) => results[index] === null)
      .map((el) => ({ elementId: el.id, before: el.locked, after: nextLocked }))
    if (succeededChanges.length > 0) {
      pushHistory({ type: 'lock', changes: succeededChanges })
    }
  }

  /**
   * FASE 12: applies a template as ONE logical action — every element it
   * creates is appended, never replacing or removing what's already on the
   * board ("agregar esta composición," not "reemplazar mi Vision Board"),
   * scaled to the real board size (fitTemplateToSize), and stacked
   * entirely above the current content — `baseZIndex` is one past
   * whatever the highest existing zIndex is, so the whole composition
   * lands visible on top, in the template's own internal order, without
   * touching any existing element's zIndex. Errors are left to propagate
   * (not shown via the shared `dragError` banner) so VisionBoardTemplatesAction's
   * own popover can show them next to the preview — same precedent as
   * handleEditElement's re-throw for VisionBoardElementEditor.
   */
  async function handleApplyTemplate(template: VisionBoardTemplate) {
    await trackSaveStatus(async () => {
      const fitted = fitTemplateToSize(template, board.width, board.height)
      const baseZIndex = elements.length > 0 ? Math.max(...elements.map((el) => el.zIndex)) + 1 : 0
      const applied = await Promise.all(
        fitted.map(async (templateElement, index) => {
          const input: CreateVisionBoardElementInput = {
            type: templateElement.type,
            x: templateElement.x,
            y: templateElement.y,
            width: templateElement.width,
            height: templateElement.height,
            data: templateElement.data,
          }
          const zIndex = baseZIndex + index
          const element = await createWithOverrides(input, templateElement.rotation, zIndex)
          return { element, input, rotation: templateElement.rotation, zIndex }
        }),
      )
      setElements((current) => [...current, ...applied.map((a) => a.element)])
      setSelectedIds(applied.map((a) => a.element.id))
      pushHistory({
        type: 'template',
        items: applied.map((a) => ({ elementId: a.element.id, input: a.input, rotation: a.rotation, zIndex: a.zIndex })),
      })
    })
  }

  function handleZoomIn() {
    setZoom((current) => Math.min(ZOOM_MAX, Math.round((current + ZOOM_STEP) * 100) / 100))
  }

  function handleZoomOut() {
    setZoom((current) => Math.max(ZOOM_MIN, Math.round((current - ZOOM_STEP) * 100) / 100))
  }

  function handleZoomReset() {
    setZoom(ZOOM_DEFAULT)
  }

  // BLOQUE G: restores this board's own last pan (scroll position) once,
  // right after mount — has to be imperative (`scrollLeft`/`scrollTop`
  // assignment), there's no declarative "initial scroll" prop. Zoom
  // itself is already restored synchronously in `useState`'s initializer
  // above (so the very first paint is already at the right scale); scroll
  // restore waits for this effect since the scrollable content's real
  // size depends on that zoom having been applied to the DOM first.
  useEffect(() => {
    const saved = loadPersistedViewport(board.id)
    if (saved && viewportRef.current) {
      viewportRef.current.scrollLeft = saved.scrollLeft
      viewportRef.current.scrollTop = saved.scrollTop
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Persists on every zoom change (button/keyboard zoom) and on scroll
  // (panning — native browser scroll, see FASE 10's own doc comment),
  // debounced so a scroll gesture doesn't write to localStorage on every
  // pixel. Deliberately reads `zoom` fresh each time rather than needing
  // it threaded through the scroll handler.
  const persistViewportTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  function schedulePersistViewport() {
    if (persistViewportTimer.current) clearTimeout(persistViewportTimer.current)
    persistViewportTimer.current = setTimeout(() => {
      const node = viewportRef.current
      if (!node) return
      savePersistedViewport(board.id, { zoom, scrollLeft: node.scrollLeft, scrollTop: node.scrollTop })
    }, 300)
  }
  useEffect(() => {
    schedulePersistViewport()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [zoom])

  /** FASE 18: "antes de ejecutar el cambio de board, asegúrate de que los
      cambios pendientes se hayan guardado o marcado como error de manera
      explícita." Flushes any pending drag/resize/rotate autosave *now*
      (not waiting out the debounce) before ever calling the parent's own
      `onSwitchBoard` — VisionBoardPage owns the actual board switch
      (fetch + `key={board.id}` remount), this only guards the moment
      right before that happens. If the flush leaves anything still
      pending (a real failure), the switch is not attempted — staying on
      the current board with the error visible (and retryable) is what
      "nunca cambies de board silenciosamente perdiendo modificaciones"
      requires; the user can retry and try switching again once resolved. */
  async function handleSwitchBoardRequest(id: string) {
    if (id === board.id) return
    await flushPendingAutosavesNow()
    if (pendingAutosavePatches.current.size > 0) return
    onSwitchBoard(id)
  }

  /** FASE 24: creating a new board is, from this component's perspective,
      the same "about to remount onto a different board" moment
      `handleSwitchBoardRequest` above already guards — VisionBoardPage's
      own `handleCreated` calls `setBoard(created)` immediately, which
      remounts this whole component via `key={board.id}` exactly like a
      real switch does. Same flush-first guard, so a pending drag/resize/
      rotate save on *this* board is never silently dropped just because
      the user opened "Nuevo Vision Board" mid-edit. */
  async function handleBoardCreatedRequest(created: VisionBoard) {
    await flushPendingAutosavesNow()
    if (pendingAutosavePatches.current.size > 0) return
    onBoardCreated(created)
  }

  /** BLOQUE A: deletes the whole board for real (DELETE /vision-boards/{id},
      already confirmed by VisionBoardDeleteConfirm's alertdialog before this
      ever runs — the confirmation *is* the safety net here; there's no Undo
      for this one, see this function's own note below). No flush-pending-
      autosave guard like `handleSwitchBoardRequest`/`handleBoardCreatedRequest`
      above — the board and every one of its elements are about to stop
      existing regardless of whether a debounced PUT for one of them is still
      in flight. */
  async function handleDeleteBoard() {
    try {
      await deleteVisionBoard(board.id)
      // Undo/Redo (undoStack/redoStack above) is scoped to *this* board and
      // lives entirely in this component's local state — deleting the board
      // unmounts this exact component (VisionBoardPage picks a different
      // board or the empty state next, both remount via `key={board.id}`),
      // so there is no surviving stack to push a 'deleteBoard' entry onto.
      // Undoing a hard, cross-board delete would need server-side
      // soft-delete/restore support that doesn't exist today — flagged as a
      // Mejora Futura rather than invented here; the confirmation dialog is
      // the only safety net for this one action.
      onBoardDeleted(board.id)
    } catch (e) {
      const message = e instanceof Error ? e.message : 'No se pudo eliminar el Vision Board.'
      setDragError(message)
      setSaveStatus({ kind: 'error', message })
      throw e
    }
  }

  // FASE 18: "cierre/recarga" — the debounce window is short (≈700ms), but
  // non-zero, so a genuine loss window exists if the tab closes right
  // after a gesture ends. Native `beforeunload` is the minimal, native-API
  // way to cover it — no IndexedDB/offline persistence, exactly as asked.
  useEffect(() => {
    function handleBeforeUnload(event: BeforeUnloadEvent) {
      if (saveStatus.kind === 'saved') return
      event.preventDefault()
    }
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [saveStatus])

  const selectedElements = elements.filter((el) => selectedIds.includes(el.id))
  // BLOQUE E (post-MVP): same derived flags VisionBoardToolbar.tsx already
  // computes for its own Editar/Capas/Bloquear/Eliminar buttons — mirrored
  // here (not lifted/shared) since the context menu needs them to decide
  // its own content, and they're cheap, pure derivations of `selectedElements`.
  const singleSelectedLocked = selectedElements.length === 1 && (selectedElements[0]?.locked ?? false)
  const allSelectedLocked = selectedElements.length > 0 && selectedElements.every((el) => el.locked)
  const hasDeletableSelection = selectedElements.some((el) => !el.locked)

  /** BLOQUE E: right-click (Windows) / two-finger tap (macOS — the browser
      already normalizes this into the same native `contextmenu` event, no
      separate handling needed) opens VisionBoardContextMenu at the real
      click point. Right-clicking an element that isn't already part of
      the current selection selects *just* that element first (same
      "right-click selects" convention most editors use); right-clicking
      one that's already selected (including as part of a multi-selection)
      leaves the selection alone, so a right-click inside an existing
      multi-select doesn't collapse it down to one. Right-clicking empty
      canvas clears the selection, same as a left click already does. */
  function handleCanvasContextMenu(event: ReactMouseEvent<HTMLDivElement>) {
    event.preventDefault()
    const target = event.target as HTMLElement
    const elementNode = target.closest<HTMLElement>('[data-element-id]')
    const clickedId = elementNode?.getAttribute('data-element-id') ?? null
    if (clickedId) {
      if (!selectedIdsRef.current.includes(clickedId)) {
        setSelectedIds([clickedId])
        groupDragStartRef.current = null
      }
    } else {
      setSelectedIds([])
      groupDragStartRef.current = null
    }
    setContextMenuPosition({ x: event.clientX, y: event.clientY })
  }

  // FASE 16: Board Theme is applied here, as inline CSS custom properties
  // — never a `theme-*` class (that's the app theme's own mechanism,
  // deliberately not reused, see visionBoardThemes.ts's doc comment).
  // `--vb-*` custom properties only inherit to `.canvas`'s descendants
  // (every element), so this scoping needs nothing beyond "set it here."
  const canvasStyle: CSSProperties = {
    width: board.width,
    height: board.height,
    transform: `scale(${zoom})`,
    transformOrigin: 'top left',
    ...(visionBoardThemeStyleVars(theme) as CSSProperties),
  }

  return (
    <div className={styles.canvasPage}>
      <VisuallyHidden>
        <div role="status" aria-live="polite">
          {selectionAnnouncement(selectedElements)}
        </div>
      </VisuallyHidden>

      {/* FASE 6: a sibling of the scrollable canvas viewport below, not a
          child of it and never inside .canvas — clicks here can't bubble
          into the canvas's own onClick (clear selection) or an element's
          onPointerDown (start a drag), nothing extra needed to guard that. */}
      <VisionBoardToolbar
        board={board}
        boards={boards}
        switchingBoard={switchingBoard}
        onSwitchBoard={handleSwitchBoardRequest}
        onBoardCreated={handleBoardCreatedRequest}
        onDeleteBoard={handleDeleteBoard}
        elements={elements}
        theme={theme}
        themeSaving={themeSaving}
        onThemeChange={handleThemeChange}
        saveStatus={saveStatus}
        onRetrySave={retryPendingAutosaves}
        onCreate={handleCreateElement}
        selectedElements={selectedElements}
        selectedElementSaving={selectedIds.length === 1 && selectedIds[0] === savingElementId}
        onSaveElement={handleEditElement}
        onApplyEntranceAnimation={handleApplyEntranceAnimation}
        onLayerChange={handleLayerChange}
        onDuplicate={handleDuplicateSelection}
        onDeleteSelected={handleDeleteSelection}
        deleteConfirmOpen={deleteConfirmOpen}
        onDeleteConfirmOpenChange={setDeleteConfirmOpen}
        onToggleLock={handleToggleLock}
        onApplyTemplate={handleApplyTemplate}
        canUndo={undoStack.length > 0 && !historyBusy}
        canRedo={redoStack.length > 0 && !historyBusy}
        onUndo={handleUndo}
        onRedo={handleRedo}
        zoom={zoom}
        onZoomIn={handleZoomIn}
        onZoomOut={handleZoomOut}
        onZoomReset={handleZoomReset}
        templatesOpen={templatesOpen}
        onTemplatesOpenChange={setTemplatesOpen}
        elementLibraryOpen={elementLibraryOpen}
        onElementLibraryOpenChange={setElementLibraryOpen}
        themeSwitcherOpen={themeSwitcherOpen}
        onThemeSwitcherOpenChange={setThemeSwitcherOpen}
        createBoardOpen={createBoardOpen}
        onCreateBoardOpenChange={setCreateBoardOpen}
        elementEditorOpen={elementEditorOpen}
        onElementEditorOpenChange={setElementEditorOpen}
      />

      <VisionBoardContextMenu
        position={contextMenuPosition}
        onOpenChange={(open) => {
          if (!open) setContextMenuPosition(null)
        }}
        selectedCount={selectedElements.length}
        singleSelectedLocked={singleSelectedLocked}
        allSelectedLocked={allSelectedLocked}
        hasDeletable={hasDeletableSelection}
        onEdit={() => setElementEditorOpen(true)}
        onDuplicate={handleDuplicateSelection}
        onToggleLock={handleToggleLock}
        onDelete={() => setDeleteConfirmOpen(true)}
        onLayerChange={handleLayerChange}
        onOpenTemplates={() => setTemplatesOpen(true)}
        onOpenElements={() => setElementLibraryOpen(true)}
        onOpenTheme={() => setThemeSwitcherOpen(true)}
        onNewBoard={() => setCreateBoardOpen(true)}
      />

      <div className={styles.viewport} ref={viewportRef} onScroll={schedulePersistViewport}>
        {dragError && (
          <p role="alert" className={styles.dragError}>
            {dragError}
          </p>
        )}
        {/* FASE 10: sized to the *scaled* footprint so the scrollable
            .viewport's native scrollbars/pan reflect the real zoomed extent
            — `transform: scale()` on .canvas below doesn't affect layout
            size on its own, only paint, so without this wrapper a zoomed-in
            canvas would visually overflow without anything to scroll to
            reach the parts outside the original (unscaled) box. */}
        <div
          className={styles.canvasScaleWrapper}
          style={{ width: board.width * zoom, height: board.height * zoom }}
        >
          <div
            ref={canvasRef}
            className={styles.canvas}
            style={canvasStyle}
            // Clicking empty canvas space clears the current selection.
            onClick={() => {
              setSelectedIds([])
              groupDragStartRef.current = null
            }}
            // BLOQUE B: tracks the pointer purely for paste placement (see
            // uploadAndPlaceImage's own doc comment) — never touches
            // React state, so this never triggers a re-render.
            onPointerMove={(event) => {
              lastPointerScreenPos.current = { x: event.clientX, y: event.clientY }
            }}
            onDragOver={(event) => {
              if (event.dataTransfer.types.includes('Files')) event.preventDefault()
            }}
            onDrop={(event) => {
              const dropped = event.dataTransfer.files[0]
              if (!dropped) return
              event.preventDefault()
              void uploadAndPlaceImage(dropped, { x: event.clientX, y: event.clientY })
            }}
            onContextMenu={handleCanvasContextMenu}
          >
            {/* FASE 21 perf fix: every callback below is now passed directly
                (the same `useCallback`'d function reference for every
                element), not wrapped in a fresh per-element arrow function
                — see VisionBoardElementView.tsx's own doc comment on why
                that's what makes its `React.memo` actually skip
                re-rendering the elements not involved in the current
                gesture. `selected`/`saving` stay computed inline (cheap
                booleans, correct for memo's shallow comparison either way). */}
            {elements.map((element) => (
              <VisionBoardElementView
                key={element.id}
                element={element}
                selected={selectedIds.includes(element.id)}
                saving={element.id === savingElementId}
                settling={settlingElementIds.has(element.id)}
                playingEntranceAnimation={playingAnimation?.elementId === element.id ? playingAnimation.animationId : undefined}
                zoom={zoom}
                onSelect={handleSelectElement}
                onDragMove={handleDragMove}
                onDragEnd={handleDragEnd}
                onResizeMove={handleResizeMove}
                onResizeEnd={handleResizeEnd}
                onRotateMove={handleRotateMove}
                onRotateEnd={handleRotateEnd}
                onStartEditText={handleStartEditText}
              />
            ))}

            {/* BLOQUE F (post-MVP): "empty states" typography — a board
                with zero elements used to just be a bare rectangle, no
                different from an empty CRUD table. `pointer-events: none`
                (VisionBoardCanvas.module.css's `.canvasEmptyHint`) so it
                never intercepts the canvas's own click-to-deselect,
                right-click menu, or paste/drop handlers — purely a visual
                hint, gone the instant a first element exists (`elements`
                is the same live array driving the `.map` above, so this
                needs no separate state). */}
            {elements.length === 0 && (
              <div className={styles.canvasEmptyHint} aria-hidden="true">
                <Sparkles width={28} height={28} strokeWidth={1.5} />
                <p className={styles.canvasEmptyHintTitle}>Tu Vision Board está en blanco</p>
                <p className={styles.canvasEmptyHintSubtitle}>
                  Agrega texto, imágenes, formas o stickers desde "Elementos" para empezar a construirlo.
                </p>
              </div>
            )}

            {/* BLOQUE C (post-MVP): the inline text editor for a SHAPE —
                rendered as its own overlay (not inside the memoized
                VisionBoardElementView above) so starting/stopping an edit
                never has to touch that component's own props/memoization.
                Positioned with the exact same x/y/width/height/rotation
                style every element itself uses. */}
            {editingTextElementId &&
              (() => {
                const editingElement = elements.find((el) => el.id === editingTextElementId)
                return editingElement ? (
                  <VisionBoardShapeTextEditor
                    element={editingElement}
                    onCommit={handleCommitEditText}
                    onCancel={handleCancelEditText}
                  />
                ) : null
              })()}

            {/* FASE 9: Smart Guides — purely visual, never part of `elements`
                or sent to the backend; cleared the instant a drag/resize
                gesture ends. */}
            {activeGuides.map((guide, index) =>
              guide.orientation === 'vertical' ? (
                <span
                  key={`v-${index}-${guide.position}`}
                  aria-hidden="true"
                  className={styles.guideLineVertical}
                  style={{ left: guide.position }}
                />
              ) : (
                <span
                  key={`h-${index}-${guide.position}`}
                  aria-hidden="true"
                  className={styles.guideLineHorizontal}
                  style={{ top: guide.position }}
                />
              ),
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

/** BLOQUE C (post-MVP): the inline SHAPE text editor — a real `<textarea>`
    positioned exactly over the element (same x/y/width/height/rotation
    style every VisionBoardElementView instance uses), not a second overlay
    nested inside that component's own `<button>` (a `<textarea>` inside a
    `<button>` is invalid HTML and risks real focus/click-forwarding
    quirks) — see this file's own render for why it's a sibling instead.
    Enter (no Shift) or blur commits; Escape cancels — both call back into
    VisionBoardCanvas, which owns the actual persist/undo (this component
    holds only the in-progress draft string). */
function VisionBoardShapeTextEditor({
  element,
  onCommit,
  onCancel,
}: {
  element: VisionBoardElement
  onCommit: (elementId: string, text: string) => void
  onCancel: (elementId: string) => void
}) {
  const [value, setValue] = useState(typeof element.data.text === 'string' ? element.data.text : '')
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    textareaRef.current?.focus()
    textareaRef.current?.select()
  }, [])

  const style: CSSProperties = {
    left: element.x,
    top: element.y,
    width: element.width,
    height: element.height,
    transform: `rotate(${element.rotation}deg)`,
    zIndex: 999_999,
  }

  return (
    <textarea
      ref={textareaRef}
      className={styles.shapeTextEditor}
      style={style}
      value={value}
      aria-label="Texto de la forma"
      onChange={(event) => setValue(event.target.value)}
      onClick={(event) => event.stopPropagation()}
      onPointerDown={(event) => event.stopPropagation()}
      onBlur={() => onCommit(element.id, value)}
      onKeyDown={(event) => {
        if (event.key === 'Escape') {
          event.stopPropagation()
          onCancel(element.id)
        } else if (event.key === 'Enter' && !event.shiftKey) {
          event.preventDefault()
          onCommit(element.id, value)
        }
      }}
    />
  )
}
