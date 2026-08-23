import { memo, useRef, useState, type CSSProperties, type PointerEvent as ReactPointerEvent, type ReactElement } from 'react'
import { ImageOff, Lock } from 'lucide-react'
import { findStickerOption } from '../../core/ui/pickers/pickerCatalog'
import { shapeVariantOf, type VisionBoardElement } from './api'
import type { EntranceAnimationId } from './VisionBoardAnimationAction'
import { findEmojiOption } from './visionBoardEmojis'
import { useVisionBoardImageSrc } from './visionBoardImages'
import { buildChart, CHART_TYPE_OPTIONS } from './visionBoardCharts'
import { frameStyleOf } from './visionBoardFrames'
import { GRID_CELL_COLORS, gridLayoutOf } from './visionBoardGrids'
import { shapeDefOf } from './visionBoardShapes'
import styles from './VisionBoardCanvas.module.css'

/** FASE 5: smallest size a resize is allowed to shrink an element to.
    Exported so FASE 9's smart-guide snapping (VisionBoardCanvas.tsx) clamps
    a snapped width/height to the same floor instead of duplicating the
    number. */
export const MIN_SIZE = 24

interface VisionBoardElementViewProps {
  element: VisionBoardElement
  selected: boolean
  saving: boolean
  /** BLOQUE G (post-MVP): true for ~260ms right after this element's own
      drag/resize/rotate gesture ends — drives a brief, reduced-motion-safe
      settle animation (see VisionBoardCanvas.module.css's `.settling`).
      Never true during the live gesture itself. */
  settling: boolean
  /** 2026-08-22 (pedido explícito del usuario, catálogo estilo Canva):
      distinto de `settling` — no es una reacción a un gesto, es una
      reproducción bajo demanda de la animación de entrada elegida en
      VisionBoardAnimationAction.tsx (botón "Animar" del toolbar). `undefined`
      la mayor parte del tiempo; VisionBoardCanvas.tsx la pone por ~650ms
      solo en el elemento que se acaba de animar y la limpia después —
      nunca se reproduce sola al cargar el tablero (evitar que "todo tiemble"
      cada vez que se abre). */
  playingEntranceAnimation?: EntranceAnimationId
  /** FASE 10: current canvas zoom (1 = 100%) — screen-space pointer deltas
      must be divided by this to land in real canvas-space units; see the
      class doc comment below for why rotate doesn't need it too. */
  zoom: number
  /** FASE 11: `shiftKey` — true when the selection should toggle this
      element (add/remove) instead of replacing it. Fires from either
      pointerdown (mouse) or click (keyboard activation) — never both for
      the same gesture, see `pointerHandledSelectionRef` below.
      FASE 21 perf fix: every callback below now takes `elementId` as its
      first argument (this component passes its own `element.id`), instead
      of VisionBoardCanvas.tsx wrapping each one in a fresh per-element
      arrow function inside its `.map()`. That let VisionBoardCanvas pass
      the exact same `useCallback`'d function reference to every element —
      required for `React.memo` below to actually skip re-rendering
      elements that aren't involved in the current gesture; see
      VisionBoardCanvas.tsx's own `handleDragMove` doc comment for the full
      reasoning. */
  onSelect: (elementId: string, shiftKey: boolean) => void
  /** Live position during a drag — parent updates its optimistic copy of
      the element; no network call happens here (see VisionBoardCanvas). */
  onDragMove: (elementId: string, x: number, y: number) => void
  /** Drag finished — parent persists via PUT. `startX`/`startY` let the
      parent revert cleanly if that PUT fails, and skip the call entirely
      when nothing actually moved (a plain click/tap). */
  onDragEnd: (elementId: string, x: number, y: number, startX: number, startY: number) => void
  /** Same shape as onDragMove/onDragEnd, for width/height (FASE 5). */
  onResizeMove: (elementId: string, width: number, height: number) => void
  onResizeEnd: (elementId: string, width: number, height: number, startWidth: number, startHeight: number) => void
  /** Same shape again, for rotation in degrees (FASE 5). */
  onRotateMove: (elementId: string, rotation: number) => void
  onRotateEnd: (elementId: string, rotation: number, startRotation: number) => void
  /** BLOQUE C (post-MVP): double-click opens inline text editing for a
      SHAPE (VisionBoardCanvas.tsx's own `VisionBoardShapeTextEditor`
      renders as a sibling overlay on top of this element once it's
      active — nothing here needs to know editing is in progress, the
      overlay simply covers this button's own rendered text). */
  onStartEditText: (elementId: string) => void
}

