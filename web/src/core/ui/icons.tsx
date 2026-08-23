import type { SVGProps } from 'react'

/**
 * UX-006: small hand-rolled icon set (stroke-based, currentColor) instead of
 * adding an icon library dependency — the same "only add a dependency when
 * it's actually justified" call already made on Android for
 * material-icons-extended (there it was justified: zero icon assets existed
 * at all). Here a dozen simple glyphs cover every screen this task needs.
 */
type IconProps = SVGProps<SVGSVGElement>

function base(props: IconProps) {
  return {
    xmlns: 'http://www.w3.org/2000/svg',
    width: 20,
    height: 20,
    viewBox: '0 0 24 24',
    fill: 'none',
    stroke: 'currentColor',
    strokeWidth: 2,
    strokeLinecap: 'round' as const,
    strokeLinejoin: 'round' as const,
    'aria-hidden': true,
    ...props,
  }
}

export function IconHome(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M3 11.5 12 4l9 7.5" />
      <path d="M5 10v9a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1v-9" />
    </svg>
  )
}

export function IconTasks(props: IconProps) {
  return (
    <svg {...base(props)}>
      <rect x="4" y="3" width="16" height="18" rx="2" />
      <path d="M8 8h8M8 12h8M8 16h5" />
    </svg>
  )
}

export function IconShared(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="9" cy="8" r="3" />
      <path d="M2 20a7 7 0 0 1 14 0" />
      <circle cx="18" cy="7" r="2.5" />
      <path d="M15.5 13.5a5.5 5.5 0 0 1 6.5 5.4" />
    </svg>
  )
}

export function IconFolder(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2Z" />
    </svg>
  )
}

export function IconInventory(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="m3.5 7.5 8.5-4 8.5 4-8.5 4-8.5-4Z" />
      <path d="M3.5 7.5v9l8.5 4 8.5-4v-9" />
      <path d="M12 11.5v9" />
    </svg>
  )
}

export function IconShield(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 3 5 6v6c0 4.4 3 7.4 7 9 4-1.6 7-4.6 7-9V6Z" />
      <path d="m9 12 2 2 4-4" />
    </svg>
  )
}

export function IconWrench(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M14.7 6.3a4 4 0 0 0-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 0 0 5.4-5.4l-2.83 2.83-2.4-2.4Z" />
    </svg>
  )
}

export function IconRepeat(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M17 2 21 6l-4 4" />
      <path d="M3 12a9 9 0 0 1 18-4.5" />
      <path d="M7 22 3 18l4-4" />
      <path d="M21 12a9 9 0 0 1-18 4.5" />
    </svg>
  )
}

/** ADR-016 (Módulo Laboral) — Inbox de captura rápida (FR-028). Bandeja con flecha de entrada, sin precedente en el set existente. */
export function IconInbox(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M3.5 12.5h5l1.7 3h3.6l1.7-3h5" />
      <path d="M6 5h12l2 7.5V18a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 18v-5.5Z" />
    </svg>
  )
}

export function IconUsers(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="9" cy="8" r="3.5" />
      <path d="M2.5 20a6.5 6.5 0 0 1 13 0" />
      <circle cx="18" cy="8.5" r="2.5" />
      <path d="M16 13a5.5 5.5 0 0 1 5.5 5.5" />
    </svg>
  )
}

export function IconBell(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M6 9a6 6 0 1 1 12 0c0 4 1.5 5.5 1.5 5.5H4.5S6 13 6 9Z" />
      <path d="M9.5 18a2.5 2.5 0 0 0 5 0" />
    </svg>
  )
}

export function IconSearch(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="10.5" cy="10.5" r="6.5" />
      <path d="m20 20-4.35-4.35" />
    </svg>
  )
}

export function IconPlus(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 5v14M5 12h14" />
    </svg>
  )
}

export function IconChevronRight(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="m9 5 7 7-7 7" />
    </svg>
  )
}

export function IconCheckCircle(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="12" r="9" />
      <path d="m8.5 12.5 2.5 2.5 5-5" />
    </svg>
  )
}

