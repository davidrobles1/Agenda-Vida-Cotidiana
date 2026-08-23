import {
  useCallback,
  useEffect,
  useState,
} from 'react'

import { useActiveMode } from '../../core/user/ActiveModeContext'

import {
  completeReminder,
  createReminder,
  deleteReminder,
  listReminders,
  updateReminder,
  type Reminder,
} from '../reminders/api'

import {
  cancelLocalReminder,
  scheduleLocalReminder,
} from '../../core/notifications/localReminderTimer'

import {
  listMyInvitations,
  type Invitation,
} from '../sharing/api'

import {
  completeWarranty,
  listWarranties,
  type Warranty,
} from '../warranties/api'

import {
  completeMaintenanceRecord,
  listMaintenanceRecords,
  type MaintenanceRecord,
} from '../maintenance/api'

export interface CalendarState {
  loading: boolean
  error: string | null

  reminders: Reminder[]
  invitations: Invitation[]

  warranties: Warranty[]
  maintenanceRecords: MaintenanceRecord[]

  completeReminderAction: (
    reminder: Reminder,
  ) => Promise<void>

  completeWarrantyAction: (
    warranty: Warranty,
  ) => Promise<void>

  completeMaintenanceAction: (
    record: MaintenanceRecord,
  ) => Promise<void>

  createReminderAction: (
    input: {
      title: string
      description?: string
      dueAt?: string
      iconId?: string
      stickerId?: string
    },
  ) => Promise<void>

  updateReminderAction: (
    reminder: Reminder,
    input: {
      title: string
      description?: string
      dueAt?: string
      iconId?: string
      stickerId?: string
    },
  ) => Promise<void>

  deleteReminderAction: (
    reminder: Reminder,
  ) => Promise<void>
}

export function useCalendarData(): CalendarState {
  const activeMode =
    useActiveMode()

  const [
    reminders,
    setReminders,
  ] = useState<Reminder[]>(
    [],
  )

  const [
    invitations,
    setInvitations,
  ] = useState<Invitation[]>(
    [],
  )

  const [
    warranties,
    setWarranties,
  ] = useState<Warranty[]>(
    [],
  )

  const [
    maintenanceRecords,
    setMaintenanceRecords,
  ] = useState<
    MaintenanceRecord[]
  >([])

  const [
    loading,
    setLoading,
  ] = useState(true)

  const [
    error,
    setError,
  ] = useState<
    string | null
  >(null)

  /* =====================================================
     REFRESH
     ===================================================== */

  const refresh =
    useCallback(
      async () => {
        try {
          const [
            remindersPage,
            invitationsList,
            warrantiesPage,
            maintenanceRecordsPage,
          ] =
            await Promise.all([
              listReminders(),
              listMyInvitations(),
              listWarranties(),
              listMaintenanceRecords(),
            ])

          setReminders(
            remindersPage.items.filter(
              (reminder) =>
                reminder.dueAt,
            ),
          )

          setInvitations(
            invitationsList.filter(
              (invitation) =>
                invitation.status ===
                'PENDING',
            ),
          )

          setWarranties(
            warrantiesPage.items,
          )

          setMaintenanceRecords(
            maintenanceRecordsPage.items,
          )

          setError(null)
        } catch (e) {
          setError(
            e instanceof Error
              ? e.message
              : 'Failed to load calendar data',
          )
        } finally {
          setLoading(false)
        }
      },
      [],
    )

  useEffect(() => {
    refresh()
  }, [refresh])

  /* =====================================================
     COMPLETE REMINDER
     ===================================================== */

  async function completeReminderAction(
    reminder: Reminder,
  ) {
    try {
      await completeReminder(
        reminder.id,
        reminder.version,
      )

      cancelLocalReminder(
        reminder.id,
      )

      await refresh()
    } catch (e) {
      setError(
        e instanceof Error
          ? e.message
          : 'Failed to complete reminder',
      )
    }
  }

  /* =====================================================
     COMPLETE WARRANTY
     ===================================================== */

  async function completeWarrantyAction(
    warranty: Warranty,
  ) {
    try {
      await completeWarranty(
        warranty.id,
        warranty.version,
      )

      await refresh()
    } catch (e) {
      setError(
        e instanceof Error
          ? e.message
          : 'Failed to complete warranty',
      )
    }
  }

  /* =====================================================
     COMPLETE MAINTENANCE
     ===================================================== */

  async function completeMaintenanceAction(
    record: MaintenanceRecord,
  ) {
    try {
      await completeMaintenanceRecord(
        record.id,
        record.version,
      )

      await refresh()
    } catch (e) {
      setError(
        e instanceof Error
          ? e.message
          : 'Failed to complete maintenance record',
      )
    }
  }

  /* =====================================================
     CREATE / UPDATE / DELETE REMINDER
     ===================================================== */

  /**
   * A diferencia de `completeReminderAction`/`completeWarrantyAction`
   * (que atrapan el error y lo dejan en `state.error`), estas tres no
   * atrapan nada — se propagan a quien llama (`CreateReminderDialog`/
   * `ReminderDrawer`) para mostrarse inline y mantener el formulario
   * abierto con los datos intactos, mismo contrato que `useNotes.ts`.
   */

  async function createReminderAction(input: {
    title: string
    description?: string
    dueAt?: string
    iconId?: string
    stickerId?: string
  }) {
    const created = await createReminder({
      title: input.title.trim(),
      description: input.description,
      dueAt: input.dueAt,
      context: activeMode ?? undefined,
      iconId: input.iconId,
      stickerId: input.stickerId,
    })

    if (input.dueAt) {
      scheduleLocalReminder(
        created.id,
        created.title,
        new Date(input.dueAt).getTime(),
      )
    }

    await refresh()
  }

  async function updateReminderAction(
    reminder: Reminder,
    input: {
      title: string
      description?: string
      dueAt?: string
      iconId?: string
      stickerId?: string
    },
  ) {
    const updated = await updateReminder(reminder.id, {
      title: input.title.trim(),
      description: input.description,
      dueAt: input.dueAt,
      iconId: input.iconId,
      stickerId: input.stickerId,
      version: reminder.version,
    })

    if (updated.dueAt) {
      scheduleLocalReminder(
        updated.id,
        updated.title,
        new Date(updated.dueAt).getTime(),
      )
    } else {
      cancelLocalReminder(updated.id)
    }

    await refresh()
  }

  async function deleteReminderAction(reminder: Reminder) {
    await deleteReminder(reminder.id)
    cancelLocalReminder(reminder.id)
    await refresh()
  }

  return {
    loading,
    error,

    reminders,
    invitations,

    warranties,
    maintenanceRecords,

    completeReminderAction,
    completeWarrantyAction,
    completeMaintenanceAction,

    createReminderAction,
    updateReminderAction,
    deleteReminderAction,
  }
}