/** BLOQUE D (post-MVP) fix — root cause of "mover una imagen/sticker recién
    creado puede hacer que desaparezca": confirmed for real, via live
    instrumentation counting actual `pointermove` deliveries (not guessed),
    that dragging a STICKER/IMAGE element specifically (never TEXT/NOTE/
    SHAPE, confirmed by reproducing the identical gesture against a TEXT
    element under the same conditions and seeing it work perfectly) stopped
    receiving `pointermove` events after the very first one, every time.
    The actual cause: an `<img>` element is *draggable by default* in every
    browser (no `draggable="false"` was ever set) — mid-gesture, the
    browser's own native HTML5 image drag-and-drop engages and takes over
    the pointer from this component's `setPointerCapture()`-based custom
    drag, silently starving `handlePointerMove` of further events. The
    eventual `pointerup` that *does* arrive comes through whatever's left
    of that hijacked gesture, sometimes carrying nowhere-near-real
    coordinates (`clientX: 0, clientY: 0` was reproduced directly) — which
    a naive `startX + (event.clientX - startClientX)` computation applies
    as if the pointer had jumped to the screen's corner, flinging the
    element to a wildly wrong (often negative, off-board) position. The
    element itself was never deleted, it just landed somewhere nobody would
    think to look. Two-part fix: `draggable={false}` on both `<img>` tags
    below (the real fix — never let the native image drag start at all,
    confirmed to restore all 9 expected pointermove deliveries) plus, as
    defense in depth against any *other* source of a bad pointerup reading,
    `lastClientX`/`lastClientY` (updated on every real `pointermove`) so
    every gesture's *end* handler computes its final delta from the last
    known-good move position instead of trusting the up event's own
    coordinates, which are only ever used to identify *that* the gesture
    ended, never *where*. */
interface DragState {
  pointerId: number
  startClientX: number
  startClientY: number
  lastClientX: number
  lastClientY: number
  startX: number
  startY: number
}

interface ResizeState {
  pointerId: number
  axis: 'e' | 's' | 'se'
  startClientX: number
  startClientY: number
  lastClientX: number
  lastClientY: number
  startWidth: number
  startHeight: number
  /** The element's rotation while resizing — fixed for the gesture's
      duration (Fase 5 never resizes and rotates in the same gesture), used
      to translate on-screen pointer movement into the element's own
      unrotated width/height axes (see `toLocalDelta`). */
  rotation: number
}

interface RotateState {
  pointerId: number
  /** Screen-space center of the element, captured once at gesture start.
      Not re-measured on every move — panning (native scroll) *during* an
      in-progress rotate is the one case this can go slightly stale for,
      same as it already could between Fase 5 and Fase 10 for reasons
      outside this component's control; narrow enough (holding a rotate
      handle down while also scrolling) that re-measuring every pointermove
      isn't worth it for this phase. */
  centerClientX: number
  centerClientY: number
  lastClientX: number
  lastClientY: number
  startRotation: number
}

/** Rotates a screen-space pointer delta into the element's own (unrotated)
    axes, so dragging "right" on a 90°-rotated element still changes the
    axis that visually reads as width, not the one that happens to align
    with the screen. Undoing the element's own rotation is what "resize
    handles rotate with the element but must still resize along its own
    edges" requires — this is that undo. */
function toLocalDelta(dxScreen: number, dyScreen: number, rotationDeg: number): { dx: number; dy: number } {
  const rad = (-rotationDeg * Math.PI) / 180
  const cos = Math.cos(rad)
  const sin = Math.sin(rad)
  return {
    dx: dxScreen * cos - dyScreen * sin,
    dy: dxScreen * sin + dyScreen * cos,
  }
}

/** Angle from a fixed center to the current pointer, in degrees, adjusted
    so "pointer straight above center" reads as 0° — matches the rotate
    handle's own resting position (top-center of the element). */
function angleFromCenter(centerX: number, centerY: number, pointerX: number, pointerY: number): number {
  const radians = Math.atan2(pointerY - centerY, pointerX - centerX)
  return radians * (180 / Math.PI) + 90
}

