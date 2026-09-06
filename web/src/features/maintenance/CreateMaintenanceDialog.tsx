import { useState, type FormEvent } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { Plus } from 'lucide-react'
import { motionTokens } from '../../core/motion/tokens'
import { handleRadiogroupKeyDown, radioTabIndex } from '../../core/ui/keyboard/radiogroupKeyboard'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import { createMaintenanceRecord, type MaintenanceRecord } from './api'
import { ShareWithFamily } from '../sharing/ShareWithFamily'
import { useResourceSharing } from '../sharing/useResourceSharing'
import styles from './CreateMaintenanceDialog.module.css'
import { useActiveMode } from '../../core/user/ActiveModeContext'
import { DatePicker } from '../../core/ui/pickers/DatePicker'

const MotionDialog = motion.create(Dialog)

interface CreateMaintenanceDialogProps {
  onCreated: (record: MaintenanceRecord) => void
}

/**
 * ADR-021: la periodicidad dejó de ser un atajo para calcular la fecha y
 * pasó a decidir el comportamiento del registro — con ella el mantenimiento
 * VUELVE al completarlo; sin ella, termina.
 *
 * Por eso "Sin repetición" es ahora una opción explícita y no la ausencia de
 * elección: encontrado validando con datos reales, crear un mantenimiento
 * sin tocar estos botones producía en silencio uno puntual, y el usuario
 * solo lo descubría meses después, cuando no volvía.
 */
const QUICK_INTERVALS: Array<{ id: string; label: string; months: number | null }> = [
  { id: 'none', label: 'Sin repetición', months: null },
  { id: '1m', label: '1 mes', months: 1 },
  { id: '3m', label: '3 meses', months: 3 },
  { id: '6m', label: '6 meses', months: 6 },
  { id: '12m', label: '1 año', months: 12 },
]

function dateFromMonthsFromNow(months: number): string {
  const date = new Date()
  date.setMonth(date.getMonth() + months)
  return date.toISOString().slice(0, 10)
}

/** Pedido explícito del usuario (2026-08-21): Mantenimiento "es simple
    [pero] tenemos que ser sofisticados... necesitamos que la gente lo
    utilice." El backend no cambió (mismo CRUD simple de siempre) — la
    "sofisticación" vive aquí, en quitarle fricción a registrar un
    mantenimiento: en vez de siempre teclear una fecha, un intervalo común
    (1/3/6/12 meses) la calcula con un clic; seguir editable a mano para
    cualquier otra fecha. */
