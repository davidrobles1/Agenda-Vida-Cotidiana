import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'

export type VisualTheme =
  // ADR-023 — las seis agendas aprobadas.
  | 'aurora' | 'lumen' | 'neo' | 'calm' | 'studio' | 'papel'
  // UX-014 — los tres que se conservan. Solo Editorial se eliminó.
  | 'minimal' | 'productivity' | 'organic'

export const VISUAL_THEMES: Array<{ id: VisualTheme; label: string; tagline: string; swatch: string }> = [
  { id: 'aurora', label: 'Aurora', tagline: 'Noche premium', swatch: '#84b0ff' },
  { id: 'lumen', label: 'Lumen', tagline: 'Aire y tipografía', swatch: '#0f1012' },
  { id: 'neo', label: 'Neo', tagline: 'Cartel de alto contraste', swatch: '#ff3b18' },
  { id: 'calm', label: 'Calm', tagline: 'Cálido y orgánico', swatch: '#7d8f68' },
  { id: 'studio', label: 'Studio', tagline: 'Revista', swatch: '#96412f' },
  { id: 'papel', label: 'Papel', tagline: 'Cuaderno', swatch: '#bb6440' },
  // UX-014: se conservan junto a las seis nuevas — el Product Owner pidió
  // eliminar únicamente Editorial.
  { id: 'minimal', label: 'Premium Minimal', tagline: 'Escala dramática', swatch: '#23425f' },
  { id: 'productivity', label: 'Modern Productivity', tagline: 'Centro de control', swatch: '#2c5f8c' },
  { id: 'organic', label: 'Organic / Human', tagline: 'Formas vivas', swatch: '#5b7a5e' },
]

/**
 * ADR-023: **Editorial** es el único tema eliminado. Una preferencia ya
 * guardada apuntaría a un tema inexistente y dejaría el portal sin clase de
 * tema, así que se traduce a Papel —el candidato más cercano en intención,
 * la misma agenda de papel cálido— en vez de descartarse en silencio.
 */
const RETIRED: Record<string, VisualTheme> = {
  editorial: 'papel',
}

const STORAGE_KEY = 'vidacotidiana.visualTheme'
const DEFAULT_THEME: VisualTheme = 'papel'

function isVisualTheme(value: string | null): value is VisualTheme {
  return VISUAL_THEMES.some((theme) => theme.id === value)
}

/** Resuelve lo que hay guardado: tema válido, tema retirado que se migra, o
    el valor por defecto. */
function resolveStored(stored: string | null): VisualTheme {
  if (isVisualTheme(stored)) return stored
  if (stored && stored in RETIRED) return RETIRED[stored]
  return DEFAULT_THEME
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
      return resolveStored(stored)
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
