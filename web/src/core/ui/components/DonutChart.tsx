/**
 * UX-006: from-scratch SVG donut (stacked stroke-dasharray arcs) — no
 * charting library, same call as Android's Canvas/drawArc DonutChart.kt.
 * Only ever fed with real data in this app (Home's completed-vs-pending
 * task split) — never used for the excluded Finanzas "Gastos por categoría".
 */
export interface DonutSlice {
  value: number
  color: string
}

interface DonutChartProps {
  slices: DonutSlice[]
  size?: number
  strokeWidth?: number
}

export function DonutChart({ slices, size = 96, strokeWidth = 14 }: DonutChartProps) {
  const total = slices.reduce((sum, s) => sum + s.value, 0)
  const radius = (size - strokeWidth) / 2
  const circumference = 2 * Math.PI * radius
  let offset = 0

  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} role="img" aria-hidden="true">
      <g transform={`rotate(-90 ${size / 2} ${size / 2})`}>
        {total === 0 ? (
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="none"
            stroke="var(--color-border)"
            strokeWidth={strokeWidth}
          />
        ) : (
          slices.map((slice, i) => {
            const fraction = slice.value / total
            const dash = fraction * circumference
            const circle = (
              <circle
                key={i}
                cx={size / 2}
                cy={size / 2}
                r={radius}
                fill="none"
                stroke={slice.color}
                strokeWidth={strokeWidth}
                strokeDasharray={`${dash} ${circumference - dash}`}
                strokeDashoffset={-offset}
                strokeLinecap="butt"
              />
            )
            offset += dash
            return circle
          })
        )}
      </g>
    </svg>
  )
}
