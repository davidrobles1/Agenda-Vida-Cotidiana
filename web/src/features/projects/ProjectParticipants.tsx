import { useEffect, useState } from 'react'
import { useVocabulary } from '../../core/user/useVocabulary'
import shellStyles from '../../core/ui/dialogs/DialogShell.module.css'
import { PersonPicker } from '../people/PersonPicker'
import type { Person } from '../people/api'
import {
  PARTICIPANT_ROLES,
  PARTICIPANT_ROLE_LABELS,
  addParticipant,
  listParticipants,
  removeParticipant,
  type ParticipantRole,
  type Project,
  type ProjectParticipant,
} from './api'
import styles from './ProjectParticipants.module.css'

interface ProjectParticipantsProps {
  project: Project
  clientPerson?: Person
  people: Person[]
  onPersonCreated?: (person: Person) => void
}

/**
 * Quién está en este proyecto (V32).
 *
 * QUÉ RESUELVE: un proyecto solo sabía de UNA persona, su cliente. El
 * proveedor, el contacto en obra y el colaborador no tenían dónde vivir, así
 * que la única forma de relacionarlos era colgarles una tarea y deducirlo. No
 * se podía responder "¿quién está en esta obra?".
 *
 * EL CLIENTE SE MUESTRA AQUÍ PERO NO ES UN PARTICIPANTE. Sigue siendo el campo
 * del proyecto (DECISION del Product Owner, 2026-09-06: cliente y
 * participantes conviven). Que convivan sin contradecirse exige que sean
 * disjuntos: por eso el cliente aparece arriba, marcado y sin botón de quitar,
 * y CLIENTE no está entre los roles. El servidor rechaza mezclarlos con un 400,
 * y ese mensaje se muestra tal cual porque explica exactamente qué hacer.
 */
export function ProjectParticipants({ project, clientPerson, people, onPersonCreated }: ProjectParticipantsProps) {
  const vocabulary = useVocabulary()
  const [participants, setParticipants] = useState<ProjectParticipant[] | null>(null)
  const [adding, setAdding] = useState(false)
  const [personId, setPersonId] = useState('')
  const [role, setRole] = useState<ParticipantRole>('PROVEEDOR')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    listParticipants(project.id)
      .then((rows) => {
        if (!cancelled) setParticipants(rows)
      })
      .catch(() => {
        // Que falle esta lista no debe tumbar el detalle entero: el resto del
        // diálogo (tareas, notas, documentos) sigue siendo útil.
        if (!cancelled) setParticipants([])
      })
    return () => {
      cancelled = true
    }
  }, [project.id])

  const nameById = new Map(people.map((person) => [person.id, person]))

  async function handleAdd() {
    if (!personId || saving) return
    setSaving(true)
    setError(null)
    try {
      const added = await addParticipant(project.id, personId, role)
      setParticipants((current) => [...(current ?? []), added])
      setPersonId('')
      setAdding(false)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo añadir a esa persona.')
    } finally {
      setSaving(false)
    }
  }

  async function handleRemove(participantId: string) {
    setError(null)
    try {
      await removeParticipant(project.id, participantId)
      setParticipants((current) => (current ?? []).filter((row) => row.id !== participantId))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo quitar a esa persona.')
    }
  }

  const rows = participants ?? []
  const total = rows.length + (clientPerson ? 1 : 0)

  return (
    <section className={styles.section}>
      <div className={styles.header}>
        <h3 className={styles.title}>Quién está aquí</h3>
        <span className={styles.count}>
          {total === 0 ? 'nadie todavía' : total === 1 ? '1 persona' : `${total} personas`}
        </span>
      </div>

      {error && (
        <p className={shellStyles.formError} role="alert">
          {error}
        </p>
      )}

      <ul className={styles.list}>
        {/* El cliente encabeza la lista: es la relación más fuerte y la única
            que no se quita desde aquí — se cambia editando el proyecto. */}
        {clientPerson && (
          <li className={styles.row} data-client="true">
            <span className={styles.name}>{clientPerson.name}</span>
            <span className={styles.role}>Cliente</span>
            <span className={styles.rowNote}>se cambia al editar el {vocabulary.project.toLowerCase()}</span>
          </li>
        )}

        {rows.map((participant) => {
          const person = nameById.get(participant.personId)
          return (
            <li key={participant.id} className={styles.row}>
              <span className={styles.name}>{person?.name ?? 'Persona eliminada'}</span>
              <span className={styles.role}>{PARTICIPANT_ROLE_LABELS[participant.role]}</span>
              {person?.organization && <span className={styles.rowNote}>{person.organization}</span>}
              <button
                type="button"
                className={styles.remove}
                onClick={() => void handleRemove(participant.id)}
                aria-label={`Quitar a ${person?.name ?? 'esta persona'} del ${vocabulary.project.toLowerCase()}`}
              >
                Quitar
              </button>
            </li>
          )
        })}
      </ul>

      {participants !== null && total === 0 && !adding && (
        <p className={styles.empty}>
          Añade a quien participe —proveedor, contacto, colaborador— y sabrás de un vistazo quién está en{' '}
          {vocabulary.projectGender === 'f' ? 'esta' : 'este'} {vocabulary.project.toLowerCase()}.
        </p>
      )}

      {!adding && (
        <button type="button" className={styles.addAction} onClick={() => setAdding(true)}>
          Añadir a alguien
        </button>
      )}

      {adding && (
        <div className={styles.addForm}>
          <PersonPicker
            people={people}
            value={personId}
            onChange={setPersonId}
            onCreated={onPersonCreated}
            required
            label="¿Quién?"
          />
          <label className={shellStyles.field}>
            <span className={shellStyles.fieldLabel}>¿A qué título?</span>
            <select
              className={shellStyles.textInput}
              value={role}
              onChange={(event) => setRole(event.target.value as ParticipantRole)}
            >
              {PARTICIPANT_ROLES.map((option) => (
                <option key={option} value={option}>
                  {PARTICIPANT_ROLE_LABELS[option]}
                </option>
              ))}
            </select>
          </label>
          <div className={styles.addActions}>
            <button type="button" data-variant="secondary" onClick={() => setAdding(false)} disabled={saving}>
              Cancelar
            </button>
            <button type="button" onClick={() => void handleAdd()} disabled={saving || !personId}>
              {saving ? 'Añadiendo…' : 'Añadir'}
            </button>
          </div>
        </div>
      )}
    </section>
  )
}
