import { useEffect, useState, type ReactNode } from 'react'
import { NavLink } from 'react-router-dom'
import { logout } from '../../auth/authClient'
import { getCurrentUser } from '../../user/api'
import {
  IconBell,
  IconCalendar,
  IconFolder,
  IconHome,
  IconInventory,
  IconLogout,
  IconPlus,
  IconRepeat,
  IconSearch,
  IconShared,
  IconShield,
  IconTasks,
  IconUsers,
  IconWrench,
} from '../icons'
import styles from './AppShell.module.css'

/**
 * UX-006: sidebar item list. Real items (Inicio/Tareas/Compartidos) have
 * working screens backed by real endpoints; the rest are the 6 scaffolding
 * modules (mock data only, see core/mock/mockData.ts). Finanzas and "IA
 * Asistente" — present in the reference sidebar — are intentionally absent:
 * CLAUDE.md excludes both from V1 entirely, with no "Próximamente" exception.
 * "Configuración" is also absent: no settings screen exists or is in scope
 * for this task, and inventing one isn't warranted just to match the image.
 * "Compartidos" isn't in the reference sidebar at all (it's implied by the
 * phone mockup's bottom nav) — placed right after "Tareas" here since
 * sharing is reminder-scoped, the same adaptation call made for Android's
 * bottom nav.
 *
 * UX-007: "Calendario" is a first-class sidebar item (unlike Android, where
 * it lives inside "Más" — the bottom nav has real history with overflow at
 * only 4 items + a center FAB, see MoreScreen.kt's comment; the sidebar has
 * no such width constraint).
 */
const navItems = [
  { to: '/home', label: 'Inicio', icon: IconHome },
  { to: '/reminders', label: 'Tareas', icon: IconTasks },
  { to: '/invitations', label: 'Compartidos', icon: IconShared },
  { to: '/calendar', label: 'Calendario', icon: IconCalendar },
  { to: '/documents', label: 'Documentos', icon: IconFolder },
  { to: '/inventory', label: 'Inventario', icon: IconInventory },
  { to: '/warranties', label: 'Garantías', icon: IconShield },
  { to: '/maintenance', label: 'Mantenimiento', icon: IconWrench },
  { to: '/subscriptions', label: 'Suscripciones', icon: IconRepeat },
  { to: '/family', label: 'Familia', icon: IconUsers },
]

interface AppShellProps {
  title: string
  subtitle?: string
  children: ReactNode
}

export function AppShell({ title, subtitle, children }: AppShellProps) {
  const [name, setName] = useState<string | null>(null)

  useEffect(() => {
    getCurrentUser()
      .then((user) => setName(user.username))
      .catch(() => {
        /* Sidebar still works without a greeting name — not fatal. */
      })
  }, [])

  const initial = name ? name.charAt(0).toUpperCase() : '…'

  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar}>
        <div className={styles.logo}>
          <span className={styles.logoMark}>VC</span>
          <span>Vida Cotidiana</span>
        </div>

        <nav className={styles.nav}>
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`}
            >
              <item.icon width={18} height={18} />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className={styles.account}>
          <span className={styles.avatar}>{initial}</span>
          <span className={styles.accountName}>{name ?? 'Mi cuenta'}</span>
          <button type="button" className={styles.logoutButton} onClick={logout} aria-label="Log out">
            <IconLogout width={18} height={18} />
          </button>
        </div>
      </aside>

      <div className={styles.main}>
        <header className={styles.topBar}>
          <div className={styles.greeting}>
            <h1 className={styles.greetingTitle}>{title}</h1>
            {subtitle && <p className={styles.greetingSubtitle}>{subtitle}</p>}
          </div>
          <div className={styles.topBarActions}>
            <IconSearch className={styles.searchIcon} width={20} height={20} />
            <NavLink to="/notifications" className={styles.bellLink} aria-label="Notifications">
              <IconBell width={20} height={20} />
            </NavLink>
            <NavLink to="/reminders" className={styles.newButton}>
              <IconPlus width={16} height={16} /> Nuevo
            </NavLink>
          </div>
        </header>
        <main className={styles.content}>{children}</main>
      </div>
    </div>
  )
}
