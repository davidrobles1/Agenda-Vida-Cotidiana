import type { PaymentKind, Subscription } from './api'

/**
 * ADR-020: la lógica de presentación de la sección "Pagos", separada de la
 * pantalla y sin dependencias de React.
 *
 * Vive aparte por dos motivos concretos: el resumen y la agrupación son
 * reglas de producto que conviene poder leer de un tirón, y así la pantalla
 * queda dedicada a maquetar.
 *
 * Lo que este archivo NO hace, a propósito: convertir divisas. Si el
 * usuario tiene pagos en MXN y en USD, se suman por separado y se muestran
 * por separado. Una conversión necesitaría un tipo de cambio, y un tipo de
 * cambio es un dato financiero que esta sección tiene prohibido manejar
 * (ADR-020(b)).
 */

export type PaymentState = 'paid' | 'finished' | 'overdue' | 'due-soon' | 'scheduled'

/**
 * INCOHERENCIA CORREGIDA (2026-08-29). El estado "pagado" era inalcanzable.
 *
 * Marcar un pago AVANZA `nextPaymentDate` al siguiente ciclo, y la
 * comprobación anterior buscaba un registro para esa fecha ya avanzada —
 * que por definición no existe todavía. Resultado: el grupo "Pagado", el
 * botón "Deshacer" y el estado entero eran código muerto, y el usuario
 * marcaba un pago sin ver ninguna señal de que hubiera ocurrido.
 *
 * La regla correcta no mira la fecha futura sino el pasado: un compromiso
 * está "pagado" cuando existe un registro para un ciclo del PERIODO EN
 * CURSO. Eso es además lo que el usuario quiere saber al mirar la
 * pantalla — "¿qué ya cubrí este mes?"—, no si el ciclo de dentro de 30
 * días está pagado.
 */
export function isPaidThisPeriod(
  records: Array<{ periodDate: string; paidOn: string }>,
  today = new Date(),
): boolean {
  const year = today.getFullYear()
  const month = String(today.getMonth() + 1).padStart(2, '0')
  const prefix = `${year}-${month}`
  // Se mira `paidOn` —cuándo lo marcó el usuario— y NO `periodDate` —qué
  // ciclo cubre—. La diferencia importa y se detectó validando con datos
  // reales: al pagar por adelantado un recibo de septiembre estando en
  // agosto, el registro guarda `periodDate = 2026-09` y comparar contra el
  // mes en curso daba "no pagado", con el pago ya hecho y la fecha ya
  // avanzada. "Pagado este mes" significa lo que YO cubrí este mes.
  return records.some((record) => record.paidOn.startsWith(prefix))
}

/**
 * Ciclos que un compromiso arrastra sin pagar. Un pago vencido hace tres
 * meses necesita marcarse tres veces, porque cada marca cubre un ciclo —
 * la interfaz debe decirlo en vez de dejar al usuario descubriéndolo a
 * base de repetir el gesto.
 */
export function overdueCycles(payment: Subscription, today = new Date()): number {
  const days = -daysUntil(payment.nextPaymentDate, today)
  if (days <= 0) return 0
  const cycleDays = payment.billingCycle === 'WEEKLY' ? 7 : payment.billingCycle === 'YEARLY' ? 365 : 30
  return Math.max(1, Math.floor(days / cycleDays) + 1)
}

/**
 * Un crédito a plazos termina. Sin esto la aplicación sigue pidiendo pagos
 * de un crédito ya liquidado, que es una mentira con consecuencias: genera
 * alertas en el calendario y suma a los totales del mes.
 */
export function isFinishedCredit(payment: Subscription): boolean {
  return (
    payment.kind === 'CREDIT' &&
    payment.totalInstallments !== undefined &&
    payment.currentInstallment !== undefined &&
    payment.currentInstallment > payment.totalInstallments
  )
}

/** Días a partir de los cuales un pago deja de considerarse "próximo". */
export const DUE_SOON_DAYS = 7

