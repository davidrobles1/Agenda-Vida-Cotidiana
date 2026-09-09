import { useState } from 'react'
import { useVocabulary } from '../../core/user/useVocabulary'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import styles from '../inventory/InventoryItemPicker.module.css'
import { createPerson, type Person } from './api'

interface PersonPickerProps {
  /** Las personas ya cargadas por la pantalla. No las pide otra vez. */
  people: Person[]
  value: string
  onChange: (personId: string) => void
  /** Avisa a la pantalla de la persona nueva, para que su lista no quede vieja. */
  onCreated?: (person: Person) => void
  required?: boolean
  label?: string
  emptyOptionLabel?: string
  hint?: string
}

/**
 * Elegir una persona — y, si todavía no existe, crearla aquí mismo.
 *
 * LO SEGUNDO ES EL PUNTO, igual que en `InventoryItemPicker`. Un seguimiento
 * EXIGE una persona (`CreateCommitmentRequest.personId` es `@NotNull`), así que
 * sin ninguna dada de alta el formulario no se podía enviar: el desplegable
 * salía vacío, el botón no hacía nada y la pantalla no ofrecía ninguna salida.
 * Era el único punto de toda la aplicación donde el usuario quedaba atascado
 * sin explicación.
 *
 * Reutiliza los estilos del selector de inventario: son el mismo patrón
 * resolviendo el mismo problema, y darles dos apariencias distintas los haría
 * parecer dos cosas.
 *
 * UX-014/UX-015: el rótulo sale del vocabulario del perfil — "Persona",
 * "Contacto" o "Prospecto" según a qué se dedique quien usa la aplicación.
 */
export function PersonPicker({
  people,
  value,
  onChange,
  onCreated,
  required = false,
  label,
  emptyOptionLabel = '— Sin asignar —',
  hint,
}: PersonPickerProps) {
  const vocabulary = useVocabulary()
  const term = label ?? vocabulary.person
  const [creating, setCreating] = useState(people.length === 0 && required)
  const [name, setName] = useState('')
  const [role, setRole] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleCreate() {
    if (!name.trim() || saving) return
    setSaving(true)
    setError(null)
    try {
      const created = await createPerson({ name: name.trim(), role: role.trim() || undefined })
      onCreated?.(created)
      onChange(created.id)
      setName('')
      setRole('')
      if (people.length > 0) setCreating(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : `No se pudo crear ${vocabulary.person.toLowerCase()}.`)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className={shellStyles.field}>
      <span className={shellStyles.fieldLabel}>
        {term}
        {!required && <span className={shellStyles.optionalTag}>opcional</span>}
      </span>

      {people.length > 0 && (
        <select
          className={shellStyles.textInput}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          required={required}
        >
          <option value="">{required ? `— Elige ${term.toLowerCase()} —` : emptyOptionLabel}</option>
          {people.map((person) => (
            <option key={person.id} value={person.id}>
              {person.name}
              {person.organization ? ` · ${person.organization}` : ''}
            </option>
          ))}
        </select>
      )}

      {hint && <p className={styles.hint}>{hint}</p>}

      {!creating && (
        <button type="button" className={styles.inlineAction} onClick={() => setCreating(true)}>
          {people.length > 0 ? `¿No está en la lista? Añádela` : `Añadir ${term.toLowerCase()}`}
        </button>
      )}

      {creating && (
        // No es un <form> anidado: el navegador no los admite dentro de otro
        // formulario. Por eso el botón es type="button" y llama a mano.
        <div className={styles.inlineForm}>
          <span className={styles.inlineTitle}>{`${term} nueva`}</span>
          <input
            className={shellStyles.textInput}
            value={name}
            onChange={(event) => setName(event.target.value)}
            placeholder="Nombre"
            aria-label={`Nombre de ${term.toLowerCase()}`}
          />
          <input
            className={shellStyles.textInput}
            value={role}
            onChange={(event) => setRole(event.target.value)}
            placeholder="Rol (opcional). Ej. Arquitecto, Proveedor"
            aria-label="Rol"
          />
          {error && (
            <p className={shellStyles.formError} role="alert">
              {error}
            </p>
          )}
          <div className={styles.inlineActions}>
            {people.length > 0 && (
              <button type="button" data-variant="secondary" onClick={() => setCreating(false)} disabled={saving}>
                Cancelar
              </button>
            )}
            <button type="button" onClick={() => void handleCreate()} disabled={saving || !name.trim()}>
              {saving ? 'Guardando…' : 'Añadir'}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
