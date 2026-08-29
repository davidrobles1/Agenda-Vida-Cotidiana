import { useState } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Popover } from 'react-aria-components'
import { AnimatePresence, motion } from 'motion/react'
import { LayoutTemplate } from 'lucide-react'
import { motionTokens } from '../../core/motion/tokens'
import { findStickerOption } from '../../core/ui/pickers/pickerCatalog'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import toolbarStyles from './VisionBoardToolbar.module.css'
import { PORTAL_VISION_BOARD_TEMPLATES } from './templates/portalTemplates'
import styles from './VisionBoardTemplates.module.css'
import {
  VISION_BOARD_TEMPLATES,
  fitTemplateToSize,
  type FittedTemplateElement,
  type VisionBoardTemplate,
} from './visionBoardTemplates'

const MotionDialog = motion.create(Dialog)

const CARD_PREVIEW_WIDTH = 200
const CARD_PREVIEW_HEIGHT = 125
const CONFIRM_PREVIEW_WIDTH = 560
const CONFIRM_PREVIEW_HEIGHT = 350

interface VisionBoardTemplatesActionProps {
  onApply: (template: VisionBoardTemplate) => Promise<void>
  /** BLOQUE E (post-MVP): optionally controlled from outside — the context
      menu's "Templates" entry needs to open this same popover, same
      "falls back to internal state when omitted" shape
      VisionBoardDeleteConfirm.tsx's own isOpen/onOpenChange already
      established (FASE 17), so the toolbar's own trigger button keeps
      working completely unchanged. */
  isOpen?: boolean
  onOpenChange?: (open: boolean) => void
}

/**
 * FASE 12: "Templates" — a two-step popover (grid → preview/confirm), same
 * role="dialog" pattern the other toolbar actions already use
 * (TextEntryAction/StickerAction in VisionBoardToolbar.tsx), not a new
 * dialog primitive. Picking a template never applies it immediately —
 * unlike StickerAction's "pick and go," a template creates several
 * elements at once, so it gets the same "stage, then explicit confirm"
 * shape VisionBoardElementEditor already uses for edits.
 *
 * FASE 23 fix: used to animate `Popover` directly — see
 * VisionBoardExportAction.tsx's own doc comment for the real bug that
 * causes (a permanently invisible-but-clickable panel). Now animates
 * `Dialog` instead.
 */
