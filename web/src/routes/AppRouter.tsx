import { useSyncExternalStore } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { isAuthenticated, subscribe } from '../core/auth/authClient'
import { LoginPage } from '../features/auth/LoginPage'
import { CallbackPage } from '../features/auth/CallbackPage'
import { HomePage } from '../features/home/HomePage'
import { CalendarPage } from '../features/calendar/CalendarPage'
import { RemindersPage } from '../features/reminders/RemindersPage'
import { InvitationsPage } from '../features/sharing/InvitationsPage'
import { NotificationsPage } from '../features/notifications/NotificationsPage'
import { DocumentsPage } from '../features/documents/DocumentsPage'
import { InventoryPage } from '../features/inventory/InventoryPage'
import { WarrantiesPage } from '../features/warranties/WarrantiesPage'
import { MaintenancePage } from '../features/maintenance/MaintenancePage'
import { SubscriptionsPage } from '../features/subscriptions/SubscriptionsPage'
import { FamilyPage } from '../features/family/FamilyPage'

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
        path="/home"
        element={
          <RequireAuth>
            <HomePage />
          </RequireAuth>
        }
      />
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
        path="/calendar"
        element={
          <RequireAuth>
            <CalendarPage />
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
      <Route
        path="/documents"
        element={
          <RequireAuth>
            <DocumentsPage />
          </RequireAuth>
        }
      />
      <Route
        path="/inventory"
        element={
          <RequireAuth>
            <InventoryPage />
          </RequireAuth>
        }
      />
      <Route
        path="/warranties"
        element={
          <RequireAuth>
            <WarrantiesPage />
          </RequireAuth>
        }
      />
      <Route
        path="/maintenance"
        element={
          <RequireAuth>
            <MaintenancePage />
          </RequireAuth>
        }
      />
      <Route
        path="/subscriptions"
        element={
          <RequireAuth>
            <SubscriptionsPage />
          </RequireAuth>
        }
      />
      <Route
        path="/family"
        element={
          <RequireAuth>
            <FamilyPage />
          </RequireAuth>
        }
      />
    </Routes>
  )
}
