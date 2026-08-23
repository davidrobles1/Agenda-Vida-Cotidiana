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
  /** ADR-015/FR-019/BE-038 — real on the backend now (always present in
      GET /reminders responses; `?` kept only because pre-ADR-015 rows are
      backfilled to PERSONAL, not because it can be genuinely absent). */
  context?: 'PERSONAL' | 'LABORAL'
  /** Real on the backend now (`icon_id`/`sticker_id` columns, V7 migration)
      — see `core/ui/pickers/pickerCatalog.ts` for the shared catalog. */
  iconId?: string
  stickerId?: string
  /** ADR-016/FR-023/FR-024 (Módulo Laboral) — vínculo opcional a una
      Persona/Proyecto propios, y ubicación (solo relevante para una
      "reunión"). Usados por `features/laboral/TareasPage.tsx`; el resto de
      la app (RemindersPage/CalendarPage) los ignora sin problema. */
  personId?: string
  projectId?: string
  location?: string
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

export interface CreateReminderInput {
  title: string
  description?: string
  dueAt?: string
  /**
   * `context` is FR-019 (ADR-015): PERSONAL/LABORAL, inferred by the caller
   * from which navbar the creation happened in (RemindersPage.tsx/
   * CalendarPage.tsx read it via useActiveMode(), never a form field — see
   * ADR-015's own "Refinamientos cerrados" for why there's no selector).
   */
  context?: 'PERSONAL' | 'LABORAL'
  iconId?: string
  stickerId?: string
  /** ADR-016/FR-023/FR-024. */
  personId?: string
  projectId?: string
  location?: string
}

export async function createReminder(input: CreateReminderInput): Promise<Reminder> {
  const body: Record<string, unknown> = { title: input.title }
  if (input.description) body.description = input.description
  if (input.dueAt) body.dueAt = input.dueAt
  if (input.context) body.context = input.context
  if (input.iconId) body.iconId = input.iconId
  if (input.stickerId) body.stickerId = input.stickerId
  if (input.personId) body.personId = input.personId
  if (input.projectId) body.projectId = input.projectId
  if (input.location) body.location = input.location
  const response = await apiFetch('/reminders', {
    method: 'POST',
    body: JSON.stringify(body),
  })
  if (!response.ok) throw new Error(`POST /reminders failed: ${response.status}`)
  return response.json()
}

export interface UpdateReminderInput {
  title?: string
  description?: string
  dueAt?: string
  /** Always-overwrite semantics on the backend (see Reminder.java's
      applyEdit) — omitting these clears them server-side. The caller
      (ReminderDrawer) always resubmits the reminder's complete current
      selection, same contract as Note's iconId/stickerId. */
  iconId?: string
  stickerId?: string
  version: number
}

export async function updateReminder(id: string, input: UpdateReminderInput): Promise<Reminder> {
  const response = await apiFetch(`/reminders/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(input),
  })
  if (!response.ok) throw new Error(`PATCH /reminders/${id} failed: ${response.status}`)
  return response.json()
}

export async function deleteReminder(id: string): Promise<void> {
  const response = await apiFetch(`/reminders/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /reminders/${id} failed: ${response.status}`)
}

export async function completeReminder(id: string, version: number): Promise<Reminder> {
  const response = await apiFetch(`/reminders/${id}/complete`, {
    method: 'POST',
    body: JSON.stringify({ version }),
  })
  if (!response.ok) throw new Error(`POST /reminders/${id}/complete failed: ${response.status}`)
  return response.json()
}
