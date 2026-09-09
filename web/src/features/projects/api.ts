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

export async function updateProject(
  id: string,
  input: CreateProjectInput & { clientPersonId?: string | null },
  version: number,
): Promise<Project> {
  const response = await apiFetch(`/projects/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ ...input, version }),
  })
  if (!response.ok) {
    if (response.status === 400) throw new Error(await parseErrorMessage(response, 'Los datos del proyecto no son válidos.'))
    if (response.status === 409) throw new Error('El proyecto cambió mientras lo editabas. Vuelve a abrirlo.')
    throw new Error(`PATCH /projects/${id} failed: ${response.status}`)
  }
  return response.json()
}

/* ---------------------------------------------------------------------------
   Participantes (V32) — quién más está en el proyecto, además del cliente.

   El CLIENTE no vive aquí: sigue siendo `Project.clientPersonId`. Son dos
   listas disjuntas, y el servidor rechaza mezclarlas (400).
   --------------------------------------------------------------------------- */

export type ParticipantRole = 'PROVEEDOR' | 'CONTACTO' | 'COLABORADOR' | 'OTRO'

export const PARTICIPANT_ROLES: ParticipantRole[] = ['PROVEEDOR', 'CONTACTO', 'COLABORADOR', 'OTRO']

export const PARTICIPANT_ROLE_LABELS: Record<ParticipantRole, string> = {
  PROVEEDOR: 'Proveedor',
  CONTACTO: 'Contacto',
  COLABORADOR: 'Colaborador',
  OTRO: 'Otro',
}

export interface ProjectParticipant {
  id: string
  projectId: string
  personId: string
  role: ParticipantRole
  createdAt: string
}

async function parseErrorMessage(response: Response, fallback: string): Promise<string> {
  const body = await response.json().catch(() => null)
  return body?.message ?? fallback
}

export async function listParticipants(projectId: string): Promise<ProjectParticipant[]> {
  const response = await apiFetch(`/projects/${projectId}/participants`)
  if (!response.ok) throw new Error(`GET /projects/${projectId}/participants failed: ${response.status}`)
  return response.json()
}

/** Todas las participaciones del usuario: responde "¿en cuántos proyectos
    está esta persona?" con UNA llamada, no una por persona. */
export async function listAllParticipations(): Promise<ProjectParticipant[]> {
  const response = await apiFetch('/projects/participations')
  if (!response.ok) throw new Error(`GET /projects/participations failed: ${response.status}`)
  return response.json()
}

export async function addParticipant(
  projectId: string,
  personId: string,
  role: ParticipantRole,
): Promise<ProjectParticipant> {
  const response = await apiFetch(`/projects/${projectId}/participants`, {
    method: 'POST',
    body: JSON.stringify({ personId, role }),
  })
  if (!response.ok) {
    // El 400 y el 409 llevan un mensaje escrito para el usuario ("ya es el
    // cliente", "ya participa"): descartarlo por un texto genérico le quitaría
    // justo lo que necesita para corregir.
    if (response.status === 400 || response.status === 409) {
      throw new Error(await parseErrorMessage(response, 'No se pudo añadir a esa persona.'))
    }
    throw new Error(`POST /projects/${projectId}/participants failed: ${response.status}`)
  }
  return response.json()
}

export async function removeParticipant(projectId: string, participantId: string): Promise<void> {
  const response = await apiFetch(`/projects/${projectId}/participants/${participantId}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE participant failed: ${response.status}`)
}
