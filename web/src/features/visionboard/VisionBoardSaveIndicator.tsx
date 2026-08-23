import { AnimatePresence, motion } from 'motion/react'
import { AlertTriangle, Check, Circle } from 'lucide-react'
import { motionTokens } from '../../core/motion/tokens'
import type { SaveStatus } from './VisionBoardCanvas'
import styles from './VisionBoardSaveIndicator.module.css'

interface VisionBoardSaveIndicatorProps {
  status: SaveStatus
  onRetry: () => void
}

/**
 * FASE 18: the one visible surface for the autosave status — plain text +
 * icon (no new icon library, `lucide-react` is already the project's set),
 * never a modal, never blocks the canvas. `aria-live="polite"` on the
 * status text itself: since every transition here already corresponds to
 * a real, debounced/coalesced action boundary (drag/resize/rotate end,
 * or any other single mutation) rather than firing per pointermove/
 * keystroke, this naturally avoids the "Guardando / Guardando / Guardando"
 * spam the phase explicitly warns against — there's nothing upstream that
 * calls `setSaveStatus` more often than that already.
 *
 * Motion: a short opacity crossfade between states (`motionTokens.quick`,
 * the fastest token in the set — this is an ambient status readout, not a
 * moment to linger on), the same `AnimatePresence mode="wait"` shape
 * VisionBoardElementLibrary.tsx already uses for its own step swaps.
 * `MotionConfig reducedMotion="user"` (global, main.tsx) covers this the
 * same as everywhere else in the app.
 */
export function VisionBoardSaveIndicator({ status, onRetry }: VisionBoardSaveIndicatorProps) {
  return (
    <span className={styles.indicator} role="status" aria-live="polite">
      <AnimatePresence mode="wait" initial={false}>
        {status.kind === 'saved' && (
          <motion.span
            key="saved"
            className={styles.indicatorSaved}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={motionTokens.quick}
            style={{ display: 'inline-flex', alignItems: 'center', gap: 5 }}
          >
            <Check width={13} height={13} aria-hidden="true" />
            Guardado
          </motion.span>
        )}
        {status.kind === 'dirty' && (
          <motion.span
            key="dirty"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={motionTokens.quick}
            style={{ display: 'inline-flex', alignItems: 'center', gap: 5 }}
          >
            <Circle width={9} height={9} aria-hidden="true" fill="currentColor" />
            Sin guardar
          </motion.span>
        )}
        {status.kind === 'saving' && (
          <motion.span
            key="saving"
            className={styles.indicatorSaving}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={motionTokens.quick}
            style={{ display: 'inline-flex', alignItems: 'center', gap: 5 }}
          >
            <span className={styles.spinner} aria-hidden="true" />
            Guardando…
          </motion.span>
        )}
        {status.kind === 'error' && (
          <motion.span
            key="error"
            className={styles.indicatorError}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={motionTokens.quick}
            style={{ display: 'inline-flex', alignItems: 'center', gap: 5 }}
          >
            <AlertTriangle width={13} height={13} aria-hidden="true" />
            Error al guardar
            <button type="button" className={styles.retryButton} onClick={onRetry}>
              Reintentar
            </button>
          </motion.span>
        )}
      </AnimatePresence>
    </span>
  )
}
