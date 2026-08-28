import { apiFetch } from '../../core/api/httpClient'

/**
 * ADR-016 Fase 3e1/FR-031 (Módulo Laboral). Mismo patrón que people/api.ts —
 * owner-only, sin colaboradores. Entidad independiente en este incremento:
 * sin vínculo a Persona/Proyecto (fuera de alcance explícito de FR-031).
 */
export interface Objective {
  id: string
  ownerUserId: string
  title: string
  targetValue?: number
  currentValue: number
  deadline?: string
  completed: boolean
  version: number
  createdAt: string
  updatedAt: string
}

interface ObjectivesPage {
  items: Objective[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function listObjectives(): Promise<ObjectivesPage> {
  const response = await apiFetch('/objectives?size=100')
  if (!response.ok) throw new Error(`GET /objectives failed: ${response.status}`)
  return response.json()
}

export interface CreateObjectiveInput {
  title: string
  targetValue?: number
  deadline?: string
}

export async function createObjective(input: CreateObjectiveInput): Promise<Objective> {
  const body: Record<string, unknown> = { title: input.title }
  if (input.targetValue !== undefined) body.targetValue = input.targetValue
  if (input.deadline) body.deadline = input.deadline
  const response = await apiFetch('/objectives', { method: 'POST', body: JSON.stringify(body) })
  if (!response.ok) throw new Error(`POST /objectives failed: ${response.status}`)
  return response.json()
}

export interface UpdateObjectiveInput {
  title?: string
  targetValue?: number
  currentValue?: number
  deadline?: string
  completed?: boolean
  version: number
}

export async function updateObjective(id: string, input: UpdateObjectiveInput): Promise<Objective> {
  const response = await apiFetch(`/objectives/${id}`, { method: 'PATCH', body: JSON.stringify(input) })
  if (!response.ok) throw new Error(`PATCH /objectives/${id} failed: ${response.status}`)
  return response.json()
}

export async function deleteObjective(id: string): Promise<void> {
  const response = await apiFetch(`/objectives/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /objectives/${id} failed: ${response.status}`)
}
