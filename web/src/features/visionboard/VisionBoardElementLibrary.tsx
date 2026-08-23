import { useEffect, useRef, useState, type DragEvent, type FormEvent, type ReactNode } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Popover } from 'react-aria-components'
import { AnimatePresence, motion } from 'motion/react'
import { ArrowLeft, BarChart3, Image, LayoutGrid, LibraryBig, Smile, Square, StickyNote, Table2, Type, UploadCloud } from 'lucide-react'
import { motionTokens } from '../../core/motion/tokens'
import { STICKER_FIELD_ICON } from '../../core/ui/pickers/pickerCatalog'
import { StickerPicker } from '../../core/ui/pickers/StickerPicker'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import { uploadVisionBoardImage, type CreateVisionBoardElementInput } from './api'
import { CHART_TYPE_OPTIONS, buildChart } from './visionBoardCharts'
import { buildInput, buildInputWithSize } from './visionBoardElementDefaults'
import { FRAME_STYLE_OPTIONS } from './visionBoardFrames'
import { GRID_LAYOUT_OPTIONS } from './visionBoardGrids'
import { handleRadiogroupKeyDown, radioTabIndex } from '../../core/ui/keyboard/radiogroupKeyboard'
import { VisionBoardEmojiPicker } from './VisionBoardEmojiPicker'
import { fitImageElementSize, loadImageNaturalSize } from './visionBoardImages'
import { VisionBoardShapePicker } from './VisionBoardShapePicker'
import toolbarStyles from './VisionBoardToolbar.module.css'
import styles from './VisionBoardElementLibrary.module.css'

const MotionDialog = motion.create(Dialog)

// BLOQUE D (post-MVP): 'EMOJI' is a library-only distinction — it always
// creates a real STICKER-type element with `data.emojiId` (see
// visionBoardEmojis.ts's own doc comment on why no 4th backend element
// type exists), same "one extra library entry, not a new element kind"
// shape SHAPE's own variants already use.
type LibraryItemType = 'TEXT' | 'NOTE' | 'STICKER' | 'EMOJI' | 'IMAGE' | 'SHAPE' | 'TABLE' | 'CHART' | 'GRID'
type LibraryStep = 'menu' | LibraryItemType

const LIBRARY_ITEMS: Array<{ type: LibraryItemType; label: string; icon: ReactNode }> = [
  { type: 'TEXT', label: 'Texto', icon: <Type width={20} height={20} /> },
  { type: 'NOTE', label: 'Nota', icon: <StickyNote width={20} height={20} /> },
  { type: 'STICKER', label: 'Sticker', icon: <STICKER_FIELD_ICON width={20} height={20} /> },
  { type: 'EMOJI', label: 'Emojis', icon: <Smile width={20} height={20} /> },
  { type: 'IMAGE', label: 'Imagen', icon: <Image width={20} height={20} /> },
  { type: 'SHAPE', label: 'Forma', icon: <Square width={20} height={20} /> },
  { type: 'TABLE', label: 'Tabla', icon: <Table2 width={20} height={20} /> },
  { type: 'CHART', label: 'Gráfica', icon: <BarChart3 width={20} height={20} /> },
  { type: 'GRID', label: 'Cuadrícula', icon: <LayoutGrid width={20} height={20} /> },
]

/** 2026-08-22 (pedido explícito del usuario, catálogo estilo Canva): tabla
    de ejemplo — sin edición celda por celda todavía (ver
    VisionBoardElementView.tsx's `TableElementContent`), así que se crea
    directamente al elegirla en el menú, sin un paso intermedio (mismo
    "pick and go" que Sticker/Emoji, sin siquiera el paso de elegir). */
const DEFAULT_TABLE_ROWS = [
  ['Columna 1', 'Columna 2'],
  ['', ''],
  ['', ''],
]

interface VisionBoardElementLibraryProps {
  onCreate: (input: CreateVisionBoardElementInput) => void
  /** BLOQUE E (post-MVP): same optional-controlled shape as
      VisionBoardTemplatesAction.tsx's own isOpen/onOpenChange — the
      context menu's "Elementos" entry needs to open this popover too. */
  isOpen?: boolean
  onOpenChange?: (open: boolean) => void
}

