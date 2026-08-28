import { useSyncExternalStore } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { isAuthenticated, subscribe } from '../core/auth/authClient'
import { PersonalLayout, LaboralLayout } from '../core/user/ActiveModeContext'
import { LoginPage } from '../features/auth/LoginPage'
import { CallbackPage } from '../features/auth/CallbackPage'
import { OnboardingPage } from '../features/onboarding/OnboardingPage'
import { SettingsPage } from '../features/settings/SettingsPage'
import { HomePage } from '../features/home/HomePage'
import { CalendarPage } from '../features/calendar/CalendarPage'
import { RemindersPage } from '../features/reminders/RemindersPage'
import { VisionBoardPage } from '../features/visionboard/VisionBoardPage'
import { InvitationsPage } from '../features/sharing/InvitationsPage'
import { NotificationsPage } from '../features/notifications/NotificationsPage'
import { DocumentsPage } from '../features/documents/DocumentsPage'
import { InventoryPage } from '../features/inventory/InventoryPage'
import { WarrantiesPage } from '../features/warranties/WarrantiesPage'
import { MaintenancePage } from '../features/maintenance/MaintenancePage'
import { SubscriptionsPage } from '../features/subscriptions/SubscriptionsPage'
import { FamilyPage } from '../features/family/FamilyPage'
import { HoyPage } from '../features/laboral/HoyPage'
import { TareasPage } from '../features/laboral/TareasPage'
import { InboxPage } from '../features/laboral/InboxPage'
import { PeoplePage } from '../features/people/PeoplePage'
import { ProjectsPage } from '../features/projects/ProjectsPage'
import { CommitmentsPage } from '../features/commitments/CommitmentsPage'
import { ObjectivesPage } from '../features/objectives/ObjectivesPage'
import { RoutinesPage } from '../features/routines/RoutinesPage'

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
        path="/onboarding"
        element={
          <RequireAuth>
            <OnboardingPage />
          </RequireAuth>
        }
      />
      <Route
        path="/settings"
        element={
          <RequireAuth>
            <SettingsPage />
          </RequireAuth>
        }
      />
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
      {/* UX-012/ADR-015: Personal/Laboral each reuse the exact same
          Home/Calendar/Reminders/Invitations components as the legacy bare
          routes above — PersonalLayout/LaboralLayout only wrap them in
          ActiveModeProvider (core/user/ActiveModeContext.tsx) so AppShell
          picks the 4-item navbar + color theme instead of the legacy one. */}
      <Route
        path="/personal"
        element={
          <RequireAuth>
            <PersonalLayout />
          </RequireAuth>
        }
      >
        <Route path="home" element={<HomePage />} />
        <Route path="calendar" element={<CalendarPage />} />
        <Route path="reminders" element={<RemindersPage />} />
        <Route path="vision-board" element={<VisionBoardPage />} />
        <Route path="shared" element={<InvitationsPage />} />
      </Route>
      <Route
        path="/laboral"
        element={
          <RequireAuth>
            <LaboralLayout />
          </RequireAuth>
        }
      >
        <Route path="home" element={<HomePage />} />
        <Route path="calendar" element={<CalendarPage />} />
        <Route path="reminders" element={<RemindersPage />} />
        <Route path="vision-board" element={<VisionBoardPage />} />
        <Route path="shared" element={<InvitationsPage />} />
        {/* ADR-016 (Módulo Laboral, 2026-08-22): 6 pantallas nuevas del
            navbar de Laboral (§AppShell.tsx laboralNavItems) — "Agenda"
            reutiliza la ruta "calendar" de arriba, sin ruta propia. Las 5
            rutas de arriba (home/calendar/reminders/vision-board/shared)
            se dejan intactas a propósito, aunque ya ninguna tenga enlace en
            el navbar de Laboral: pedido explícito del usuario de no borrar
            módulos/rutas/componentes, solo el enlace del menú. */}
        <Route path="hoy" element={<HoyPage />} />
        <Route path="tasks" element={<TareasPage />} />
        <Route path="people" element={<PeoplePage />} />
        <Route path="projects" element={<ProjectsPage />} />
        <Route path="commitments" element={<CommitmentsPage />} />
        <Route path="inbox" element={<InboxPage />} />
        {/* ADR-016 Fase 3e1 (FR-031): Objetivos — ruta real SIN enlace en el
            navbar de Laboral (las 7 secciones núcleo quedaron cerradas en la
            Fase 2). Se llega desde "Ver todos" en la tarjeta de Hoy, mismo
            patrón de ruta-sin-enlace que Documentos/Inventario en Personal. */}
        <Route path="objectives" element={<ObjectivesPage />} />
        {/* ADR-016 Fase 3e2 (FR-032): Rutinas — misma regla que Objetivos,
            ruta real sin enlace en el navbar, alcanzable desde Hoy. */}
        <Route path="routines" element={<RoutinesPage />} />
      </Route>
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