export function IconAlert(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 9v4" />
      <path d="M10.3 4.4 2.7 18a1.6 1.6 0 0 0 1.4 2.4h15.8a1.6 1.6 0 0 0 1.4-2.4L13.7 4.4a1.6 1.6 0 0 0-2.8 0Z" />
      <path d="M12 16.5h.01" />
    </svg>
  )
}

export function IconLogout(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
      <path d="M16 17l5-5-5-5" />
      <path d="M21 12H9" />
    </svg>
  )
}

export function IconCalendar(props: IconProps) {
  return (
    <svg {...base(props)}>
      <rect x="3.5" y="5" width="17" height="16" rx="2" />
      <path d="M8 3v4M16 3v4M3.5 10h17" />
    </svg>
  )
}

export function IconChevronLeft(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="m15 5-7 7 7 7" />
    </svg>
  )
}

export function IconMenu(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M4 6h16M4 12h16M4 18h16" />
    </svg>
  )
}

export function IconHeart(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 20.5s-7.5-4.6-10-9.3C.5 8 2 4.5 5.5 4a5 5 0 0 1 6.5 3 5 5 0 0 1 6.5-3c3.5.5 5 4 3.5 7.2-2.5 4.7-10 9.3-10 9.3Z" />
    </svg>
  )
}

export function IconLeaf(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M20 4c.5 8-3 15-14 16C5 9 12 4.5 20 4Z" />
      <path d="M7 19c2-4 5-8 12-13" />
    </svg>
  )
}

export function IconSparkle(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M12 3c.6 3.4 1.6 5.4 3 6.8 1.4 1.4 3.4 2.4 6.8 3-3.4.6-5.4 1.6-6.8 3-1.4 1.4-2.4 3.4-3 6.8-.6-3.4-1.6-5.4-3-6.8-1.4-1.4-3.4-2.4-6.8-3 3.4-.6 5.4-1.6 6.8-3 1.4-1.4 2.4-3.4 3-6.8Z" />
    </svg>
  )
}

export function IconUser(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 20c0-4.4 3.6-7 8-7s8 2.6 8 7" />
    </svg>
  )
}

export function IconUserPlus(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="9" cy="8" r="4" />
      <path d="M2 20c0-4.4 3.1-7 7-7s7 2.6 7 7" />
      <path d="M18 8v6M15 11h6" />
    </svg>
  )
}

export function IconArrowRight(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M5 12h14" />
      <path d="m13 6 6 6-6 6" />
    </svg>
  )
}

export function IconLock(props: IconProps) {
  return (
    <svg {...base(props)}>
      <rect x="5" y="11" width="14" height="9" rx="2" />
      <path d="M8 11V7a4 4 0 0 1 8 0v4" />
    </svg>
  )
}

/** FASE 3 (Vision Board): sidebar nav item, replacing IconTasks now that
    "Tareas" points at the Vision Board canvas instead of RemindersPage. */
export function IconLayoutGrid(props: IconProps) {
  return (
    <svg {...base(props)}>
      <rect x="3.5" y="3.5" width="7.5" height="7.5" rx="1.5" />
      <rect x="13" y="3.5" width="7.5" height="7.5" rx="1.5" />
      <rect x="3.5" y="13" width="7.5" height="7.5" rx="1.5" />
      <rect x="13" y="13" width="7.5" height="7.5" rx="1.5" />
    </svg>
  )
}

/** UX-012/ADR-015: Ajustes (Settings) link in AppShell's account area. */
export function IconSettings(props: IconProps) {
  return (
    <svg {...base(props)}>
      <circle cx="12" cy="12" r="3" />
      <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.6a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" />
    </svg>
  )
}

/** Canvas de notas por día (2026-08-22): mini-toolbar de edición inline. */
export function IconBold(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M7 4h6a3.5 3.5 0 0 1 0 7H7z" />
      <path d="M7 11h7a3.5 3.5 0 0 1 0 7H7z" />
    </svg>
  )
}

export function IconItalic(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M11 4h6M5 20h6M14 4 8 20" />
    </svg>
  )
}

export function IconTrash(props: IconProps) {
  return (
    <svg {...base(props)}>
      <path d="M4 7h16M9 7V4h6v3M6 7l1 13a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-13" />
    </svg>
  )
}
