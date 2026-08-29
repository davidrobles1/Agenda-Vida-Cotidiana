import { useEffect, useMemo, useRef, useState } from 'react'
import {
  createDayNoteElement,
  deleteDayNoteElement,
  editDayNoteElementData,
  listDayNoteElements,
  type DayNoteElement,
} from './api'
import { useActiveMode } from '../../../core/user/ActiveModeContext'
import styles from './DayNotesCanvas.module.css'

/**
 * Notas del día — "Notas en el margen" (propuesta visual aprobada por el
 * usuario, 2026-08-23).
 *
 * Las notas dejaron de ser objetos manipulables y pasaron a ser texto
 * anotado al margen del día. Respecto a la versión de lienzo libre
 * desaparecen por completo: banners y formas, arrastre, guías de
 * alineación, anti-solapamiento, selector de tipografías, negrita/cursiva,
 * botón de crear y el editor flotante. Todo eso era manipulación de
 * objetos, no escritura.
 *
 * Queda una lista de líneas de texto y un renglón siempre listo para
 * escribir: clic en una nota para corregirla, Enter para guardar.
 *
 * PERSISTENCIA SIN CAMBIOS (requisito explícito): se siguen usando los
 * mismos cuatro endpoints y el mismo modelo. `x`/`y`/`width`/`height`
 * se mandan porque el backend los exige, pero el layout ya no los usa
 * para posicionar — `y` pasa a definir el ORDEN, así que las notas
 * creadas con la versión anterior conservan su secuencia y ninguna se
 * pierde. Un elemento guardado como BANNER se muestra como texto: el
 * dato sigue intacto, solo cambia cómo se dibuja.
 */

/** El backend exige medidas; el layout no las usa. */
const STORED_SIZE = { width: 280, height: 84 }

/** Separación entre los `y` de notas consecutivas, solo para ordenar. */
const ORDER_STEP = 100

interface DayNotesCanvasProps {
  dateKey: string
  /** La cabecera de la hoja (CalendarPage) muestra "N actividades · M
      notas", y ese conteo vive aquí. */
  onCountChange?: (count: number) => void
}

export function DayNotesCanvas({ dateKey, onCountChange }: DayNotesCanvasProps) {
  // ADR-019: las notas del día pertenecen al módulo desde el que se
  // escriben; las del otro módulo ni se piden.
  const activeMode = useActiveMode()
  const [elements, setElements] = useState<DayNoteElement[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [editingId, setEditingId] = useState<string | null>(null)
  const [draftText, setDraftText] = useState('')
  const [composerText, setComposerText] = useState('')

  const listRef = useRef<HTMLDivElement>(null)
  const composerRef = useRef<HTMLTextAreaElement>(null)
  const editorRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    let cancelled = false

    setLoading(true)
    setError(null)
    setEditingId(null)

    listDayNoteElements(dateKey, activeMode)
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
  }, [dateKey, activeMode])

  useEffect(() => {
    onCountChange?.(elements.length)
  }, [elements.length, onCountChange])

  /** `y` conserva el orden de las notas creadas con la versión de lienzo
      libre; `createdAt` desempata. */
  const ordered = useMemo(
    () => [...elements].sort((a, b) => a.y - b.y || a.createdAt.localeCompare(b.createdAt)),
    [elements],
  )

  /** Un `<textarea>` no crece solo: sin esto el texto largo quedaría
      cortado tras una barra de scroll interna, justo lo que se pidió
      evitar. */
  function autoGrow(node: HTMLTextAreaElement | null) {
    if (!node) return
    node.style.height = 'auto'
    node.style.height = `${node.scrollHeight}px`
  }

  useEffect(() => {
    autoGrow(composerRef.current)
  }, [composerText])

  useEffect(() => {
    if (editingId) autoGrow(editorRef.current)
  }, [editingId, draftText])

  async function handleCreate() {
    const text = composerText.trim()
    if (!text) return

    setComposerText('')

    const y = elements.reduce((max, el) => Math.max(max, el.y), 0) + ORDER_STEP

    try {
      const created = await createDayNoteElement(
        {
          noteDate: dateKey,
          type: 'TEXT',
          x: 0,
          y,
          ...STORED_SIZE,
          data: { text, bold: false, italic: false, font: 'sans' },
        },
        activeMode,
      )

      setElements((prev) => [...prev, created])
      requestAnimationFrame(() => {
        if (listRef.current) listRef.current.scrollTop = listRef.current.scrollHeight
      })
    } catch {
      setError('No se pudo guardar la nota.')
      setComposerText(text)
    }
  }

  function startEditing(element: DayNoteElement) {
    setEditingId(element.id)
    setDraftText(element.data.text ?? '')
  }

  async function commitEditing() {
    const id = editingId
    if (!id) return

    const element = elements.find((el) => el.id === id)
    setEditingId(null)
    if (!element) return

    const text = draftText.trim()

    // Vaciar una nota la elimina: es lo natural en una lista de texto y
    // evita dejar renglones fantasma imposibles de distinguir.
    if (!text) {
      void handleDelete(id)
      return
    }

    if (text === element.data.text) return

    try {
      const updated = await editDayNoteElementData(
        id,
        { ...element.data, text },
        element.version,
      )
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
      setError('No se pudo eliminar la nota.')
      listDayNoteElements(dateKey, activeMode).then(setElements).catch(() => undefined)
    }
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.colLabel}>Notas</div>

      {error && <p className={styles.error}>{error}</p>}

      {/* El scroll vive solo aquí dentro: nunca alarga la página. */}
      <div ref={listRef} className={styles.noteList}>
        {loading && <p className={styles.hint}>Cargando…</p>}

        {!loading && ordered.length === 0 && (
          <p className={styles.hint}>Sin notas para este día.</p>
        )}

        {ordered.map((element) => {
          const isEditing = editingId === element.id

          if (isEditing) {
            return (
              <div key={element.id} className={styles.note}>
                <textarea
                  ref={editorRef}
                  autoFocus
                  className={styles.noteEditor}
                  value={draftText}
                  onChange={(event) => setDraftText(event.target.value)}
                  onBlur={() => void commitEditing()}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter' && !event.shiftKey) {
                      event.preventDefault()
                      event.currentTarget.blur()
                    }
                    if (event.key === 'Escape') {
                      setDraftText(element.data.text ?? '')
                      setEditingId(null)
                    }
                  }}
                />
              </div>
            )
          }

          return (
            <div
              key={element.id}
              className={styles.note}
              // Un clic entra a editar (el doble clic también, porque el
              // primero ya abre el editor y el segundo solo coloca el
              // cursor dentro del textarea).
              onClick={() => startEditing(element)}
            >
              {element.data.text}

              <button
                type="button"
                className={styles.noteDelete}
                aria-label="Eliminar nota"
                onClick={(event) => {
                  event.stopPropagation()
                  void handleDelete(element.id)
                }}
              >
                ×
              </button>
            </div>
          )
        })}
      </div>

      {/* Escritura directa: un renglón siempre listo, sin botones ni modos. */}
      <div className={styles.composer}>
        <span className={styles.composerMark} aria-hidden="true">
          +
        </span>

        <textarea
          ref={composerRef}
          rows={1}
          className={styles.composerInput}
          placeholder="Escribe una nota…"
          aria-label="Escribe una nota"
          value={composerText}
          onChange={(event) => setComposerText(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
              event.preventDefault()
              void handleCreate()
            }
          }}
        />
      </div>

      <div className={styles.composerHint}>Enter guarda · Shift+Enter salto de línea</div>
    </div>
  )
}
