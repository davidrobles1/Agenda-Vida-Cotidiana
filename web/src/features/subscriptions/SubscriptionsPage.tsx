import { AppShell } from '../../core/ui/layout/AppShell'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { IconRepeat } from '../../core/ui/icons'
import { subscriptions } from '../../core/mock/mockData'

/**
 * UX-006: scaffolding-only module — mock data (core/mock/mockData.ts), zero
 * endpoints, zero logic. Price is a descriptive field of each mock item
 * (like a warranty's expiry date), not a Finanzas dashboard — no totals,
 * no spend-over-time, nothing that reads as a budget/expenses view.
 */
export function SubscriptionsPage() {
  return (
    <AppShell title="Suscripciones" subtitle="Tus suscripciones recurrentes.">
      <ListSectionCard title="Activas">
        {subscriptions.map((sub) => (
          <ListItemRow
            key={sub.id}
            title={sub.name}
            subtitle={sub.cycle}
            icon={IconRepeat}
            tone="primary"
            pillLabel={sub.price}
            pillTone="info"
          />
        ))}
      </ListSectionCard>
    </AppShell>
  )
}
