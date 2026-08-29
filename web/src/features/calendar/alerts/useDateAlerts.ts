import { useEffect, useMemo, useState } from 'react'
import { listWarranties, type Warranty } from '../../warranties/api'
import { listMaintenanceRecords, type MaintenanceRecord } from '../../maintenance/api'
import { listSubscriptions, type Subscription } from '../../subscriptions/api'
import { buildDateAlerts, upcomingAlerts, type DateAlert } from './dateAlerts'
import type { ModuleContext } from '../../../core/user/moduleContext'

/**
 * Alertas de fecha próximas, para las pantallas de entrada (ADR-018).
 *
 * `useCalendarData` ya calcula estas alertas para el Calendario, pero
 * arrastra consigo recordatorios, invitaciones y todas sus acciones de
 * escritura. Inicio y Hoy solo necesitan leer las alertas, así que este
 * hook carga exactamente los tres orígenes y nada más — misma función pura
 * (`buildDateAlerts`), sin duplicar la regla de negocio.
 */
export function useDateAlerts(horizonDays = 30, context?: ModuleContext | null): {
  alerts: DateAlert[]
  loading: boolean
} {
  const [warranties, setWarranties] = useState<Warranty[]>([])
  const [maintenanceRecords, setMaintenanceRecords] = useState<MaintenanceRecord[]>([])
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false

    // ADR-019: las alertas se derivan de recursos, así que heredan su
    // aislamiento — se piden ya acotadas al módulo.
    Promise.all([listWarranties(context), listMaintenanceRecords(context), listSubscriptions(context)])
      .then(([warrantiesPage, maintenancePage, subscriptionsPage]) => {
        if (cancelled) return
        setWarranties(warrantiesPage.items)
        setMaintenanceRecords(maintenancePage.items)
        setSubscriptions(subscriptionsPage.items)
      })
      .catch(() => {
        // Las alertas son un complemento de la pantalla, no su contenido
        // principal: si fallan, la pantalla sigue siendo útil y no se
        // muestra un error que el usuario no puede accionar.
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [context])

  const alerts = useMemo(
    () =>
      upcomingAlerts(
        buildDateAlerts({ warranties, maintenanceRecords, subscriptions }),
        horizonDays,
      ),
    [warranties, maintenanceRecords, subscriptions, horizonDays],
  )

  return { alerts, loading }
}
