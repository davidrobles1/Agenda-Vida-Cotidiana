import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import { VisionBoardColorPalette } from './VisionBoardColorPalette'
import {
  DEFAULT_SHAPE_FILL,
  SHAPE_FILL_SWATCHES,
  SHAPE_TEXT_SWATCHES,
  contrastRatio,
  meetsAA,
  shapeColorOf,
  suggestTextColor,
} from './visionBoardShapeColors'
import styles from './VisionBoardShapeColorFields.module.css'

interface VisionBoardShapeColorFieldsProps {
  /** El `data` de la forma — al crear es un borrador vacío, al editar es el
      draft del editor. Se lee y se escribe igual en ambos casos. */
  data: Record<string, unknown>
  onChange: (next: Record<string, unknown>) => void
}

/**
 * Color de fondo y de texto de una forma, con contraste automático.
 *
 * REGLA DEL CONTRASTE (pedido explícito del usuario, 2026-09-02): al
 * cambiar el fondo, el color del texto se recalcula SIEMPRE a partir del
 * contraste real —luminancia relativa de WCAG 2.1, no el nombre del
 * color—. Una elección manual de texto se respeta hasta el siguiente
 * cambio de fondo, o hasta que el usuario pulse "Automático".
 *
 * Lo usan las dos superficies donde se toca una forma: el paso de alta
 * (`VisionBoardElementLibrary`) y el de edición
 * (`VisionBoardElementEditor`), para que se comporten igual.
 */
export function VisionBoardShapeColorFields({ data, onChange }: VisionBoardShapeColorFieldsProps) {
  const fill = shapeColorOf(data, 'fill')
  const textColor = shapeColorOf(data, 'textColor')

  // Sin color propio, la forma hereda el del tema del tablero: es el
  // comportamiento que tenían todas antes de esto y el que conservan los
  // tableros ya creados. Para calcular la sugerencia hace falta un color
  // concreto, así que se parte del de referencia.
  const effectiveFill = fill ?? DEFAULT_SHAPE_FILL
  const suggested = suggestTextColor(effectiveFill)
  const effectiveText = textColor ?? suggested

  function handleFillChange(next: string) {
    // El texto se recalcula con cada cambio de fondo — incluso si el
    // usuario lo había fijado a mano, que es justo lo que pide la regla.
    onChange({ ...data, fill: next, textColor: suggestTextColor(next) })
  }

  function handleTextChange(next: string) {
    onChange({ ...data, textColor: next })
  }

  function handleAuto() {
    onChange({ ...data, textColor: suggested })
  }

  const ratio = contrastRatio(effectiveFill, effectiveText)
  const legible = meetsAA(effectiveFill, effectiveText)
  const isManual = !!textColor && textColor.toLowerCase() !== suggested.toLowerCase()

  return (
    <>
      <div className={shellStyles.field}>
        <span className={shellStyles.fieldLabel}>Fondo</span>
        <VisionBoardColorPalette
          label="Color de fondo"
          swatches={SHAPE_FILL_SWATCHES}
          value={fill}
          onChange={handleFillChange}
        />
      </div>

      <div className={shellStyles.field}>
        <span className={shellStyles.fieldLabel}>
          Texto
          {isManual && (
            <button type="button" className={styles.autoButton} onClick={handleAuto}>
              Automático
            </button>
          )}
        </span>
        <VisionBoardColorPalette
          label="Color del texto"
          swatches={SHAPE_TEXT_SWATCHES}
          value={textColor}
          onChange={handleTextChange}
          hint={
            legible
              ? undefined
              : // No se bloquea la elección: es su tablero. Pero decirlo es
                // mejor que dejarle descubrir en la exportación que el texto
                // no se lee.
                `Contraste bajo (${ratio ? ratio.toFixed(1) : '—'}:1). Puede costar leerlo.`
          }
        />
      </div>

      {/* Vista previa: el cambio se ve aquí antes de guardar, y en el
          lienzo en cuanto se guarda. */}
      <div className={shellStyles.field}>
        <span className={shellStyles.fieldLabel}>Vista previa</span>
        <div className={styles.preview} style={{ background: effectiveFill, color: effectiveText }}>
          Texto de ejemplo
        </div>
      </div>
    </>
  )
}