/**
 * FASE 3: position/select. FASE 4 adds free-form dragging via Pointer
 * Events + `setPointerCapture` — deliberately not HTML5 Drag and Drop
 * (already used elsewhere in this app, e.g. the calendar's Agenda
 * reschedule): native DnD is built for dropping onto discrete targets and
 * hands the visual feedback to the browser's own drag-image ghost, which
 * doesn't let the real element track the pointer continuously — exactly
 * what "debe moverse visualmente" during a free-form canvas drag needs.
 * Pointer Events need no library (same "no dependency added" bar as DnD)
 * and `setPointerCapture` keeps move/up events targeting this element even
 * once the pointer leaves its bounds, without any window-level listener to
 * attach/clean up by hand.
 *
 * FASE 5 reuses the exact same mechanism for resize/rotate, via small
 * handle elements that only render when `selected`. Each handle captures
 * its own pointer and calls `event.stopPropagation()` on pointerdown —
 * without that, the event would also bubble to this element's own
 * onPointerDown/onClick (starting a move-drag and re-toggling selection at
 * the same time as the resize/rotate gesture).
 *
 * FASE 10: `zoom` (VisionBoardCanvas's `transform: scale()` on `.canvas`)
 * means one screen pixel of pointer movement no longer equals one real
 * canvas-space unit — drag/resize divide the raw `clientX`/`clientY` delta
 * by `zoom` before applying it, so a 10-screen-pixel move at 200% zoom
 * correctly becomes a 5-unit change in x/y/width/height, matching what's
 * visually happening. Rotate needs no such correction: its angle comes
 * from `atan2` of a delta vector, and scaling a vector uniformly (which is
 * all `zoom` does to on-screen distances) never changes the *angle*
 * between two points, only the distance — and the center itself is read
 * via `getBoundingClientRect()`, which already reports the real
 * post-transform screen position regardless of zoom. Pan (native scroll on
 * `.viewport`) needs no correction anywhere here either: every calculation
 * below is a *delta* between two `clientX`/`clientY` samples taken in the
 * same scroll state or later, and scrolling shifts both samples by the
 * same amount, cancelling out of the subtraction.
 *
 * Still a plain `<button>`, not a React Aria component — see the FASE 3
 * doc comment this one continues: React Aria's selection primitives lay
 * out their own collection, which fights a free-form canvas rather than
 * helping it. Native pointer capture plus a semantic `<button>` (real
 * keyboard access, `aria-pressed` for selection) covers exactly what this
 * phase needs without inventing new ARIA. The resize/rotate handles are
 * plain, non-focusable `<span>`s — pointer-only by design for this phase,
 * same as the master plan's own Fase 10 (a full React Aria/keyboard pass
 * across the whole editor) not being now.
 *
 * FASE 21 perf fix: wrapped in `React.memo` (default shallow prop
 * comparison — no custom comparator needed). This only pays off because
 * every prop here is now cheap to compare correctly: `element` keeps a
 * stable object reference across renders for every element that ISN'T the
 * one being mutated (VisionBoardCanvas's `setElements` updaters always
 * return the same `el` reference, unchanged, for untouched elements — see
 * `handleDragMove`'s own doc comment), `selected`/`saving` are booleans,
 * `zoom` is a number, and the 7 callback props below are now stable
 * `useCallback` references from the parent rather than fresh closures
 * created per element on every render. Without ALL of that being true at
 * once, `React.memo` here would do nothing — a single unstable prop is
 * enough to make every element "look changed" on every render.
 */
const ENTRANCE_CLASS: Record<EntranceAnimationId, keyof typeof styles> = {
  fadeIn: 'entranceFadeIn',
  slideUp: 'entranceSlideUp',
  slideLeft: 'entranceSlideLeft',
  popIn: 'entrancePopIn',
  bounceIn: 'entranceBounceIn',
  pulse: 'entrancePulse',
}

