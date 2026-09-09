import { useEffect, useState, type FormEvent } from 'react'
import { Dialog, Heading, Modal, ModalOverlay } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../core/motion/tokens'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import {
  completeOccurrence,
  listMaintenanceLogFor,
  undoLastOccurrence,
  updateMaintenanceRecord,
  type MaintenanceLogEntry,
  type MaintenanceRecord,
} from './api'
import { everyLabel, formatDate, maintenanceState, STATE_LABELS } from './maintenanceView'
import styles from './MaintenanceDetailDialog.module.css'
import { DatePicker } from '../../core/ui/pickers/DatePicker'
import { InventoryItemPicker } from '../inventory/InventoryItemPicker'

const MotionDialog = motion.create(Dialog)

/** Los mismos intervalos que ofrece el alta (`CreateMaintenanceDialog`),
    más la opción de no repetir — que hasta ahora no se podía elegir ni
    cambiar después de crear el registro. */
const INTERVALS: Array<{ months: number | null; label: string }> = [
  { months: null, label: 'Sin repetición' },
  { months: 1, label: 'Cada mes' },
  { months: 3, label: 'Cada 3 meses' },
  { months: 6, label: 'Cada 6 meses' },
  { months: 12, label: 'Cada año' },
]

interface MaintenanceDetailDialogProps {
  record: MaintenanceRecord
  onClose: () => void
  onSaved: (record: MaintenanceRecord) => void
  onCompleted: (record: MaintenanceRecord) => void
  onLogChanged: () => void
}

/**
 * ADR-021: detalle de un mantenimiento — ver, editar e historial.
 *
 * CARENCIA QUE CUBRE: hasta ahora **no se podía editar**. El backend tenía
 * `PATCH /maintenance-records/{id}` y `api.ts` tenía
 * `updateMaintenanceRecord`, pero ninguna pantalla los llamaba: equivocarse
 * de fecha obligaba a borrar y volver a crear, perdiendo el registro. Y el
 * historial se guardaba sin que nada lo mostrara.
 *
 * Es un diálogo y no una ruta nueva, igual que el detalle de Pagos: la
 * información cabe en una capa y añadir navegación para leer cuatro campos
 * y cinco fechas sería tránsito de más.
 */
