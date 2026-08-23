import { ICON_OPTIONS } from './pickerCatalog'
import styles from './Pickers.module.css'

interface IconPickerProps {
  value: string | undefined
  onChange: (value: string | undefined) => void
}

/** Grilla de selección de icono (Lucide) — compartida por Notas y Agenda. */
export function IconPicker({ value, onChange }: IconPickerProps) {
  return (
    <div className={styles.iconGrid} role="radiogroup" aria-label="Icono">
      {ICON_OPTIONS.map(({ id, label, Icon }) => (
        <button
          key={id}
          type="button"
          role="radio"
          aria-checked={value === id}
          aria-label={label}
          className={`${styles.iconOptionButton} ${value === id ? styles.iconOptionButtonActive : ''}`}
          onClick={() => onChange(value === id ? undefined : id)}
        >
          <Icon width={18} height={18} />
        </button>
      ))}
    </div>
  )
}
