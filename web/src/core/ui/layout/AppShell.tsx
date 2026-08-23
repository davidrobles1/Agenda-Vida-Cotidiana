import { useEffect, useState, type ReactNode } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { logout } from '../../auth/authClient'
import { getCurrentUser } from '../../user/api'
import { useModePath } from '../../user/ActiveModeContext'
import { useModeContext } from '../../user/ModeContext'
import { useVisualTheme } from '../../theme/VisualThemeContext'
import { ThemeSwitcher } from '../../theme/ThemeSwitcher'
import {
  IconBell,
  IconCalendar,
  IconFolder,
  IconHome,
  IconInbox,
  IconInventory,
  IconLayoutGrid,
  IconLogout,
  IconMenu,
  IconPlus,
  IconRepeat,
  IconSearch,
  IconSettings,
  IconShared,
  IconShield,
  IconTasks,
  IconUsers,
  IconWrench,
} from '../icons'
import styles from './AppShell.module.css'

type SidebarContext = 'GENERAL' | 'PERSONAL' | 'LABORAL'

/**
 * Corrección de navegación (2026-08-18, pedido explícito del usuario):
 * Personal debe contener **todas** las opciones que antes vivían en el
 * sidebar "legacy" plano — nada se pierde, solo se reorganiza bajo un
 * único contexto, para que la campanita/otras pantallas nunca disparen un
 * segundo TabNav ni dupliquen opciones. Los 4 ítems mode-scoped
 * (`/personal/*`) van primero; los 6 módulos de maqueta (`UX-006`, mock
 * data) siguen en sus rutas planas de siempre — no se reestructuró
 * `AppRouter.tsx`, por instrucción explícita de no tocar nada fuera de
 * navegación/visibilidad. Finanzas/"IA Asistente" siguen ausentes
 * (CLAUDE.md, V1). "Ajustes" sigue en el área de cuenta, no aquí.
 */
const personalNavItems = [
  { to: '/personal/home', label: 'Inicio', icon: IconHome },
  { to: '/personal/calendar', label: 'Calendario personal', icon: IconCalendar },
  { to: '/personal/vision-board', label: 'Vision Board', icon: IconLayoutGrid },
  { to: '/personal/shared', label: 'Compartidos', icon: IconShared },
  { to: '/documents', label: 'Documentos', icon: IconFolder },
  { to: '/inventory', label: 'Inventario', icon: IconInventory },
  { to: '/warranties', label: 'Garantías', icon: IconShield },
  { to: '/maintenance', label: 'Mantenimiento', icon: IconWrench },
  { to: '/subscriptions', label: 'Suscripciones', icon: IconRepeat },
  { to: '/family', label: 'Familia', icon: IconUsers },
]

/**
 * ADR-016 (Módulo Laboral, 2026-08-22): Laboral pasa de 4 ítems genéricos a
 * las 7 secciones núcleo del espacio profesional (FR-021 a FR-028) —
 * "Agenda" reutiliza la misma ruta/componente que antes era "Calendario
 * laboral" (`/laboral/calendar` → `CalendarPage`, sin cambios), solo cambia
 * la etiqueta. Pedido explícito del usuario: esto es *solo* un cambio de
 * navegación — Vision Board y Compartidos siguen existiendo exactamente
 * igual (rutas `/laboral/vision-board`/`/laboral/shared` en `AppRouter.tsx`
 * intactas, mismos componentes, Personal sin tocar), simplemente ya no
 * tienen un enlace en *este* navbar. Mismo patrón ya usado por
 * Documentos/Inventario/etc.: rutas reales sin enlace de Laboral.
 */
const laboralNavItems = [
  { to: '/laboral/hoy', label: 'Hoy', icon: IconHome },
  { to: '/laboral/calendar', label: 'Agenda', icon: IconCalendar },
  { to: '/laboral/tasks', label: 'Tareas', icon: IconTasks },
  { to: '/laboral/people', label: 'Personas', icon: IconUsers },
  { to: '/laboral/projects', label: 'Proyectos', icon: IconFolder },
  { to: '/laboral/commitments', label: 'Seguimientos', icon: IconRepeat },
  { to: '/laboral/inbox', label: 'Inbox', icon: IconInbox },
]

interface AppShellProps {
  title: string
  subtitle?: string
  children: ReactNode
}

