import { apiFetch } from '../../core/api/httpClient'

/**
 * ADR-016 Fase 3e2/FR-032 (Módulo Laboral). Mismo patrón que people/api.ts —
 * owner-only.
 *
 * Una Rutina NUNCA genera una Tarea ni un Compromiso (decisión explícita del
 * Product Owner) — "ejecutarla" solo avanza su próxima fecha. No existe
 * `completed`: su estado permanente es `active`.
 */
export type RoutineFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY'

export interface Routine {
  id: string
  ownerUserId: string
  title: string
  description?: string
  frequency: RoutineFrequency
  nextExecutionDate: string
  active: boolean
  version: number
  createdAt: string
  updatedAt: string
}

interface RoutinesPage {
  items: Routine[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export const FREQUENCY_LABELS: Record<RoutineFrequency, string> = {
  DAILY: 'Diaria',
  WEEKLY: 'Semanal',
  MONTHLY: 'Mensual',
}

export async function listRoutines(): Promise<RoutinesPage> {
  const response = await apiFetch('/routines?size=100')
  if (!response.ok) throw new Error(`GET /routines failed: ${response.status}`)
  return response.json()
}

export interface CreateRoutineInput {
  title: string
  description?: string
  frequency: RoutineFrequency
  nextExecutionDate: string
}

export async function createRoutine(input: CreateRoutineInput): Promise<Routine> {
  const body: Record<string, unknown> = {
    title: input.title,
    frequency: input.frequency,
    nextExecutionDate: input.nextExecutionDate,
  }
  if (input.description) body.description = input.description
  const response = await apiFetch('/routines', { method: 'POST', body: JSON.stringify(body) })
  if (!response.ok) throw new Error(`POST /routines failed: ${response.status}`)
  return response.json()
}

/**
 * UC-25: marca la ocurrencia actual como realizada. El backend avanza
 * `nextExecutionDate` un periodo **desde la fecha programada**, no desde hoy
 * (decisión del Product Owner) — el cliente no calcula nada.
 */
export async function executeRoutine(id: string, version: number): Promise<Routine> {
  const response = await apiFetch(`/routines/${id}/execute`, {
    method: 'POST',
    body: JSON.stringify({ version }),
  })
  if (!response.ok) throw new Error(`POST /routines/${id}/execute failed: ${response.status}`)
  return response.json()
}

export async function updateRoutine(id: string, input: { active?: boolean; version: number }): Promise<Routine> {
  const response = await apiFetch(`/routines/${id}`, { method: 'PATCH', body: JSON.stringify(input) })
  if (!response.ok) throw new Error(`PATCH /routines/${id} failed: ${response.status}`)
  return response.json()
}

export async function deleteRoutine(id: string): Promise<void> {
  const response = await apiFetch(`/routines/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /routines/${id} failed: ${response.status}`)
}
