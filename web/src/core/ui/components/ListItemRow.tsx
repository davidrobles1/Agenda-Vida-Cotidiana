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
}

/**
 * UX-006: "row with colored icon + title + subtitle + status pill" — the
 * reference's list-item pattern, repeated across Próximas tareas,
 * Documentos, Garantías, Inventario, Mantenimiento, Suscripciones, Familia.
 */
export function ListItemRow({ title, subtitle, icon: Icon, tone, pillLabel, pillTone, trailing }: ListItemRowProps) {
  return (
    <div className={styles.row}>
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
