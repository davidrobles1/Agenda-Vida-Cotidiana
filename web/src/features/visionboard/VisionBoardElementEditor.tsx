import { useEffect, useState, type DragEvent, type FormEvent } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Popover } from 'react-aria-components'
import { motion } from 'motion/react'
import { UploadCloud } from 'lucide-react'
import { motionTokens } from '../../core/motion/tokens'
import { EDIT_ICON } from '../../core/ui/pickers/pickerCatalog'
import { StickerPicker } from '../../core/ui/pickers/StickerPicker'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import { shapeVariantOf, uploadVisionBoardImage, type VisionBoardElement } from './api'
import { buildChart, CHART_TYPE_OPTIONS } from './visionBoardCharts'
import { FRAME_STYLE_OPTIONS } from './visionBoardFrames'
import { GRID_LAYOUT_OPTIONS } from './visionBoardGrids'
import { handleRadiogroupKeyDown, radioTabIndex } from '../../core/ui/keyboard/radiogroupKeyboard'
import { VisionBoardEmojiPicker } from './VisionBoardEmojiPicker'
import { useVisionBoardImageSrc } from './visionBoardImages'
import {
  DEFAULT_VISION_BOARD_FONT,
  MAX_TEXT_FONT_SIZE,
  MIN_TEXT_FONT_SIZE,
  TEXT_FONT_SIZE_STEP,
  VISION_BOARD_FONTS,
  fontSizeOf,
  fontStackOf,
} from './visionBoardFonts'
import { VisionBoardShapePicker } from './VisionBoardShapePicker'
import libraryStyles from './VisionBoardElementLibrary.module.css'
import styles from './VisionBoardToolbar.module.css'

/** BLOQUE A fix: this was `motion.create(Popover)` — the same anti-pattern
    already root-caused and fixed in FASE 23 for Templates/Elementos/
    Exportar (see core/theme/ThemeSwitcher.tsx's own doc comment for the
    original diagnosis): React Aria's `Popover` re-renders itself after
    mount (its internal `useResizeObserver`/`useLayoutEffect`, for trigger-
    width measurement and dialog-role detection), clobbering Motion's
    animated inline `style` back to `initial` — the panel stays in the DOM
    and clickable but fully invisible. The fix is always to animate the
    innermost content (`Dialog`), never the outer overlay-positioning
    primitive (`Popover`). */
const MotionDialog = motion.create(Dialog)

type Draft = Record<string, unknown>

function draftFromElement(element: VisionBoardElement): Draft {
  switch (element.type) {
    case 'TEXT':
    case 'NOTE':
      return { text: typeof element.data.text === 'string' ? element.data.text : '' }
    case 'IMAGE':
      // BLOQUE B: `imageId` (an internal upload) travels alongside `url`
      // (the original external-URL path) — handleEditElement replaces the
      // *whole* `data` object on save (VisionBoardCanvas.tsx), so both must
      // be carried into the draft even though only one is ever set at a
      // time, or editing an uploaded image without touching it would
      // silently drop its imageId.
      return {
        url: typeof element.data.url === 'string' ? element.data.url : '',
        imageId: typeof element.data.imageId === 'string' ? element.data.imageId : undefined,
        // 2026-08-23: mismo motivo — "Marcos" también vive en `data`, se
        // pierde en el guardado si no viaja en el draft.
        frameStyle: typeof element.data.frameStyle === 'string' ? element.data.frameStyle : undefined,
      }
    case 'STICKER':
      // BLOQUE D: `emojiId` travels alongside `stickerId` in the draft —
      // same "handleEditElement replaces the whole `data` object" reason
      // ImageEditFields' `imageId`/`url` pair already needs this for.
      return {
        stickerId: typeof element.data.stickerId === 'string' ? element.data.stickerId : undefined,
        emojiId: typeof element.data.emojiId === 'string' ? element.data.emojiId : undefined,
      }
    case 'SHAPE':
      return { shape: shapeVariantOf(element.data) }
    case 'TABLE':
      // 2026-08-22: nada editable todavía — ver EditFields' propio caso
      // TABLE y TableElementContent's doc comment en VisionBoardElementView.tsx.
      return {}
    case 'CHART':
      return { chartType: typeof element.data.chartType === 'string' ? element.data.chartType : 'bar' }
    case 'GRID':
      return { layout: typeof element.data.layout === 'string' ? element.data.layout : 'four' }
  }
}

