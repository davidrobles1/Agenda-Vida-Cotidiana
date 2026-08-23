import type { ComponentType, ReactNode, SVGProps } from 'react'
import type { Tone } from './MetricCard'
import styles from './ListItemRow.module.css'

interface ListItemRowProps {
  title: string
  subtitle?: string
  icon: ComponentType<SVGProps<SVGSVGElement>>
  tone: Tone
  pillLabel?: string
  pillTone?: Tone
  trailing?: ReactNode
  /** ADR-015/FR-017: general Calendario's per-item origin-mode accent — see
      CalendarView.tsx's `CalendarMarker.contextColor` doc for the same
      reasoning. A left-edge stripe, independent of `tone`. */
  contextColor?: string
}

/**
 * UX-006: "row with colored icon + title + subtitle + status pill" — the
 * reference's list-item pattern, repeated across Próximas tareas,
 * Documentos, Garantías, Inventario, Mantenimiento, Suscripciones, Familia.
 */
export function ListItemRow({
  title,
  subtitle,
  icon: Icon,
  tone,
  pillLabel,
  pillTone,
  trailing,
  contextColor,
}: ListItemRowProps) {
  return (
    <div
      className={styles.row}
      style={contextColor ? { borderLeft: `3px solid ${contextColor}`, paddingLeft: 'var(--space-2)' } : undefined}
    >
      <span className={styles.iconBadge} data-tone={tone}>
        <Icon width={18} height={18} />
      </span>
      <div className={styles.text}>
        <span className={styles.title}>{title}</span>
        {subtitle && <span className={styles.subtitle}>{subtitle}</span>}
      </div>
      <div className={styles.trailing}>
        {pillLabel && <span className={`badge badge-${pillTone ?? 'primary'}`}>{pillLabel}</span>}
        {trailing}
      </div>
    </div>
  )
}
