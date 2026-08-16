import { useSyncExternalStore } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { isAuthenticated, subscribe } from '../core/auth/authClient'
import { LoginPage } from '../features/auth/LoginPage'
import { CallbackPage } from '../features/auth/CallbackPage'
import { RemindersPage } from '../features/reminders/RemindersPage'
import { InvitationsPage } from '../features/sharing/InvitationsPage'
import { NotificationsPage } from '../features/notifications/NotificationsPage'

function useIsAuthenticated(): boolean {
  return useSyncExternalStore(subscribe, isAuthenticated)
}

function RequireAuth({ children }: { children: React.ReactElement }) {
  const authenticated = useIsAuthenticated()
  return authenticated ? children : <Navigate to="/" replace />
}

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<LoginPage />} />
      <Route path="/callback" element={<CallbackPage />} />
      <Route
        path="/reminders"
        element={
          <RequireAuth>
            <RemindersPage />
          </RequireAuth>
        }
      />
      <Route
        path="/invitations"
        element={
          <RequireAuth>
            <InvitationsPage />
          </RequireAuth>
        }
      />
      <Route
        path="/notifications"
        element={
          <RequireAuth>
            <NotificationsPage />
          </RequireAuth>
        }
      />
    </Routes>
  )
}
