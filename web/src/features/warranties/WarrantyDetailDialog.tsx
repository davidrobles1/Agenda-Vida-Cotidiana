import { useEffect, useState, type FormEvent } from 'react'
import { Dialog, Heading, Modal, ModalOverlay } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../core/motion/tokens'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import { listInventoryItems, type InventoryItem } from '../inventory/api'
import { useActiveMode } from '../../core/user/ActiveModeContext'
import { completeWarranty, updateWarranty, type Warranty } from './api'
import { STATE_LABELS, fileLabel, formatFullDate, warrantyState } from './warrantiesView'
import styles from './WarrantyDetailDialog.module.css'
import { DatePicker } from '../../core/ui/pickers/DatePicker'

const MotionDialog = motion.create(Dialog)

interface WarrantyDetailDialogProps {
  warranty: Warranty
  onClose: () => void
  onSaved: (warranty: Warranty) => void
}

/**
 * ADR-022: detalle de una garantía — ver, editar y enlazar con el
 * inventario.
 *
 * CARENCIA QUE CUBRE: hasta ahora **no se podía editar**. El backend tenía
 * `PATCH /warranties/{id}` y `api.ts` tenía `updateWarranty`, pero ninguna
 * pantalla los llamaba: equivocarse en la fecha de vencimiento o en el
 * nombre obligaba a borrar el registro y volver a subir el comprobante. Es
 * exactamente la misma carencia que ADR-021 corrigió en Mantenimiento.
 *
 * Es un diálogo y no una ruta nueva, igual que el detalle de Pagos y el de
 * Mantenimiento: la información cabe en una capa.
 */
export function WarrantyDetailDialog({ warranty, onClose, onSaved }: WarrantyDetailDialogProps) {
  const activeMode = useActiveMode()
  const [item, setItem] = useState(warranty.item)
  const [expiresAt, setExpiresAt] = useState(warranty.expiresAt.slice(0, 10))
  const [inventoryItemId, setInventoryItemId] = useState<string>(warranty.inventoryItemId ?? '')
  const [items, setItems] = useState<InventoryItem[] | null>(null)
  const [saving, setSaving] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const state = warrantyState(warranty)

  useEffect(() => {
    let cancelled = false
    // El selector de artículo ofrece solo los del módulo activo: enlazar
    // una garantía Personal con un artículo Laboral rompería el
    // aislamiento del ADR-019 por la puerta de atrás.
    listInventoryItems(activeMode)
      .then((page) => {
        if (!cancelled) setItems(page.items)
      })
      .catch(() => {
        // El enlace es contexto, no el propósito del diálogo: si falla, se
        // sigue pudiendo editar el resto.
        if (!cancelled) setItems([])
      })
    return () => {
      cancelled = true
    }
  }, [activeMode])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!item.trim() || !expiresAt || saving) return
    setSaving(true)
    setError(null)
    try {
      const saved = await updateWarranty(
        warranty.id,
        item.trim(),
        new Date(`${expiresAt}T12:00:00`).toISOString(),
        warranty.version,
        inventoryItemId || null,
        // Siempre explícito: así elegir "Sin enlazar" DESENLAZA de verdad,
        // en vez de interpretarse como "no tocar" (ADR-021(i)).
        true,
      )
      onSaved(saved)
      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar la garantía.')
    } finally {
      setSaving(false)
    }
  }

  /** Marca o desmarca la garantía como usada. El endpoint `/complete`
      alterna (`Warranty#toggleCompletion`), pero la pantalla anterior
      escondía el botón al completarse: deshacer era imposible. */
  async function handleToggleUsed() {
    setBusy(true)
    setError(null)
    try {
      const updated = await completeWarranty(warranty.id, warranty.version)
      onSaved(updated)
      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo cambiar el estado de la garantía.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <ModalOverlay isOpen isDismissable onOpenChange={(open) => !open && onClose()} className={shellStyles.modalOverlay}>
      <Modal>
        <MotionDialog
          className={shellStyles.panel}
          initial={{ opacity: 0, scale: 0.96, y: 8 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          transition={motionTokens.smooth}
        >
          <div className={shellStyles.panelScroll}>
            <form onSubmit={handleSubmit}>
              <div className={shellStyles.headerRow}>
                <div>
                  <Heading slot="title" className={shellStyles.heading}>
                    {warranty.item}
                  </Heading>
                  <p className={styles.subtitle}>
                    {STATE_LABELS[state]} · vence el {formatFullDate(warranty.expiresAt)} ·{' '}
                    {fileLabel(warranty.documentContentType)}
                  </p>
                </div>
                <button type="button" className={shellStyles.closeButton} onClick={onClose} aria-label="Cerrar">
                  ×
                </button>
              </div>

              {error && (
                <p className={shellStyles.formError} role="alert">
                  {error}
                </p>
              )}

              <label className={shellStyles.field}>
                <span className={shellStyles.fieldLabel}>¿Qué artículo cubre?</span>
                <input
                  className={shellStyles.textInput}
                  value={item}
                  onChange={(event) => setItem(event.target.value)}
                  required
                />
              </label>

              <DatePicker
                    label="Vence el"
                    value={expiresAt}
                    onChange={(next) => setExpiresAt(next)}
                    isRequired
                  />

              {/* ADR-022: el mismo objeto vivía en dos listas que se
                  ignoraban. Enlazarlo es lo que permite responder "¿este
                  artículo todavía tiene garantía?" desde Inventario. */}
              <label className={shellStyles.field}>
                <span className={shellStyles.fieldLabel}>Artículo del inventario</span>
                <select
                  className={shellStyles.textInput}
                  value={inventoryItemId}
                  onChange={(event) => setInventoryItemId(event.target.value)}
                  disabled={items === null}
                >
                  <option value="">— Sin enlazar —</option>
                  {(items ?? []).map((option) => (
                    <option key={option.id} value={option.id}>
                      {option.name}
                      {option.location ? ` · ${option.location}` : ''}
                    </option>
                  ))}
                </select>
              </label>
              {items !== null && items.length === 0 && (
                <p className={styles.helper}>
                  Todavía no tienes artículos en el inventario de este módulo con los que enlazar esta garantía.
                </p>
              )}

              <div className={styles.actions}>
                <button
                  type="button"
                  className={state === 'used' ? styles.secondary : styles.primary}
                  disabled={busy}
                  onClick={() => void handleToggleUsed()}
                >
                  {state === 'used' ? 'Volver a vigente' : 'Marcar como usada'}
                </button>

                <button type="submit" className={styles.secondary} disabled={saving}>
                  {saving ? 'Guardando…' : 'Guardar cambios'}
                </button>
              </div>
            </form>
          </div>
        </MotionDialog>
      </Modal>
    </ModalOverlay>
  )
}
