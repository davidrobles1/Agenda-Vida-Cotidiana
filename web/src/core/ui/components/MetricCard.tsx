import type { ComponentType, SVGProps } from 'react'
import styles from './MetricCard.module.css'

export type Tone = 'primary' | 'success' | 'warning' | 'info' | 'error'

interface MetricCardProps {
  label: string
  value: string | number
  subtitle?: string
  subtitleTone?: Tone
  icon: ComponentType<SVGProps<SVGSVGElement>>
  tone: Tone
  onClick?: () => void
  testId?: string
}

/**
 * UX-006: reference dashboard's metric-card pattern (icon badge + number +
 * label + subtitle) — the Web counterpart of Android's MetricCard.kt, used
 * by Home (real data) and the mock-data screens' own summary cards.
 */
export function MetricCard({ label, value, subtitle, subtitleTone, icon: Icon, tone, onClick, testId }: MetricCardProps) {
  const content = (
    <>
      <div className={styles.top}>
        <span className={styles.label}>{label}</span>
        <span className={styles.iconBadge} data-tone={tone}>
          <Icon width={18} height={18} />
        </span>
      </div>
      <span className={styles.value}>{value}</span>
      {subtitle && (
        <span className={styles.subtitle} data-tone={subtitleTone}>
          {subtitle}
        </span>
      )}
    </>
  )

  if (onClick) {
    return (
      <button type="button" className={styles.card} onClick={onClick} data-testid={testId}>
        {content}
      </button>
    )
  }

  return (
    <div className={styles.card} data-testid={testId}>
      {content}
    </div>
  )
}