/**
 * FASE 13: "Elementos" — replaces the five separate Texto/Nota/Sticker/
 * Imagen/Forma buttons that used to sit inline in the toolbar with one
 * consolidated entry point: a small visual gallery (step "menu") that
 * transitions, inside the same popover, to that type's own quick-create
 * step. Every step reuses the exact same building blocks the old inline
 * actions used — `buildInput` (visionBoardElementDefaults.ts), `StickerPicker`
 * (core/ui/pickers/StickerPicker.tsx), `VisionBoardShapePicker`
 * (BLOQUE C, shared with VisionBoardElementEditor.tsx) — nothing here
 * reimplements what already exists, only regroups how it's reached.
 *
 * A two-step popover, not step-per-type nested popovers: nesting a second
 * live `DialogTrigger`+`Popover` inside this one's `Dialog` content is a
 * pattern nothing else in this app currently does (the closest precedent,
 * NoteDeleteConfirm's confirm dialog, nests an explicit `Modal` inside a
 * Drawer's own `Modal` — a different overlay primitive with its own
 * "establish a fresh context" fix already built for it). Swapping the
 * single popover's own content between "menu" and a picked type avoids
 * that untested surface entirely — there is exactly one `Popover`/`Dialog`
 * for the whole flow, Sticker included (its own StickerPicker grid is
 * just another step, not a second overlay).
 *
 * FASE 14 fix: the menu↔step swap now crossfades via `AnimatePresence
 * mode="wait"` instead of an instant content swap — same "panel
 * cross-fade" shape (opacity-only, `motionTokens.base`) NoteDrawer.tsx
 * already uses for its own view↔edit swap inside one Dialog, so this
 * isn't a new animation pattern. `mode="wait"` means the outgoing step
 * fully exits before the next one enters — no layout jump from two steps
 * of different height being present at once. `reducedMotion="user"` is
 * already configured once, globally, in main.tsx's `<MotionConfig>` — every
 * `motion.*`/`AnimatePresence` usage in this app already honors it for
 * free, this needs no extra handling.
 *
 * FASE 23 fix: the outer open/close animation used to be on `Popover`
 * itself (`motion.create(Popover)`) — see VisionBoardExportAction.tsx's
 * own doc comment for the real bug that causes (a permanently
 * invisible-but-clickable panel, already diagnosed once for
 * `core/theme/ThemeSwitcher.tsx`). Now animates `Dialog` instead; the
 * inner menu↔step `AnimatePresence` above was never the problem and is
 * untouched.
 */
export function VisionBoardElementLibrary({
  onCreate,
  isOpen: isOpenProp,
  onOpenChange: onOpenChangeProp,
}: VisionBoardElementLibraryProps) {
  const [internalOpen, setInternalOpen] = useState(false)
  const isOpen = isOpenProp ?? internalOpen
  const [step, setStep] = useState<LibraryStep>('menu')

  function handleOpenChange(open: boolean) {
    if (onOpenChangeProp) {
      onOpenChangeProp(open)
    } else {
      setInternalOpen(open)
    }
    if (!open) setStep('menu')
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      {/* FASE 23: "acción principal" treatment — see
          VisionBoardToolbar.module.css's own doc comment on
          .actionButtonPrimary. */}
      <Button className={`${toolbarStyles.actionButton} ${toolbarStyles.actionButtonPrimary}`}>
        <LibraryBig width={16} height={16} />
        Elementos
      </Button>
      <Popover placement="bottom start" offset={8} className={styles.libraryPopover}>
        {/* FASE 20 audit fix: no static aria-label — every step (menu,
            "Agregar texto", "Elegir sticker", ...) already renders its own
            `Heading slot="title"` (LibraryMenu/StepBackHeader below); a
            static "Elementos" label would override that and announce the
            same name on every step regardless of which one is showing.
            Dropping it lets react-aria-components auto-wire
            aria-labelledby to whichever Heading is actually rendered. */}
        <MotionDialog
          className={styles.libraryDialog}
          initial={{ opacity: 0, scale: 0.96, y: -4 }}
          animate={{ opacity: isOpen ? 1 : 0, scale: isOpen ? 1 : 0.96, y: isOpen ? 0 : -4 }}
          transition={motionTokens.smooth}
        >
          {({ close }) => (
            <AnimatePresence mode="wait" initial={false}>
              {step === 'menu' ? (
                <motion.div
                  key="menu"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  transition={motionTokens.base}
                >
                  <LibraryMenu onPick={setStep} />
                </motion.div>
              ) : (
                <motion.div
                  key={step}
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  transition={motionTokens.base}
                >
                  <LibraryStepContent
                    type={step}
                    onCreate={onCreate}
                    onBack={() => setStep('menu')}
                    // FASE 13: "cerrarse después de crear un elemento cuando
                    // tenga sentido" — a real creation always closes the whole
                    // popover (not just back to the gallery), same instinct as
                    // StickerAction's old "pick and go."
                    onDone={close}
                  />
                </motion.div>
              )}
            </AnimatePresence>
          )}
        </MotionDialog>
      </Popover>
    </DialogTrigger>
  )
}

