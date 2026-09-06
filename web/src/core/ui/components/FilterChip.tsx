import { ToggleButton, type Key } from 'react-aria-components'
import styles from './FilterChip.module.css'

interface FilterChipProps {
  id: Key
  label: string
  /**
   * ADR-022: estilo alternativo para las secciones que siguen el patrón de
   * `SectionList.module.css`. Es opcional y por defecto se mantiene el
   * estilo original, así que Calendario y Compromisos no cambian.
   *
   * Existe para poder cambiar la APARIENCIA sin renunciar al
   * COMPORTAMIENTO: `ToggleButtonGroup` aporta semántica real de
   * `radiogroup`/`radio` y navegación con flechas entre chips (UX-011 Fase
   * 3), que unos `<button aria-pressed>` sueltos no tienen.
   */
  className?: string
}

/**
 * UX-011 Fase 3: single item of a `ToggleButtonGroup` (real single-select
 * group semantics — grouped role + arrow-key navigation between chips,
 * neither of which the previous bare `aria-pressed` button group had). Must
 * be used inside `<ToggleButtonGroup>`, not standalone.
 */
export function FilterChip({ id, label, className }: FilterChipProps) {
  return (
    <ToggleButton id={id} className={className ?? styles.chip}>
      {label}
    </ToggleButton>
  )
}