function typeLabel(type: VisionBoardElement['type']): string {
  switch (type) {
    case 'TEXT':
      return 'texto'
    case 'NOTE':
      return 'nota'
    case 'STICKER':
      return 'sticker'
    case 'IMAGE':
      return 'imagen'
    case 'SHAPE':
      return 'forma'
    case 'TABLE':
      return 'tabla'
    case 'CHART':
      return 'gráfica'
    case 'GRID':
      return 'cuadrícula'
  }
}

/** BLOQUE C (post-MVP): the 3-item literal list here moved to the real
    catalog (visionBoardShapes.ts's `SHAPE_CATALOG`, ~30 shapes) — both this
    file's own SHAPE edit step and VisionBoardElementLibrary.tsx's Forma
    step now render it through the shared `VisionBoardShapePicker` instead
    of each keeping its own copy of the list. */

interface VisionBoardElementEditorProps {
  /** `null` disables the trigger — nothing selected, nothing to edit. */
  element: VisionBoardElement | null
  saving: boolean
  /** FASE 17: true when the selected element is locked — "no puede
      editarse." Separate from `element === null` so the trigger can stay
      disabled for a real, existing selection rather than looking like
      nothing's selected at all. */
  disabled?: boolean
  onSave: (data: Draft) => Promise<void>
  /** BLOQUE E (post-MVP): same optional-controlled shape as
      VisionBoardDeleteConfirm.tsx's own isOpen/onOpenChange — the context
      menu's "Editar" entry needs to open this popover too. */
  isOpen?: boolean
  onOpenChange?: (open: boolean) => void
}

/**
 * FASE 7: edits the selected element's `data` — content for TEXT/NOTE, the
 * sticker for STICKER (real picker, core/ui/pickers/StickerPicker.tsx), the
 * URL for IMAGE, the variant for SHAPE (Fase 6 pendiente #2, real now:
 * rectangle/circle/line, still `data.shape`, no ElementType/model change).
 *
 * Everything here is a local draft — `onSave` (VisionBoardCanvas's
 * `persistElementChange`) is only called once, when the user presses
 * Guardar. Cancelar (or dismissing the popover) just discards the draft;
 * nothing in `elements` ever changes until a real save happens, which is
 * what "si el usuario cancela la edición, no debe modificarse el elemento"
 * requires — unlike Fase 6's sticker picker (pick-and-create-immediately),
 * this one stages the pick until Guardar, same as every other type here.
 *
 * Same trigger surface as the create actions (VisionBoardToolbar, a
 * sibling of the canvas) — not a button glued onto the selected element
 * itself, so it can't be mistaken for another resize/rotate handle and
 * needs no extra stopPropagation wiring beyond what the toolbar already
 * has (Fase 6: toolbar clicks can't reach the canvas's click-to-deselect
 * or an element's drag start).
 */