function LibraryMenu({ onPick }: { onPick: (type: LibraryItemType) => void }) {
  return (
    <>
      <Heading slot="title" className={shellStyles.heading}>
        Elementos
      </Heading>
      <p className={styles.libraryIntro}>Elige qué quieres agregar al Vision Board.</p>
      <div className={styles.libraryGrid}>
        {LIBRARY_ITEMS.map((item) => (
          <button key={item.type} type="button" className={styles.libraryTile} onClick={() => onPick(item.type)}>
            {item.icon}
            {item.label}
          </button>
        ))}
      </div>
    </>
  )
}

interface LibraryStepContentProps {
  type: LibraryItemType
  onCreate: (input: CreateVisionBoardElementInput) => void
  onBack: () => void
  onDone: () => void
}

function LibraryStepContent({ type, onCreate, onBack, onDone }: LibraryStepContentProps) {
  switch (type) {
    case 'TEXT':
      return (
        <TextEntryFields
          type="TEXT"
          dialogTitle="Agregar texto"
          fieldLabel="Contenido"
          onCreate={onCreate}
          onBack={onBack}
          onDone={onDone}
        />
      )
    case 'NOTE':
      return (
        <TextEntryFields
          type="NOTE"
          dialogTitle="Agregar nota"
          fieldLabel="Contenido"
          multiline
          onCreate={onCreate}
          onBack={onBack}
          onDone={onDone}
        />
      )
    case 'IMAGE':
      return <ImageEntryFields onCreate={onCreate} onBack={onBack} onDone={onDone} />
    case 'STICKER':
      return <StickerFields onCreate={onCreate} onBack={onBack} onDone={onDone} />
    case 'EMOJI':
      return <EmojiFields onCreate={onCreate} onBack={onBack} onDone={onDone} />
    case 'SHAPE':
      return <ShapeFields onCreate={onCreate} onBack={onBack} onDone={onDone} />
    case 'TABLE':
      return <TableFields onCreate={onCreate} onDone={onDone} />
    case 'CHART':
      return <ChartFields onCreate={onCreate} onBack={onBack} onDone={onDone} />
    case 'GRID':
      return <GridFields onCreate={onCreate} onBack={onBack} onDone={onDone} />
  }
}

function StepBackHeader({ title, onBack }: { title: string; onBack: () => void }) {
  return (
    <div className={styles.libraryStepHeader}>
      <button type="button" className={styles.libraryBackButton} onClick={onBack}>
        <ArrowLeft width={16} height={16} />
        Volver
      </button>
      <Heading slot="title" className={shellStyles.heading}>
        {title}
      </Heading>
    </div>
  )
}

interface TextEntryFieldsProps {
  type: 'TEXT' | 'NOTE'
  dialogTitle: string
  fieldLabel: string
  multiline?: boolean
  placeholder?: string
  inputType?: string
  onCreate: (input: CreateVisionBoardElementInput) => void
  onBack: () => void
  onDone: () => void
}

/** Same single-field "type a value, submit" shape TextEntryAction used for
    TEXT/NOTE (IMAGE moved to its own ImageEntryFields below once it grew a
    second, upload-based source) — identical `data.text` key
    VisionBoardElementView.tsx already reads back out for each type. */
function TextEntryFields({
  type,
  dialogTitle,
  fieldLabel,
  multiline,
  placeholder,
  inputType,
  onCreate,
  onBack,
  onDone,
}: TextEntryFieldsProps) {
  const [value, setValue] = useState('')

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!value.trim()) return
    onCreate(buildInput(type, { text: value.trim() }))
    onDone()
  }

  return (
    <form onSubmit={handleSubmit}>
      <StepBackHeader title={dialogTitle} onBack={onBack} />
      <label className={shellStyles.field}>
        <span className={shellStyles.fieldLabel}>{fieldLabel}</span>
        {multiline ? (
          <textarea
            className={shellStyles.textArea}
            value={value}
            onChange={(event) => setValue(event.target.value)}
            placeholder={placeholder}
            rows={3}
            autoFocus
            required
          />
        ) : (
          <input
            type={inputType ?? 'text'}
            className={shellStyles.textInput}
            value={value}
            onChange={(event) => setValue(event.target.value)}
            placeholder={placeholder}
            autoFocus
            required
          />
        )}
      </label>
      <div className={shellStyles.formActions}>
        <button type="button" data-variant="secondary" onClick={onBack}>
          Cancelar
        </button>
        <button type="submit">Agregar</button>
      </div>
    </form>
  )
}

