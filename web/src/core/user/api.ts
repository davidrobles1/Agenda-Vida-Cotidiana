import { apiFetch } from '../api/httpClient'

export interface CurrentUser {
  id: string
  email: string
  username: string
  deletionStatus: string
}

export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await apiFetch('/me')
  if (!response.ok) throw new Error(`GET /me failed: ${response.status}`)
  return response.json()
}
