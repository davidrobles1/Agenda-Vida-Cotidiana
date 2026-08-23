import { apiFetch } from '../api/httpClient'

export interface CurrentUser {
  id: string
  email: string
  username: string
  deletionStatus: string
  /** ADR-015/FR-014-FR-016 — optional because the backend block implementing
      them may not have shipped yet; see core/user/modes.ts's
      resolveModeState() for how the rest of the app treats their absence. */
  personalEnabled?: boolean
  laboralEnabled?: boolean
}

export async function getCurrentUser(): Promise<CurrentUser> {
  const response = await apiFetch('/me')
  if (!response.ok) throw new Error(`GET /me failed: ${response.status}`)
  return response.json()
}