export function VisionBoardElementEditor({
  element,
  saving,
  disabled,
  onSave,
  isOpen: isOpenProp,
  onOpenChange: onOpenChangeProp,
}: VisionBoardElementEditorProps) {
  const [internalOpen, setInternalOpen] = useState(false)
  const isOpen = isOpenProp ?? internalOpen
  const setIsOpen = onOpenChangeProp ?? setInternalOpen
  const [draft, setDraft] = useState<Draft>({})
  const [error, setError] = useState<string | null>(null)
  // BLOQUE B: true while ImageEditFields has an upload in flight — Guardar
  // stays disabled meanwhile, so a click that lands in the ~200-500ms
  // window between picking a file and its `uploaded.id` actually landing
  // in `draft` can never save stale/missing image data.
  const [imageUploading, setImageUploading] = useState(false)

  // The selection changed (or was cleared) while this was open — editing a
  // stale/no-longer-selected element doesn't make sense.
  useEffect(() => {
    if (isOpen && !element) setIsOpen(false)
  }, [isOpen, element])

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (open && element) {
      setDraft(draftFromElement(element))
      setError(null)
      setImageUploading(false)
    }
  }

  async function handleSubmit(event: FormEvent, close: () => void) {
    event.preventDefault()
    if (!element || imageUploading) return
    setError(null)
    try {
      await onSave(draft)
      close()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar el cambio.')
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      <Button className={styles.actionButton} isDisabled={!element || disabled} aria-label={disabled ? 'Editar (elemento bloqueado)' : 'Editar'}>
        <EDIT_ICON width={16} height={16} />
      </Button>
      <Popover placement="bottom start" offset={8} className={styles.popover}>
        <MotionDialog
          className={styles.popoverDialog}
          initial={{ opacity: 0, scale: 0.96, y: -4 }}
          animate={{ opacity: isOpen ? 1 : 0, scale: isOpen ? 1 : 0.96, y: isOpen ? 0 : -4 }}
          transition={motionTokens.smooth}
        >
          {({ close }) =>
            element && (
              <form onSubmit={(event) => handleSubmit(event, close)}>
                <Heading slot="title" className={shellStyles.heading}>
                  Editar {typeLabel(element.type)}
                </Heading>

                {error && (
                  <p className={shellStyles.formError} role="alert">
                    {error}
                  </p>
                )}

                <EditFields
                  element={element}
                  draft={draft}
                  onDraftChange={setDraft}
                  onImageUploadingChange={setImageUploading}
                />

                <div className={shellStyles.formActions}>
                  {saving && <span className={shellStyles.savingHint}>Guardando…</span>}
                  <button type="button" data-variant="secondary" onClick={close} disabled={saving}>
                    Cancelar
                  </button>
                  <button type="submit" disabled={saving || imageUploading}>
                    Guardar
                  </button>
                </div>
              </form>
            )
          }
        </MotionDialog>
      </Popover>
    </DialogTrigger>
  )
}

interface EditFieldsProps {
  element: VisionBoardElement
  draft: Draft
  onDraftChange: (draft: Draft) => void
  onImageUploadingChange: (uploading: boolean) => void
}

/**
 * Caligrafía y tamaño de un elemento TEXT (2026-08-29).
 *
 * El tamaño se maneja con dos botones y un número, al estilo de un
 * procesador de texto, en vez de un deslizador: el usuario pidió
 * "aumentar/reducir su tamaño, similar a Word", y ahí el gesto es dar pasos
 * discretos, no barrer un rango.
 *
 * La vista previa de cada opción se dibuja con su propia tipografía: elegir
 * una caligrafía por su nombre es adivinar.
 */
function TextTypographyFields({
  draft,
  onDraftChange,
}: {
  draft: Draft
  onDraftChange: (draft: Draft) => void
}) {
  const fontId = typeof draft.font === 'string' ? draft.font : DEFAULT_VISION_BOARD_FONT
  const size = fontSizeOf(draft.fontSize)

  const setSize = (next: number) =>
    onDraftChange({
      ...draft,
      fontSize: Math.min(MAX_TEXT_FONT_SIZE, Math.max(MIN_TEXT_FONT_SIZE, next)),
    })

  return (
    <>
      <label className={shellStyles.field}>
        <span className={shellStyles.fieldLabel}>Caligrafía</span>
        <select
          className={shellStyles.textInput}
          value={fontId}
          onChange={(event) => onDraftChange({ ...draft, font: event.target.value })}
          style={{ fontFamily: fontStackOf(fontId) }}
        >
          {VISION_BOARD_FONTS.map((font) => (
            <option key={font.id} value={font.id} style={{ fontFamily: font.stack }}>
              {font.label}
            </option>
          ))}
        </select>
      </label>

      <div className={shellStyles.field}>
        <span className={shellStyles.fieldLabel}>Tamaño</span>
        <div className={styles.fontSizeRow}>
          <button
            type="button"
            className={styles.fontSizeButton}
            aria-label="Reducir el tamaño del texto"
            onClick={() => setSize(size - TEXT_FONT_SIZE_STEP)}
            disabled={size <= MIN_TEXT_FONT_SIZE}
          >
            −
          </button>

          <input
            type="number"
            className={styles.fontSizeInput}
            aria-label="Tamaño del texto en píxeles"
            value={size}
            min={MIN_TEXT_FONT_SIZE}
            max={MAX_TEXT_FONT_SIZE}
            onChange={(event) => setSize(Number(event.target.value))}
          />

          <button
            type="button"
            className={styles.fontSizeButton}
            aria-label="Aumentar el tamaño del texto"
            onClick={() => setSize(size + TEXT_FONT_SIZE_STEP)}
            disabled={size >= MAX_TEXT_FONT_SIZE}
          >
            +
          </button>
        </div>
      </div>
    </>
  )
}

