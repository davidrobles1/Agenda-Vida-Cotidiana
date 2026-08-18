import { useCallback, useEffect, useState } from 'react'
import { completeReminder, createReminder, listReminders, type Reminder } from '../reminders/api'
import { cancelLocalReminder, scheduleLocalReminder } from '../../core/notifications/localReminderTimer'
import { listMyInvitations, type Invitation } from '../sharing/api'
import { maintenanceRecords, warranties, type MockMaintenanceRecord, type MockWarranty } from '../../core/mock/mockData'

export interface CalendarState {
  loading: boolean
  error: string | null
  /** UX-010: ALL reminders with a dueAt — pending, overdue AND completed —
      not filtered to pending-only anymore. Real data already returned by
      GET /reminders; consumers (CalendarPage) derive each one's visual
      state (pending/overdue/completed) instead of the fetch silently
      dropping completed ones, so the month grid can show a real "done"
      state instead of just disappearing a finished task. */
  reminders: Reminder[]
  invitations: Invitation[]
  warranties: MockWarranty[]
  maintenanceRecords: MockMaintenanceRecord[]
  mockCompletedIds: Set<string>
  completeReminderAction: (reminder: Reminder) => Promise<void>
  toggleMockComplete: (id: string) => void
  /** UX-010: creates a real reminder due on a given day (from the day-detail
      panel's quick-add) — calls the exact same POST /reminders
      RemindersPage's form uses, just with the selected day's date
      pre-filled instead of a date the user typed in a separate picker. */
  createReminderForDay: (title: string, dateKey: string) => Promise<void>
}

/**
 * UX-007/UX-010: Calendario's data — real reminders (`GET /reminders`, kept
 * whenever they have a `dueAt`, any status) and real pending invitations
 * (`GET /me/invitations`). Zero new backend: every call reuses
 * `features/reminders/api.ts`/`features/sharing/api.ts` exactly as
 * `useHomeData.ts`/`RemindersPage.tsx` already do — `completeReminderAction`/
 * `createReminderForDay` call the same real endpoints RemindersPage's own
 * "Complete" button and creation form use, not reimplementations.
 * Garantías/Mantenimiento are mock (`core/mock/mockData.ts`, imported
 * as-is) — `mockCompletedIds` is the local-only, lost-on-reload "completed"
 * state, never sent anywhere real.
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
      setReminders(remindersPage.items.filter((r) => r.dueAt))
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

  async function createReminderForDay(title: string, dateKey: string) {
    if (!title.trim()) return
    try {
      // Noon local time on the selected day — matches the day it was
      // created for regardless of the reader's timezone offset, avoiding
      // the "created for the 5th, shows up on the 4th" off-by-one a
      // midnight timestamp risks near a UTC boundary.
      const dueAt = new Date(`${dateKey}T12:00:00`)
      const created = await createReminder(title.trim(), dueAt.toISOString())
      scheduleLocalReminder(created.id, created.title, dueAt.getTime())
      await refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to create reminder')
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
    createReminderForDay,
  }
}
