import { apiFetch } from '../../core/api/httpClient'

/**
 * Familia (ADR-025 §1).
 *
 * El backend nunca devuelve correos por aquí: la búsqueda y los listados
 * hablan de nombre de usuario, que es el identificador que la propia persona
 * comparte. El correo sigue protegido por SEC-001.
 */

/** Mínimo de caracteres antes de consultar. El backend valida lo mismo. */
export const MIN_SEARCH_LENGTH = 5

export type RelationState = 'NONE' | 'INVITATION_SENT' | 'INVITATION_RECEIVED' | 'FAMILY'

export interface UserSearchResult {
  userId: string
  username: string
  relation: RelationState
}

export interface FamilyMember {
  userId: string
  username: string
  since: string
}

export interface FamilyInvitation {
  id: string
  counterpartUserId?: string
  counterpartUsername?: string
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED'
  createdAt: string
}

async function readError(response: Response, fallback: string): Promise<never> {
  const body = await response.json().catch(() => null)
  throw new Error(body?.detail ?? body?.message ?? body?.code ?? fallback)
}

/**
 * Busca personas por nombre de usuario.
 *
 * Corta ANTES de llamar si no se llega al mínimo: la petición que no se hace
 * es la más barata de todas, y así el servidor no recibe consultas que ya sabe
 * que va a rechazar.
 */
export async function searchUsers(query: string): Promise<UserSearchResult[]> {
  const term = query.trim()
  if (term.length < MIN_SEARCH_LENGTH) return []
  const response = await apiFetch(`/users/search?q=${encodeURIComponent(term)}`)
  if (!response.ok) await readError(response, 'No se pudo buscar.')
  return response.json()
}

export async function listMembers(): Promise<FamilyMember[]> {
  const response = await apiFetch('/family/members')
  if (!response.ok) await readError(response, 'No se pudieron cargar los integrantes.')
  return response.json()
}

export async function removeMember(userId: string): Promise<void> {
  const response = await apiFetch(`/family/members/${userId}`, { method: 'DELETE' })
  if (!response.ok) await readError(response, 'No se pudo quitar a esa persona.')
}

export async function listSentInvitations(): Promise<FamilyInvitation[]> {
  const response = await apiFetch('/family/invitations/sent')
  if (!response.ok) await readError(response, 'No se pudieron cargar las invitaciones enviadas.')
  return response.json()
}

export async function listReceivedInvitations(): Promise<FamilyInvitation[]> {
  const response = await apiFetch('/family/invitations/received')
  if (!response.ok) await readError(response, 'No se pudieron cargar las invitaciones recibidas.')
  return response.json()
}

export async function invite(userId: string): Promise<FamilyInvitation> {
  const response = await apiFetch('/family/invitations', {
    method: 'POST',
    body: JSON.stringify({ userId }),
  })
  if (!response.ok) await readError(response, 'No se pudo enviar la invitación.')
  return response.json()
}

export async function acceptInvitation(id: string): Promise<void> {
  const response = await apiFetch(`/family/invitations/${id}/accept`, { method: 'POST' })
  if (!response.ok) await readError(response, 'No se pudo aceptar la invitación.')
}

export async function rejectInvitation(id: string): Promise<void> {
  const response = await apiFetch(`/family/invitations/${id}/reject`, { method: 'POST' })
  if (!response.ok) await readError(response, 'No se pudo rechazar la invitación.')
}

export async function cancelInvitation(id: string): Promise<void> {
  const response = await apiFetch(`/family/invitations/${id}`, { method: 'DELETE' })
  if (!response.ok) await readError(response, 'No se pudo cancelar la invitación.')
}
