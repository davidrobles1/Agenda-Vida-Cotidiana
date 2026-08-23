import { createContext, useContext, type ReactNode } from 'react'
import { Outlet } from 'react-router-dom'
import type { Mode } from './modes'

/**
 * ADR-015/UX-012: which mode's navbar the current route is rendered inside —
 * `null` outside any mode (the general Calendario, legacy bare routes like
 * `/reminders`/`/home`, and the untouched scaffolding modules from UX-006).
 * Set by the two layout routes below (AppRouter.tsx wraps `/personal/*` and
 * `/laboral/*` with them), read by AppShell.tsx (to pick the 4-item navbar +
 * color theme) and by useModePath() (so pages reused across modes —
 * HomePage/CalendarPage's internal navigate() calls — stay inside the same
 * mode instead of dropping the user back onto an unthemed legacy route).
 */
const ActiveModeContext = createContext<Mode | null>(null)

export function ActiveModeProvider({ mode, children }: { mode: Mode; children: ReactNode }) {
  return <ActiveModeContext.Provider value={mode}>{children}</ActiveModeContext.Provider>
}

export function useActiveMode(): Mode | null {
  return useContext(ActiveModeContext)
}

/** Route layout for `/personal/*` — see AppRouter.tsx. */
export function PersonalLayout() {
  return (
    <ActiveModeProvider mode="PERSONAL">
      <Outlet />
    </ActiveModeProvider>
  )
}

/** Route layout for `/laboral/*` — see AppRouter.tsx. */
export function LaboralLayout() {
  return (
    <ActiveModeProvider mode="LABORAL">
      <Outlet />
    </ActiveModeProvider>
  )
}

type ModePathKind = 'home' | 'reminders' | 'shared' | 'calendar'

/**
 * Navigation helper for pages reused across modes (Home/Calendar's own
 * internal `navigate('/reminders')`-style links) — resolves to the
 * mode-scoped route when inside a mode, or the original legacy bare route
 * otherwise (general Calendario, or a mode-less/legacy page). Keeps a click
 * on "Ver agenda" from /personal/home landing on the un-themed, 10-item-nav
 * /reminders instead of /personal/reminders.
 */
export function useModePath(kind: ModePathKind): string {
  const mode = useActiveMode()
  if (!mode) {
    return kind === 'shared' ? '/invitations' : kind === 'home' ? '/home' : `/${kind}`
  }
  const prefix = mode === 'PERSONAL' ? '/personal' : '/laboral'
  return kind === 'shared' ? `${prefix}/shared` : `${prefix}/${kind}`
}
