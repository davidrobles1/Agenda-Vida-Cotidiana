import styles from './FilterChip.module.css'

interface FilterChipProps {
  label: string
  selected: boolean
  onClick: () => void
}

export function FilterChip({ label, selected, onClick }: FilterChipProps) {
  return (
    <button type="button" className={styles.chip} aria-pressed={selected} onClick={onClick}>
      {label}
    </button>
  )
}
