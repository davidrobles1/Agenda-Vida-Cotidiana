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
 * UX-006/UX-007: reference dashboard's metric-card pattern — the Web
 * counterpart of Android's MetricCard.kt, used by Home (real data) and the
 * mock-data screens' own summary cards.
 *
 * UX-007: compacted — icon/value/label sit on one row instead of three
 * stacked rows (label+icon row, value row, subtitle row), so this reads as a
 * dense stat chip rather than its own mini-card. Not just smaller text: the
 * label moved from its own row above the value to a caption directly under
 * it (value+label now read as one grouped unit next to the icon), and the
 * subtitle moved from a full-width third line to a trailing tag that only
 * takes as much width as it needs.
 */
export function MetricCard({ label, value, subtitle, subtitleTone, icon: Icon, tone, onClick, testId }: MetricCardProps) {
  const content = (
    <>
      <span className={styles.iconBadge} data-tone={tone}>
        <Icon width={16} height={16} />
      </span>
      <span className={styles.textGroup}>
        <span className={styles.value}>{value}</span>
        <span className={styles.label}>{label}</span>
      </span>
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
