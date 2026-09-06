import { useState, type FormEvent, type ReactNode } from 'react'
import { Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../core/motion/tokens'
import { handleRadiogroupKeyDown, radioTabIndex } from '../../core/ui/keyboard/radiogroupKeyboard'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import {
  BILLING_CYCLES,
  BILLING_CYCLE_LABELS,
  CURRENCIES,
  DEFAULT_CURRENCY,
  PAYMENT_KINDS,
  PAYMENT_KIND_LABELS,
  createSubscription,
  updateSubscription,
  type BillingCycle,
  type PaymentInput,
  type PaymentKind,
  type Subscription,
} from './api'
import { PAYMENT_TEMPLATES, type PaymentTemplate } from './paymentTemplates'
import { PaymentHistory } from './PaymentHistory'
import { ShareWithFamily } from '../sharing/ShareWithFamily'
import { useResourceSharing } from '../sharing/useResourceSharing'
import styles from './SubscriptionDialog.module.css'
import { useActiveMode } from '../../core/user/ActiveModeContext'
import { DatePicker } from '../../core/ui/pickers/DatePicker'

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
  // ADR-020: tipo, importe y divisa por pago.
  const [kind, setKind] = useState<PaymentKind>(subscription?.kind ?? 'SUBSCRIPTION')
  const [amount, setAmount] = useState(subscription?.amount != null ? String(subscription.amount) : '')
  const [currency, setCurrency] = useState(subscription?.currency ?? DEFAULT_CURRENCY)
  const [paymentMethod, setPaymentMethod] = useState(subscription?.paymentMethod ?? '')
  const [notes, setNotes] = useState(subscription?.notes ?? '')
  const [variableAmount, setVariableAmount] = useState(subscription?.variableAmount ?? false)
  const [statementDay, setStatementDay] = useState(subscription?.statementDay != null ? String(subscription.statementDay) : '')
  const [dueDay, setDueDay] = useState(subscription?.dueDay != null ? String(subscription.dueDay) : '')
  const [institution, setInstitution] = useState(subscription?.institution ?? '')
  const [lastFour, setLastFour] = useState(subscription?.lastFour ?? '')
  const [totalInstallments, setTotalInstallments] = useState(subscription?.totalInstallments != null ? String(subscription.totalInstallments) : '')
  const [currentInstallment, setCurrentInstallment] = useState(subscription?.currentInstallment != null ? String(subscription.currentInstallment) : '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  // ADR-025 §2/§5: "hacer mi parte" de un pago es pagarlo.
  const sharing = useResourceSharing('SUBSCRIPTION')

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (open) {
      setService(subscription?.service ?? '')
      setCompany(subscription?.company ?? '')
      setPlan(subscription?.plan ?? '')
      setNextPaymentDate(subscription ? toDateInputValue(subscription.nextPaymentDate) : '')
      setBillingCycle(subscription?.billingCycle ?? 'MONTHLY')
      setKind(subscription?.kind ?? 'SUBSCRIPTION')
      setAmount(subscription?.amount != null ? String(subscription.amount) : '')
      setCurrency(subscription?.currency ?? DEFAULT_CURRENCY)
      setPaymentMethod(subscription?.paymentMethod ?? '')
      setNotes(subscription?.notes ?? '')
      setVariableAmount(subscription?.variableAmount ?? false)
      setStatementDay(subscription?.statementDay != null ? String(subscription.statementDay) : '')
      setDueDay(subscription?.dueDay != null ? String(subscription.dueDay) : '')
      setInstitution(subscription?.institution ?? '')
      setLastFour(subscription?.lastFour ?? '')
      setTotalInstallments(subscription?.totalInstallments != null ? String(subscription.totalInstallments) : '')
      setCurrentInstallment(subscription?.currentInstallment != null ? String(subscription.currentInstallment) : '')
      sharing.reset()
      setError(null)
    }
  }

  /** Solo prellena: el usuario puede cambiar cualquier campo después. */
  function applyTemplate(template: PaymentTemplate) {
    setKind(template.kind)
    setBillingCycle(template.billingCycle)
    if (template.service) setService(template.service)
    if (template.kind === 'CARD') setVariableAmount(true)
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!service.trim() || !nextPaymentDate || saving) return
    setSaving(true)
    setError(null)
    try {
      const isoDate = new Date(nextPaymentDate).toISOString()
      const numeric = (raw: string) => (raw.trim() === '' ? undefined : Number(raw))
      const input: PaymentInput = {
        service: service.trim(),
        company: company.trim() || undefined,
        plan: plan.trim() || undefined,
        nextPaymentDate: isoDate,
        billingCycle,
        kind,
        // Una tarjeta no conoce su importe hasta el corte: se guarda como
        // variable y sin cifra, nunca como cero.
        amount: kind === 'CARD' || variableAmount ? undefined : numeric(amount),
        currency: kind === 'CARD' || variableAmount ? undefined : currency,
        paymentMethod: paymentMethod.trim() || undefined,
        notes: notes.trim() || undefined,
        variableAmount: kind === 'CARD' ? true : variableAmount,
        statementDay: kind === 'CARD' ? numeric(statementDay) : undefined,
        dueDay: kind === 'CARD' ? numeric(dueDay) : undefined,
        institution: kind === 'CARD' ? institution.trim() || undefined : undefined,
        lastFour: lastFour.trim() || undefined,
        totalInstallments: kind === 'CREDIT' ? numeric(totalInstallments) : undefined,
        currentInstallment: kind === 'CREDIT' ? numeric(currentInstallment) : undefined,
      }
      const saved =
        isEdit && subscription
          ? await updateSubscription(subscription.id, input, subscription.version)
          : await createSubscription(input, activeMode)
      onSaved(saved)

      const shareError = await sharing.commit(saved.id)
      if (shareError) {
        setError(shareError)
        return
      }
      setIsOpen(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar el pago.')
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
                    {isEdit ? 'Editar pago' : 'Agregar pago'}
                  </Heading>
                  <button type="button" className={shellStyles.closeButton} onClick={close} aria-label="Cerrar">
                    ×
                  </button>
                </div>

                {error && <p className={shellStyles.formError} role="alert">{error}</p>}

                {!isEdit && (
                  <div className={shellStyles.field}>
                    <span className={shellStyles.fieldLabel}>Empieza por una plantilla (opcional)</span>
                    <div className={styles.templateRow}>
                      {PAYMENT_TEMPLATES.map((template) => (
                        <button
                          key={template.id}
                          type="button"
                          className={styles.templateChip}
                          onClick={() => applyTemplate(template)}
                        >
                          {template.label}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {/* ADR-020: el tipo va primero porque decide qué campos
                    tienen sentido debajo. Un formulario que enseña "día de
                    corte" a quien registra Spotify es un formulario que se
                    abandona. */}
                <div className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Tipo de pago</span>
                  <div className={styles.kindGrid} role="radiogroup" aria-label="Tipo de pago">
                    {PAYMENT_KINDS.map((option) => (
                      <button
                        key={option}
                        type="button"
                        role="radio"
                        aria-checked={kind === option}
                        className={`${styles.cycleButton} ${kind === option ? styles.cycleButtonActive : ''}`}
                        onClick={() => setKind(option)}
                      >
                        {PAYMENT_KIND_LABELS[option]}
                      </button>
                    ))}
                  </div>
                </div>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Nombre</span>
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

                {/* Una tarjeta no conoce su importe hasta el corte: el campo
                    no se muestra en vez de pedir una cifra inventada. */}
                {kind !== 'CARD' && (
                  <div className={styles.amountRow}>
                    <label className={shellStyles.field}>
                      <span className={shellStyles.fieldLabel}>Importe (opcional)</span>
                      <input
                        className={shellStyles.textInput}
                        type="number"
                        inputMode="decimal"
                        step="0.01"
                        min="0"
                        value={amount}
                        onChange={(event) => setAmount(event.target.value)}
                        disabled={variableAmount}
                        placeholder={variableAmount ? 'Variable' : '0.00'}
                      />
                    </label>

                    <label className={shellStyles.field}>
                      <span className={shellStyles.fieldLabel}>Divisa</span>
                      <select
                        className={shellStyles.textInput}
                        value={currency}
                        onChange={(event) => setCurrency(event.target.value)}
                        disabled={variableAmount}
                      >
                        {CURRENCIES.map((code) => (
                          <option key={code} value={code}>
                            {code}
                          </option>
                        ))}
                      </select>
                    </label>
                  </div>
                )}

                {kind !== 'CARD' && (
                  <label className={styles.checkboxRow}>
                    <input
                      type="checkbox"
                      checked={variableAmount}
                      onChange={(event) => setVariableAmount(event.target.checked)}
                    />
                    <span>El importe cambia cada vez (luz, agua…)</span>
                  </label>
                )}

                {kind === 'CARD' && (
                  <>
                    <div className={styles.amountRow}>
                      <label className={shellStyles.field}>
                        <span className={shellStyles.fieldLabel}>Día de corte</span>
                        <input
                          className={shellStyles.textInput}
                          type="number"
                          min="1"
                          max="31"
                          value={statementDay}
                          onChange={(event) => setStatementDay(event.target.value)}
                          required
                        />
                      </label>

                      <label className={shellStyles.field}>
                        <span className={shellStyles.fieldLabel}>Día límite de pago</span>
                        <input
                          className={shellStyles.textInput}
                          type="number"
                          min="1"
                          max="31"
                          value={dueDay}
                          onChange={(event) => setDueDay(event.target.value)}
                          required
                        />
                      </label>
                    </div>

                    <label className={shellStyles.field}>
                      <span className={shellStyles.fieldLabel}>Institución (opcional)</span>
                      <input
                        className={shellStyles.textInput}
                        value={institution}
                        onChange={(event) => setInstitution(event.target.value)}
                        placeholder="BBVA, Banorte…"
                      />
                    </label>
                  </>
                )}

                {kind === 'CREDIT' && (
                  <div className={styles.amountRow}>
                    <label className={shellStyles.field}>
                      <span className={shellStyles.fieldLabel}>Plazos totales (opcional)</span>
                      <input
                        className={shellStyles.textInput}
                        type="number"
                        min="1"
                        max="600"
                        value={totalInstallments}
                        onChange={(event) => setTotalInstallments(event.target.value)}
                      />
                    </label>

                    <label className={shellStyles.field}>
                      <span className={shellStyles.fieldLabel}>Plazo actual (opcional)</span>
                      <input
                        className={shellStyles.textInput}
                        type="number"
                        min="0"
                        max="600"
                        value={currentInstallment}
                        onChange={(event) => setCurrentInstallment(event.target.value)}
                      />
                    </label>
                  </div>
                )}

                {/* Etiqueta para reconocer la tarjeta con la que se paga.
                    NO es un dato bancario: nunca se pide el número
                    completo, ni CVV, ni titular. */}
                <div className={styles.amountRow}>
                  <label className={shellStyles.field}>
                    <span className={shellStyles.fieldLabel}>Método de pago (opcional)</span>
                    <input
                      className={shellStyles.textInput}
                      value={paymentMethod}
                      onChange={(event) => setPaymentMethod(event.target.value)}
                      placeholder="Domiciliado, transferencia…"
                    />
                  </label>

                  <label className={shellStyles.field}>
                    <span className={shellStyles.fieldLabel}>Últimos 4 dígitos (opcional)</span>
                    <input
                      className={shellStyles.textInput}
                      inputMode="numeric"
                      maxLength={4}
                      pattern="[0-9]{4}"
                      value={lastFour}
                      onChange={(event) => setLastFour(event.target.value.replace(/\D/g, ''))}
                      placeholder="4521"
                    />
                  </label>
                </div>

                {isEdit && subscription && <PaymentHistory subscriptionId={subscription.id} />}

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Notas (opcional)</span>
                  <textarea
                    className={shellStyles.textArea}
                    rows={2}
                    value={notes}
                    onChange={(event) => setNotes(event.target.value)}
                  />
                </label>

                <DatePicker
                    label="¿Qué día se tiene que pagar?"
                    value={nextPaymentDate}
                    onChange={(next) => setNextPaymentDate(next)}
                    isRequired
                  />

                <div className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Compartir con tu familia</span>
                  <ShareWithFamily
                    type="SUBSCRIPTION"
                    resourceId={subscription?.id ?? null}
                    value={sharing.shares}
                    onChange={sharing.setShares}
                  />
                </div>

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
