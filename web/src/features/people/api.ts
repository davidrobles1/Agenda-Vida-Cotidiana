import { apiFetch } from '../../core/api/httpClient'

/** ADR-016/FR-021 (Módulo Laboral). Mismo patrón que warranties/api.ts — owner-only, sin colaboradores. */
export interface Person {
  id: string
  ownerUserId: string
  name: string
  role?: string
  organization?: string
  version: number
  createdAt: string
  updatedAt: string
}

interface PeoplePage {
  items: Person[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function listPeople(): Promise<PeoplePage> {
  const response = await apiFetch('/people?size=100')
  if (!response.ok) throw new Error(`GET /people failed: ${response.status}`)
  return response.json()
}

export interface CreatePersonInput {
  name: string
  role?: string
  organization?: string
}

export async function createPerson(input: CreatePersonInput): Promise<Person> {
  const body: Record<string, unknown> = { name: input.name }
  if (input.role) body.role = input.role
  if (input.organization) body.organization = input.organization
  const response = await apiFetch('/people', { method: 'POST', body: JSON.stringify(body) })
  if (!response.ok) throw new Error(`POST /people failed: ${response.status}`)
  return response.json()
}

export async function deletePerson(id: string): Promise<void> {
  const response = await apiFetch(`/people/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /people/${id} failed: ${response.status}`)
}
