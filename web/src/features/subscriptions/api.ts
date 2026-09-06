import { apiFetch } from '../../core/api/httpClient'
import { creationContext, withContext, type ModuleContext } from '../../core/user/moduleContext'

export type BillingCycle = 'WEEKLY' | 'MONTHLY' | 'YEARLY'

/**
 * ADR-020(d): tipo de compromiso de pago. Seis valores, TRES formas —
 * los cuatro primeros comparten campos y solo cambian etiqueta e icono;
 * CARD añade corte/límite e importe variable; CREDIT añade plazos.
 */
export type PaymentKind = 'SUBSCRIPTION' | 'SERVICE' | 'MEMBERSHIP' | 'CUSTOM' | 'CARD' | 'CREDIT'

export const PAYMENT_KIND_LABELS: Record<PaymentKind, string> = {
  SUBSCRIPTION: 'Suscripción',
  SERVICE: 'Servicio',
  MEMBERSHIP: 'Membresía',
  CUSTOM: 'Personalizado',
  CARD: 'Tarjeta',
  CREDIT: 'Crédito',
}

/** Tres letras para la marca de la fila: se leen en gris, en una columna
    estrecha y sin depender del color (el color solo refuerza). */
export const PAYMENT_KIND_MARKS: Record<PaymentKind, string> = {
  SUBSCRIPTION: 'SUB',
  SERVICE: 'SRV',
  MEMBERSHIP: 'MEM',
  CUSTOM: 'PAG',
  CARD: 'TDC',
  CREDIT: 'CRÉ',
}

export const PAYMENT_KINDS: PaymentKind[] = [
  'SUBSCRIPTION',
  'SERVICE',
  'MEMBERSHIP',
  'CUSTOM',
  'CARD',
  'CREDIT',
]

/** ADR-020(c): divisa por pago. La lista es la de uso realista en el
    producto hoy; el modelo admite cualquier ISO 4217. */
export const CURRENCIES = ['MXN', 'USD', 'EUR'] as const
export const DEFAULT_CURRENCY = 'MXN'

export const BILLING_CYCLE_LABELS: Record<BillingCycle, string> = {
  WEEKLY: 'Semanal',
  MONTHLY: 'Mensual',
  YEARLY: 'Anual',
}

export const BILLING_CYCLES: BillingCycle[] = ['WEEKLY', 'MONTHLY', 'YEARLY']

export interface Subscription {
  id: string
  ownerUserId: string
  service: string
  company?: string
  plan?: string
  nextPaymentDate: string
  billingCycle: BillingCycle

  /** ADR-020. Ausente en registros anteriores a la migración V26. */
  kind: PaymentKind
  amount?: number
  currency?: string
  paymentMethod?: string
  notes?: string
  variableAmount: boolean

  /** Solo kind = CARD. */
  statementDay?: number
  dueDay?: number
  minPayment?: number
  noInterestPayment?: number
  institution?: string
  lastFour?: string

  /** Solo kind = CREDIT. */
  totalInstallments?: number
  currentInstallment?: number

  version: number
  createdAt: string
  updatedAt: string
}

interface SubscriptionsPageResponse {
  items: Subscription[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function listSubscriptions(context?: ModuleContext | null): Promise<SubscriptionsPageResponse> {
  const response = await apiFetch(withContext('/subscriptions?size=100', context))
  if (!response.ok) throw new Error(`GET /subscriptions failed: ${response.status}`)
  return response.json()
}

/**
 * ADR-020: el alta pasó de 6 argumentos posicionales a un objeto.
 *
 * Con los 14 campos nuevos, la firma posicional tendría 20 parámetros —
 * dos importes y tres enteros seguidos, imposibles de distinguir en la
 * llamada. El objeto además deja claro qué campos son de qué tipo de pago.
 */
export interface PaymentInput {
  service: string
  company?: string
  plan?: string
  nextPaymentDate: string
  billingCycle: BillingCycle

  kind: PaymentKind
  amount?: number
  currency?: string
  paymentMethod?: string
  notes?: string
  variableAmount?: boolean

  statementDay?: number
  dueDay?: number
  minPayment?: number
  noInterestPayment?: number
  institution?: string
  lastFour?: string

  totalInstallments?: number
  currentInstallment?: number
}

/** Quita las claves vacías: el backend trata `null` como "no lo toques" y
    mandar cadenas vacías sobreescribiría datos buenos con nada. */
function cleanInput(input: Record<string, unknown>): Record<string, unknown> {
  const out: Record<string, unknown> = {}
  for (const [key, value] of Object.entries(input)) {
    if (value === undefined || value === null || value === '') continue
    out[key] = value
  }
  return out
}

export async function createSubscription(
  input: PaymentInput,
  context?: ModuleContext | null,
): Promise<Subscription> {
  const response = await apiFetch('/subscriptions', {
    method: 'POST',
    body: JSON.stringify({
      ...cleanInput({ ...input }),
      // `variableAmount: false` es significativo y `cleanInput` no lo quita
      // porque solo descarta undefined/null/''.
      variableAmount: input.variableAmount ?? false,
      context: creationContext(context),
    }),
  })
  if (!response.ok) throw new Error(`POST /subscriptions failed: ${response.status}`)
  return response.json()
}

export async function updateSubscription(
  id: string,
  input: PaymentInput,
  version: number,
): Promise<Subscription> {
  const response = await apiFetch(`/subscriptions/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({
      ...cleanInput({ ...input }),
      variableAmount: input.variableAmount ?? false,
      version,
    }),
  })
  if (!response.ok) throw new Error(`PATCH /subscriptions/${id} failed: ${response.status}`)
  return response.json()
}

export async function deleteSubscription(id: string): Promise<void> {
  const response = await apiFetch(`/subscriptions/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /subscriptions/${id} failed: ${response.status}`)
}

/**
 * ADR-020(f): un ciclo pagado. `periodDate` es el ciclo que se pagó, no el
 * día en que se marcó — así se distingue "pagué el de septiembre" de
 * "pagué el de octubre" aunque ambos se registren el mismo día.
 */
export interface PaymentRecord {
  id: string
  subscriptionId: string
  periodDate: string
  paidOn: string
  amount?: number
  currency?: string
}

/** Todos los ciclos pagados del usuario, en una sola consulta: la pantalla
    los necesita en bloque para saber qué filas van marcadas. */
export async function listPaymentRecords(): Promise<PaymentRecord[]> {
  const response = await apiFetch('/subscriptions/payments')
  if (!response.ok) throw new Error(`GET /subscriptions/payments failed: ${response.status}`)
  return response.json()
}

/** Marca el ciclo actual como pagado y avanza al siguiente. Idempotente. */
export async function markPaymentPaid(
  id: string,
  amount?: number,
  currency?: string,
): Promise<Subscription> {
  const response = await apiFetch(`/subscriptions/${id}/payments`, {
    method: 'POST',
    body: JSON.stringify({ amount, currency }),
  })
  if (!response.ok) throw new Error(`POST /subscriptions/${id}/payments failed: ${response.status}`)
  return response.json()
}

export async function undoLastPayment(id: string): Promise<Subscription> {
  const response = await apiFetch(`/subscriptions/${id}/payments/last`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /subscriptions/${id}/payments/last failed: ${response.status}`)
  return response.json()
}

/** Historial de un compromiso concreto, lo más reciente primero. */
export async function listPaymentRecordsFor(id: string): Promise<PaymentRecord[]> {
  const response = await apiFetch(`/subscriptions/${id}/payments`)
  if (!response.ok) throw new Error(`GET /subscriptions/${id}/payments failed: ${response.status}`)
  return response.json()
}
