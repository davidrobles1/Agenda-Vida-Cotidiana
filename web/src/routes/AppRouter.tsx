import { useSyncExternalStore } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { getAuthStatus, subscribe, type AuthStatus } from '../core/auth/authClient'
import { SessionRestoring } from '../core/auth/SessionRestoring'
import { PersonalLayout, LaboralLayout } from '../core/user/ActiveModeContext'
import { AuthGateway } from '../features/auth/AuthGateway'
import { CallbackPage } from '../features/auth/CallbackPage'
import { OnboardingPage } from '../features/onboarding/OnboardingPage'
import { SettingsPage } from '../features/settings/SettingsPage'
import { HomePage } from '../features/home/HomePage'
import { CalendarPage } from '../features/calendar/CalendarPage'
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
import { AgendaPage } from '../features/laboral/AgendaPage'
import { TareasPage } from '../features/laboral/TareasPage'
import { TareaDetallePage } from '../features/laboral/TareaDetallePage'
import { InboxPage } from '../features/laboral/InboxPage'
import { PeoplePage } from '../features/people/PeoplePage'
import { ProjectsPage } from '../features/projects/ProjectsPage'
import { CommitmentsPage } from '../features/commitments/CommitmentsPage'
import { ObjectivesPage } from '../features/objectives/ObjectivesPage'
import { RoutinesPage } from '../features/routines/RoutinesPage'

function useAuthStatus(): AuthStatus {
  return useSyncExternalStore(subscribe, getAuthStatus)
}

/**
 * El estado `restoring` es la diferencia clave respecto a la versión
 * anterior, que solo preguntaba `isAuthenticated()`: al arrancar, la
 * respuesta todavía no se sabe (authClient está comprobando la sesión SSO
 * en un iframe oculto). Contestar "no" mientras tanto es lo que mandaba al
 * login en cada recarga aun teniendo sesión válida.
 */
function RequireAuth({ children }: { children: React.ReactElement }) {
  const status = useAuthStatus()
  if (status === 'restoring') return <SessionRestoring />
  return status === 'authenticated' ? children : <Navigate to="/" replace />
}

/**
 * Misma espera para "/": si la sesión se recupera, no llega ni a verse y se
 * entra directo a Calendario — el mismo destino que usa CallbackPage tras un
 * login normal (ADR-015(d)/FR-015).
 *
 * UX-016: cuando no hay sesión, esta ruta ya no dibuja una portada propia
 * (`LoginPage`, retirada) sino que manda directo al formulario de Keycloak,
 * que ahora lleva la identidad visual del portal. Ver `AuthGateway`.
 */
function LoginRoute() {
  const status = useAuthStatus()
  if (status === 'restoring') return <SessionRestoring />
  return status === 'authenticated' ? <Navigate to="/calendar" replace /> : <AuthGateway />
}

export function AppRouter() {
  return (
    <Routes>
      <Route path="/" element={<LoginRoute />} />
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
      {/* Rutas de `RemindersPage` retiradas (2026-08-28, pedido explícito
          del usuario: el botón "Nuevo", su destino y su contenido dejan de
          existir a nivel visual). Eran tres — la plana heredada y las dos
          mode-scoped de abajo. `features/reminders/RemindersPage.tsx` sigue
          en el repositorio, ya sin enrutar: no se borran componentes, y los
          REMINDER siguen siendo datos vivos que se ven en el Calendario y en
          Hoy. */}
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
        {/* "Agenda" (2026-08-28): la ruta no cambia — sigue siendo
            `/laboral/calendar`, el mismo enlace del navbar — pero ahora
            renderiza `AgendaPage`, la vista semanal del prototipo aprobado
            ("Agenda Laboral", artifact fca1566a). `CalendarPage` queda
            intacta y la siguen usando Personal (`/personal/calendar`) y el
            Calendario general (`/calendar`): el propio artifact modela esas
            tres como pantallas distintas, así que rehacer el componente
            compartido habría cambiado las tres a la vez. */}
        <Route path="calendar" element={<AgendaPage />} />
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
        {/* "Tareas" se retiró el 2026-08-28 y se restauró el 2026-08-29,
            ambas veces por pedido explícito del usuario ("debe estar siempre
            en este módulo"). Vuelve a su sitio del prototipo: tercera
            sección, entre Agenda y Personas. */}
        <Route path="tasks" element={<TareasPage />} />
        {/* Detalle de una Tarea (2026-08-29). El prototipo lo modela como
            pantalla propia con breadcrumb `Laboral / Tareas / <título>`, y
            un breadcrumb solo tiene sentido sobre navegación real: es lo que
            sitúa al usuario y lo devuelve al listado. */}
        <Route path="tasks/:taskId" element={<TareaDetallePage />} />
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