export function VisionBoardTemplatesAction({
  onApply,
  isOpen: isOpenProp,
  onOpenChange: onOpenChangeProp,
}: VisionBoardTemplatesActionProps) {
  const [internalOpen, setInternalOpen] = useState(false)
  const isOpen = isOpenProp ?? internalOpen
  const [picked, setPicked] = useState<VisionBoardTemplate | null>(null)
  const [applying, setApplying] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function handleOpenChange(open: boolean) {
    if (onOpenChangeProp) {
      onOpenChangeProp(open)
    } else {
      setInternalOpen(open)
    }
    if (!open) {
      setPicked(null)
      setError(null)
    }
  }

  async function handleApply(close: () => void) {
    if (!picked) return
    setApplying(true)
    setError(null)
    try {
      await onApply(picked)
      close()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo aplicar la plantilla.')
    } finally {
      setApplying(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      {/* FASE 23: "acción principal" treatment — see
          VisionBoardToolbar.module.css's own doc comment on
          .actionButtonPrimary. */}
      {/* Barra icon-only (2026-08-23) — ver el mismo comentario en
          VisionBoardElementLibrary.tsx sobre por qué el trigger necesita su
          propio aria-label ahora que no hay texto visible. */}
      <Button
        className={`${toolbarStyles.actionButton} ${toolbarStyles.actionButtonPrimary}`}
        aria-label="Templates"
      >
        <LayoutTemplate width={16} height={16} />
      </Button>
      <Popover placement="bottom start" offset={8} className={styles.templatePopover}>
        {/* FASE 20 audit fix: no static aria-label — the grid step and the
            preview step each render their own `Heading slot="title"`
            ("Elige un template" vs. the picked template's own name); a
            static "Templates" label would override that on every step. */}
        <MotionDialog
          className={styles.templateDialog}
          initial={{ opacity: 0, scale: 0.96, y: -4 }}
          animate={{ opacity: isOpen ? 1 : 0, scale: isOpen ? 1 : 0.96, y: isOpen ? 0 : -4 }}
          transition={motionTokens.smooth}
        >
          {({ close }) => (
            // FASE 23: crossfade between grid ↔ preview, same
            // `AnimatePresence mode="wait"` + opacity-only shape
            // VisionBoardElementLibrary.tsx already established for its own
            // menu ↔ step swap — reused, not a new pattern.
            <AnimatePresence mode="wait" initial={false}>
              {picked ? (
                <motion.div
                  key="preview"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  transition={motionTokens.base}
                  className={styles.previewStep}
                >
                  <div className={styles.previewHeading}>
                    <Heading slot="title" className={shellStyles.heading}>
                      {picked.name}
                    </Heading>
                    <p className={styles.templateCardDescription}>{picked.description}</p>
                  </div>
                  <TemplatePreview
                    template={picked}
                    width={CONFIRM_PREVIEW_WIDTH}
                    height={CONFIRM_PREVIEW_HEIGHT}
                  />
                  {error && (
                    <p role="alert" className={shellStyles.formError}>
                      {error}
                    </p>
                  )}
                  <div className={styles.previewActions}>
                    <button type="button" data-variant="secondary" onClick={() => setPicked(null)} disabled={applying}>
                      Cancelar
                    </button>
                    <button type="button" onClick={() => handleApply(close)} disabled={applying}>
                      {applying ? 'Aplicando…' : 'Aplicar'}
                    </button>
                  </div>
                </motion.div>
              ) : (
                <motion.div
                  key="grid"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  transition={motionTokens.base}
                >
                  <Heading slot="title" className={shellStyles.heading}>
                    Elige un template
                  </Heading>
                  <p className={styles.templateIntro}>
                    Agrega una composición prediseñada a tu Vision Board — no reemplaza lo que ya tienes.
                  </p>
                  {/* Dos grupos, no una lista revuelta: las del portal son
                      obra del propio usuario y las genéricas un punto de
                      arranque. */}
                  <p className={styles.templateGroupLabel}>Plantillas del portal</p>
                  <div className={styles.templateGrid}>
                    {PORTAL_VISION_BOARD_TEMPLATES.map((template) => (
                      <button
                        key={template.id}
                        type="button"
                        className={`${styles.templateCard} ${styles.templateCardPortal}`}
                        onClick={() => setPicked(template)}
                      >
                        <TemplatePreview template={template} width={CARD_PREVIEW_WIDTH} height={CARD_PREVIEW_HEIGHT} />
                        <p className={styles.templateCardName}>{template.name}</p>
                        <p className={styles.templateCardDescription}>{template.description}</p>
                      </button>
                    ))}
                  </div>

                  <p className={styles.templateGroupLabel}>Plantillas de partida</p>
                  <div className={styles.templateGrid}>
                    {VISION_BOARD_TEMPLATES.map((template) => (
                      <button
                        key={template.id}
                        type="button"
                        className={styles.templateCard}
                        onClick={() => setPicked(template)}
                      >
                        <TemplatePreview template={template} width={CARD_PREVIEW_WIDTH} height={CARD_PREVIEW_HEIGHT} />
                        <p className={styles.templateCardName}>{template.name}</p>
                        <p className={styles.templateCardDescription}>{template.description}</p>
                      </button>
                    ))}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          )}
        </MotionDialog>
      </Popover>
    </DialogTrigger>
  )
}

/** Purely decorative — renders each template element as a small positioned
    block via the exact same fit-to-size math `handleApplyTemplate` uses
    for the real thing (fitTemplateToSize), just targeting a tiny preview
    box instead of the real board. TEXT/NOTE show their real (truncated)
    copy; STICKER shows the real catalog asset; SHAPE reuses the same
    rectangle/circle/line look as the real canvas. */
function TemplatePreview({ template, width, height }: { template: VisionBoardTemplate; width: number; height: number }) {
  const fitted = fitTemplateToSize(template, width, height)
  return (
    <div className={styles.previewBox} style={{ width, height }} aria-hidden="true">
      {fitted.map((element, index) => (
        <PreviewElement key={index} element={element} />
      ))}
    </div>
  )
}

function PreviewElement({ element }: { element: FittedTemplateElement }) {
  const style = { left: element.x, top: element.y, width: element.width, height: element.height }
  const text = typeof element.data.text === 'string' ? element.data.text : ''

  switch (element.type) {
    case 'TEXT':
      return (
        <span className={`${styles.previewElement} ${styles.previewText}`} style={style}>
          {text}
        </span>
      )
    case 'NOTE':
      return (
        <span className={`${styles.previewElement} ${styles.previewNote}`} style={style}>
          {text}
        </span>
      )
    case 'STICKER': {
      const sticker = findStickerOption(typeof element.data.stickerId === 'string' ? element.data.stickerId : undefined)
      return (
        <span className={`${styles.previewElement} ${styles.previewSticker}`} style={style}>
          {sticker && <img src={sticker.asset} alt="" className={styles.previewStickerImage} />}
        </span>
      )
    }
    case 'SHAPE': {
      const variant = element.data.shape
      const variantClass =
        variant === 'circle' ? styles.previewShapeCircle : variant === 'line' ? styles.previewShapeLine : ''
      return <span className={`${styles.previewElement} ${styles.previewShape} ${variantClass}`} style={style} />
    }
    case 'IMAGE': {
      // Antes devolvía `null` porque ninguna plantilla llevaba imágenes. En
      // las del portal la fotografía ES la composición: sin esto, sus
      // tarjetas saldrían vacías y no habría forma de distinguirlas.
      const url = typeof element.data.url === 'string' ? element.data.url : undefined
      return (
        <span className={`${styles.previewElement} ${styles.previewImage}`} style={style}>
          {url && <img src={url} alt="" className={styles.previewImageImg} loading="lazy" decoding="async" />}
        </span>
      )
    }
  }
}
