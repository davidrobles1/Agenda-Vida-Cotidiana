import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { isAuthenticated, subscribe } from '../auth/authClient'
import { getCurrentUser } from './api'
import { activateModeApi, resolveModeState, type Mode } from './modes'

interface ModeContextValue {
  loading: boolean
  personalEnabled: boolean
  laboralEnabled: boolean
  /** See modes.ts resolveModeState() — false while GET /me hasn't shipped
      personalEnabled/laboralEnabled yet, meaning the values above are a
      fallback, not a real read of the user's choice. */
  backendSupportsModes: boolean
  refresh: () => Promise<void>
  activateMode: (mode: Mode) => Promise<void>
}

const ModeContext = createContext<ModeContextValue | null>(null)

/**
 * ADR-015: app-wide source of truth for "which modes does this user have
 * enabled" — feeds the top selector and Ajustes (AppShell.tsx,
 * features/settings/SettingsPage.tsx). Wraps the whole router in App.tsx so
 * it's available regardless of which route is active, and re-fetches
 * whenever the auth state changes (login/logout) via authClient's
 * subscribe().
 */
export function ModeProvider({ children }: { children: ReactNode }) {
  // Optimistic defaults matching resolveModeState()'s own fallback (pre-ADR-015
  // account shape) — avoids a flash of "no modes enabled" before the first
  // GET /me resolves.
  const [personalEnabled, setPersonalEnabled] = useState(true)
  const [laboralEnabled, setLaboralEnabled] = useState(false)
  const [backendSupportsModes, setBackendSupportsModes] = useState(false)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    if (!isAuthenticated()) {
      setLoading(false)
      return
    }
    try {
      const user = await getCurrentUser()
      const state = resolveModeState(user)
      setPersonalEnabled(state.personalEnabled)
      setLaboralEnabled(state.laboralEnabled)
      setBackendSupportsModes(state.backendSupportsModes)
    } catch {
      /* Keep whatever we had — same fail-open posture as AppShell's own
         greeting-name fetch. Not fatal to the rest of the app. */
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    refresh()
    return subscribe(refresh)
  }, [refresh])

  async function activateMode(mode: Mode) {
    try {
      const user = await activateModeApi(mode)
      const state = resolveModeState(user)
      setPersonalEnabled(state.personalEnabled)
      setLaboralEnabled(state.laboralEnabled)
      setBackendSupportsModes(state.backendSupportsModes)
    } catch {
      // MOCK FALLBACK (see modes.ts activateModeApi's doc comment): POST
      // /me/modes doesn't exist on the backend yet at the time this was
      // written. Flip the flag locally only (lost on reload) so Ajustes'
      // "activate" flow and the top selector are genuinely demoable/testable
      // end to end before the real endpoint lands, instead of being dead UI.
      if (mode === 'PERSONAL') setPersonalEnabled(true)
      else setLaboralEnabled(true)
    }
  }

  return (
    <ModeContext.Provider
      value={{ loading, personalEnabled, laboralEnabled, backendSupportsModes, refresh, activateMode }}
    >
      {children}
    </ModeContext.Provider>
  )
}

export function useModeContext(): ModeContextValue {
  const ctx = useContext(ModeContext)
  if (!ctx) throw new Error('useModeContext must be used within a ModeProvider')
  return ctx
}