function VisionBoardElementViewImpl({
  element,
  selected,
  saving,
  settling,
  playingEntranceAnimation,
  zoom,
  onSelect,
  onDragMove,
  onDragEnd,
  onResizeMove,
  onResizeEnd,
  onRotateMove,
  onRotateEnd,
  onStartEditText,
}: VisionBoardElementViewProps) {
  const dragRef = useRef<DragState | null>(null)
  const resizeRef = useRef<ResizeState | null>(null)
  const rotateRef = useRef<RotateState | null>(null)
  /** FASE 11: onPointerDown and onClick both fire for a plain mouse click
      (pointerdown then, after release, a synthetic click) — calling
      onSelect from both would toggle a shift-click twice (select, then
      immediately deselect again). Set when pointerdown already handled
      selection for this gesture, so the following click is a no-op for
      selection purposes; stays false across a *keyboard* activation
      (Enter/Space fires only click, never pointerdown), so that path still
      calls onSelect itself — this is what "mantener selección mediante
      teclado" needs, without a second mechanism. */
  const pointerHandledSelectionRef = useRef(false)

  if (!element.visible) return null

  function handlePointerDown(event: ReactPointerEvent<HTMLButtonElement>) {
    // Fase 8 owns the real "lock" UI; nothing sets `locked` yet, but the
    // field exists precisely so a drag can already respect it once
    // something does — avoids having to revisit this handler then. A
    // locked element still falls through to onClick's own onSelect call
    // below, so it stays selectable — only dragging is refused.
    if (element.locked || !event.isPrimary) return
    event.currentTarget.setPointerCapture(event.pointerId)
    pointerHandledSelectionRef.current = true
    onSelect(element.id, event.shiftKey)
    dragRef.current = {
      pointerId: event.pointerId,
      startClientX: event.clientX,
      startClientY: event.clientY,
      lastClientX: event.clientX,
      lastClientY: event.clientY,
      startX: element.x,
      startY: element.y,
    }
  }

  function handlePointerMove(event: ReactPointerEvent<HTMLButtonElement>) {
    const drag = dragRef.current
    if (!drag || drag.pointerId !== event.pointerId) return
    drag.lastClientX = event.clientX
    drag.lastClientY = event.clientY
    onDragMove(
      element.id,
      drag.startX + (event.clientX - drag.startClientX) / zoom,
      drag.startY + (event.clientY - drag.startClientY) / zoom,
    )
  }

  function handlePointerUp(event: ReactPointerEvent<HTMLButtonElement>) {
    const drag = dragRef.current
    if (!drag || drag.pointerId !== event.pointerId) return
    dragRef.current = null
    // BLOQUE D fix: `drag.lastClientX/Y` (the last real pointermove), never
    // `event.clientX/Y` — see this file's DragState doc comment for why.
    onDragEnd(
      element.id,
      drag.startX + (drag.lastClientX - drag.startClientX) / zoom,
      drag.startY + (drag.lastClientY - drag.startClientY) / zoom,
      drag.startX,
      drag.startY,
    )
  }

  function handleResizePointerDown(axis: 'e' | 's' | 'se', event: ReactPointerEvent<HTMLSpanElement>) {
    if (element.locked || !event.isPrimary) return
    event.stopPropagation()
    event.currentTarget.setPointerCapture(event.pointerId)
    resizeRef.current = {
      pointerId: event.pointerId,
      axis,
      startClientX: event.clientX,
      startClientY: event.clientY,
      lastClientX: event.clientX,
      lastClientY: event.clientY,
      startWidth: element.width,
      startHeight: element.height,
      rotation: element.rotation,
    }
  }

  function handleResizePointerMove(event: ReactPointerEvent<HTMLSpanElement>) {
    const resize = resizeRef.current
    if (!resize || resize.pointerId !== event.pointerId) return
    event.stopPropagation()
    resize.lastClientX = event.clientX
    resize.lastClientY = event.clientY
    const { width, height } = computeResize(resize, event.clientX, event.clientY)
    onResizeMove(element.id, width, height)
  }

  function handleResizePointerUp(event: ReactPointerEvent<HTMLSpanElement>) {
    const resize = resizeRef.current
    if (!resize || resize.pointerId !== event.pointerId) return
    event.stopPropagation()
    resizeRef.current = null
    // BLOQUE D fix: same as drag — the last real pointermove, never the up
    // event's own (occasionally zeroed) coordinates.
    const { width, height } = computeResize(resize, resize.lastClientX, resize.lastClientY)
    onResizeEnd(element.id, width, height, resize.startWidth, resize.startHeight)
  }

  function computeResize(resize: ResizeState, clientX: number, clientY: number) {
    const { dx, dy } = toLocalDelta(
      (clientX - resize.startClientX) / zoom,
      (clientY - resize.startClientY) / zoom,
      resize.rotation,
    )
    let width = resize.axis === 's' ? resize.startWidth : Math.max(MIN_SIZE, resize.startWidth + dx)
    let height = resize.axis === 'e' ? resize.startHeight : Math.max(MIN_SIZE, resize.startHeight + dy)

    // Pedido explícito del usuario (2026-08-21): "revisa por qué se
    // deforman raramente las imágenes al cambiar el tamaño... adecua para
    // que no se deforme." Ninguna imagen llega a estirar sus propios
    // píxeles — tanto la vista en vivo (`.elementImage`'s own `object-fit:
    // cover`) como la exportación (visionBoardExport.ts's `drawCoverFit`)
    // ya preservan proporción por construcción, recortando en vez de
    // estirar. Lo que sí podía pasar sin este bloque: arrastrar un solo
    // manejador (E o S, "single-edge") cambiaba un solo eje libremente,
    // así que `object-fit: cover` terminaba recortando la foto de forma
    // muy distinta a como se veía un segundo antes — visualmente eso se
    // lee como "se deformó" aunque ningún píxel se haya estirado. Ahora,
    // solo para IMAGE, cualquier manejador (E, S o la esquina SE) escala
    // ambos ejes juntos, preservando la proporción con la que el elemento
    // empezó este gesto — mismo comportamiento de "resize proporcional"
    // que herramientas como Canva ya usan para fotos.
    if (element.type === 'IMAGE' && resize.startWidth > 0 && resize.startHeight > 0) {
      const aspect = resize.startWidth / resize.startHeight
      if (resize.axis === 'e') {
        height = Math.max(MIN_SIZE, width / aspect)
      } else if (resize.axis === 's') {
        width = Math.max(MIN_SIZE, height * aspect)
      } else {
        const scale = Math.max(width / resize.startWidth, height / resize.startHeight)
        width = Math.max(MIN_SIZE, resize.startWidth * scale)
        height = Math.max(MIN_SIZE, resize.startHeight * scale)
      }
    }

    return { width, height }
  }

  function handleRotatePointerDown(event: ReactPointerEvent<HTMLSpanElement>) {
    if (element.locked || !event.isPrimary) return
    event.stopPropagation()
    event.currentTarget.setPointerCapture(event.pointerId)
    const rect = event.currentTarget.parentElement!.getBoundingClientRect()
    rotateRef.current = {
      pointerId: event.pointerId,
      centerClientX: rect.left + rect.width / 2,
      centerClientY: rect.top + rect.height / 2,
      lastClientX: event.clientX,
      lastClientY: event.clientY,
      startRotation: element.rotation,
    }
  }

  function handleRotatePointerMove(event: ReactPointerEvent<HTMLSpanElement>) {
    const rotate = rotateRef.current
    if (!rotate || rotate.pointerId !== event.pointerId) return
    event.stopPropagation()
    rotate.lastClientX = event.clientX
    rotate.lastClientY = event.clientY
    onRotateMove(element.id, angleFromCenter(rotate.centerClientX, rotate.centerClientY, event.clientX, event.clientY))
  }

  function handleRotatePointerUp(event: ReactPointerEvent<HTMLSpanElement>) {
    const rotate = rotateRef.current
    if (!rotate || rotate.pointerId !== event.pointerId) return
    event.stopPropagation()
    rotateRef.current = null
    // BLOQUE D fix: same as drag/resize — the last real pointermove, never
    // the up event's own (occasionally zeroed) coordinates.
    onRotateEnd(
      element.id,
      angleFromCenter(rotate.centerClientX, rotate.centerClientY, rotate.lastClientX, rotate.lastClientY),
      rotate.startRotation,
    )
  }

  const style: CSSProperties = {
    left: element.x,
    top: element.y,
    width: element.width,
    height: element.height,
    transform: `rotate(${element.rotation}deg)`,
    zIndex: element.zIndex,
  }

  return (
    <button
      type="button"
      className={styles.element}
      aria-pressed={selected}
      aria-label={element.locked ? `${elementLabel(element)} (bloqueado)` : elementLabel(element)}
      data-selected={selected || undefined}
      data-saving={saving || undefined}
      data-locked={element.locked || undefined}
      data-type={element.type}
      data-element-id={element.id}
      style={style}
      onClick={(event) => {
        event.stopPropagation()
        // A real mouse click already got its onSelect call from
        // handlePointerDown above — only a keyboard activation (no
        // preceding pointerdown) still needs one here.
        if (pointerHandledSelectionRef.current) {
          pointerHandledSelectionRef.current = false
          return
        }
        onSelect(element.id, event.shiftKey)
      }}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerCancel={handlePointerUp}
      onDoubleClick={(event) => {
        if (element.type !== 'SHAPE' || element.locked) return
        event.stopPropagation()
        onStartEditText(element.id)
      }}
    >
      <span
        className={`${styles.elementContent} ${settling ? styles.settling : ''} ${
          playingEntranceAnimation ? styles[ENTRANCE_CLASS[playingEntranceAnimation]] : ''
        }`}
      >
        <ElementContent element={element} />
      </span>

      {/* FASE 17: "el elemento bloqueado debe seguir siendo identificable"
          — always visible when locked, not just while selected, same as a
          real corkboard pin would be. */}
      {element.locked && (
        <span className={styles.lockBadge} aria-hidden="true">
          <Lock width={11} height={11} />
        </span>
      )}

      {selected && !element.locked && (
        <>
          <span
            role="presentation"
            className={`${styles.resizeHandle} ${styles.resizeHandleE}`}
            onPointerDown={(event) => handleResizePointerDown('e', event)}
            onPointerMove={handleResizePointerMove}
            onPointerUp={handleResizePointerUp}
            onPointerCancel={handleResizePointerUp}
            onClick={(event) => event.stopPropagation()}
          />
          <span
            role="presentation"
            className={`${styles.resizeHandle} ${styles.resizeHandleS}`}
            onPointerDown={(event) => handleResizePointerDown('s', event)}
            onPointerMove={handleResizePointerMove}
            onPointerUp={handleResizePointerUp}
            onPointerCancel={handleResizePointerUp}
            onClick={(event) => event.stopPropagation()}
          />
          <span
            role="presentation"
            className={`${styles.resizeHandle} ${styles.resizeHandleSe}`}
            onPointerDown={(event) => handleResizePointerDown('se', event)}
            onPointerMove={handleResizePointerMove}
            onPointerUp={handleResizePointerUp}
            onPointerCancel={handleResizePointerUp}
            onClick={(event) => event.stopPropagation()}
          />
          <span
            role="presentation"
            aria-hidden="true"
            className={styles.rotateHandle}
            onPointerDown={handleRotatePointerDown}
            onPointerMove={handleRotatePointerMove}
            onPointerUp={handleRotatePointerUp}
            onPointerCancel={handleRotatePointerUp}
            onClick={(event) => event.stopPropagation()}
          />
        </>
      )}
    </button>
  )
}

