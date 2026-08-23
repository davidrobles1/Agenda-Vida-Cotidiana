import { useState, type FormEvent, type ReactNode } from 'react'
import { Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../core/motion/tokens'
import { handleRadiogroupKeyDown, radioTabIndex } from '../../core/ui/keyboard/radiogroupKeyboard'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import {
  INVENTORY_CATEGORIES,
  INVENTORY_CATEGORY_LABELS,
  createInventoryItem,
  updateInventoryItem,
  type InventoryCategory,
  type InventoryItem,
} from './api'
import styles from './InventoryItemDialog.module.css'

const MotionDialog = motion.create(Dialog)

interface InventoryItemDialogProps {
  /** Presente = editar (precarga y hace PATCH); ausente = crear (POST).
      Un solo componente para ambos casos — mismo formulario, misma
      validación, sin duplicar lógica entre "crear" y "editar". */
  item?: InventoryItem
  trigger: ReactNode
  onSaved: (item: InventoryItem) => void
}

/** Pedido explícito del usuario (2026-08-22): "Inventario registrar,
    actualizar borrar artículos según la categoría." */
export function InventoryItemDialog({ item, trigger, onSaved }: InventoryItemDialogProps) {
  const isEdit = item !== undefined
  const [isOpen, setIsOpen] = useState(false)
  const [name, setName] = useState(item?.name ?? '')
  const [category, setCategory] = useState<InventoryCategory>(item?.category ?? 'ELECTRONICOS')
  const [location, setLocation] = useState(item?.location ?? '')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (open) {
      setName(item?.name ?? '')
      setCategory(item?.category ?? 'ELECTRONICOS')
      setLocation(item?.location ?? '')
      setError(null)
    }
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!name.trim() || saving) return
    setSaving(true)
    setError(null)
    try {
      const saved =
        isEdit && item
          ? await updateInventoryItem(item.id, name.trim(), category, location.trim(), item.version)
          : await createInventoryItem(name.trim(), category, location.trim())
      onSaved(saved)
      setIsOpen(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar el artículo.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      {trigger}
      <Modal isDismissable className={shellStyles.modalOverlay}>
        <MotionDialog
          className={shellStyles.panel}
          initial={{ opacity: 0, scale: 0.96, y: 8 }}
          animate={{ opacity: isOpen ? 1 : 0, scale: isOpen ? 1 : 0.96, y: isOpen ? 0 : 8 }}
          transition={motionTokens.smooth}
        >
          {({ close }) => (
            <div className={shellStyles.panelScroll}>
              <form onSubmit={handleSubmit}>
                <div className={shellStyles.headerRow}>
                  <Heading slot="title" className={shellStyles.heading}>
                    {isEdit ? 'Editar artículo' : 'Nuevo artículo'}
                  </Heading>
                  <button type="button" className={shellStyles.closeButton} onClick={close} aria-label="Cerrar">
                    ×
                  </button>
                </div>

                {error && <p className={shellStyles.formError} role="alert">{error}</p>}

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Nombre</span>
                  <input
                    className={shellStyles.textInput}
                    value={name}
                    onChange={(event) => setName(event.target.value)}
                    required
                    autoFocus
                  />
                </label>

                <div className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Categoría</span>
                  <div
                    className={styles.categoryGrid}
                    role="radiogroup"
                    aria-label="Categoría del artículo"
                    onKeyDown={handleRadiogroupKeyDown}
                  >
                    {INVENTORY_CATEGORIES.map((option, index) => (
                      <button
                        key={option}
                        type="button"
                        role="radio"
                        aria-checked={category === option}
                        tabIndex={radioTabIndex(category === option, index === 0, true)}
                        className={`${styles.categoryButton} ${category === option ? styles.categoryButtonActive : ''}`}
                        onClick={() => setCategory(option)}
                      >
                        {INVENTORY_CATEGORY_LABELS[option]}
                      </button>
                    ))}
                  </div>
                </div>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Ubicación / estado (opcional)</span>
                  <input
                    className={shellStyles.textInput}
                    value={location}
                    onChange={(event) => setLocation(event.target.value)}
                    placeholder="Ej. En uso, Guardado, Prestado…"
                  />
                </label>

                <div className={shellStyles.formActions}>
                  {saving && <span className={shellStyles.savingHint}>Guardando…</span>}
                  <button type="button" data-variant="secondary" onClick={close} disabled={saving}>
                    Cancelar
                  </button>
                  <button type="submit" disabled={saving || !name.trim()}>
                    Guardar
                  </button>
                </div>
              </form>
            </div>
          )}
        </MotionDialog>
      </Modal>
    </DialogTrigger>
  )
}
