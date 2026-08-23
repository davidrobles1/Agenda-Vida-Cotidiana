import { useEffect, useState } from 'react'
import { listReminders, type Reminder } from '../reminders/api'
import { listMyInvitations } from '../sharing/api'
import { getCurrentUser } from '../../core/user/api'

export interface HomeState {
  greetingName: string
  pendingCount: number
  completedCount: number
  overdueCount: number
  sharedCount: number
  upcoming: Reminder[]
  overdue: Reminder[]
  /** Pedido explícito del usuario (2026-08-22): widget de calendario en
      Inicio, "mostrar únicamente la semana actual y sus pendientes" —
      pendientes (status PENDING) cuyo dueAt cae dentro de la semana
      actual (lunes-domingo, mismo `firstDayOfWeek="mon"` que
      CalendarView.tsx ya usa), derivado del mismo array de recordatorios
      que `upcoming`/`overdue` ya calculan — ningún fetch nuevo. */
  thisWeek: Reminder[]
  loading: boolean
  error: string | null
}

const initialState: HomeState = {
  greetingName: '',
  pendingCount: 0,
  completedCount: 0,
  overdueCount: 0,
  sharedCount: 0,
  upcoming: [],
  overdue: [],
  thisWeek: [],
  loading: true,
  error: null,
}

/** Lunes 00:00 de la semana que contiene `date`, y el domingo
    correspondiente a las 23:59:59.999 — mismo criterio "semana empieza en
    lunes" que CalendarView.tsx (`firstDayOfWeek="mon"`). */
function currentWeekRange(now: Date): { start: Date; end: Date } {
  const day = now.getDay() // 0 = domingo ... 6 = sábado
  const diffToMonday = day === 0 ? -6 : 1 - day
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate() + diffToMonday, 0, 0, 0, 0)
  const end = new Date(start.getFullYear(), start.getMonth(), start.getDate() + 6, 23, 59, 59, 999)
  return { start, end }
}

/**
 * UX-006: Web counterpart of Android's HomeViewModel — same real-data-only
 * rule (Section 1/4): only Tareas/Compartidos counts and real reminder rows,
 * nothing from the 6 mock modules or Finanzas here.
 */
export function useHomeData(): HomeState {
  const [state, setState] = useState<HomeState>(initialState)

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        const [user, remindersPage, invitations] = await Promise.all([
          getCurrentUser(),
          listReminders(),
          listMyInvitations(),
        ])
        if (cancelled) return

        const now = Date.now()
        const reminders = remindersPage.items
        const pending = reminders.filter((r) => r.status === 'PENDING')
        const completed = reminders.filter((r) => r.status === 'COMPLETED')
        const overdue = pending.filter((r) => r.dueAt && new Date(r.dueAt).getTime() < now)
        const overdueIds = new Set(overdue.map((r) => r.id))
        const upcoming = pending
          .filter((r) => !overdueIds.has(r.id))
          .sort((a, b) => {
            if (!a.dueAt) return 1
            if (!b.dueAt) return -1
            return new Date(a.dueAt).getTime() - new Date(b.dueAt).getTime()
          })
          .slice(0, 4)
        const sharedCount = invitations.filter((i) => i.status === 'PENDING').length

        const { start: weekStart, end: weekEnd } = currentWeekRange(new Date(now))
        const thisWeek = pending
          .filter((r) => r.dueAt && new Date(r.dueAt) >= weekStart && new Date(r.dueAt) <= weekEnd)
          .sort((a, b) => new Date(a.dueAt!).getTime() - new Date(b.dueAt!).getTime())

        setState({
          greetingName: user.username,
          pendingCount: pending.length,
          completedCount: completed.length,
          overdueCount: overdue.length,
          sharedCount,
          upcoming,
          overdue,
          thisWeek,
          loading: false,
          error: null,
        })
      } catch (e) {
        if (cancelled) return
        setState((prev) => ({ ...prev, loading: false, error: e instanceof Error ? e.message : 'Failed to load' }))
      }
    }

    load()
    return () => {
      cancelled = true
    }
  }, [])

  return state
}
