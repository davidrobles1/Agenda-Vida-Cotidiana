import { apiFetch } from '../../core/api/httpClient'

/** ADR-016/FR-022 (Módulo Laboral). Mismo patrón que warranties/api.ts — owner-only, sin colaboradores. */
export interface Project {
  id: string
  ownerUserId: string
  name: string
  clientPersonId?: string
  status?: string
  deadline?: string
  version: number
  createdAt: string
  updatedAt: string
}

interface ProjectsPage {
  items: Project[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export async function listProjects(): Promise<ProjectsPage> {
  const response = await apiFetch('/projects?size=100')
  if (!response.ok) throw new Error(`GET /projects failed: ${response.status}`)
  return response.json()
}

export interface CreateProjectInput {
  name: string
  clientPersonId?: string
  status?: string
  deadline?: string
}

export async function createProject(input: CreateProjectInput): Promise<Project> {
  const body: Record<string, unknown> = { name: input.name }
  if (input.clientPersonId) body.clientPersonId = input.clientPersonId
  if (input.status) body.status = input.status
  if (input.deadline) body.deadline = input.deadline
  const response = await apiFetch('/projects', { method: 'POST', body: JSON.stringify(body) })
  if (!response.ok) throw new Error(`POST /projects failed: ${response.status}`)
  return response.json()
}

export async function deleteProject(id: string): Promise<void> {
  const response = await apiFetch(`/projects/${id}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE /projects/${id} failed: ${response.status}`)
}
