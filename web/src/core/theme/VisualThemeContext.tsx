import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'

export type VisualTheme = 'editorial' | 'minimal' | 'productivity' | 'organic'

export const VISUAL_THEMES: Array<{ id: VisualTheme; label: string; tagline: string; swatch: string }> = [
  { id: 'editorial', label: 'Editorial', tagline: 'Agenda de papel premium', swatch: '#2c5f8c' },
  { id: 'minimal', label: 'Premium Minimal', tagline: 'Escala dramática', swatch: '#23425f' },
  { id: 'productivity', label: 'Modern Productivity', tagline: 'Centro de control', swatch: '#2c5f8c' },
  { id: 'organic', label: 'Organic / Human', tagline: 'Formas vivas', swatch: '#5b7a5e' },
]

const STORAGE_KEY = 'vidacotidiana.visualTheme'
const DEFAULT_THEME: VisualTheme = 'editorial'

function isVisualTheme(value: string | null): value is VisualTheme {
  return value === 'editorial' || value === 'minimal' || value === 'productivity' || value === 'organic'
}

interface VisualThemeContextValue {
  theme: VisualTheme
  setTheme: (theme: VisualTheme) => void
}

const VisualThemeContext = createContext<VisualThemeContextValue | null>(null)

/**
 * UX-014: client-only visual identity preference for the post-login portal
 * (Sidebar/Calendario/Notas/Tareas) — Editorial/Premium Minimal/Modern
 * Productivity/Organic-Human. Deliberately NOT backend-synced like
 * ModeContext's personalEnabled/laboralEnabled: there is no product concept
 * of "the account's visual theme" server-side, this is purely a local
 * rendering preference, so localStorage is the correct persistence layer
 * (same reasoning that already applies to nothing else in this app — every
 * other cross-session value here really does live on the backend, this is
 * the first one that legitimately shouldn't).
 *
 * Applied as a class on AppShell's `.shell` root (see AppShell.tsx), the
 * same mechanism `laboral-theme` already uses — never touches `:root`, so
 * LoginPage/Keycloak stay completely unaffected.
 */
export function VisualThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setThemeState] = useState<VisualTheme>(() => {
    // Storage access can throw for reasons outside our control (Safari
    // Private Browsing's historical zero-quota, storage disabled by an
    // extension/corporate policy, a sandboxed embedding context, quota
    // exhausted by unrelated site data). The theme preference is a
    // best-effort convenience, never a requirement to render — a failure
    // here must fall back to the default, not crash the app.
    try {
      const stored = typeof window !== 'undefined' ? window.localStorage.getItem(STORAGE_KEY) : null
      return isVisualTheme(stored) ? stored : DEFAULT_THEME
    } catch {
      return DEFAULT_THEME
    }
  })

  useEffect(() => {
    try {
      window.localStorage.setItem(STORAGE_KEY, theme)
    } catch {
      // Persistence is best-effort: the theme still applies for this
      // session via React state even if it can't be saved for next time.
    }
  }, [theme])

  const setTheme = useCallback((next: VisualTheme) => {
    setThemeState(next)
  }, [])

  return <VisualThemeContext.Provider value={{ theme, setTheme }}>{children}</VisualThemeContext.Provider>
}

export function useVisualTheme(): VisualThemeContextValue {
  const ctx = useContext(VisualThemeContext)
  if (!ctx) throw new Error('useVisualTheme must be used within a VisualThemeProvider')
  return ctx
}