function EditFields({ element, draft, onDraftChange, onImageUploadingChange }: EditFieldsProps) {
  switch (element.type) {
    case 'TEXT':
    case 'NOTE': {
      const text = typeof draft.text === 'string' ? draft.text : ''
      return (
        <>
          <label className={shellStyles.field}>
            <span className={shellStyles.fieldLabel}>Contenido</span>
            <textarea
              className={shellStyles.textArea}
              value={text}
              onChange={(event) => onDraftChange({ ...draft, text: event.target.value })}
              rows={3}
              autoFocus
              required
            />
          </label>

          {/* 2026-08-29 (peticiones 1.1 y 1.4): caligrafía y tamaño, solo
              para TEXT. NOTE se deja igual: su aspecto de nota adhesiva es
              parte de lo que la distingue de un texto suelto, y cambiarlo
              no estaba en lo pedido. */}
          {element.type === 'TEXT' && (
            <TextTypographyFields draft={draft} onDraftChange={onDraftChange} />
          )}
        </>
      )
    }
    case 'IMAGE':
      return <ImageEditFields draft={draft} onDraftChange={onDraftChange} onUploadingChange={onImageUploadingChange} />
    case 'STICKER':
      return <StickerOrEmojiEditFields draft={draft} onDraftChange={onDraftChange} />
    case 'SHAPE': {
      const shape = typeof draft.shape === 'string' ? draft.shape : 'rectangle'
      return (
        <div className={shellStyles.field}>
          <span className={shellStyles.fieldLabel}>Variante</span>
          <VisionBoardShapePicker
            value={shape}
            onChange={(id) => onDraftChange({ ...draft, shape: id })}
            label="Variante de forma"
          />
        </div>
      )
    }
    case 'TABLE':
      // 2026-08-22: sin edición celda por celda todavía (ver
      // TableElementContent's doc comment) — Editar solo sirve aquí para
      // mover/redimensionar/rotar/bloquear, que el toolbar ya cubre fuera
      // de este popover; nada que mostrar en el formulario en sí.
      return <p className={shellStyles.fieldLabel}>Esta tabla usa contenido de ejemplo — todavía no se puede editar celda por celda.</p>
    case 'CHART': {
      const chartType = typeof draft.chartType === 'string' ? draft.chartType : 'bar'
      return (
        <div className={shellStyles.field}>
          <span className={shellStyles.fieldLabel}>Tipo de gráfica</span>
          <div className={libraryStyles.libraryGrid}>
            {CHART_TYPE_OPTIONS.map((chart) => {
              const { viewBox, markup } = buildChart(chart.id)
              return (
                <button
                  key={chart.id}
                  type="button"
                  className={libraryStyles.libraryTile}
                  aria-pressed={chartType === chart.id}
                  style={chartType === chart.id ? { borderColor: 'var(--color-primary)' } : undefined}
                  onClick={() => onDraftChange({ ...draft, chartType: chart.id })}
                >
                  {/* eslint-disable-next-line react/no-danger -- markup es generado por nosotros (visionBoardCharts.ts), nunca input del usuario. */}
                  <svg viewBox={viewBox} width={20} height={20} aria-hidden="true" dangerouslySetInnerHTML={{ __html: markup }} />
                  {chart.label}
                </button>
              )
            })}
          </div>
        </div>
      )
    }
    case 'GRID': {
      const layout = typeof draft.layout === 'string' ? draft.layout : 'four'
      return (
        <div className={shellStyles.field}>
          <span className={shellStyles.fieldLabel}>Diseño</span>
          <div className={libraryStyles.libraryGrid}>
            {GRID_LAYOUT_OPTIONS.map((option) => (
              <button
                key={option.id}
                type="button"
                className={libraryStyles.libraryTile}
                aria-pressed={layout === option.id}
                style={layout === option.id ? { borderColor: 'var(--color-primary)' } : undefined}
                onClick={() => onDraftChange({ ...draft, layout: option.id })}
              >
                {option.label}
              </button>
            ))}
          </div>
        </div>
      )
    }
  }
}

