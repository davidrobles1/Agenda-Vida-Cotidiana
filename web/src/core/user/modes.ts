import { apiFetch } from '../api/httpClient'
import type { CurrentUser } from './api'

export type Mode = 'PERSONAL' | 'LABORAL'

export interface ModeState {
  personalEnabled: boolean
  laboralEnabled: boolean
  /** True once GET /me actually returned personalEnabled/laboralEnabled — see
      resolveModeState() below for what happens while it doesn't. */
  backendSupportsModes: boolean
}

/**
 * ADR-015 (Documentacion/22-decision-log.md) / FR-014-FR-016.
 *
 * MOCK / ASSUMPTION (2026-08-18): the backend block implementing ADR-015 was
 * still in progress when this was written — verified directly against
 * `backend/src/main/java/com/vidacotidiana/user/api/UserController.java`,
 * whose `GET /me` response (`UserResponse`) only has id/email/username/
 * deletionStatus, no personalEnabled/laboralEnabled yet. When those two keys
 * are absent from the JSON, `user.personalEnabled`/`user.laboralEnabled` are
 * `undefined` (not `false`) — this function treats that specific case as "a
 * pre-ADR-015 account", i.e. personalEnabled=true/laboralEnabled=false,
 * mirroring the real backfill migration the backend team is applying
 * (`personal_enabled = true` for existing rows) so this app never sends an
 * existing, already-real user into onboarding just because the backend
 * hasn't shipped the fields yet. `backendSupportsModes` tells callers whether
 * this fallback fired, so UI that depends on it (Ajustes, onboarding gating)
 * can be honest about it rather than pretend it's live. Once GET /me returns
 * both booleans for real, this fallback never triggers again — the actual
 * values are used unconditionally.
 */
export function resolveModeState(user: CurrentUser): ModeState {
  const backendSupportsModes =
    typeof user.personalEnabled === 'boolean' && typeof user.laboralEnabled === 'boolean'
  return {
    personalEnabled: user.personalEnabled ?? true,
    laboralEnabled: user.laboralEnabled ?? false,
    backendSupportsModes,
  }
}

/**
 * POST /me/modes — activates (never deactivates; ADR-015 leaves deactivation
 * explicitly TBD) one mode for the current user. Contract as specified by the
 * parallel backend task implementing ADR-015: body `{ mode: 'PERSONAL' |
 * 'LABORAL' }`, response is the updated user object (same shape as GET /me,
 * with personalEnabled/laboralEnabled reflecting the activation).
 *
 * If the endpoint doesn't exist yet (404) or the backend isn't reachable,
 * this throws — see ModeContext.tsx's `activateMode` for the local-only
 * mock fallback callers use in that case.
 */
export async function activateModeApi(mode: Mode): Promise<CurrentUser> {
  const response = await apiFetch('/me/modes', {
    method: 'POST',
    body: JSON.stringify({ mode }),
  })
  if (!response.ok) throw new Error(`POST /me/modes failed: ${response.status}`)
  return response.json()
}
