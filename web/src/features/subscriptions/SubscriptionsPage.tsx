import { useEffect, useMemo, useState } from 'react'
import { Button } from 'react-aria-components'
import { AppShell } from '../../core/ui/layout/AppShell'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import { useActiveMode } from '../../core/user/ActiveModeContext'
import {
  BILLING_CYCLE_LABELS,
  PAYMENT_KIND_LABELS,
  PAYMENT_KIND_MARKS,
  deleteSubscription,
  listPaymentRecords,
  listSubscriptions,
  markPaymentPaid,
  undoLastPayment,
  updateSubscription,
  type PaymentRecord,
  type PaymentKind,
  type Subscription,
} from './api'
import {
  DUE_SOON_DAYS,
  cardIsAfterStatement,
  daysUntil,
  cardStatementDate,
  formatAmount,
  groupPayments,
  isPaidThisPeriod,
  overdueCycles,
  paymentState,
  showsAmount,
  summarize,
  toLocalDate,
  whenLabel,
} from './paymentsView'
import { SubscriptionDialog } from './SubscriptionDialog'
import styles from './SubscriptionsPage.module.css'

/**
 * ADR-020: "Pagos" — compromisos de pago de cualquier tipo.
 *
 * Sustituye a la lista plana de suscripciones. La pantalla responde una
 * sola pregunta —qué tengo que pagar, cuánto es y cuándo— y por eso tiene
 * tres bloques en orden de urgencia: resumen, filtros y lista agrupada por
 * cercanía. Sin pestañas: cambiar de vista para ver el siguiente pago sería
 * fricción sobre la pregunta más frecuente.
 *
 * Etapa 3 del plan aprobado. "Marcar como pagado" llega en la etapa 4; hasta
 * entonces `isPaid` es siempre falso y el grupo "Pagado" no aparece.
 */

type Filter = 'all' | 'pending' | 'paid' | PaymentKind

/**
 * INCOHERENCIA CORREGIDA: "Próximos", "Todos" y "Pendientes" devolvían
 * exactamente lo mismo, porque el estado "pagado" nunca se alcanzaba. Con
 * el estado ya funcionando, cada uno filtra algo distinto y "Próximos"
 * sobra: la lista ya viene ordenada por cercanía y agrupada.
 */
const FILTERS: Array<{ id: Filter; label: string }> = [
  { id: 'all', label: 'Todos' },
  { id: 'pending', label: 'Pendientes' },
  { id: 'paid', label: 'Pagados' },
  { id: 'SUBSCRIPTION', label: 'Suscripciones' },
  { id: 'CARD', label: 'Tarjetas' },
  { id: 'SERVICE', label: 'Servicios' },
  { id: 'CREDIT', label: 'Créditos' },
]