export function dayKeyOf(iso: string): string {
  return iso.slice(0, 10)
}

/**
 * DEFECTO CORREGIDO (2026-08-29, encontrado validando con un registro
 * real): todas las fechas se mostraban UN DÍA ANTES en zonas al oeste de
 * UTC. Un mantenimiento del 23 de septiembre aparecía como "22 sep", y uno
 * completado hoy 31 de agosto decía "Última vez: 30 ago".
 *
 * Causa: `new Date('2026-08-31')` interpreta la cadena como UTC, y el
 * backend guarda estas fechas a medianoche UTC. Al formatearlas en local
 * (CST = UTC-6) caían en el día anterior.
 *
 * Se toman los 10 primeros caracteres —el día tal como lo eligió el
 * usuario, sirve igual para `YYYY-MM-DD` que para un ISO completo— y se
 * construye el mediodía LOCAL, que ninguna zona horaria puede desplazar de
 * día. Es el mismo criterio que ya usaban el calendario y las notas del día
 * (`new Date(`${key}T12:00:00`)`), y por eso el calendario sí mostraba bien
 * las fechas mientras estas listas no.
 */
export function toLocalDate(iso: string): Date {
  return new Date(`${iso.slice(0, 10)}T12:00:00`)
}

function midnight(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate())
}

export function daysUntil(iso: string, today = new Date()): number {
  return Math.round((midnight(toLocalDate(iso)).getTime() - midnight(today).getTime()) / 86400000)
}

/**
 * Estado de un pago. `paid` no se deduce de la fecha sino de si existe un
 * registro de pago para el ciclo actual — por eso llega como argumento en
 * vez de calcularse aquí.
 */
export function paymentState(payment: Subscription, isPaid: boolean, today = new Date()): PaymentState {
  if (isFinishedCredit(payment)) return 'finished'
  if (isPaid) return 'paid'
  const days = daysUntil(payment.nextPaymentDate, today)
  if (days < 0) return 'overdue'
  if (days <= DUE_SOON_DAYS) return 'due-soon'
  return 'scheduled'
}

/**
 * Texto de cuándo toca. Relativo cerca y absoluto lejos: "en 4 días" se
 * entiende sin pensar; "en 63 días" no dice nada y "12 nov" sí.
 */
export function whenLabel(iso: string, today = new Date()): string {
  const days = daysUntil(iso, today)
  if (days < -1) return `hace ${Math.abs(days)} días`
  if (days === -1) return 'ayer'
  if (days === 0) return 'hoy'
  if (days === 1) return 'mañana'
  if (days <= DUE_SOON_DAYS) return `en ${days} días`
  return absoluteDate(iso, today)
}

/**
 * Fecha absoluta. El año solo aparece cuando NO es el del día de hoy: sin
 * esa condición, un pago para el 25 oct del año que viene se pintaba
 * "25 oct", exactamente igual que uno de este año — la fila decía algo
 * distinto de lo que había en la base de datos. Con pagos anuales o a
 * plazos eso deja de ser un detalle. Mantenimiento ya ponía el año
 * siempre; aquí se omite en el caso corriente porque casi todos los pagos
 * caen dentro del año en curso y repetirlo en cada fila es ruido.
 */
function absoluteDate(iso: string, today: Date): string {
  const date = toLocalDate(iso)
  return date.toLocaleDateString('es-MX', {
    day: 'numeric',
    month: 'short',
    ...(date.getFullYear() === today.getFullYear() ? {} : { year: 'numeric' }),
  })
}

export function formatAmount(amount: number | undefined, currency: string | undefined): string {
  if (amount === undefined || amount === null) return '—'
  try {
    return new Intl.NumberFormat('es-MX', {
      style: 'currency',
      currency: currency || 'MXN',
      maximumFractionDigits: 2,
    }).format(amount)
  } catch {
    // Divisa no reconocida por Intl: se muestra el número con su código en
    // vez de romper la fila.
    return `${amount.toFixed(2)} ${currency ?? ''}`.trim()
  }
}