type ImageSource = 'upload' | 'url'

/** BLOQUE B (post-MVP): same two-source shape as VisionBoardElementLibrary's
    `ImageEntryFields` (URL vs. a real upload/drag&drop), adapted for
    editing an *existing* element instead of creating a new one — uploads
    immediately on file pick (not deferred to Guardar, unlike the create
    flow) so `draft.imageId` is already a real, saved id by the time the
    outer form's `onSave(draft)` runs; the parent's `handleSubmit`
    (VisionBoardElementEditor above) needed no changes at all as a result.
    An upload picked then abandoned via Cancelar leaves one unreferenced
    row in vision_board_images — flagged as a Mejora Futura ("limpieza de
    imágenes huérfanas"), not worth the extra plumbing here. */
function ImageEditFields({
  draft,
  onDraftChange,
  onUploadingChange,
}: {
  draft: Draft
  onDraftChange: (draft: Draft) => void
  onUploadingChange: (uploading: boolean) => void
}) {
  const currentImageId = typeof draft.imageId === 'string' ? draft.imageId : undefined
  const currentUrl = typeof draft.url === 'string' ? draft.url : ''
  const [source, setSource] = useState<ImageSource>(currentImageId ? 'upload' : 'url')
  const [dragOver, setDragOver] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [localPreviewUrl, setLocalPreviewUrl] = useState<string | null>(null)
  const { src: resolvedImageSrc } = useVisionBoardImageSrc(currentImageId ? { imageId: currentImageId } : {})
  const previewSrc = localPreviewUrl ?? resolvedImageSrc

  async function handleFile(selected: File | null) {
    if (!selected) return
    setError(null)
    setLocalPreviewUrl(URL.createObjectURL(selected))
    setUploading(true)
    onUploadingChange(true)
    try {
      const uploaded = await uploadVisionBoardImage(selected)
      onDraftChange({ ...draft, imageId: uploaded.id, url: undefined })
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo subir la imagen.')
    } finally {
      setUploading(false)
      onUploadingChange(false)
    }
  }

  function handleDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault()
    setDragOver(false)
    const dropped = event.dataTransfer.files[0]
    if (dropped) handleFile(dropped)
  }

  return (
    <>
      <div
        className={styles.shapeVariantGrid}
        role="radiogroup"
        aria-label="Origen de la imagen"
        onKeyDown={handleRadiogroupKeyDown}
      >
        <button
          type="button"
          role="radio"
          aria-checked={source === 'upload'}
          tabIndex={radioTabIndex(source === 'upload', true, true)}
          className={`${styles.shapeVariantButton} ${source === 'upload' ? styles.shapeVariantButtonActive : ''}`}
          onClick={() => setSource('upload')}
        >
          Subir archivo
        </button>
        <button
          type="button"
          role="radio"
          aria-checked={source === 'url'}
          tabIndex={radioTabIndex(source === 'url', false, true)}
          className={`${styles.shapeVariantButton} ${source === 'url' ? styles.shapeVariantButtonActive : ''}`}
          onClick={() => {
            setSource('url')
            onDraftChange({ ...draft, imageId: undefined })
          }}
        >
          URL
        </button>
      </div>

      {source === 'url' ? (
        <label className={shellStyles.field}>
          <span className={shellStyles.fieldLabel}>URL de la imagen</span>
          <input
            type="url"
            className={shellStyles.textInput}
            value={currentUrl}
            onChange={(event) => onDraftChange({ ...draft, url: event.target.value, imageId: undefined })}
            placeholder="https://…"
            required
          />
        </label>
      ) : (
        <label
          className={`${libraryStyles.imageDropzone} ${dragOver ? libraryStyles.imageDropzoneActive : ''}`}
          onDragOver={(event) => {
            event.preventDefault()
            setDragOver(true)
          }}
          onDragLeave={() => setDragOver(false)}
          onDrop={handleDrop}
        >
          {previewSrc ? (
            <img src={previewSrc} alt="" className={libraryStyles.imageDropzonePreview} />
          ) : (
            <>
              <UploadCloud width={22} height={22} aria-hidden="true" />
              <span>Arrastra una imagen aquí, o haz clic para elegir un archivo</span>
            </>
          )}
          {uploading && <span>Subiendo…</span>}
          <input
            type="file"
            accept="image/png,image/jpeg,image/webp,image/gif"
            className={libraryStyles.imageDropzoneInput}
            onChange={(event) => handleFile(event.target.files?.[0] ?? null)}
          />
        </label>
      )}

      {/* 2026-08-23 (pedido explícito del usuario, catálogo estilo Canva):
          "Marcos" — mismo picker que ImageEntryFields.tsx (creación), aquí
          editando `draft.frameStyle` en vez de crear directo. */}
      <div className={shellStyles.field}>
        <span className={shellStyles.fieldLabel}>Marco (opcional)</span>
        <div className={styles.shapeVariantGrid} role="radiogroup" aria-label="Marco de la imagen" onKeyDown={handleRadiogroupKeyDown}>
          <button
            type="button"
            role="radio"
            aria-checked={!draft.frameStyle}
            tabIndex={radioTabIndex(!draft.frameStyle, true, true)}
            className={`${styles.shapeVariantButton} ${!draft.frameStyle ? styles.shapeVariantButtonActive : ''}`}
            onClick={() => onDraftChange({ ...draft, frameStyle: undefined })}
          >
            Ninguno
          </button>
          {FRAME_STYLE_OPTIONS.map((frame) => (
            <button
              key={frame.id}
              type="button"
              role="radio"
              aria-checked={draft.frameStyle === frame.id}
              tabIndex={radioTabIndex(draft.frameStyle === frame.id, false, true)}
              className={`${styles.shapeVariantButton} ${draft.frameStyle === frame.id ? styles.shapeVariantButtonActive : ''}`}
              onClick={() => onDraftChange({ ...draft, frameStyle: frame.id })}
            >
              {frame.label}
            </button>
          ))}
        </div>
      </div>

      {error && (
        <p className={shellStyles.formError} role="alert">
          {error}
        </p>
      )}
    </>
  )
}

