import { useState, type FormEvent } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../core/motion/tokens'
import { IconPlus } from '../../core/ui/icons'
import { useVocabulary } from '../../core/user/useVocabulary'
import { createPerson, type CreatePersonInput, type Person } from './api'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'

const MotionDialog = motion.create(Dialog)

interface CreatePersonDialogProps {
  onCreated: (person: Person) => void
}

/** ADR-016/FR-021, UC-17/UC-18. Mismo patrón exacto que CreateReminderDialog.tsx. */
export function CreatePersonDialog({ onCreated }: CreatePersonDialogProps) {
  // UX-014/UX-015: mismo formulario, mismos campos — solo el nombre cambia.
  const vocabulary = useVocabulary()
  const [isOpen, setIsOpen] = useState(false)
  const [name, setName] = useState('')
  const [role, setRole] = useState('')
  const [organization, setOrganization] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function reset() {
    setName('')
    setRole('')
    setOrganization('')
    setError(null)
  }

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (!open) reset()
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!name.trim() || saving) return

    setSaving(true)
    setError(null)
    try {
      const input: CreatePersonInput = { name: name.trim() }
      if (role.trim()) input.role = role.trim()
      if (organization.trim()) input.organization = organization.trim()
      const created = await createPerson(input)
      onCreated(created)
      setIsOpen(false)
      reset()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar la persona.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      <Button>
        <IconPlus width={16} height={16} /> {vocabulary.personGender === 'f' ? 'Nueva' : 'Nuevo'}{' '}
        {vocabulary.person.toLowerCase()}
      </Button>
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
                    {vocabulary.personGender === 'f' ? 'Nueva' : 'Nuevo'} {vocabulary.person.toLowerCase()}
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
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Carlos Martínez"
                    autoFocus
                  />
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Rol</span>
                  <input
                    className={shellStyles.textInput}
                    value={role}
                    onChange={(e) => setRole(e.target.value)}
                    placeholder="Cliente, colega, proveedor…"
                  />
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Organización</span>
                  <input
                    className={shellStyles.textInput}
                    value={organization}
                    onChange={(e) => setOrganization(e.target.value)}
                    placeholder="ACME Solutions"
                  />
                </label>

                <div className={shellStyles.formActions}>
                  {saving && <span className={shellStyles.savingHint}>Guardando…</span>}
                  <button type="button" data-variant="secondary" onClick={close} disabled={saving}>
                    Cancelar
                  </button>
                  <button type="submit" disabled={saving}>
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
