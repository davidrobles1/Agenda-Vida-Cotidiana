import { useEffect, useState } from 'react'
import { useActiveMode } from '../../core/user/ActiveModeContext'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import {
  INVENTORY_CATEGORIES,
  INVENTORY_CATEGORY_LABELS,
  createInventoryItem,
  listInventoryItems,
  type InventoryCategory,
  type InventoryItem,
} from './api'
import styles from './InventoryItemPicker.module.css'

interface InventoryItemPickerProps {
  value: string
  onChange: (inventoryItemId: string) => void
  /** Obligatorio en Garantías, opcional en Mantenimiento. */
  required?: boolean
  label?: string
  /** Qué se muestra cuando no hay artículo elegido y el campo es opcional. */
  emptyOptionLabel?: string
  /** Explica para qué sirve el enlace en ESTA pantalla. */
  hint?: string
}

/**
 * Elegir el artículo del inventario al que se refiere un recurso — y, si no
 * existe todavía, crearlo aquí mismo.
 *
 * LO SEGUNDO ES EL PUNTO. Un vínculo obligatorio que depende de que ya
 * exista otro registro es una trampa: con el inventario vacío, "Nueva
 * garantía" no se podría guardar y la pantalla no ofrecería ninguna salida.
 * Antes de esto, `WarrantyDetailDialog` solo mostraba un texto muerto
 * ("Todavía no tienes artículos…") y dejaba al usuario ahí. La regla es que
 * donde algo sea obligatorio, se pueda crear el destino en ese momento.
 *
 * Y es además el caso normal: registras la garantía porque acabas de comprar
 * el artículo, así que lo habitual es que no esté todavía en el inventario.
 *
 * ADR-019 regla 2 — solo ofrece los artículos del módulo activo, y crea el
 * nuevo en ese mismo módulo: enlazar un recurso Personal con un artículo
 * Laboral rompería el aislamiento. El servidor lo valida también
 * (`WarrantyService#requireLinkableItem`); esto es la mitad visible.
 */
export function InventoryItemPicker({
  value,
  onChange,
  required = false,
  label = 'Artículo del inventario',
  emptyOptionLabel = '— Sin artículo —',
  hint,
}: InventoryItemPickerProps) {
  const activeMode = useActiveMode()
  const [items, setItems] = useState<InventoryItem[] | null>(null)
  const [creating, setCreating] = useState(false)
  const [newName, setNewName] = useState('')
  const [newCategory, setNewCategory] = useState<InventoryCategory>('ELECTRONICOS')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    listInventoryItems(activeMode)
      .then((page) => {
        if (cancelled) return
        setItems(page.items)
        // Con el inventario vacío el formulario de creación se abre solo: es
        // la única acción posible y esconderla tras un botón añadiría un
        // paso que no decide nada.
        if (page.items.length === 0 && required) setCreating(true)
      })
      .catch(() => {
        if (!cancelled) setItems([])
      })
    return () => {
      cancelled = true
    }
  }, [activeMode, required])

  async function handleCreate() {
    if (!newName.trim() || saving) return
    setSaving(true)
    setError(null)
    try {
      const created = await createInventoryItem(newName.trim(), newCategory, undefined, activeMode)
      setItems((current) => [...(current ?? []), created])
      onChange(created.id)
      setNewName('')
      setCreating(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo crear el artículo.')
    } finally {
      setSaving(false)
    }
  }

  const hasItems = (items ?? []).length > 0

  return (
    <div className={shellStyles.field}>
      <span className={shellStyles.fieldLabel}>
        {label}
        {!required && <span className={shellStyles.optionalTag}>opcional</span>}
      </span>

      {hasItems && (
        <select
          className={shellStyles.textInput}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          disabled={items === null}
          required={required}
        >
          <option value="">{required ? '— Elige el artículo —' : emptyOptionLabel}</option>
          {(items ?? []).map((option) => (
            <option key={option.id} value={option.id}>
              {option.name}
              {option.location ? ` · ${option.location}` : ''}
            </option>
          ))}
        </select>
      )}

      {hint && <p className={styles.hint}>{hint}</p>}

      {!creating && (
        <button type="button" className={styles.inlineAction} onClick={() => setCreating(true)}>
          {hasItems ? '¿No está en la lista? Añádelo' : 'Añadir el primer artículo'}
        </button>
      )}

      {creating && (
        // No es un <form> anidado: iría dentro del formulario del diálogo y
        // el navegador no admite formularios anidados. Por eso el botón es
        // type="button" y llama a handleCreate a mano.
        <div className={styles.inlineForm}>
          <span className={styles.inlineTitle}>Nuevo artículo</span>
          <input
            className={shellStyles.textInput}
            value={newName}
            onChange={(event) => setNewName(event.target.value)}
            placeholder="¿Qué es? Ej. Refrigerador Mabe"
            aria-label="Nombre del artículo nuevo"
          />
          <select
            className={shellStyles.textInput}
            value={newCategory}
            onChange={(event) => setNewCategory(event.target.value as InventoryCategory)}
            aria-label="Categoría del artículo nuevo"
          >
            {INVENTORY_CATEGORIES.map((option) => (
              <option key={option} value={option}>
                {INVENTORY_CATEGORY_LABELS[option]}
              </option>
            ))}
          </select>
          {error && (
            <p className={shellStyles.formError} role="alert">
              {error}
            </p>
          )}
          <div className={styles.inlineActions}>
            {hasItems && (
              <button type="button" data-variant="secondary" onClick={() => setCreating(false)} disabled={saving}>
                Cancelar
              </button>
            )}
            <button type="button" onClick={() => void handleCreate()} disabled={saving || !newName.trim()}>
              {saving ? 'Guardando…' : 'Añadir al inventario'}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
