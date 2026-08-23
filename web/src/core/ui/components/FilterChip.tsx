import { ToggleButton, type Key } from 'react-aria-components'
import styles from './FilterChip.module.css'

interface FilterChipProps {
  id: Key
  label: string
}

/**
 * UX-011 Fase 3: single item of a `ToggleButtonGroup` (real single-select
 * group semantics — grouped role + arrow-key navigation between chips,
 * neither of which the previous bare `aria-pressed` button group had). Must
 * be used inside `<ToggleButtonGroup>`, not standalone — see
 * `InventoryPage.tsx` for the real usage.
 */
export function FilterChip({ id, label }: FilterChipProps) {
  return (
    <ToggleButton id={id} className={styles.chip}>
      {label}
    </ToggleButton>
  )
}
