import { apiFetch } from '../../core/api/httpClient'

export interface Reminder {
  id: string
  ownerUserId: string
  title: string
  description?: string
  dueAt?: string
  status: 'PENDING' | 'COMPLETED'
  version: number
  createdAt: string
  updatedAt: string
}

interface RemindersPage {
  items: Reminder[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function listReminders(): Promise<RemindersPage> {
  const response = await apiFetch('/reminders')
  if (!response.ok) throw new Error(`GET /reminders failed: ${response.status}`)
  return response.json()
}

export async function createReminder(title: string, dueAt?: string): Promise<Reminder> {
  const response = await apiFetch('/reminders', {
    method: 'POST',
    body: JSON.stringify(dueAt ? { title, dueAt } : { title }),
  })
  if (!response.ok) throw new Error(`POST /reminders failed: ${response.status}`)
  return response.json()
}

export async function completeReminder(id: string, version: number): Promise<Reminder> {
  const response = await apiFetch(`/reminders/${id}/complete`, {
    method: 'POST',
    body: JSON.stringify({ version }),
  })
  if (!response.ok) throw new Error(`POST /reminders/${id}/complete failed: ${response.status}`)
  return response.json()
}