type StickerSource = 'sticker' | 'emoji'

/** BLOQUE D (post-MVP): editing an existing STICKER-type element — same
    two-source shape as ImageEditFields (a real toggle, not just whichever
    catalog the element happened to be created from), since a sticker can
    become an emoji on Guardar and vice versa. */
function StickerOrEmojiEditFields({ draft, onDraftChange }: { draft: Draft; onDraftChange: (draft: Draft) => void }) {
  const currentEmojiId = typeof draft.emojiId === 'string' ? draft.emojiId : undefined
  const currentStickerId = typeof draft.stickerId === 'string' ? draft.stickerId : undefined
  const [source, setSource] = useState<StickerSource>(currentEmojiId ? 'emoji' : 'sticker')

  return (
    <div className={shellStyles.field}>
      <span className={shellStyles.fieldLabel}>{source === 'emoji' ? 'Emoji' : 'Sticker'}</span>
      <div
        className={styles.shapeVariantGrid}
        role="radiogroup"
        aria-label="Origen"
        onKeyDown={handleRadiogroupKeyDown}
      >
        <button
          type="button"
          role="radio"
          aria-checked={source === 'sticker'}
          tabIndex={radioTabIndex(source === 'sticker', true, true)}
          className={`${styles.shapeVariantButton} ${source === 'sticker' ? styles.shapeVariantButtonActive : ''}`}
          onClick={() => setSource('sticker')}
        >
          Sticker
        </button>
        <button
          type="button"
          role="radio"
          aria-checked={source === 'emoji'}
          tabIndex={radioTabIndex(source === 'emoji', false, true)}
          className={`${styles.shapeVariantButton} ${source === 'emoji' ? styles.shapeVariantButtonActive : ''}`}
          onClick={() => setSource('emoji')}
        >
          Emojis
        </button>
      </div>
      {source === 'sticker' ? (
        <StickerPicker
          value={currentStickerId}
          onChange={(id) => onDraftChange({ ...draft, stickerId: id, emojiId: undefined })}
        />
      ) : (
        <VisionBoardEmojiPicker
          value={currentEmojiId}
          onChange={(id) => onDraftChange({ ...draft, emojiId: id, stickerId: undefined })}
        />
      )}
    </div>
  )
}
