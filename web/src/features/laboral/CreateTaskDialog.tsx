import { useState, type FormEvent } from 'react'
import { Button, Dialog, DialogTrigger, Heading, Modal } from 'react-aria-components'
import { motion } from 'motion/react'
import { motionTokens } from '../../core/motion/tokens'
import { IconPlus } from '../../core/ui/icons'
import { useVocabulary } from '../../core/user/useVocabulary'
import type { Person } from '../people/api'
import type { Project } from '../projects/api'
import { createPlace, placeLocationText, type Place } from '../places/api'
import { createReminder, type CreateReminderInput, type Reminder } from '../reminders/api'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import laboralUI from '../../core/ui/laboral/LaboralUI.module.css'

const MotionDialog = motion.create(Dialog)

interface CreateTaskDialogProps {
  people: Person[]
  projects: Project[]
  /** ADR-016 Fase 3e3/FR-033 — catálogo de Lugares guardados (UC-26). */
  places: Place[]
  onCreated: (task: Reminder) => void
  /** Notifica un Lugar creado inline, para que la página refresque su catálogo. */
  onPlaceCreated?: (place: Place) => void
}

/** ADR-016/FR-023, UC-17. Tarea = REMINDER context=LABORAL con vínculo opcional a Persona/Proyecto. */
export function CreateTaskDialog({ people, projects, places, onCreated, onPlaceCreated }: CreateTaskDialogProps) {
  // UX-014/UX-015: solo las etiquetas de Persona/Proyecto cambian.
  const vocabulary = useVocabulary()
  const [isOpen, setIsOpen] = useState(false)
  const [title, setTitle] = useState('')
  const [dueAtLocal, setDueAtLocal] = useState('')
  const [personId, setPersonId] = useState('')
  const [projectId, setProjectId] = useState('')
  const [location, setLocation] = useState('')
  const [newPlaceName, setNewPlaceName] = useState('')
  const [newPlaceAddress, setNewPlaceAddress] = useState('')
  const [showNewPlace, setShowNewPlace] = useState(false)
  const [savingPlace, setSavingPlace] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function reset() {
    setTitle('')
    setDueAtLocal('')
    setPersonId('')
    setProjectId('')
    setLocation('')
    setNewPlaceName('')
    setNewPlaceAddress('')
    setShowNewPlace(false)
    setError(null)
  }

  /**
   * UC-26: elegir un Lugar guardado **copia su texto** al campo `location`
   * del REMINDER — no crea ninguna relación en base de datos (no existe
   * `REMINDER.place_id`, fuera de alcance explícito de FR-033). El usuario
   * puede editar el texto resultante libremente después.
   */
  function handlePickPlace(placeId: string) {
    const place = places.find((p) => p.id === placeId)
    if (place) setLocation(placeLocationText(place))
  }

  async function handleCreatePlace() {
    if (!newPlaceName.trim() || savingPlace) return
    setSavingPlace(true)
    setError(null)
    try {
      const created = await createPlace({
        name: newPlaceName.trim(),
        address: newPlaceAddress.trim() || undefined,
        personId: personId || undefined,
      })
      onPlaceCreated?.(created)
      setLocation(placeLocationText(created))
      setShowNewPlace(false)
      setNewPlaceName('')
      setNewPlaceAddress('')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar el lugar.')
    } finally {
      setSavingPlace(false)
    }
  }

  function handleOpenChange(open: boolean) {
    setIsOpen(open)
    if (!open) reset()
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!title.trim() || saving) return

    setSaving(true)
    setError(null)
    try {
      const input: CreateReminderInput = { title: title.trim(), context: 'LABORAL' }
      if (dueAtLocal) input.dueAt = new Date(dueAtLocal).toISOString()
      if (personId) input.personId = personId
      if (projectId) input.projectId = projectId
      if (location.trim()) input.location = location.trim()
      const created = await createReminder(input)
      onCreated(created)
      setIsOpen(false)
      reset()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar la tarea.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={handleOpenChange}>
      {/* El `button` global de la app mide 44px de alto, va a 15px y usa un
          radio de 20px; el prototipo pide la acción primaria a 9px/16px,
          14px y radio de control. Se aplica la clase del sistema de Laboral
          en vez de tocar el estilo global, que también viste a Personal. */}
      <Button className={laboralUI.btnPrimary}>
        <IconPlus width={16} height={16} /> Nueva tarea
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
                    Nueva tarea
                  </Heading>
                  <button type="button" className={shellStyles.closeButton} onClick={close} aria-label="Cerrar">
                    ×
                  </button>
                </div>

                {error && <p className={shellStyles.formError} role="alert">{error}</p>}

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Título</span>
                  <input
                    className={shellStyles.textInput}
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="Enviar propuesta comercial"
                    autoFocus
                  />
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Fecha (opcional)</span>
                  <input
                    className={shellStyles.textInput}
                    type="date"
                    value={dueAtLocal}
                    onChange={(e) => setDueAtLocal(e.target.value)}
                  />
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>{vocabulary.person} (opcional)</span>
                  <select className={shellStyles.textInput} value={personId} onChange={(e) => setPersonId(e.target.value)}>
                    <option value="">— Ninguna —</option>
                    {people.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                  </select>
                </label>

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>{vocabulary.project} (opcional)</span>
                  <select className={shellStyles.textInput} value={projectId} onChange={(e) => setProjectId(e.target.value)}>
                    <option value="">— Ninguno —</option>
                    {projects.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                  </select>
                </label>

                {/* ADR-016 Fase 3e3/FR-033, UC-26: catálogo de Lugares guardados.
                    Elegir uno copia su texto al campo de ubicación de siempre
                    (REMINDER.location, FR-024) — sin FK nueva. */}
                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Lugar guardado (opcional)</span>
                  <select
                    className={shellStyles.textInput}
                    value=""
                    onChange={(e) => {
                      if (e.target.value === '__new__') {
                        setShowNewPlace(true)
                      } else if (e.target.value) {
                        handlePickPlace(e.target.value)
                      }
                    }}
                  >
                    <option value="">— Elegir un lugar —</option>
                    {places.map((p) => (
                      <option key={p.id} value={p.id}>
                        {p.name}
                      </option>
                    ))}
                    <option value="__new__">+ Nuevo lugar…</option>
                  </select>
                </label>

                {showNewPlace && (
                  <>
                    <label className={shellStyles.field}>
                      <span className={shellStyles.fieldLabel}>Nombre del lugar</span>
                      <input
                        className={shellStyles.textInput}
                        value={newPlaceName}
                        onChange={(e) => setNewPlaceName(e.target.value)}
                        placeholder="Oficina ACME"
                      />
                    </label>
                    <label className={shellStyles.field}>
                      <span className={shellStyles.fieldLabel}>Dirección (opcional)</span>
                      <input
                        className={shellStyles.textInput}
                        value={newPlaceAddress}
                        onChange={(e) => setNewPlaceAddress(e.target.value)}
                        placeholder="Av. Reforma 123"
                      />
                    </label>
                    <div className={shellStyles.formActions}>
                      {savingPlace && <span className={shellStyles.savingHint}>Guardando lugar…</span>}
                      <button type="button" data-variant="secondary" onClick={() => setShowNewPlace(false)} disabled={savingPlace}>
                        Cancelar lugar
                      </button>
                      <button type="button" data-variant="secondary" onClick={() => void handleCreatePlace()} disabled={savingPlace}>
                        Guardar lugar
                      </button>
                    </div>
                  </>
                )}

                <label className={shellStyles.field}>
                  <span className={shellStyles.fieldLabel}>Ubicación (opcional)</span>
                  <input
                    className={shellStyles.textInput}
                    value={location}
                    onChange={(e) => setLocation(e.target.value)}
                    placeholder="Sala de juntas, obra, oficina del cliente…"
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
