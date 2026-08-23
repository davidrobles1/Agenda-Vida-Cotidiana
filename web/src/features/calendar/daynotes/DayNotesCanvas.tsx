import { useEffect, useMemo, useRef, useState, type PointerEvent as ReactPointerEvent } from 'react'
import { IconBold, IconItalic, IconPlus, IconTrash } from '../../../core/ui/icons'
import { shapeDefOf, shapesByFamily } from '../../visionboard/visionBoardShapes'
import { computeDragSnap, type Guide } from '../../visionboard/snapGuides'
import {
  createDayNoteElement,
  deleteDayNoteElement,
  editDayNoteElementData,
  listDayNoteElements,
  moveDayNoteElement,
  type DayNoteElement,
  type DayNoteElementData,
  type DayNoteElementType,
} from './api'
import styles from './DayNotesCanvas.module.css'

/**
 * Canvas de notas por día (pedido explícito del usuario, 2026-08-22):
 * "Reemplazar la vista actual de notas por un único Canvas, similar al de
 * Visión Board." Único, propio de este componente — sin panel/widget de
 * notas en ningún otro lugar del calendario (ese requisito explícito es
 * justo por qué esto vive en su propio archivo en vez de reutilizar
 * VisionBoardCanvas: las reglas son deliberadamente más estrechas — solo 2
 * tipos, sin rotación/resize, sin múltiples tableros — no la misma
 * herramienta genérica).
 */

const BANNER_SIZE = { width: 208, height: 68 }
const TEXT_SIZE = { width: 176, height: 52 }
const GAP = 10
const CANVAS_HEIGHT = 520
const DRAG_THRESHOLD = 5

/** 2026-08-22 (pedido explícito del usuario, ampliar con el catálogo de
    Vision Board): el banner ya no está fijo en "Cinta" — puede ser
    cualquiera de las 5 formas de la familia "banners" del mismo catálogo
    (visionBoardShapes.ts). Deliberadamente NO se abre a otras familias
    (estrellas, flechas, etc.) — el usuario pidió explícitamente "1 tipo de
    forma: sticker/banner", esto solo enriquece esa única forma, no la
    convierte en un picker genérico como el de Vision Board. */
const BANNER_SHAPE_OPTIONS = shapesByFamily('banners')

const FONT_ROLES: Array<{ id: NonNullable<DayNoteElementData['font']>; label: string; family: string }> = [
  { id: 'sans', label: 'Sans', family: 'var(--font-sans)' },
  { id: 'serif', label: 'Serif', family: 'var(--font-serif)' },
  { id: 'script', label: 'Script', family: 'var(--font-script)' },
  { id: 'mono', label: 'Mono', family: 'var(--font-mono)' },
]
function fontFamilyOf(font: DayNoteElementData['font']): string {
  return FONT_ROLES.find((f) => f.id === font)?.family ?? 'var(--font-sans)'
}

interface DayNotesCanvasProps {
  dateKey: string
}

interface Box {
  x: number
  y: number
  width: number
  height: number
}

function overlaps(a: Box, b: Box): boolean {
  return a.x < b.x + b.width && a.x + a.width > b.x && a.y < b.y + b.height && a.y + a.height > b.y
}

function findFreeSlot(existing: Box[], width: number, height: number, canvasWidth: number): Box {
  const candidate = { x: GAP, y: GAP, width, height }
  for (let row = 0; row < 40; row++) {
    candidate.y = GAP + row * (Math.max(BANNER_SIZE.height, TEXT_SIZE.height) + GAP)
    for (let col = 0; col < 20; col++) {
      candidate.x = GAP + col * (Math.max(BANNER_SIZE.width, TEXT_SIZE.width) + GAP)
      if (candidate.x + width > canvasWidth) break
      if (!existing.some((box) => overlaps(candidate, box))) {
        return { ...candidate }
      }
    }
  }
  // Sin hueco visible: apila debajo de todo (el canvas hace scroll vertical).
  const maxBottom = existing.reduce((max, box) => Math.max(max, box.y + box.height), 0)
  return { x: GAP, y: maxBottom + GAP, width, height }
}

