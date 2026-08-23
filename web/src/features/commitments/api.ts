import { apiFetch } from '../../core/api/httpClient'

/**
 * ADR-016/FR-025/FR-027 (Módulo Laboral). "Seguimientos" (direction=MINE) y
 * "Esperando" (direction=THEIRS) son la misma entidad, filtrada — no dos
 * recursos separados. Mismo patrón owner-only que warranties/api.ts.
 */
export type CommitmentDirection = 'MINE' | 'THEIRS'
export type CommitmentStatus = 'OPEN' | 'DONE'

export interface Commitment {
  id: string
  ownerUserId: string
  personId: string
  projectId?: string
  description: string
  direction: CommitmentDirection
  dueAt: string
  status: CommitmentStatus
  originReminderId?: string
  version: number
  createdAt: string
  updatedAt: string
}

interface CommitmentsPage {
  items: Commitment[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function listCommitments(direction?: CommitmentDirection): Promise<CommitmentsPage> {
  const query = direction ? `&direction=${direction}` : ''
  const response = await apiFetch(`/commitments?size=100${query}`)
  if (!response.ok) throw new Error(`GET /commitments failed: ${response.status}`)
  return response.json()
}

export interface CreateCommitmentInput {
  personId: string
  description: string
  direction: CommitmentDirection
  dueAt: string
  projectId?: string
  originReminderId?: string
}

export async function createCommitment(input: CreateCommitmentInput): Promise<Commitment> {
  const body: Record<string, unknown> = {
    personId: input.personId,
    description: input.description,
    direction: input.direction,
    dueAt: input.dueAt,
  }
  if (input.projectId) body.projectId = input.projectId
  if (input.originReminderId) body.originReminderId = input.originReminderId
  const response = await apiFetch('/commitments', { method: 'POST', body: JSON.stringify(body) })
  if (!response.ok) throw new Error(`POST /commitments failed: ${response.status}`)
  return response.json()
}

export async function resolveCommitment(id: string, version: number): Promise<Commitment> {
  const response = await apiFetch(`/commitments/${id}/resolve`, {
    method: 'POST',
    body: JSON.stringify({ version }),
  })
  if (!response.ok) throw new Error(`POST /commitments/${id}/resolve failed: ${response.status}`)
  return response.json()
}

export async function rescheduleCommitment(id: string, dueAt: string, version: number): Promise<Commitment> {
  const response = await apiFetch(`/commitments/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ dueAt, version }),
  })
  if (!response.ok) throw new Error(`PATCH /commitments/${id} failed: ${response.status}`)
  return response.json()
}

export async function deleteCommitment(id: string): Promise<void> {
  const response = await apiFetch(`/commitments/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /commitments/${id} failed: ${response.status}`)
}