// FASE 21 perf fix: see the doc comment on VisionBoardElementViewImpl above
// for why default shallow comparison is sufficient here (every prop is
// already cheap/correct to compare by the time this wraps it).
export const VisionBoardElementView = memo(VisionBoardElementViewImpl)

function elementLabel(element: VisionBoardElement): string {
  const text = typeof element.data.text === 'string' ? element.data.text : null
  switch (element.type) {
    case 'TEXT':
      return text ? `Texto: ${text}` : 'Elemento de texto'
    case 'NOTE':
      return text ? `Nota: ${text}` : 'Nota'
    case 'STICKER': {
      // BLOQUE D (post-MVP): "😊 Emojis" is a STICKER element with
      // `data.emojiId` instead of `data.stickerId` — see
      // visionBoardEmojis.ts's own doc comment.
      const emojiId = typeof element.data.emojiId === 'string' ? element.data.emojiId : undefined
      if (emojiId) {
        const emoji = findEmojiOption(emojiId)
        return emoji ? `Emoji: ${emoji.label}` : 'Emoji'
      }
      const sticker = findStickerOption(typeof element.data.stickerId === 'string' ? element.data.stickerId : undefined)
      return sticker ? `Sticker: ${sticker.label}` : 'Sticker'
    }
    case 'IMAGE':
      return 'Imagen'
    case 'SHAPE':
      return `Forma: ${shapeDefOf(shapeVariantOf(element.data)).label}`
    case 'TABLE':
      return 'Tabla'
    case 'CHART':
      return `Gráfica: ${CHART_TYPE_OPTIONS.find((c) => c.id === element.data.chartType)?.label ?? 'Barras'}`
    case 'GRID':
      return `Cuadrícula: ${gridLayoutOf(typeof element.data.layout === 'string' ? element.data.layout : undefined).label}`
  }
}

