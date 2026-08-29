import { useEffect, useState } from 'react'
import { Button } from 'react-aria-components'
import { Pencil, Plus } from 'lucide-react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { ListItemRow } from '../../core/ui/components/ListItemRow'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { IconRepeat } from '../../core/ui/icons'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import type { Tone } from '../../core/ui/components/MetricCard'
import { BILLING_CYCLE_LABELS, deleteSubscription, listSubscriptions, type Subscription } from './api'
import { SubscriptionDialog } from './SubscriptionDialog'
import styles from './SubscriptionsPage.module.css'
import { useActiveMode } from '../../core/user/ActiveModeContext'

/** Pedido explícito del usuario (2026-08-22): "lo más relevante para esta
    ventana" — la próxima fecha de pago es lo único realmente accionable de
    una suscripción (a diferencia de servicio/compañía/plan, que son datos
    descriptivos), así que es lo que se muestra como pill destacado —
    cuenta regresiva relativa ("Hoy", "Mañana", "En 5 días") en vez de solo
    una fecha, más fácil de escanear de un vistazo que "2026-09-15". */
function relativePaymentLabel(iso: string): { label: string; tone: Tone } {
  const dueDate = new Date(iso)
  const now = new Date()
  const dueMidnight = new Date(dueDate.getFullYear(), dueDate.getMonth(), dueDate.getDate())
  const nowMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const diffDays = Math.round((dueMidnight.getTime() - nowMidnight.getTime()) / (1000 * 60 * 60 * 24))

  if (diffDays < 0) return { label: `Venció hace ${Math.abs(diffDays)} día${Math.abs(diffDays) === 1 ? '' : 's'}`, tone: 'error' }
  if (diffDays === 0) return { label: 'Hoy', tone: 'error' }
  if (diffDays === 1) return { label: 'Mañana', tone: 'warning' }
  if (diffDays <= 7) return { label: `En ${diffDays} días`, tone: 'warning' }
  return { label: dueDate.toLocaleDateString('es-MX', { day: '2-digit', month: 'short' }), tone: 'success' }
}

/** Módulo Suscripciones real (pedido explícito del usuario, 2026-08-22) —
    reemplaza el scaffolding UX-006 por backend real
    (subscription.domain.Subscription): registrar/actualizar/borrar,
    Servicio/Compañía/Plan/próximo pago/periodicidad. Sin precio/monto —
    ver SubscriptionDialog.tsx's own doc comment. */
export function SubscriptionsPage() {
  // ADR-019: el recurso nace en el módulo desde el que se crea, y la
  // lista solo pide los de ese módulo. Fuera de /personal y /laboral
  // `activeMode` es null: se devuelve todo y las altas nacen PERSONAL.
  const activeMode = useActiveMode()
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  async function refresh() {
    setLoading(true)
    setError(null)
    try {
      const page = await listSubscriptions(activeMode)
      setSubscriptions(page.items)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar las suscripciones.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  function handleSaved(saved: Subscription) {
    setSubscriptions((current) => {
      const exists = current.some((sub) => sub.id === saved.id)
      return exists ? current.map((sub) => (sub.id === saved.id ? saved : sub)) : [saved, ...current]
    })
  }

  async function handleDelete(id: string) {
    await deleteSubscription(id)
    setSubscriptions((current) => current.filter((sub) => sub.id !== id))
  }

  return (
    <AppShell title="Suscripciones" subtitle="Tus suscripciones recurrentes.">
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      <ListSectionCard
        title="Activas"
        action={
          <SubscriptionDialog
            onSaved={handleSaved}
            trigger={
              <Button className={styles.addButton}>
                <Plus width={16} height={16} /> Nueva suscripción
              </Button>
            }
          />
        }
      >
        {loading && <p className={styles.emptyHint}>Cargando…</p>}
        {!loading && subscriptions.length === 0 && <p className={styles.emptyHint}>Todavía no hay suscripciones registradas.</p>}
        {!loading &&
          subscriptions.map((sub) => {
            const { label, tone } = relativePaymentLabel(sub.nextPaymentDate)
            const subtitleParts = [sub.company, sub.plan, BILLING_CYCLE_LABELS[sub.billingCycle]].filter(Boolean)
            return (
              <ListItemRow
                key={sub.id}
                title={sub.service}
                subtitle={subtitleParts.join(' · ')}
                icon={IconRepeat}
                tone="primary"
                pillLabel={label}
                pillTone={tone}
                trailing={
                  <div className={styles.rowActions}>
                    <SubscriptionDialog
                      subscription={sub}
                      onSaved={handleSaved}
                      trigger={
                        <Button className={styles.iconButton} aria-label="Editar suscripción">
                          <Pencil width={16} height={16} />
                        </Button>
                      }
                    />
                    <SimpleDeleteConfirm
                      resourceLabel="suscripción"
                      itemName={sub.service}
                      ariaLabel="Eliminar suscripción"
                      onConfirm={() => handleDelete(sub.id)}
                    />
                  </div>
                }
              />
            )
          })}
      </ListSectionCard>
    </AppShell>
  )
}