/** Los cuatro tipos que comparten forma (ADR-020(d)). */
const FIXED_KINDS: PaymentKind[] = ['SUBSCRIPTION', 'SERVICE', 'MEMBERSHIP', 'CUSTOM']

export function isFixedKind(kind: PaymentKind): boolean {
  return FIXED_KINDS.includes(kind)
}

/** Una tarjeta nunca tiene importe conocido de antemano. */
export function showsAmount(payment: Subscription): boolean {
  return !payment.variableAmount && payment.amount !== undefined && payment.amount !== null
}

export interface PaymentGroup {
  id: 'overdue' | 'this-week' | 'later' | 'paid' | 'finished'
  label: string
  items: Subscription[]
}

/**
 * Agrupa por CERCANÍA, no por tipo ni por nombre: la pregunta que trae al
 * usuario a esta pantalla es "qué sigue", y ordenar por urgencia la
 * responde sin que tenga que elegir un criterio.
 */
export function groupPayments(
  payments: Subscription[],
  isPaid: (payment: Subscription) => boolean,
  today = new Date(),
): PaymentGroup[] {
  const groups: Record<PaymentGroup['id'], Subscription[]> = {
    overdue: [],
    'this-week': [],
    later: [],
    paid: [],
    finished: [],
  }

  for (const payment of payments) {
    const state = paymentState(payment, isPaid(payment), today)
    if (state === 'finished') groups.finished.push(payment)
    else if (state === 'paid') groups.paid.push(payment)
    else if (state === 'overdue') groups.overdue.push(payment)
    else if (state === 'due-soon') groups['this-week'].push(payment)
    else groups.later.push(payment)
  }

  const byDate = (a: Subscription, b: Subscription) =>
    a.nextPaymentDate.localeCompare(b.nextPaymentDate)

  return [
    { id: 'overdue' as const, label: 'Vencido', items: groups.overdue.sort(byDate) },
    { id: 'this-week' as const, label: 'Esta semana', items: groups['this-week'].sort(byDate) },
    { id: 'later' as const, label: 'Más adelante', items: groups.later.sort(byDate) },
    { id: 'paid' as const, label: 'Pagado este mes', items: groups.paid.sort(byDate) },
    { id: 'finished' as const, label: 'Terminados', items: groups.finished.sort(byDate) },
  ].filter((group) => group.items.length > 0)
}

export interface PaymentsSummary {
  next: Subscription | undefined
  /** Lo que cuesta el mes ENTERO, pagado y por pagar, por divisa. Nunca se
      suman divisas distintas — ver la cabecera de este archivo. */
  periodTotals: Array<{ currency: string; total: number }>
  /** De ese total, lo que todavía NO está pagado, por divisa. Es el dato
      accionable: el grande dice cuánto cuesta el mes, este cuánto falta. */
  pendingTotals: Array<{ currency: string; total: number }>
  /** Pagos del periodo cuyo importe no se conoce (variables). */
  unknownAmountCount: number
  pendingCount: number
  overdueCount: number
  paidCount: number
}

/**
 * Resumen del periodo = mes natural en curso. Tres datos, no un tablero:
 * qué sigue, cuánto suma el mes y cuántos quedan pendientes.
 */