/**
 * Per-type rendering. Content (text/note copy, sticker, image URL, shape
 * variant) is now editable — see VisionBoardElementEditor.tsx (Fase 7).
 * Still no font/weight/color for TEXT or fill/stroke for SHAPE — nothing
 * in `data` carries them yet, and this phase only edits properties the
 * model already supports.
 */
function ElementContent({ element }: { element: VisionBoardElement }) {
  const text = typeof element.data.text === 'string' ? element.data.text : undefined

  switch (element.type) {
    case 'TEXT':
      return <span className={styles.elementText}>{text ?? 'Texto'}</span>
    case 'NOTE':
      return <span className={styles.elementNote}>{text ?? 'Nota'}</span>
    case 'STICKER': {
      // BLOQUE D (post-MVP): an emoji, exactly like a sticker except its
      // catalog entry is always an Icon+color badge (visionBoardEmojis.ts),
      // never a real asset file — checked first so `data.emojiId` always
      // wins over a (never-both-set) `data.stickerId`.
      const emojiId = typeof element.data.emojiId === 'string' ? element.data.emojiId : undefined
      if (emojiId) {
        const emoji = findEmojiOption(emojiId)
        return emoji ? (
          <span className={styles.elementStickerBadge} style={{ background: emoji.color }} aria-hidden="true">
            <emoji.Icon width="60%" height="60%" />
          </span>
        ) : (
          <span className={styles.elementPlaceholder}>Emoji</span>
        )
      }
      // FASE 6: real catalog contract — same stickerId + findStickerOption
      // used by Reminder/Note, not a raw emoji character.
      const stickerId = typeof element.data.stickerId === 'string' ? element.data.stickerId : undefined
      const sticker = findStickerOption(stickerId)
      if (!sticker) return <span className={styles.elementPlaceholder}>Sticker</span>
      // BLOQUE D: the expanded catalog's icon-badge entries (no `asset`
      // file — see StickerOption's own doc comment) render the same way
      // as an emoji above; the original 12 Fluent Emoji stickers keep
      // their unchanged `<img>` path.
      if (!sticker.asset) {
        return sticker.Icon ? (
          <span className={styles.elementStickerBadge} style={{ background: sticker.color }} aria-hidden="true">
            <sticker.Icon width="60%" height="60%" />
          </span>
        ) : (
          <span className={styles.elementPlaceholder}>Sticker</span>
        )
      }
      // FASE 21 perf fix: native, zero-dependency hints — a board with many
      // STICKER/IMAGE elements outside the current scroll position of
      // `.viewport` (the pannable container, its own scrollable ancestor
      // for `loading="lazy"` purposes) skips decoding those images' bytes
      // until they're actually scrolled into view, and `decoding="async"`
      // keeps whichever ones DO decode off the main thread instead of
      // blocking a paint. No behavior change — same image, same box.
      return (
        <img src={sticker.asset} alt="" className={styles.elementStickerImage} loading="lazy" decoding="async" draggable={false} />
      )
    }
    case 'IMAGE':
      return <ImageElementContent element={element} />

    case 'SHAPE': {
      const variant = shapeVariantOf(element.data)
      // BLOQUE C (post-MVP): rectangle/circle/line/capsule stay pure CSS
      // (unchanged since FASE 7) — every other variant renders its
      // catalog path as an SVG, `preserveAspectRatio="none"` so it
      // stretches non-uniformly under resize exactly like circle's own
      // `border-radius: 50%` already does on a non-square box (an
      // ellipse), the existing, expected "shapes deform to fit the box
      // unless you hold an aspect lock" behavior — nothing new here.
      let visual: ReactElement
      if (variant === 'circle' || variant === 'line' || variant === 'rectangle') {
        const variantClass = variant === 'circle' ? styles.elementShapeCircle : variant === 'line' ? styles.elementShapeLine : ''
        visual = <span className={`${styles.elementShape} ${variantClass}`} aria-hidden="true" />
      } else if (variant === 'capsule') {
        visual = <span className={`${styles.elementShape} ${styles.elementShapeCapsule}`} aria-hidden="true" />
      } else {
        const def = shapeDefOf(variant)
        visual = (
          <svg viewBox="0 0 100 100" preserveAspectRatio="none" className={styles.elementShapeSvg} aria-hidden="true">
            <path d={def.path} fillRule={def.fillRule} />
          </svg>
        )
      }
      // BLOQUE C: text lives directly on the shape (double-click/Enter to
      // edit, see onDoubleClick above and VisionBoardCanvas.tsx's own
      // VisionBoardShapeTextEditor) — an absolutely-centered overlay on
      // top of the shape visual, same "sits over the fill, never affects
      // its own layout" shape TEXT/NOTE don't need (they *are* the text)
      // but SHAPE does (the fill is the shape, the text is additional).
      const shapeText = typeof element.data.text === 'string' ? element.data.text : ''
      return (
        <span className={styles.elementShapeWrapper}>
          {visual}
          {shapeText && <span className={styles.elementShapeText}>{shapeText}</span>}
        </span>
      )
    }

    case 'TABLE':
      return <TableElementContent element={element} />

    case 'CHART': {
      const { viewBox, markup } = buildChart(typeof element.data.chartType === 'string' ? element.data.chartType : undefined)
      return (
        <span className={styles.elementChartWrapper}>
          {/* eslint-disable-next-line react/no-danger -- markup is our own
              generated SVG (visionBoardCharts.ts), never user input. */}
          <svg viewBox={viewBox} className={styles.elementChartSvg} aria-hidden="true" dangerouslySetInnerHTML={{ __html: markup }} />
        </span>
      )
    }

    case 'GRID': {
      const layout = gridLayoutOf(typeof element.data.layout === 'string' ? element.data.layout : undefined)
      return (
        <span
          className={styles.elementGrid}
          style={{ gridTemplateRows: layout.gridTemplateRows, gridTemplateColumns: layout.gridTemplateColumns }}
        >
          {Array.from({ length: layout.cells }, (_, index) => (
            <span
              key={index}
              className={styles.elementGridCell}
              style={{
                background: GRID_CELL_COLORS[index % GRID_CELL_COLORS.length],
                gridRow: layout.spanCell === index ? 'span 2' : undefined,
              }}
            />
          ))}
        </span>
      )
    }
  }
}

