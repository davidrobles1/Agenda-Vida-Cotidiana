import { useState } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Popover } from 'react-aria-components'
import { motion } from 'motion/react'
import { Download, FileImage, FileText, Image as ImageIcon } from 'lucide-react'
import { motionTokens } from '../../core/motion/tokens'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import type { VisionBoard, VisionBoardElement } from './api'
import {
  canvasToJpegBlob,
  canvasToPngBlob,
  downloadBlob,
  downloadVisionBoardPdf,
  exportColorsForTheme,
  renderVisionBoardToCanvas,
  slugifyForFilename,
} from './visionBoardExport'
import toolbarStyles from './VisionBoardToolbar.module.css'
import styles from './VisionBoardExportAction.module.css'

const MotionDialog = motion.create(Dialog)

type ExportFormat = 'png' | 'jpg' | 'pdf'

interface VisionBoardExportActionProps {
  board: VisionBoard
  elements: VisionBoardElement[]
}

/**
 * FASE 14: "Exportar" — all three formats reuse the exact same
 * `renderVisionBoardToCanvas` output, so what a user gets as PNG/JPG/PDF
 * only differs in the final encode step — not in what's drawn.
 *
 * BLOQUE H (post-MVP): PDF used to open a dedicated print window and rely
 * on the browser's native print dialog ("Guardar como PDF") — a real
 * window.open + popup-blocker dance, and a non-deterministic export a
 * human had to complete by hand. Now a direct, deterministic download via
 * `downloadVisionBoardPdf` (see visionBoardExport.ts's own doc comment),
 * same one-click shape PNG/JPG already had — no window, no dialog, no
 * popup-blocker handling needed for this format anymore.
 *
 * FASE 19: added JPG (`canvasToJpegBlob`) — the original plan asked for
 * PNG/JPG/PDF but only PNG/PDF existed. Same Blob-download path as PNG,
 * same renderer, no new dependency (`canvas.toBlob('image/jpeg', quality)`
 * is native).
 *
 * FASE 16: colors come from the board's own Board Theme
 * (`exportColorsForTheme(board.theme)`), not from reading the live DOM —
 * export no longer needs a reference to the `.canvas` element at all,
 * since a Board Theme's colors are already fully known from `board.theme`
 * alone (visionBoardThemes.ts is the single source, on-screen or exported).
 *
 * `processing` gates both format buttons at once — "evitar múltiples
 * ejecuciones simultáneas" applies to the whole export action, not just
 * per-format, since generating two exports of the same board at once has
 * no benefit and doubles the (real, if brief) main-thread + image-loading
 * work for nothing.
 *
 * FASE 23 fix: this used to animate `Popover` directly (`motion.create(Popover)`)
 * — the exact bug `core/theme/ThemeSwitcher.tsx`'s own doc comment already
 * diagnosed and fixed elsewhere: `Popover`'s internal `useResizeObserver`/
 * `useLayoutEffect` re-renders it after mount and clobbers Motion's own
 * inline style back to `initial`, leaving the panel fully present and
 * clickable but permanently invisible. Now animates `Dialog` instead
 * (`MotionDialog`), `Popover` itself left un-animated — same fix shape as
 * `VisionBoardDeleteConfirm.tsx` (animates `Dialog`, not `Modal`) and
 * `VisionBoardThemeSwitcher.tsx`/`VisionBoardSwitcher.tsx` (animate `Menu`,
 * not `Popover`).
 */
export function VisionBoardExportAction({ board, elements }: VisionBoardExportActionProps) {
  const [isOpen, setIsOpen] = useState(false)
  const [processing, setProcessing] = useState<ExportFormat | null>(null)
  const [error, setError] = useState<string | null>(null)

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (!open) setError(null)
  }

  async function handleExport(format: ExportFormat) {
    if (processing) return

    setError(null)
    setProcessing(format)
    try {
      const colors = exportColorsForTheme(board.theme)
      const canvas = await renderVisionBoardToCanvas(board, elements, colors)
      if (format === 'png') {
        const blob = await canvasToPngBlob(canvas)
        downloadBlob(blob, `${slugifyForFilename(board.name)}.png`)
      } else if (format === 'jpg') {
        const blob = await canvasToJpegBlob(canvas)
        downloadBlob(blob, `${slugifyForFilename(board.name)}.jpg`)
      } else {
        await downloadVisionBoardPdf(canvas, board.width, board.height, `${slugifyForFilename(board.name)}.pdf`)
      }
      setIsOpen(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo exportar el Vision Board.')
    } finally {
      setProcessing(null)
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
        aria-label="Exportar"
      >
        <Download width={16} height={16} />
      </Button>
      <Popover placement="bottom start" offset={8} className={styles.exportPopover}>
        {/* FASE 20 audit fix: dropped the static aria-label="Exportar" — it
            silently overrode the Heading below ("Exportar Vision Board")
            for screen readers; removing it lets react-aria-components
            auto-wire aria-labelledby to that Heading instead, so the
            announced name matches what's visibly on screen. */}
        <MotionDialog
          className={styles.exportDialog}
          initial={{ opacity: 0, scale: 0.96, y: -4 }}
          animate={{ opacity: isOpen ? 1 : 0, scale: isOpen ? 1 : 0.96, y: isOpen ? 0 : -4 }}
          transition={motionTokens.smooth}
        >
          <Heading slot="title" className={shellStyles.heading}>
            Exportar Vision Board
          </Heading>
          <p className={styles.exportIntro}>Solo se exporta el contenido del tablero, sin controles ni interfaz.</p>
          {error && (
            <p role="alert" className={shellStyles.formError}>
              {error}
            </p>
          )}
          <div className={styles.exportActions}>
            <button
              type="button"
              className={styles.exportFormatButton}
              onClick={() => handleExport('png')}
              disabled={processing !== null}
            >
              <FileImage width={20} height={20} />
              {processing === 'png' ? 'Generando…' : 'PNG'}
            </button>
            <button
              type="button"
              className={styles.exportFormatButton}
              onClick={() => handleExport('jpg')}
              disabled={processing !== null}
            >
              <ImageIcon width={20} height={20} />
              {processing === 'jpg' ? 'Generando…' : 'JPG'}
            </button>
            <button
              type="button"
              className={styles.exportFormatButton}
              onClick={() => handleExport('pdf')}
              disabled={processing !== null}
            >
              <FileText width={20} height={20} />
              {processing === 'pdf' ? 'Generando…' : 'PDF'}
            </button>
          </div>
        </MotionDialog>
      </Popover>
    </DialogTrigger>
  )
}