export function SubscriptionsPage() {
  // ADR-019: el recurso nace en el módulo desde el que se crea, y la lista
  // solo pide los de ese módulo.
  const activeMode = useActiveMode()
  const [payments, setPayments] = useState<Subscription[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filter, setFilter] = useState<Filter>('all')
  // ADR-020(f): qué ciclos ya se pagaron. Se piden en bloque, no por fila.
  const [records, setRecords] = useState<PaymentRecord[]>([])
  const [busyId, setBusyId] = useState<string | null>(null)

  async function refresh() {
    setLoading(true)
    setError(null)
    try {
      const [page, paid] = await Promise.all([listSubscriptions(activeMode), listPaymentRecords()])
      setPayments(page.items)
      setRecords(paid)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar tus pagos.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeMode])

  function handleSaved(saved: Subscription) {
    setPayments((current) => {
      const exists = current.some((payment) => payment.id === saved.id)
      return exists ? current.map((p) => (p.id === saved.id ? saved : p)) : [saved, ...current]
    })
  }

  async function handleDelete(id: string) {
    await deleteSubscription(id)
    setPayments((current) => current.filter((payment) => payment.id !== id))
  }

  /**
   * Un compromiso está "pagado" cuando existe un registro de un ciclo del
   * PERIODO EN CURSO — no cuando el ciclo futuro esté pagado, que era la
   * comprobación anterior y nunca se cumplía. Ver `isPaidThisPeriod`.
   */
  const paidPeriods = useMemo(() => {
    const map = new Map<string, PaymentRecord[]>()
    for (const record of records) {
      const list = map.get(record.subscriptionId) ?? []
      list.push(record)
      map.set(record.subscriptionId, list)
    }
    return map
  }, [records])

  const isPaid = (payment: Subscription) => isPaidThisPeriod(paidPeriods.get(payment.id) ?? [])

  async function handleMarkPaid(payment: Subscription) {
    setBusyId(payment.id)
    setError(null)
    try {
      const updated = await markPaymentPaid(payment.id)
      setPayments((current) => current.map((p) => (p.id === updated.id ? updated : p)))
      setRecords(await listPaymentRecords())
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo marcar el pago.')
    } finally {
      setBusyId(null)
    }
  }

  /**
   * INCOHERENCIA CORREGIDA (2026-08-29): esto llamaba a `markPaymentPaid`,
   * o sea que "anotar cuánto debo" registraba el ciclo como PAGADO y
   * avanzaba la fecha. Anotar un importe no es pagarlo.
   *
   * Ahora guarda el importe en el propio compromiso, que es donde vive "lo
   * que hay que pagar ahora". Marcar como pagado sigue siendo un gesto
   * aparte, y entonces sí registra ese importe como el realmente pagado.
   */
  async function handleCaptureCardAmount(payment: Subscription) {
    const raw = window.prompt(`¿Cuánto debes pagar de ${payment.service}?`, '')
    if (raw === null) return
    const amount = Number(raw.replace(/[^0-9.]/g, ''))
    if (!Number.isFinite(amount) || amount <= 0) return

    setBusyId(payment.id)
    setError(null)
    try {
      const updated = await updateSubscription(
        payment.id,
        {
          service: payment.service,
          nextPaymentDate: payment.nextPaymentDate,
          billingCycle: payment.billingCycle,
          kind: payment.kind,
          amount,
          currency: payment.currency || 'MXN',
          // El importe de este ciclo ya se conoce: deja de ser incógnita
          // hasta el siguiente corte, que volverá a marcarlo variable.
          variableAmount: false,
        },
        payment.version,
      )
      setPayments((current) => current.map((p) => (p.id === updated.id ? updated : p)))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo anotar el importe.')
    } finally {
      setBusyId(null)
    }
  }

  async function handleUndoPaid(payment: Subscription) {
    setBusyId(payment.id)
    setError(null)
    try {
      const updated = await undoLastPayment(payment.id)
      setPayments((current) => current.map((p) => (p.id === updated.id ? updated : p)))
      setRecords(await listPaymentRecords())
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo deshacer el pago.')
    } finally {
      setBusyId(null)
    }
  }

  const summary = useMemo(() => summarize(payments, isPaid), [payments, paidPeriods])

  const filtered = useMemo(() => {
    if (filter === 'all') return payments
    if (filter === 'pending') {
      return payments.filter((payment) => paymentState(payment, isPaid(payment)) !== 'paid')
    }
    if (filter === 'paid') return payments.filter((payment) => isPaid(payment))
    return payments.filter((payment) => payment.kind === filter)
  }, [payments, filter, paidPeriods])

  const groups = useMemo(() => groupPayments(filtered, isPaid), [filtered, paidPeriods])

  return (
    <AppShell title="Pagos" subtitle="Lo que pagas cada mes, y cuándo."
      actions={<><SubscriptionDialog
          onSaved={handleSaved}
          trigger={<Button className={styles.primaryButton}>+ Agregar pago</Button>}
        /></>}
    >
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      {!loading && payments.length > 0 && (
        <>
          {/* Resumen: tres datos, no un tablero financiero. */}
          <section className={styles.summary}>
            <div className={`${styles.summaryCard} ${styles.summaryLead}`}>
              <span className={styles.summaryLabel}>Lo siguiente</span>
              {summary.next ? (
                <>
                  <p className={styles.summaryValue}>
                    {summary.next.service}
                    {showsAmount(summary.next) && (
                      <> · {formatAmount(summary.next.amount, summary.next.currency)}</>
                    )}
                  </p>
                  <p className={styles.summaryMeta}>
                    {toLocalDate(summary.next.nextPaymentDate).toLocaleDateString('es-MX', {
                      weekday: 'long',
                      day: 'numeric',
                      month: 'long',
                      // El año solo si no es el de hoy: sin esto "lunes 25
                      // de octubre" era idéntico para 2026 y 2027.
                      ...(toLocalDate(summary.next.nextPaymentDate).getFullYear() !==
                      new Date().getFullYear()
                        ? { year: 'numeric' as const }
                        : {}),
                    })}
                    {/* Lejos en el tiempo `whenLabel` devuelve la fecha
                        corta, que junto a la larga de arriba sería repetir
                        el mismo dato con dos formatos. */}
                    {daysUntil(summary.next.nextPaymentDate) <= DUE_SOON_DAYS &&
                      ` · ${whenLabel(summary.next.nextPaymentDate)}`}
                  </p>
                </>
              ) : (
                <p className={styles.summaryValue}>Nada pendiente</p>
              )}
            </div>

            {/* La cifra grande es LO QUE CUESTA EL MES —pagado y por
                pagar— y la línea de abajo dice cuánto falta. Antes la
                tarjeta enseñaba un importe suelto con el pie "Todos con
                importe", que no explicaba si era el total, lo pendiente o
                qué: el usuario lo reportó tal cual ("no veo sentido, ¿es
                faltante por pagar, total del mes o qué?"). */}
            <div className={styles.summaryCard}>
              <span className={styles.summaryLabel}>Total del mes</span>
              <p className={styles.summaryValue}>
                {summary.periodTotals.length === 0
                  ? '—'
                  : summary.periodTotals
                      .map((total) => formatAmount(total.total, total.currency))
                      .join(' · ')}
              </p>
              <p className={styles.summaryMeta}>
                {summary.periodTotals.length === 0 && summary.unknownAmountCount === 0
                  ? 'Nada por pagar este mes'
                  : summary.pendingTotals.length > 0
                    ? `Te faltan ${summary.pendingTotals
                        .map((total) => formatAmount(total.total, total.currency))
                        .join(' · ')}`
                    : 'Todo pagado'}
                {summary.unknownAmountCount > 0 &&
                  ` · ${summary.unknownAmountCount} sin importe conocido`}
              </p>
            </div>

            <div className={styles.summaryCard}>
              <span className={styles.summaryLabel}>Pendientes</span>
              <p className={styles.summaryValue}>{summary.pendingCount}</p>
              <p className={styles.summaryMeta}>
                {summary.overdueCount > 0 ? `${summary.overdueCount} vencido(s)` : 'Ninguno vencido'}
              </p>
            </div>
          </section>

          <div className={styles.filters} role="group" aria-label="Filtrar pagos">
            {FILTERS.map((option) => (
              <button
                key={option.id}
                type="button"
                className={styles.chip}
                data-on={filter === option.id ? 'true' : undefined}
                aria-pressed={filter === option.id}
                onClick={() => setFilter(option.id)}
              >
                {option.label}
              </button>
            ))}
          </div>
        </>
      )}

      {loading && <p className={styles.hint}>Cargando…</p>}

      {/* `!error` es imprescindible: si la carga falla, `payments` queda
          vacío y el vacío afirmaba "Aún no tienes pagos registrados" —
          una mentira sobre los datos del usuario, que sí los tiene. Cuando
          hay error manda el aviso de error, que es lo cierto. */}
      {!loading && !error && payments.length === 0 && (
        <div className={styles.empty}>
          <h3>Aún no tienes pagos registrados</h3>
          <p>
            Agrega el primero y verás cuánto suman tus compromisos del mes y cuál toca antes.
          </p>
          <SubscriptionDialog
            onSaved={handleSaved}
            trigger={<Button className={styles.primaryButton}>+ Agregar pago</Button>}
          />
        </div>
      )}

      {!loading &&
        groups.map((group) => (
          <section key={group.id} className={styles.group}>
            <h3 className={styles.groupLabel}>{group.label}</h3>

            <div className={styles.rows}>
              {group.items.map((payment) => {
                const state = paymentState(payment, isPaid(payment))
                return (
                  <article
                    key={payment.id}
                    className={styles.row}
                    data-kind={payment.kind}
                    data-state={state}
                  >
                    <span className={styles.rowMark} aria-hidden="true">
                      {PAYMENT_KIND_MARKS[payment.kind] ?? 'PAG'}
                    </span>

                    <div className={styles.rowBody}>
                      <p className={styles.rowName}>
                        {payment.service}
                        {payment.variableAmount && (
                          <span className={styles.tag} data-t="var">
                            Variable
                          </span>
                        )}
                        {state === 'overdue' && (
                          <span className={styles.tag} data-t="due">
                            {overdueCycles(payment) > 1
                              ? `${overdueCycles(payment)} ciclos sin pagar`
                              : 'Vencido'}
                          </span>
                        )}
                        {state === 'paid' && (
                          <span className={styles.tag} data-t="paid">
                            Pagado
                          </span>
                        )}
                        {state === 'finished' && (
                          <span className={styles.tag} data-t="paid">
                            Terminado
                          </span>
                        )}
                      </p>

                      <p className={styles.rowMeta}>
                        {PAYMENT_KIND_LABELS[payment.kind] ?? 'Pago'}
                        {' · '}
                        {BILLING_CYCLE_LABELS[payment.billingCycle]}
                        {payment.kind === 'CARD' && cardStatementDate(payment) && (
                          <>
                            {' · Corte '}
                            {cardStatementDate(payment)!.toLocaleDateString('es-MX', {
                              day: 'numeric',
                              month: 'short',
                            })}
                            {' · Límite '}
                            {toLocalDate(payment.nextPaymentDate).toLocaleDateString('es-MX', {
                              day: 'numeric',
                              month: 'short',
                            })}
                          </>
                        )}
                        {payment.kind === 'CREDIT' && payment.totalInstallments && (
                          <>
                            {' '}
                            · Mensualidad {payment.currentInstallment ?? 1} de{' '}
                            {payment.totalInstallments}
                          </>
                        )}
                        {payment.lastFour && <> · ···· {payment.lastFour}</>}
                        {payment.paymentMethod && <> · {payment.paymentMethod}</>}
                      </p>
                    </div>

                    <div className={styles.rowRight}>
                      <span className={styles.rowAmount}>
                        {showsAmount(payment) ? formatAmount(payment.amount, payment.currency) : '—'}
                      </span>
                      <span className={styles.rowWhen}>{whenLabel(payment.nextPaymentDate)}</span>
                    </div>

                    <div className={styles.rowActions}>
                      {/* La acción central de la sección: sin ella la lista
                          miente al día siguiente. */}
                      <button
                        type="button"
                        className={styles.payButton}
                        disabled={busyId === payment.id}
                        aria-label={
                          state === 'paid'
                            ? `Deshacer el pago de ${payment.service}`
                            : `Marcar ${payment.service} como pagado`
                        }
                        onClick={() =>
                          state === 'paid' ? void handleUndoPaid(payment) : void handleMarkPaid(payment)
                        }
                      >
                        {state === 'paid' ? 'Deshacer' : 'Pagado'}
                      </button>

                      {/* El importe de una tarjeta solo se conoce tras el
                          corte: es entonces cuando se pide, no antes. */}
                      {payment.kind === 'CARD' &&
                        state !== 'paid' &&
                        payment.variableAmount &&
                        cardIsAfterStatement(payment) && (
                          <button
                            type="button"
                            className={styles.payButton}
                            disabled={busyId === payment.id}
                            onClick={() => void handleCaptureCardAmount(payment)}
                          >
                            Anotar importe
                          </button>
                        )}

                      <SubscriptionDialog
                        subscription={payment}
                        onSaved={handleSaved}
                        trigger={
                          <Button className={styles.iconButton} aria-label={`Editar ${payment.service}`}>
                            ✎
                          </Button>
                        }
                      />
                      <SimpleDeleteConfirm
                        resourceLabel="pago"
                        itemName={payment.service}
                        ariaLabel={`Eliminar ${payment.service}`}
                        onConfirm={() => handleDelete(payment.id)}
                      />
                    </div>
                  </article>
                )
              })}
            </div>
          </section>
        ))}

    </AppShell>
  )
}
