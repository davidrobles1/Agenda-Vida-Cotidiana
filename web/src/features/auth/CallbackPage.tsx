import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { consumeJustRegistered, handleCallback } from '../../core/auth/authClient'
import { getCurrentUser } from '../../core/user/api'
import { resolveModeState } from '../../core/user/modes'

export function CallbackPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)
  // The authorization code is single-use (OAuth spec) — React 19 StrictMode
  // double-invokes this effect in dev, which would exchange it twice and
  // have Keycloak correctly reject the second attempt. Guard against that.
  const exchanged = useRef(false)

  useEffect(() => {
    if (exchanged.current) return
    exchanged.current = true

    handleCallback()
      .then(async () => {
        // ADR-015(d)/FR-015: post-login lands on Calendario, not Home/Tareas
        // (supersedes the previous design-system.md §7 landing note — see
        // ADR-015's own text on that). A registration that just happened
        // (register()'s `vc_just_registered` flag, see authClient.ts) always
        // goes to onboarding first — FR-014 requires picking at least one
        // mode before the app is otherwise usable. An ordinary login checks
        // the real GET /me modes (with modes.ts's documented fallback for
        // while the backend doesn't return them yet) and only detours to
        // onboarding if the account genuinely has neither mode enabled.
        if (consumeJustRegistered()) {
          navigate('/onboarding', { replace: true })
          return
        }
        try {
          const user = await getCurrentUser()
          const state = resolveModeState(user)
          navigate(state.personalEnabled || state.laboralEnabled ? '/calendar' : '/onboarding', { replace: true })
        } catch {
          // Fail open to Calendario rather than trap the user on /callback —
          // same posture as AppShell's own greeting-name fetch.
          navigate('/calendar', { replace: true })
        }
      })
      .catch((e) => setError(e instanceof Error ? e.message : 'Login failed'))
  }, [navigate])

  if (error) return <p role="alert">{error}</p>
  return <p>Signing in…</p>
}