export function CreateMaintenanceDialog({ onCreated }: CreateMaintenanceDialogProps) {
  // ADR-019: el recurso nace en el módulo desde el que se crea, y la
  // lista solo pide los de ese módulo. Fuera de /personal y /laboral
  // `activeMode` es null: se devuelve todo y las altas nacen PERSONAL.
  const activeMode = useActiveMode()
  const [isOpen, setIsOpen] = useState(false)
  const [item, setItem] = useState('')
  const [nextDueAt, setNextDueAt] = useState('')
  const [selectedInterval, setSelectedInterval] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // ADR-025 §2/§5: "hacer mi parte" de un mantenimiento es realizarlo, así
  // que este recurso SÍ admite comprometer a alguien.
  const sharing = useResourceSharing('MAINTENANCE')

  function reset() {
    setItem('')
    setNextDueAt('')
    setSelectedInterval(null)
    sharing.reset()
    setError(null)
  }

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (!open) reset()
  }

  function pickInterval(id: string, months: number | null) {
    setSelectedInterval(id)
    // Solo propone la fecha si el usuario no ha escrito una: elegir la
    // periodicidad no debe pisar una fecha que ya decidió a mano.
    if (months !== null && !nextDueAt) {
      setNextDueAt(dateFromMonthsFromNow(months))
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!item.trim() || !nextDueAt || saving) return
    setSaving(true)
    setError(null)
    try {
      // ADR-018: el intervalo ya no se usa solo para calcular la fecha y
      // olvidarse — se guarda, y el calendario proyecta con él las
      // siguientes fechas del mantenimiento.
      const interval = QUICK_INTERVALS.find((option) => option.id === selectedInterval)
      // `months: null` ("Sin repetición") y "no elegido" acaban igual en el
      // backend —sin intervalo—, pero solo el primero es una decisión.
      const created = await createMaintenanceRecord(
        item.trim(),
        new Date(nextDueAt).toISOString(),
        interval?.months ?? undefined,
        activeMode,
      )
      onCreated(created)

      // Después de crear: hasta aquí no había id que compartir.
      const shareError = await sharing.commit(created.id)
      if (shareError) {
        setError(shareError)
        return
      }
      setIsOpen(false)
      reset()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo registrar el mantenimiento.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      <Button className={styles.addButton}>
        <Plus width={16} height={16} /> Nuevo mantenimiento
      </Button>
      <Modal isDismissable className={shellStyles.modalOverlay}>
        <MotionDialog
          className={shellStyles.panel}
          initial={{ opacity: 0, scale: 0.96, y: 8 }}
          animate={{ opacity: isOpen ? 1 : 0, scale: isOpen ? 1 : 0.96, y: isOpen ? 0 : 8 }}
          transition={motionTokens.smooth}
        >
          {({ close }) => (
            <div className={shellStyles.panelScroll}>
              <form onSubmit={handleSubmit}>
                <div className={shellStyles.headerRow}>
                  <Heading slot="title" className={shellStyles.heading}>
                    Nuevo mantenimiento
                  </Heading>
                  <button type="button" className={shellStyles.closeButton} onClick={close} aria-label="Cerrar">
                    ×
                  </button>
                </div>

                {error && <p className={shellStyles.formError} role="alert">{error}</p>}

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>¿Qué necesita mantenimiento?</span>
                  <input
                    className={shellStyles.textInput}
                    value={item}
                    onChange={(event) => setItem(event.target.value)}
                    placeholder="Ej. Filtro de agua, Aire acondicionado, Auto…"
                    required
                    autoFocus
                  />
                </label>

                <div className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>¿Cada cuánto?</span>
                  <div
                    className={styles.intervalGrid}
                    role="radiogroup"
                    aria-label="Intervalo de mantenimiento"
                    onKeyDown={handleRadiogroupKeyDown}
                  >
                    {QUICK_INTERVALS.map((interval, index) => (
                      <button
                        key={interval.id}
                        type="button"
                        role="radio"
                        aria-checked={selectedInterval === interval.id}
                        tabIndex={radioTabIndex(selectedInterval === interval.id, index === 0, selectedInterval !== null)}
                        className={`${styles.intervalButton} ${selectedInterval === interval.id ? styles.intervalButtonActive : ''}`}
                        onClick={() => pickInterval(interval.id, interval.months)}
                      >
                        {interval.label}
                      </button>
                    ))}
                  </div>
                </div>

                <DatePicker
                    label="Próxima fecha"
                    value={nextDueAt}
                    onChange={(next) => {
                      setNextDueAt(next)
                      setSelectedInterval(null)
                    }}
                    isRequired
                  />

                <div className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Compartir con tu familia</span>
                  <ShareWithFamily
                    type="MAINTENANCE"
                    resourceId={null}
                    value={sharing.shares}
                    onChange={sharing.setShares}
                  />
                </div>

                <div className={shellStyles.formActions}>
                  {saving && <span className={shellStyles.savingHint}>Guardando…</span>}
                  <button type="button" data-variant="secondary" onClick={close} disabled={saving}>
                    Cancelar
                  </button>
                  <button type="submit" disabled={saving || !item.trim() || !nextDueAt}>
                    Guardar
                  </button>
                </div>
              </form>
            </div>
          )}
        </MotionDialog>
      </Modal>
    </DialogTrigger>
  )
}