export function DayNotesCanvas({ dateKey }: DayNotesCanvasProps) {
  const [elements, setElements] = useState<DayNoteElement[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [draftText, setDraftText] = useState('')
  const [draftBold, setDraftBold] = useState(false)
  const [draftItalic, setDraftItalic] = useState(false)
  const [draftFont, setDraftFont] = useState<DayNoteElementData['font']>('sans')
  const [draftShape, setDraftShape] = useState('ribbon')
  const [guides, setGuides] = useState<Guide[]>([])

  const canvasRef = useRef<HTMLDivElement>(null)
  const dragRef = useRef<{
    id: string
    pointerId: number
    startClientX: number
    startClientY: number
    originX: number
    originY: number
    lastValidX: number
    lastValidY: number
    moved: boolean
  } | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    setEditingId(null)
    listDayNoteElements(dateKey)
      .then((data) => {
        if (!cancelled) setElements(data)
      })
      .catch(() => {
        if (!cancelled) setError('No se pudieron cargar las notas de este día.')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [dateKey])

  const canvasWidth = canvasRef.current?.clientWidth ?? 320

  async function handleAdd(type: DayNoteElementType) {
    const size = type === 'BANNER' ? BANNER_SIZE : TEXT_SIZE
    const slot = findFreeSlot(elements, size.width, size.height, canvasWidth)
    const defaultText = type === 'BANNER' ? 'Nuevo banner' : 'Nueva nota'
    try {
      const created = await createDayNoteElement({
        noteDate: dateKey,
        type,
        ...slot,
        data: { text: defaultText, bold: false, italic: false },
      })
      setElements((prev) => [...prev, created])
      startEditing(created)
    } catch {
      setError('No se pudo crear el elemento.')
    }
  }

  function startEditing(element: DayNoteElement) {
    setEditingId(element.id)
    setDraftText(element.data.text ?? '')
    setDraftBold(Boolean(element.data.bold))
    setDraftItalic(Boolean(element.data.italic))
    setDraftFont(element.data.font ?? 'sans')
    setDraftShape(element.data.shape ?? 'ribbon')
  }

  async function commitEditing() {
    const id = editingId
    if (!id) return
    const element = elements.find((el) => el.id === id)
    setEditingId(null)
    if (!element) return
    const nextData: DayNoteElementData = {
      text: draftText,
      bold: draftBold,
      italic: draftItalic,
      font: draftFont,
      ...(element.type === 'BANNER' ? { shape: draftShape } : {}),
    }
    if (
      element.data.text === nextData.text &&
      Boolean(element.data.bold) === nextData.bold &&
      Boolean(element.data.italic) === nextData.italic &&
      (element.data.font ?? 'sans') === nextData.font &&
      (element.data.shape ?? 'ribbon') === (nextData.shape ?? 'ribbon')
    ) {
      return
    }
    try {
      const updated = await editDayNoteElementData(id, nextData, element.version)
      setElements((prev) => prev.map((el) => (el.id === id ? updated : el)))
    } catch {
      setError('No se pudo guardar el texto.')
    }
  }

  async function handleDelete(id: string) {
    if (editingId === id) setEditingId(null)
    setElements((prev) => prev.filter((el) => el.id !== id))
    try {
      await deleteDayNoteElement(id)
    } catch {
      setError('No se pudo eliminar el elemento.')
      listDayNoteElements(dateKey).then(setElements).catch(() => undefined)
    }
  }

  function handlePointerDown(event: ReactPointerEvent<HTMLDivElement>, element: DayNoteElement) {
    if (editingId === element.id) return
    event.currentTarget.setPointerCapture(event.pointerId)
    dragRef.current = {
      id: element.id,
      pointerId: event.pointerId,
      startClientX: event.clientX,
      startClientY: event.clientY,
      originX: element.x,
      originY: element.y,
      lastValidX: element.x,
      lastValidY: element.y,
      moved: false,
    }
  }

  function handlePointerMove(event: ReactPointerEvent<HTMLDivElement>) {
    const drag = dragRef.current
    if (!drag || event.pointerId !== drag.pointerId) return
    const element = elements.find((el) => el.id === drag.id)
    if (!element) return

    const deltaX = event.clientX - drag.startClientX
    const deltaY = event.clientY - drag.startClientY
    if (!drag.moved && Math.hypot(deltaX, deltaY) < DRAG_THRESHOLD) return
    drag.moved = true

    const canvasHeight = canvasRef.current?.scrollHeight ?? CANVAS_HEIGHT
    const rawX = Math.max(0, Math.min(drag.originX + deltaX, canvasWidth - element.width))
    const rawY = Math.max(0, drag.originY + deltaY)

    const others = elements.filter((el) => el.id !== drag.id)
    const snapped = computeDragSnap(rawX, rawY, element.width, element.height, others, canvasWidth, canvasHeight, 1)

    const candidateBox: Box = { x: snapped.x, y: snapped.y, width: element.width, height: element.height }
    const wouldOverlap = others.some((other) => overlaps(candidateBox, other))

    if (wouldOverlap) {
      // Pedido explícito del usuario: "se detiene en el borde más cercano
      // sin encimar" — se congela en el último frame válido, no retrocede
      // al origen.
      setGuides([])
      return
    }

    drag.lastValidX = snapped.x
    drag.lastValidY = snapped.y
    setGuides(snapped.guides)
    setElements((prev) => prev.map((el) => (el.id === drag.id ? { ...el, x: snapped.x, y: snapped.y } : el)))
  }

  async function handlePointerUp(event: ReactPointerEvent<HTMLDivElement>) {
    const drag = dragRef.current
    if (!drag || event.pointerId !== drag.pointerId) return
    dragRef.current = null
    setGuides([])

    const element = elements.find((el) => el.id === drag.id)
    if (!drag.moved || !element) return

    try {
      const updated = await moveDayNoteElement(drag.id, {
        x: drag.lastValidX,
        y: drag.lastValidY,
        width: element.width,
        height: element.height,
        version: element.version,
      })
      setElements((prev) => prev.map((el) => (el.id === drag.id ? updated : el)))
    } catch {
      setError('No se pudo mover el elemento.')
      listDayNoteElements(dateKey).then(setElements).catch(() => undefined)
    }
  }

  const canvasHeightStyle = useMemo(() => {
    const maxBottom = elements.reduce((max, el) => Math.max(max, el.y + el.height), 0)
    return Math.max(CANVAS_HEIGHT, maxBottom + GAP * 4)
  }, [elements])

  return (
    <div className={styles.wrapper}>
      <div className={styles.toolbar}>
        <span className={styles.toolbarTitle}>Notas del día</span>
        <div className={styles.toolbarActions}>
          <button type="button" className={styles.addButton} onClick={() => handleAdd('BANNER')}>
            <IconPlus width={14} height={14} /> Banner
          </button>
          <button type="button" className={styles.addButton} onClick={() => handleAdd('TEXT')}>
            <IconPlus width={14} height={14} /> Texto
          </button>
        </div>
      </div>

      {error && <p className={styles.error}>{error}</p>}

      <div ref={canvasRef} className={styles.canvas} style={{ height: canvasHeightStyle }}>
        {loading && <p className={styles.hint}>Cargando…</p>}

        {!loading && elements.length === 0 && (
          <p className={styles.hint}>
            Aún no hay notas para este día. Agrega un banner o un texto arriba.
          </p>
        )}

        {guides.map((guide, index) => (
          <div
            key={index}
            className={guide.orientation === 'vertical' ? styles.guideVertical : styles.guideHorizontal}
            style={guide.orientation === 'vertical' ? { left: guide.position } : { top: guide.position }}
          />
        ))}

        {elements.map((element) => {
          const isEditing = editingId === element.id
          const isBanner = element.type === 'BANNER'
          return (
            <div
              key={element.id}
              className={isBanner ? styles.elementBanner : styles.elementText}
              style={{
                left: element.x,
                top: element.y,
                width: element.width,
                height: element.height,
                zIndex: element.zIndex,
              }}
              onPointerDown={(event) => handlePointerDown(event, element)}
              onPointerMove={handlePointerMove}
              onPointerUp={handlePointerUp}
              onPointerCancel={handlePointerUp}
              onDoubleClick={() => {
                // Pedido explícito del usuario (2026-08-23): editar el
                // texto directamente necesita 2 clics seguidos, no 1 —
                // mismo gesto que ya usa Vision Board para editar el texto
                // de una SHAPE directamente sobre el elemento
                // (VisionBoardElementView.tsx's `onDoubleClick`), así que
                // ambos "editar directo sobre el elemento" quedan
                // consistentes en toda la app.
                if (!isEditing) startEditing(element)
              }}
            >
              <div className={styles.elementInner}>
                {isBanner && (
                  <svg
                    className={styles.bannerShape}
                    viewBox="0 0 100 100"
                    preserveAspectRatio="none"
                    aria-hidden="true"
                  >
                    <path d={shapeDefOf(element.data.shape ?? 'ribbon').path} />
                  </svg>
                )}

                {!isEditing && (
                  <span
                    className={styles.textLabel}
                    style={{
                      fontWeight: element.data.bold ? 700 : 500,
                      fontStyle: element.data.italic ? 'italic' : 'normal',
                      fontFamily: fontFamilyOf(element.data.font),
                    }}
                  >
                    {element.data.text}
                  </span>
                )}
              </div>

              <button
                type="button"
                className={styles.deleteButton}
                aria-label="Eliminar elemento"
                onPointerDown={(event) => event.stopPropagation()}
                onClick={(event) => {
                  event.stopPropagation()
                  handleDelete(element.id)
                }}
              >
                <IconTrash width={12} height={12} />
              </button>

              {isEditing && (
                <div className={styles.editorLayer} onPointerDown={(event) => event.stopPropagation()}>
                  <div className={styles.editorToolbar}>
                    {/* Hallazgo real (Playwright, 2026-08-22): un botón dentro
                        del toolbar le quita el foco al textarea en el propio
                        mousedown, ANTES de que su click llegue a disparar —
                        eso ejecutaba commitEditing() con el valor viejo de
                        draftBold/draftItalic (el toggle se perdía) y cerraba
                        el editor de inmediato. preventDefault en mousedown
                        evita que el textarea pierda el foco, así el click de
                        alternar sí llega a aplicarse antes de cualquier blur
                        real (clic fuera del elemento). */}
                    <button
                      type="button"
                      className={draftBold ? styles.formatButtonActive : styles.formatButton}
                      aria-pressed={draftBold}
                      aria-label="Negrita"
                      onMouseDown={(event) => event.preventDefault()}
                      onClick={() => setDraftBold((prev) => !prev)}
                    >
                      <IconBold width={13} height={13} />
                    </button>
                    <button
                      type="button"
                      className={draftItalic ? styles.formatButtonActive : styles.formatButton}
                      aria-pressed={draftItalic}
                      aria-label="Cursiva"
                      onMouseDown={(event) => event.preventDefault()}
                      onClick={() => setDraftItalic((prev) => !prev)}
                    >
                      <IconItalic width={13} height={13} />
                    </button>
                    <select
                      className={styles.fontSelect}
                      aria-label="Tipografía"
                      value={draftFont}
                      onMouseDown={(event) => event.preventDefault()}
                      onChange={(event) => setDraftFont(event.target.value as DayNoteElementData['font'])}
                    >
                      {FONT_ROLES.map((f) => (
                        <option key={f.id} value={f.id}>
                          {f.label}
                        </option>
                      ))}
                    </select>
                    {isBanner && (
                      <div className={styles.shapeSwatches} role="radiogroup" aria-label="Forma del banner">
                        {BANNER_SHAPE_OPTIONS.map((shape) => (
                          <button
                            key={shape.id}
                            type="button"
                            role="radio"
                            aria-checked={draftShape === shape.id}
                            className={draftShape === shape.id ? styles.shapeSwatchActive : styles.shapeSwatch}
                            title={shape.label}
                            onMouseDown={(event) => event.preventDefault()}
                            onClick={() => setDraftShape(shape.id)}
                          >
                            <svg viewBox="0 0 100 100" aria-hidden="true">
                              <path d={shape.path} />
                            </svg>
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                  <textarea
                    autoFocus
                    className={styles.editorTextarea}
                    style={{
                      fontWeight: draftBold ? 700 : 500,
                      fontStyle: draftItalic ? 'italic' : 'normal',
                      fontFamily: fontFamilyOf(draftFont),
                    }}
                    value={draftText}
                    onChange={(event) => setDraftText(event.target.value)}
                    onBlur={commitEditing}
                    onKeyDown={(event) => {
                      if (event.key === 'Escape') {
                        event.currentTarget.blur()
                      }
                      if (event.key === 'Enter' && (event.metaKey || event.ctrlKey)) {
                        event.currentTarget.blur()
                      }
                    }}
                  />
                </div>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