export function AppShell({ title, subtitle, children }: AppShellProps) {
  const [name, setName] = useState<string | null>(null)
  const location = useLocation()
  const { personalEnabled, laboralEnabled } = useModeContext()
  const { theme } = useVisualTheme()

  const modeRemindersPath = useModePath('reminders')

  // Corrección de navegación (2026-08-18): el contexto del sidebar ya no se
  // deriva únicamente del wrapper de ruta activo (`ActiveModeContext`, que
  // solo cubre `/personal/*`/`/laboral/*`) — se deriva de a qué contexto
  // "pertenece" la pantalla actual. Rutas mode-scoped y el Calendario
  // general (`/calendar`) fijan el contexto explícitamente; cualquier otra
  // ruta plana (Notificaciones, Ajustes, Documentos, Inventario...)
  // conserva el último contexto real — así la campanita nunca "saca" al
  // usuario de Personal ni abre un TabNav distinto (pedido explícito).
  const [sidebarContext, setSidebarContext] = useState<SidebarContext>('PERSONAL')

  useEffect(() => {
    setSidebarContext((prev) => {
      if (location.pathname.startsWith('/personal')) return 'PERSONAL'
      if (location.pathname.startsWith('/laboral')) return 'LABORAL'
      if (location.pathname === '/calendar') return 'GENERAL'
      return prev
    })
  }, [location.pathname])

  // UX-011: sidebar open/closed toggle — web-only (not an Android/iOS
  // concern). A plain boolean + CSS transition, not Motion: the target
  // width/height differs by breakpoint (desktop collapses width, the
  // ≤900px row layout collapses height instead — see AppShell.module.css),
  // which a CSS transition handles for free via media queries; Motion
  // would need those breakpoint values duplicated into JS for no real gain,
  // since there's no gesture or physics need here.
  const [sidebarOpen, setSidebarOpen] = useState(true)

  // Corrección de navegación (2026-08-18): salir de Calendario General
  // "elimina el bloqueo" — el sidebar vuelve a estar disponible por
  // default la próxima vez que se entra a Personal/Laboral, en vez de
  // arrastrar un estado cerrado que el usuario nunca pidió.
  useEffect(() => {
    if (sidebarContext !== 'GENERAL') setSidebarOpen(true)
  }, [sidebarContext])

  useEffect(() => {
    getCurrentUser()
      .then((user) => setName(user.username))
      .catch(() => {
        /* Sidebar still works without a greeting name — not fatal. */
      })
  }, [])

  const initial = name ? name.charAt(0).toUpperCase() : '…'

  // ADR-015 (b/d/e), FR-015: Calendario General no tiene TabNav lateral —
  // "bloqueado" mientras el usuario permanezca en este contexto: ni el
  // botón para abrirlo se renderiza (pedido explícito: "bloquea cualquier
  // mecanismo que permita volver a abrirlo").
  const isGeneralContext = sidebarContext === 'GENERAL'
  const navItems = sidebarContext === 'LABORAL' ? laboralNavItems : sidebarContext === 'PERSONAL' ? personalNavItems : []
  const sidebarVisible = !isGeneralContext && sidebarOpen

  return (
    <div className={`${styles.shell} theme-${theme} ${sidebarContext === 'LABORAL' ? 'laboral-theme' : ''}`}>
      {!isGeneralContext && (
        <aside
          id="app-sidebar"
          className={`${styles.sidebar} ${sidebarVisible ? '' : styles.sidebarClosed}`}
          inert={!sidebarVisible}
        >
            <div className={styles.logo}>
              <span className={styles.logoMark} data-testid="app-logo-mark">
                VC
              </span>
              <span>Vida Cotidiana</span>
            </div>

          <nav className={styles.nav}>
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                aria-label={item.label}
                className={({ isActive }) => `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`}
              >
                <item.icon width={18} height={18} />
                <span>{item.label}</span>
              </NavLink>
            ))}
          </nav>
        </aside>
      )}

      <div className={styles.main}>
        <header className={styles.topBar}>
          <div className={styles.titleGroup}>
            {!isGeneralContext && (
              <button
                type="button"
                className={styles.menuButton}
                aria-expanded={sidebarOpen}
                aria-controls="app-sidebar"
                aria-label={sidebarOpen ? 'Cerrar navegación' : 'Abrir navegación'}
                onClick={() => setSidebarOpen((open) => !open)}
              >
                <IconMenu width={20} height={20} />
              </button>
            )}
            <div className={styles.greeting}>
              <h1 className={styles.greetingTitle}>{title}</h1>
              {subtitle && <p className={styles.greetingSubtitle}>{subtitle}</p>}
            </div>
          </div>

          {/* Corrección de navegación (2026-08-18): selector de Calendario
              General/Personal/Laboral, centrado en la barra superior — el
              nivel de navegación más alto (ADR-015(b), FR-015), ahora
              también visualmente el más prominente. Fondo transparente:
              no compite con el resto del header ni "obstruye el portal". */}
          <nav className={styles.modeSwitcher} aria-label="Selector de modo">
            <NavLink
              to="/calendar"
              className={`${styles.modePill} ${sidebarContext === 'GENERAL' ? styles.modePillActive : ''}`}
            >
              Calendario
            </NavLink>
            {personalEnabled && (
              <NavLink
                to="/personal/calendar"
                className={`${styles.modePill} ${sidebarContext === 'PERSONAL' ? styles.modePillActive : ''}`}
              >
                Personal
              </NavLink>
            )}
            {laboralEnabled && (
              <NavLink
                to="/laboral/calendar"
                className={`${styles.modePill} ${sidebarContext === 'LABORAL' ? styles.modePillActive : ''}`}
              >
                Laboral
              </NavLink>
            )}
          </nav>

          <div className={styles.topBarActions}>
            <IconSearch className={styles.searchIcon} width={20} height={20} />
            <NavLink to="/notifications" className={styles.bellLink} aria-label="Notifications">
              <IconBell width={20} height={20} />
            </NavLink>
            <NavLink to={modeRemindersPath} className={styles.newButton}>
              <IconPlus width={16} height={16} /> Nuevo
            </NavLink>

            <ThemeSwitcher />

            {/* Corrección de navegación (2026-08-18): cuenta/Ajustes/Logout
                ya no viven dentro de <aside> — Calendario General oculta el
                sidebar por completo, pero acceder a Ajustes o cerrar sesión
                no es parte del "TabNav lateral" que se pidió ocultar, así
                que se quedan siempre visibles en la barra superior. */}
            <div className={styles.account}>
              <span className={styles.avatar}>{initial}</span>
              <span className={styles.accountName}>{name ?? 'Mi cuenta'}</span>
              <NavLink to="/settings" className={styles.settingsButton} aria-label="Ajustes">
                <IconSettings width={18} height={18} />
              </NavLink>
              <button type="button" className={styles.logoutButton} onClick={logout} aria-label="Log out">
                <IconLogout width={18} height={18} />
              </button>
            </div>
          </div>
        </header>
        <main className={styles.content}>{children}</main>
      </div>
    </div>
  )
}