/** TABLE: solo lectura por ahora (mismo alcance que STICKER/IMAGE — se
    elige/coloca, no se edita celda por celda todavía; editar contenido
    completo de tabla es un incremento aparte, ver VisionBoardElementEditor.tsx
    si se decide agregarlo). `data.rows` inválido/ausente cae a una tabla
    de ejemplo 2×2, mismo criterio "valor por defecto razonable" que
    shapeDefOf ya usa. */
function TableElementContent({ element }: { element: VisionBoardElement }) {
  const raw = element.data.rows
  const rows = Array.isArray(raw) ? (raw as unknown[]).filter((row): row is string[] => Array.isArray(row)) : []
  const safeRows = rows.length > 0 ? rows : [['Columna 1', 'Columna 2'], ['', '']]
  return (
    <table className={styles.elementTable}>
      <tbody>
        {safeRows.map((row, rowIndex) => (
          <tr key={rowIndex}>
            {row.map((cell, cellIndex) => (
              <td key={cellIndex}>{cell}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  )
}

/** BLOQUE B (post-MVP): its own small component (not another `switch` case
    inline) so the extra `useState` for a real `<img>` load failure — on
    top of `useVisionBoardImageSrc`'s own resolve-time error, see that
    hook's own doc comment — doesn't complicate ElementContent's per-type
    switch above. Separate mount per IMAGE element, so this never violates
    the Rules of Hooks (`element.type` never changes for an already-mounted
    element). */
function ImageElementContent({ element }: { element: VisionBoardElement }) {
  const { src, error: resolveError } = useVisionBoardImageSrc(element.data)
  const [loadError, setLoadError] = useState(false)
  const broken = resolveError || loadError

  if (broken) {
    return (
      <div className={styles.elementImageBroken}>
        <ImageOff width={20} height={20} aria-hidden="true" />
        <span>Imagen no disponible</span>
      </div>
    )
  }
  if (!src) {
    return <span className={styles.elementPlaceholder}>Imagen</span>
  }

  // 2026-08-23 (pedido explícito del usuario, catálogo estilo Canva):
  // "Marcos" — un `data.frameStyle` opcional sobre esta misma IMAGE, real
  // desde el primer momento (a diferencia de TABLE/GRID) porque reutiliza
  // por completo esta carga/URL ya existente, solo cambia cómo se recorta/
  // enmarca.
  const frameStyle = frameStyleOf(typeof element.data.frameStyle === 'string' ? element.data.frameStyle : undefined)
  if (frameStyle?.kind === 'polaroid') {
    return (
      <span className={styles.elementImageFramePolaroid}>
        <img
          src={src}
          alt=""
          className={styles.elementImage}
          loading="lazy"
          decoding="async"
          draggable={false}
          onError={() => setLoadError(true)}
        />
      </span>
    )
  }
  return (
    <img
      src={src}
      alt=""
      className={`${styles.elementImage} ${frameStyle?.kind === 'rounded' ? styles.elementImageFrameRounded : ''} ${frameStyle?.kind === 'film' ? styles.elementImageFrameFilm : ''}`}
      style={frameStyle?.clipPath ? { clipPath: frameStyle.clipPath } : undefined}
      loading="lazy"
      decoding="async"
      draggable={false}
      onError={() => setLoadError(true)}
    />
  )
}