export function MaintenanceDetailDialog({
  record,
  onClose,
  onSaved,
  onCompleted,
  onLogChanged,
}: MaintenanceDetailDialogProps) {
  const [item, setItem] = useState(record.item)
  const [nextDueAt, setNextDueAt] = useState(record.nextDueAt.slice(0, 10))
  const [intervalMonths, setIntervalMonths] = useState<number | null>(record.intervalMonths ?? null)
  const [inventoryItemId, setInventoryItemId] = useState<string>(record.inventoryItemId ?? '')
  const [saving, setSaving] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [log, setLog] = useState<MaintenanceLogEntry[] | null>(null)

  useEffect(() => {
    let cancelled = false
    listMaintenanceLogFor(record.id)
      .then((entries) => {
        if (!cancelled) setLog(entries)
      })
      .catch(() => {
        // El historial es contexto: si falla, el resto del diálogo sigue
        // siendo usable y se dice qué pasó.
        if (!cancelled) setLog([])
      })
    return () => {
      cancelled = true
    }
  }, [record.id])

  const state = maintenanceState(record, log ?? [])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!item.trim() || !nextDueAt || saving) return
    setSaving(true)
    setError(null)
    try {
      const saved = await updateMaintenanceRecord(
        record.id,
        item.trim(),
        new Date(nextDueAt).toISOString(),
        record.version,
        intervalMonths ?? undefined,
        // Elegir "Sin repetición" tiene que poder QUITAR una periodicidad
        // ya asignada, no solo dejar de mandarla.
        intervalMonths === null,
        inventoryItemId || null,
        // Siempre explícito, igual que en Garantías: así elegir "No es un
        // artículo del inventario" DESENLAZA de verdad en vez de
        // interpretarse como "no tocar".
        true,
      )
      onSaved(saved)
      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar el mantenimiento.')
    } finally {
      setSaving(false)
    }
  }

  async function handleComplete() {
    setBusy(true)
    setError(null)
    try {
      const updated = await completeOccurrence(record.id)
      onCompleted(updated)
      onLogChanged()
      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo marcar el mantenimiento.')
    } finally {
      setBusy(false)
    }
  }

  async function handleUndo() {
    setBusy(true)
    setError(null)
    try {
      const updated = await undoLastOccurrence(record.id)
      onCompleted(updated)
      onLogChanged()
      onClose()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo deshacer.')
    } finally {
      setBusy(false)
    }
  }

  const isDone = state === 'done' || state === 'closed'

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
                    {record.item}
                  </Heading>
                  <p className={styles.subtitle}>
                    {STATE_LABELS[state]} · {everyLabel(record.intervalMonths)}
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
                <span className={shellStyles.fieldLabel}>¿Qué necesita mantenimiento?</span>
                <input
                  className={shellStyles.textInput}
                  value={item}
                  onChange={(event) => setItem(event.target.value)}
                  required
                />
              </label>

              <div className={styles.grid2}>
                <DatePicker
                    label="Próxima fecha"
                    value={nextDueAt}
                    onChange={(next) => setNextDueAt(next)}
                    isRequired
                  />

                {/* La periodicidad ahora se puede CAMBIAR, no solo elegir al
                    crear: es lo que decide si el mantenimiento vuelve. */}
                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>¿Cada cuánto?</span>
                  <select
                    className={shellStyles.textInput}
                    value={intervalMonths === null ? '' : String(intervalMonths)}
                    onChange={(event) =>
                      setIntervalMonths(event.target.value === '' ? null : Number(event.target.value))
                    }
                  >
                    {INTERVALS.map((option) => (
                      <option key={option.label} value={option.months === null ? '' : String(option.months)}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              {/* V31: el artículo al que se le hace, distinto del texto que
                  describe la tarea. Aquí se puede enlazar un mantenimiento
                  anterior a esta relación, o desenlazarlo. */}
              <InventoryItemPicker
                value={inventoryItemId}
                onChange={setInventoryItemId}
                label="¿A qué artículo se le hace?"
                emptyOptionLabel="— No es un artículo del inventario —"
              />

              <div className={styles.history}>
                <span className={styles.historyTitle}>Historial</span>
                {log === null && <p className={styles.historyEmpty}>Cargando…</p>}
                {log !== null && log.length === 0 && (
                  <p className={styles.historyEmpty}>Todavía no has registrado ninguna vez que se hiciera.</p>
                )}
                {log !== null && log.length > 0 && (
                  <ul className={styles.historyList}>
                    {log.slice(0, 6).map((entry) => {
                      // Solo decir "a tiempo" oculta QUÉ ocurrencia cubre la
                      // ejecución. Con periodicidades largas eso importa:
                      // completar hoy algo programado para dentro de un año
                      // decía "a tiempo" sin más, y no había forma de saber
                      // a qué revisión correspondía.
                      const note =
                        entry.completedDate === entry.scheduledDate
                          ? 'a tiempo'
                          : entry.completedDate > entry.scheduledDate
                            ? `con retraso · programado ${formatDate(entry.scheduledDate)}`
                            : `adelantado · programado ${formatDate(entry.scheduledDate)}`
                      return (
                        <li key={entry.id} className={styles.historyRow}>
                          <span>{formatDate(entry.completedDate)}</span>
                          <span className={styles.historyNote}>{note}</span>
                        </li>
                      )
                    })}
                  </ul>
                )}
                {log !== null && log.length > 6 && (
                  <p className={styles.historyEmpty}>y {log.length - 6} ejecuciones anteriores.</p>
                )}
              </div>

              <div className={styles.actions}>
                {isDone ? (
                  <button type="button" className={styles.secondary} disabled={busy} onClick={() => void handleUndo()}>
                    Deshacer
                  </button>
                ) : (
                  <button type="button" className={styles.primary} disabled={busy} onClick={() => void handleComplete()}>
                    Marcar como hecho
                  </button>
                )}

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