export function summarize(
  payments: Subscription[],
  isPaid: (payment: Subscription) => boolean,
  today = new Date(),
): PaymentsSummary {
  // CLAVE DEL MES en curso, `YYYY-MM`. Se compara por clave y no con un
  // `Date` de fin de mes: `monthEnd` era el día 30 a las 00:00 mientras
  // `toLocalDate` devuelve el mediodía, así que **todo lo que vencía el
  // último día del mes quedaba fuera del total**. Con tres pagos reales
  // —uno el 1 y dos el 30 de septiembre— el resumen enseñaba solo el
  // primero. Comparar claves de texto no tiene bordes que ajustar.
  const monthKey = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`

  /**
   * Un compromiso cuenta para ESTE MES si su ciclo cae aquí — y eso puede
   * ser por dos vías, no una:
   *
   *  1. su próxima fecha está dentro del mes (sigue por pagar), o
   *  2. ya se pagó dentro del mes.
   *
   * La segunda es imprescindible: marcar un pago AVANZA `nextPaymentDate`
   * al ciclo siguiente (ADR-020), así que mirando solo la fecha, pagar algo
   * lo sacaba del mes y el total del mes ENCOGÍA al pagarlo. El usuario lo
   * describió exactamente así: pagó todo septiembre y el resumen se quedó
   * en una sola cifra suelta que no correspondía a nada.
   */
  const inPeriod = payments.filter((payment) => {
    if (isFinishedCredit(payment)) return false
    if (payment.nextPaymentDate.slice(0, 7) <= monthKey) return true
    return isPaid(payment)
  })

  const totals = new Map<string, number>()
  const pendingByCurrency = new Map<string, number>()
  let unknownAmountCount = 0

  for (const payment of inPeriod) {
    if (!showsAmount(payment)) {
      unknownAmountCount += 1
      continue
    }
    const currency = payment.currency || 'MXN'
    const amount = payment.amount ?? 0
    totals.set(currency, (totals.get(currency) ?? 0) + amount)
    if (!isPaid(payment)) {
      pendingByCurrency.set(currency, (pendingByCurrency.get(currency) ?? 0) + amount)
    }
  }

  const pending = payments.filter((payment) => !isPaid(payment) && !isFinishedCredit(payment))
  const byTotalDesc = (a: { total: number }, b: { total: number }) => b.total - a.total

  return {
    next: [...pending].sort((a, b) => a.nextPaymentDate.localeCompare(b.nextPaymentDate))[0],
    periodTotals: [...totals.entries()].map(([currency, total]) => ({ currency, total })).sort(byTotalDesc),
    pendingTotals: [...pendingByCurrency.entries()]
      .map(([currency, total]) => ({ currency, total }))
      .sort(byTotalDesc),
    unknownAmountCount,
    pendingCount: pending.length,
    overdueCount: pending.filter((payment) => daysUntil(payment.nextPaymentDate, today) < 0).length,
    paidCount: payments.length - pending.length,
  }
}

/**
 * ADR-020: las DOS fechas de una tarjeta de crédito.
 *
 * Una tarjeta no tiene "próximo pago" a secas: tiene un corte (cuándo se
 * sabe cuánto debes) y un límite (cuándo hay que pagarlo). `nextPaymentDate`
 * guarda la próxima fecha LÍMITE, que es la accionable; el corte se deriva
 * de `statementDay`.
 *
 * El corte es el anterior al límite, no el siguiente: si hoy es 10, el
 * límite es el 18 y el corte es el día 5, el corte que corresponde a ese
 * límite ya pasó (el 5 de este mes). Si el día de corte es POSTERIOR al
 * límite dentro del mes, pertenece al mes anterior — es el caso corriente
 * de una tarjeta que corta a fin de mes y se paga a mitad del siguiente.
 */
export function cardStatementDate(payment: Subscription): Date | undefined {
  if (payment.kind !== 'CARD' || !payment.statementDay) return undefined
  const due = toLocalDate(payment.nextPaymentDate)
  const candidate = new Date(due.getFullYear(), due.getMonth(), payment.statementDay)
  if (candidate > due) {
    candidate.setMonth(candidate.getMonth() - 1)
  }
  return candidate
}

/** ¿Ya cortó la tarjeta? Es cuando tiene sentido pedirle el importe real. */
export function cardIsAfterStatement(payment: Subscription, today = new Date()): boolean {
  const statement = cardStatementDate(payment)
  return statement !== undefined && statement <= today
}