type ImageSource = 'upload' | 'url'

/** BLOQUE B (post-MVP): IMAGE outgrew TextEntryFields' single-field shape
    once it needed two real sources — a real upload (file picker or drag &
    drop, `uploadVisionBoardImage` → `data.imageId`) alongside the original
    URL field (`data.url`, unchanged since FASE 6). Same
    `toolbarStyles.shapeVariantGrid` pill pattern already reused for the
    Board Theme picker (CreateVisionBoardDialog.tsx) drives the source
    toggle — no new "pick one of two options" pattern. */
function ImageEntryFields({
  onCreate,
  onBack,
  onDone,
}: {
  onCreate: (input: CreateVisionBoardElementInput) => void
  onBack: () => void
  onDone: () => void
}) {
  const [source, setSource] = useState<ImageSource>('upload')
  const [url, setUrl] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [dragOver, setDragOver] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [frameStyle, setFrameStyle] = useState<string | undefined>(undefined)
  const objectUrlRef = useRef<string | null>(null)

  function pickFile(selected: File | null) {
    setFile(selected)
    setError(null)
    if (objectUrlRef.current) URL.revokeObjectURL(objectUrlRef.current)
    objectUrlRef.current = selected ? URL.createObjectURL(selected) : null
    setPreviewUrl(objectUrlRef.current)
  }

  function handleDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault()
    setDragOver(false)
    const dropped = event.dataTransfer.files[0]
    if (dropped) pickFile(dropped)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (source === 'url') {
      if (!url.trim()) return
      // 2026-08-23 (pedido explícito del usuario): "la forma se acomode en
      // base a la imagen" — mide la imagen real antes de crear el
      // elemento; si por lo que sea no se puede medir (URL rota, CORS raro
      // en la sola carga), cae al tamaño fijo de siempre en vez de
      // bloquear la creación.
      const natural = await loadImageNaturalSize(url.trim())
      const size = natural ? fitImageElementSize(natural) : undefined
      onCreate(size ? buildInputWithSize('IMAGE', size, { url: url.trim(), frameStyle }) : buildInput('IMAGE', { url: url.trim(), frameStyle }))
      onDone()
      return
    }
    if (!file) return
    setUploading(true)
    try {
      const uploaded = await uploadVisionBoardImage(file)
      // El blob local (`previewUrl`) ya está en memoria — medirlo no
      // implica una segunda descarga.
      const natural = previewUrl ? await loadImageNaturalSize(previewUrl) : null
      const size = natural ? fitImageElementSize(natural) : undefined
      onCreate(size ? buildInputWithSize('IMAGE', size, { imageId: uploaded.id, frameStyle }) : buildInput('IMAGE', { imageId: uploaded.id, frameStyle }))
      onDone()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo subir la imagen.')
    } finally {
      setUploading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <StepBackHeader title="Agregar imagen" onBack={onBack} />
      <div
        className={toolbarStyles.shapeVariantGrid}
        role="radiogroup"
        aria-label="Origen de la imagen"
        onKeyDown={handleRadiogroupKeyDown}
      >
        <button
          type="button"
          role="radio"
          aria-checked={source === 'upload'}
          tabIndex={radioTabIndex(source === 'upload', true, true)}
          className={`${toolbarStyles.shapeVariantButton} ${source === 'upload' ? toolbarStyles.shapeVariantButtonActive : ''}`}
          onClick={() => setSource('upload')}
        >
          Subir archivo
        </button>
        <button
          type="button"
          role="radio"
          aria-checked={source === 'url'}
          tabIndex={radioTabIndex(source === 'url', false, true)}
          className={`${toolbarStyles.shapeVariantButton} ${source === 'url' ? toolbarStyles.shapeVariantButtonActive : ''}`}
          onClick={() => setSource('url')}
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
            value={url}
            onChange={(event) => setUrl(event.target.value)}
            placeholder="https://…"
            autoFocus
            required
          />
        </label>
      ) : (
        <label
          className={`${styles.imageDropzone} ${dragOver ? styles.imageDropzoneActive : ''}`}
          onDragOver={(event) => {
            event.preventDefault()
            setDragOver(true)
          }}
          onDragLeave={() => setDragOver(false)}
          onDrop={handleDrop}
        >
          {previewUrl ? (
            <img src={previewUrl} alt="" className={styles.imageDropzonePreview} />
          ) : (
            <>
              <UploadCloud width={22} height={22} aria-hidden="true" />
              <span>Arrastra una imagen aquí, o haz clic para elegir un archivo</span>
            </>
          )}
          <input
            type="file"
            accept="image/png,image/jpeg,image/webp,image/gif"
            className={styles.imageDropzoneInput}
            onChange={(event) => pickFile(event.target.files?.[0] ?? null)}
          />
        </label>
      )}

      {/* 2026-08-23 (pedido explícito del usuario, catálogo estilo Canva):
          "Marcos" — opcional, "Ninguno" es el comportamiento de siempre
          (rectángulo con object-fit:cover, sin frameStyle en absoluto). */}
      <div className={shellStyles.field}>
        <span className={shellStyles.fieldLabel}>Marco (opcional)</span>
        <div
          className={toolbarStyles.shapeVariantGrid}
          role="radiogroup"
          aria-label="Marco de la imagen"
          onKeyDown={handleRadiogroupKeyDown}
        >
          <button
            type="button"
            role="radio"
            aria-checked={!frameStyle}
            tabIndex={radioTabIndex(!frameStyle, true, true)}
            className={`${toolbarStyles.shapeVariantButton} ${!frameStyle ? toolbarStyles.shapeVariantButtonActive : ''}`}
            onClick={() => setFrameStyle(undefined)}
          >
            Ninguno
          </button>
          {FRAME_STYLE_OPTIONS.map((frame) => (
            <button
              key={frame.id}
              type="button"
              role="radio"
              aria-checked={frameStyle === frame.id}
              tabIndex={radioTabIndex(frameStyle === frame.id, false, true)}
              className={`${toolbarStyles.shapeVariantButton} ${frameStyle === frame.id ? toolbarStyles.shapeVariantButtonActive : ''}`}
              onClick={() => setFrameStyle(frame.id)}
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

      <div className={shellStyles.formActions}>
        <button type="button" data-variant="secondary" onClick={onBack} disabled={uploading}>
          Cancelar
        </button>
        <button type="submit" disabled={uploading || (source === 'upload' && !file)}>
          {uploading ? 'Subiendo…' : 'Agregar'}
        </button>
      </div>
    </form>
  )
}

/** Reuses the existing sticker grid (core/ui/pickers/StickerPicker.tsx) —
    picking a sticker creates immediately, same "pick and go" StickerAction
    used, just inside this step instead of its own popover. */
function StickerFields({
  onCreate,
  onBack,
  onDone,
}: {
  onCreate: (input: CreateVisionBoardElementInput) => void
  onBack: () => void
  onDone: () => void
}) {
  function handlePick(stickerId: string | undefined) {
    if (!stickerId) return
    onCreate(buildInput('STICKER', { stickerId }))
    onDone()
  }

  return (
    <div>
      <StepBackHeader title="Elegir sticker" onBack={onBack} />
      <StickerPicker value={undefined} onChange={handlePick} commitOnArrowKey={false} />
    </div>
  )
}

/** BLOQUE D (post-MVP): "😊 Emojis" — same pick-and-create-immediately
    shape as StickerFields above (it behaves as a real board element in
    every way a sticker does — see visionBoardEmojis.ts's own doc
    comment), grouped by category since 120 entries in one flat grid would
    be an unreasonably long, un-scannable scroll. */
function EmojiFields({
  onCreate,
  onBack,
  onDone,
}: {
  onCreate: (input: CreateVisionBoardElementInput) => void
  onBack: () => void
  onDone: () => void
}) {
  function handlePick(emojiId: string) {
    onCreate(buildInput('STICKER', { emojiId }))
    onDone()
  }

  return (
    <div>
      <StepBackHeader title="Elegir emoji" onBack={onBack} />
      <VisionBoardEmojiPicker value={undefined} onChange={handlePick} commitOnArrowKey={false} />
    </div>
  )
}

/** FASE 13: unlike the old inline "Forma" button (always a rectangle,
    variant only changeable afterwards via Editar), this lets the variant
    be picked at creation time. BLOQUE C (post-MVP): now the full ~30-shape
    catalog (visionBoardShapes.ts), same shared `VisionBoardShapePicker`
    VisionBoardElementEditor.tsx's own Editar step uses — picking here
    still creates immediately (pick-and-go, like Sticker's own step),
    unlike the Editor's stage-then-Guardar flow, so `value` is left
    "nothing selected" rather than tracking a draft. */
function ShapeFields({
  onCreate,
  onBack,
  onDone,
}: {
  onCreate: (input: CreateVisionBoardElementInput) => void
  onBack: () => void
  onDone: () => void
}) {
  function handlePick(variant: string) {
    onCreate(buildInput('SHAPE', { shape: variant }))
    onDone()
  }

  return (
    <div>
      <StepBackHeader title="Elegir forma" onBack={onBack} />
      <VisionBoardShapePicker value="" onChange={handlePick} label="Elegir forma" commitOnArrowKey={false} />
    </div>
  )
}

/** Sin paso propio: se crea al instante con datos de ejemplo, igual que
    elegir un sticker — ver DEFAULT_TABLE_ROWS's propio doc comment sobre
    por qué (todavía no hay edición celda por celda). */
function TableFields({
  onCreate,
  onDone,
}: {
  onCreate: (input: CreateVisionBoardElementInput) => void
  onDone: () => void
}) {
  // Hallazgo real (Playwright, 2026-08-22): sin este guard, un solo clic en
  // "Tabla" creaba DOS tablas apiladas — <StrictMode> (main.tsx) monta,
  // desmonta y vuelve a montar cada componente una vez en desarrollo para
  // detectar efectos sin limpieza, así que este useEffect corría dos veces
  // por el mismo clic. `createdRef` sobrevive ese segundo montaje (persiste
  // mientras el componente exista) y asegura que `onCreate` se dispare
  // como máximo una vez.
  const createdRef = useRef(false)
  useEffect(() => {
    if (createdRef.current) return
    createdRef.current = true
    onCreate(buildInput('TABLE', { rows: DEFAULT_TABLE_ROWS.map((row) => [...row]) }))
    onDone()
    // eslint-disable-next-line react-hooks/exhaustive-deps -- crea una sola vez, al montar este paso.
  }, [])
  return null
}

/** Mismo "pick and go" que ShapeFields — 6 tipos de gráfica, datos de
    ejemplo fijos (visionBoardCharts.ts), no hay borrador que mantener. */
function ChartFields({
  onCreate,
  onBack,
  onDone,
}: {
  onCreate: (input: CreateVisionBoardElementInput) => void
  onBack: () => void
  onDone: () => void
}) {
  function handlePick(chartType: string) {
    onCreate(buildInput('CHART', { chartType }))
    onDone()
  }

  return (
    <div>
      <StepBackHeader title="Elegir gráfica" onBack={onBack} />
      <div className={styles.libraryGrid}>
        {CHART_TYPE_OPTIONS.map((chart) => {
          const { viewBox, markup } = buildChart(chart.id)
          return (
            <button key={chart.id} type="button" className={styles.libraryTile} onClick={() => handlePick(chart.id)}>
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

/** Mismo "pick and go" que Shape/Chart — 5 diseños, celdas decorativas de
    color (ver visionBoardGrids.ts's propio doc comment sobre el alcance). */
function GridFields({
  onCreate,
  onBack,
  onDone,
}: {
  onCreate: (input: CreateVisionBoardElementInput) => void
  onBack: () => void
  onDone: () => void
}) {
  function handlePick(layoutId: string) {
    onCreate(buildInput('GRID', { layout: layoutId }))
    onDone()
  }

  return (
    <div>
      <StepBackHeader title="Elegir cuadrícula" onBack={onBack} />
      <div className={styles.libraryGrid}>
        {GRID_LAYOUT_OPTIONS.map((layout) => (
          <button key={layout.id} type="button" className={styles.libraryTile} onClick={() => handlePick(layout.id)}>
            <span
              aria-hidden="true"
              style={{
                display: 'grid',
                gridTemplateRows: layout.gridTemplateRows,
                gridTemplateColumns: layout.gridTemplateColumns,
                gap: 2,
                width: 20,
                height: 20,
              }}
            >
              {Array.from({ length: layout.cells }, (_, index) => (
                <span
                  key={index}
                  style={{
                    background: 'currentColor',
                    opacity: 0.7,
                    gridRow: layout.spanCell === index ? 'span 2' : undefined,
                  }}
                />
              ))}
            </span>
            {layout.label}
          </button>
        ))}
      </div>
    </div>
  )
}
