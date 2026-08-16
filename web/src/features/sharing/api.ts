import { apiFetch } from '../../core/api/httpClient'

export interface ReminderShare {
  id: string
  reminderId: string
  collaboratorUserId: string
  status: 'ACTIVE' | 'REVOKED'
  createdAt: string
  revokedAt?: string
}

export interface Invitation {
  id: string
  reminderId: string
  invitedEmail?: string
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'EXPIRED' | 'CANCELLED'
  expiresAt: string
  createdAt: string
}

interface SharesAndInvitations {
  shares: ReminderShare[]
  invitations: Invitation[]
}

interface InvitationsPage {
  items: Invitation[]
}

export async function listSharesAndInvitations(reminderId: string): Promise<SharesAndInvitations> {
  const response = await apiFetch(`/reminders/${reminderId}/shares`)
  if (!response.ok) throw new Error(`GET /reminders/${reminderId}/shares failed: ${response.status}`)
  return response.json()
}

export async function createInvitation(
  reminderId: string,
  recipient: { email: string } | { username: string },
): Promise<Invitation> {
  const response = await apiFetch(`/reminders/${reminderId}/shares`, {
    method: 'POST',
    body: JSON.stringify(recipient),
  })
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.code ?? `POST /reminders/${reminderId}/shares failed: ${response.status}`)
  }
  return response.json()
}

export async function revokeShare(reminderId: string, shareId: string): Promise<void> {
  const response = await apiFetch(`/reminders/${reminderId}/shares/${shareId}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE share failed: ${response.status}`)
}

export async function cancelInvitation(invitationId: string): Promise<void> {
  const response = await apiFetch(`/invitations/${invitationId}`, { method: 'DELETE' })
  if (!response.ok) throw new Error(`DELETE invitation failed: ${response.status}`)
}

export async function listMyInvitations(): Promise<Invitation[]> {
  const response = await apiFetch('/me/invitations')
  if (!response.ok) throw new Error(`GET /me/invitations failed: ${response.status}`)
  const page: InvitationsPage = await response.json()
  return page.items
}

export async function acceptInvitation(invitationId: string): Promise<ReminderShare> {
  const response = await apiFetch(`/invitations/${invitationId}/accept`, { method: 'POST' })
  if (!response.ok) throw new Error(`POST accept failed: ${response.status}`)
  return response.json()
}

export async function rejectInvitation(invitationId: string): Promise<Invitation> {
  const response = await apiFetch(`/invitations/${invitationId}/reject`, { method: 'POST' })
  if (!response.ok) throw new Error(`POST reject failed: ${response.status}`)
  return response.json()
}
