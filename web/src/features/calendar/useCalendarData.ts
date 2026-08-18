import { useCallback, useEffect, useState } from 'react'
import { completeReminder, listReminders, type Reminder } from '../reminders/api'
import { cancelLocalReminder } from '../../core/notifications/localReminderTimer'
import { listMyInvitations, type Invitation } from '../sharing/api'
import { maintenanceRecords, warranties, type MockMaintenanceRecord, type MockWarranty } from '../../core/mock/mockData'

export interface CalendarState {
  loading: boolean
  error: string | null
  reminders: Reminder[]
  invitations: Invitation[]
  warranties: MockWarranty[]
  maintenanceRecords: MockMaintenanceRecord[]
  mockCompletedIds: Set<string>
  completeReminderAction: (reminder: Reminder) => Promise<void>
  toggleMockComplete: (id: string) => void
}

/**
 * UX-007: Calendario's data — real reminders (`GET /reminders`, filtered to
 * pending ones that actually have a `dueAt`; a date-less reminder has
 * nowhere to sit on a month grid, it still shows on Tareas) and real pending
 * invitations (`GET /me/invitations`). Zero new backend: both calls reuse
 * `features/reminders/api.ts`/`features/sharing/api.ts` exactly as
 * `useHomeData.ts`/`RemindersPage.tsx` already do — `completeReminderAction`
 * below calls the same `completeReminder()` + `cancelLocalReminder()` pair
 * `RemindersPage.handleComplete` uses, not a reimplementation.
 * Garantías/Mantenimiento are mock (`core/mock/mockData.ts`, imported
 * as-is) — `mockCompletedIds` is the local-only, lost-on-reload "completed"
 * state the task requires to be explicit about, never sent anywhere real.
 */
export function useCalendarData(): CalendarState {
  const [reminders, setReminders] = useState<Reminder[]>([])
  const [invitations, setInvitations] = useState<Invitation[]>([])
  const [mockCompletedIds, setMockCompletedIds] = useState<Set<string>>(new Set())
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    try {
      const [remindersPage, invitationsList] = await Promise.all([listReminders(), listMyInvitations()])
      const pendingWithDate = remindersPage.items.filter((r) => r.status === 'PENDING' && r.dueAt)
      setReminders(pendingWithDate)
      setInvitations(invitationsList.filter((i) => i.status === 'PENDING'))
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load calendar data')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  async function completeReminderAction(reminder: Reminder) {
    try {
      await completeReminder(reminder.id, reminder.version)
      cancelLocalReminder(reminder.id)
      await refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to complete reminder')
    }
  }

  function toggleMockComplete(id: string) {
    setMockCompletedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  return {
    loading,
    error,
    reminders,
    invitations,
    warranties,
    maintenanceRecords,
    mockCompletedIds,
    completeReminderAction,
    toggleMockComplete,
  }
}
