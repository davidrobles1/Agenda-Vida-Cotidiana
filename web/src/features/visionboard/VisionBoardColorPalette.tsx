import { useId } from 'react'
import { handleRadiogroupKeyDown, radioTabIndex } from '../../core/ui/keyboard/radiogroupKeyboard'
import { normalizeColor, type ShapeSwatch } from './visionBoardShapeColors'
import styles from './VisionBoardColorPalette.module.css'

interface VisionBoardColorPaletteProps {
  label: string
  swatches: ShapeSwatch[]
  /** Color aplicado ahora mismo. `undefined` = la forma hereda el del tema
      del tablero, que es como se comportaban todas hasta ahora. */
  value: string | undefined
  onChange: (color: string) => void
  /** Texto bajo la fila cuando hace falta explicar algo (el aviso de
      contraste bajo, por ejemplo). Opcional. */
  hint?: string
}

/**
 * Fila de muestras de color + selector personalizado.
 *
 * Es un `radiogroup` real, no un montón de botones sueltos: da navegación
 * con flechas entre muestras, el mismo criterio que ya siguen el selector
 * de formas (`VisionBoardShapePicker`) y los filtros del portal (UX-011
 * Fase 3).
 *
 * El color personalizado usa `<input type="color">` nativo — abre el
 * selector del sistema operativo, que en macOS, Windows, Android e iOS es
 * mejor que cualquier rueda que dibujemos, y no añade dependencias.
 */
export function VisionBoardColorPalette({
  label,
  swatches,
  value,
  onChange,
  hint,
}: VisionBoardColorPaletteProps) {
  const inputId = useId()

  const active = value?.toLowerCase()
  const anyChecked = !!active && swatches.some((swatch) => swatch.value.toLowerCase() === active)
  const isCustom = !!active && !anyChecked

  return (
    <div className={styles.palette}>
      <div className={styles.row} role="radiogroup" aria-label={label} onKeyDown={handleRadiogroupKeyDown}>
        {swatches.map((swatch, index) => {
          const selected = active === swatch.value.toLowerCase()
          return (
            <button
              key={swatch.value}
              type="button"
              role="radio"
              aria-checked={selected}
              aria-label={swatch.label}
              title={swatch.label}
              tabIndex={radioTabIndex(selected, index === 0, anyChecked)}
              className={styles.swatch}
              style={{ '--swatch': swatch.value } as React.CSSProperties}
              onClick={() => onChange(swatch.value)}
            />
          )
        })}

        {/* El color personalizado ocupa su sitio en el grupo: si está
            activo, se ve cuál es sin tener que abrir el selector. */}
        <label
          className={`${styles.custom} ${isCustom ? styles.customActive : ''}`}
          htmlFor={inputId}
          style={isCustom ? ({ '--swatch': active } as React.CSSProperties) : undefined}
        >
          <input
            id={inputId}
            type="color"
            className={styles.customInput}
            value={active ?? '#2f6188'}
            onChange={(event) => {
              const color = normalizeColor(event.target.value)
              if (color) onChange(color)
            }}
          />
          <span className={styles.customPlus} aria-hidden="true">
            +
          </span>
          <span className={styles.customText}>Color personalizado</span>
        </label>
      </div>

      {hint && <p className={styles.hint}>{hint}</p>}
    </div>
  )
}
