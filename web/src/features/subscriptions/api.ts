import { apiFetch } from '../../core/api/httpClient'
import { creationContext, withContext, type ModuleContext } from '../../core/user/moduleContext'

export type BillingCycle = 'WEEKLY' | 'MONTHLY' | 'YEARLY'

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

export async function createSubscription(
  service: string,
  company: string | undefined,
  plan: string | undefined,
  nextPaymentDate: string,
  billingCycle: BillingCycle,
  context?: ModuleContext | null,
): Promise<Subscription> {
  const response = await apiFetch('/subscriptions', {
    method: 'POST',
    body: JSON.stringify({
      service,
      company: company || undefined,
      plan: plan || undefined,
      nextPaymentDate,
      billingCycle,
      context: creationContext(context),
    }),
  })
  if (!response.ok) throw new Error(`POST /subscriptions failed: ${response.status}`)
  return response.json()
}

export async function updateSubscription(
  id: string,
  service: string,
  company: string | undefined,
  plan: string | undefined,
  nextPaymentDate: string,
  billingCycle: BillingCycle,
  version: number,
): Promise<Subscription> {
  const response = await apiFetch(`/subscriptions/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ service, company: company || undefined, plan: plan || undefined, nextPaymentDate, billingCycle, version }),
  })
  if (!response.ok) throw new Error(`PATCH /subscriptions/${id} failed: ${response.status}`)
  return response.json()
}

export async function deleteSubscription(id: string): Promise<void> {
  const response = await apiFetch(`/subscriptions/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /subscriptions/${id} failed: ${response.status}`)
}
