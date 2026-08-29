import { useState, type FormEvent, type ReactNode } from 'react'
import { Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../core/motion/tokens'
import { handleRadiogroupKeyDown, radioTabIndex } from '../../core/ui/keyboard/radiogroupKeyboard'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import {
  BILLING_CYCLES,
  BILLING_CYCLE_LABELS,
  createSubscription,
  updateSubscription,
  type BillingCycle,
  type Subscription,
} from './api'
import styles from './SubscriptionDialog.module.css'
import { useActiveMode } from '../../core/user/ActiveModeContext'

const MotionDialog = motion.create(Dialog)

interface SubscriptionDialogProps {
  /** Presente = editar; ausente = crear — un solo componente para ambos
      casos, mismo patrón que inventory/InventoryItemDialog.tsx. */
  subscription?: Subscription
  trigger: ReactNode
  onSaved: (subscription: Subscription) => void
}

function toDateInputValue(iso: string): string {
  return iso.slice(0, 10)
}

/** Pedido explícito del usuario (2026-08-22): "el modal registrará
    Servicio, compañía, plan contratado, qué día se tiene que pagar, si los
    pagos son mensuales semanales o anuales." Sin campo de precio/monto —
    el usuario no lo pidió en esta especificación y CLAUDE.md excluye
    Finanzas de V1 (ver V15__subscriptions.sql). */
export function SubscriptionDialog({ subscription, trigger, onSaved }: SubscriptionDialogProps) {
  // ADR-019: el recurso nace en el módulo desde el que se crea, y la
  // lista solo pide los de ese módulo. Fuera de /personal y /laboral
  // `activeMode` es null: se devuelve todo y las altas nacen PERSONAL.
  const activeMode = useActiveMode()
  const isEdit = subscription !== undefined
  const [isOpen, setIsOpen] = useState(false)
  const [service, setService] = useState(subscription?.service ?? '')
  const [company, setCompany] = useState(subscription?.company ?? '')
  const [plan, setPlan] = useState(subscription?.plan ?? '')
  const [nextPaymentDate, setNextPaymentDate] = useState(subscription ? toDateInputValue(subscription.nextPaymentDate) : '')
  const [billingCycle, setBillingCycle] = useState<BillingCycle>(subscription?.billingCycle ?? 'MONTHLY')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (open) {
      setService(subscription?.service ?? '')
      setCompany(subscription?.company ?? '')
      setPlan(subscription?.plan ?? '')
      setNextPaymentDate(subscription ? toDateInputValue(subscription.nextPaymentDate) : '')
      setBillingCycle(subscription?.billingCycle ?? 'MONTHLY')
      setError(null)
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!service.trim() || !nextPaymentDate || saving) return
    setSaving(true)
    setError(null)
    try {
      const isoDate = new Date(nextPaymentDate).toISOString()
      const saved =
        isEdit && subscription
          ? await updateSubscription(subscription.id, service.trim(), company.trim(), plan.trim(), isoDate, billingCycle, subscription.version)
          : await createSubscription(service.trim(), company.trim(), plan.trim(), isoDate, billingCycle, activeMode)
      onSaved(saved)
      setIsOpen(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar la suscripción.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      {trigger}
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
                    {isEdit ? 'Editar suscripción' : 'Nueva suscripción'}
                  </Heading>
                  <button type="button" className={shellStyles.closeButton} onClick={close} aria-label="Cerrar">
                    ×
                  </button>
                </div>

                {error && <p className={shellStyles.formError} role="alert">{error}</p>}

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Servicio</span>
                  <input
                    className={shellStyles.textInput}
                    value={service}
                    onChange={(event) => setService(event.target.value)}
                    placeholder="Ej. Netflix"
                    required
                    autoFocus
                  />
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Compañía (opcional)</span>
                  <input
                    className={shellStyles.textInput}
                    value={company}
                    onChange={(event) => setCompany(event.target.value)}
                    placeholder="Ej. Netflix Inc."
                  />
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Plan contratado (opcional)</span>
                  <input
                    className={shellStyles.textInput}
                    value={plan}
                    onChange={(event) => setPlan(event.target.value)}
                    placeholder="Ej. Premium 4K"
                  />
                </label>

                <div className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>¿Los pagos son…?</span>
                  <div
                    className={styles.cycleGrid}
                    role="radiogroup"
                    aria-label="Periodicidad de pago"
                    onKeyDown={handleRadiogroupKeyDown}
                  >
                    {BILLING_CYCLES.map((cycle, index) => (
                      <button
                        key={cycle}
                        type="button"
                        role="radio"
                        aria-checked={billingCycle === cycle}
                        tabIndex={radioTabIndex(billingCycle === cycle, index === 0, true)}
                        className={`${styles.cycleButton} ${billingCycle === cycle ? styles.cycleButtonActive : ''}`}
                        onClick={() => setBillingCycle(cycle)}
                      >
                        {BILLING_CYCLE_LABELS[cycle]}
                      </button>
                    ))}
                  </div>
                </div>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>¿Qué día se tiene que pagar?</span>
                  <input
                    type="date"
                    className={shellStyles.textInput}
                    value={nextPaymentDate}
                    onChange={(event) => setNextPaymentDate(event.target.value)}
                    required
                  />
                </label>

                <div className={shellStyles.formActions}>
                  {saving && <span className={shellStyles.savingHint}>Guardando…</span>}
                  <button type="button" data-variant="secondary" onClick={close} disabled={saving}>
                    Cancelar
                  </button>
                  <button type="submit" disabled={saving || !service.trim() || !nextPaymentDate}>
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
