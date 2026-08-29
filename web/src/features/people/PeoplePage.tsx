import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { AppShell } from '../../core/ui/layout/AppShell'
import { SimpleDeleteConfirm } from '../../core/ui/dialogs/SimpleDeleteConfirm'
import { deletePerson, listPeople, type Person } from './api'
import { listCommitments, type Commitment } from '../commitments/api'
import { listReminders, type Reminder } from '../reminders/api'
import { listProjects, type Project } from '../projects/api'
import { listNotes } from '../calendar/notes/api'
import type { Note } from '../calendar/notes/notesData'
import { listDocuments, type VidaDocument } from '../documents/api'
import { listResources, type Resource } from '../resources/api'
import { useVocabulary } from '../../core/user/useVocabulary'
import { CreatePersonDialog } from './CreatePersonDialog'
import { PersonDetailDialog } from './PersonDetailDialog'
import { computeLastInteraction, formatRelativeDate, initialsOf } from './personSummary'
import styles from './PeoplePage.module.css'

/**
 * ADR-016/FR-021, UC-18. Núcleo del Módulo Laboral — lista de Personas.
 *
 * REDISEÑO (2026-08-28): trasladada al prototipo aprobado ("Agenda
 * Laboral", artifact fca1566a, `pagePersonas()`) — cabecera con antetítulo
 * "Núcleo" y una rejilla de tarjetas, en vez de la lista de filas anterior.
 * Cada tarjeta reproduce la del prototipo: avatar grande con iniciales,
 * nombre y "rol · organización", separador, última interacción, y un badge
 * con los compromisos abiertos.
 *
 * La tarjeta entera es el destino, como en el prototipo. Ahí navega a
 * `/laboral/personas/:id`; aquí el detalle vive en un diálogo
 * (`PersonDetailDialog`, patrón real de la aplicación y el mismo que usa
 * Proyectos), así que la tarjeta lo abre en vez de navegar — no se inventó
 * una ruta de detalle que no existe.
 *
 * Se conservan dos acciones que el prototipo no dibuja en esta pantalla,
 * porque son la única vía para ellas en la aplicación y quitarlas habría
 * sido una decisión funcional, no visual: crear persona (en la cabecera,
 * mismo sitio donde el propio prototipo pone la acción primaria de Tareas)
 * y eliminar (en la tarjeta).
 */
export function PeoplePage() {
  const vocabulary = useVocabulary()
  const [people, setPeople] = useState<Person[]>([])
  const [commitments, setCommitments] = useState<Commitment[]>([])
  const [tasks, setTasks] = useState<Reminder[]>([])
  const [projects, setProjects] = useState<Project[]>([])
  const [notes, setNotes] = useState<Note[]>([])
  const [documents, setDocuments] = useState<VidaDocument[]>([])
  const [resources, setResources] = useState<Resource[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  /** La tarjeta abre el detalle; este es el registro abierto. */
  const [openPersonId, setOpenPersonId] = useState<string | null>(null)

  /* `?open=<id>` abre directamente ese detalle: es el destino al que llevan
     los chips de Persona desde una Tarea, ya que el detalle vive en este
     diálogo y no en una ruta propia. */
  const [searchParams, setSearchParams] = useSearchParams()
  const requestedPersonId = searchParams.get('open')

  useEffect(() => {
    if (requestedPersonId) setOpenPersonId(requestedPersonId)
  }, [requestedPersonId])

  async function refresh() {
    setLoading(true)
    setError(null)
    try {
      const [peoplePage, commitmentsPage, remindersPage, projectsPage, notesPage, documentsPage, resourcesPage] =
        await Promise.all([
          listPeople(),
          listCommitments(),
          listReminders(),
          listProjects(),
          listNotes(),
          listDocuments(),
          listResources(),
        ])
      setPeople(peoplePage.items)
      setCommitments(commitmentsPage.items)
      setTasks(remindersPage.items)
      setProjects(projectsPage.items)
      setNotes(notesPage.items)
      setDocuments(documentsPage.items)
      setResources(resourcesPage.items)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar las personas.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  function handleCreated(person: Person) {
    setPeople((current) => [person, ...current])
  }

  function handleCommitmentCreated(commitment: Commitment) {
    setCommitments((current) => [commitment, ...current])
  }

  function handleNoteCreated(note: Note) {
    setNotes((current) => [note, ...current])
  }

  function handleResourceCreated(resource: Resource) {
    setResources((current) => [resource, ...current])
  }

  /** ADR-016 Fase 3d: la nota vuelve con su sugerencia ya resuelta. */
  function handleNoteUpdated(note: Note) {
    setNotes((current) => current.map((n) => (n.id === note.id ? note : n)))
  }

  function handleTaskCreated(task: Reminder) {
    setTasks((current) => [task, ...current])
  }

  async function handleDelete(id: string) {
    await deletePerson(id)
    setPeople((current) => current.filter((p) => p.id !== id))
  }

  const openPerson = people.find((p) => p.id === openPersonId) ?? null

  return (
    <AppShell title={vocabulary.personPlural} subtitle="Clientes, colegas y proveedores — el mismo modelo para cualquier profesión.">
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      <div className={styles.pageHead}>
        <div>
          <p className={styles.kicker}>Núcleo</p>
          <h2 className={styles.pageTitle}>{vocabulary.personPlural}</h2>
        </div>
        <CreatePersonDialog onCreated={handleCreated} />
      </div>

      {loading && <p className={styles.emptyHint}>Cargando…</p>}

      {!loading && people.length === 0 && (
        <p className={styles.emptyHint}>
          Todavía no has agregado {vocabulary.personGender === 'f' ? 'ninguna' : 'ningún'}{' '}
          {vocabulary.person.toLowerCase()}.
        </p>
      )}

      {!loading && people.length > 0 && (
        <div className={styles.gridCards}>
          {people.map((person) => {
            const personCommitments = commitments.filter((c) => c.personId === person.id)
            const openCount = personCommitments.filter((c) => c.status === 'OPEN').length
            const lastInteractionAt = computeLastInteraction(
              personCommitments,
              tasks.filter((t) => t.personId === person.id),
              notes.filter((n) => n.personId === person.id),
            )

            return (
              <div key={person.id} className={styles.personCard}>
                <button
                  type="button"
                  className={styles.personCardTrigger}
                  onClick={() => setOpenPersonId(person.id)}
                  aria-label={`Ver ${person.name}`}
                >
                  <div className={styles.personCardHead}>
                    <span className={`${styles.avatar} ${styles.avatarLg}`} aria-hidden="true">
                      {initialsOf(person.name)}
                    </span>
                    <div className={styles.personCardBody}>
                      <div className={styles.personName}>{person.name}</div>
                      {(person.role || person.organization) && (
                        <div className={styles.personMeta}>
                          {[person.role, person.organization].filter(Boolean).join(' · ')}
                        </div>
                      )}
                    </div>
                  </div>

                  <hr className={styles.divider} />

                  <div className={styles.lastInteraction}>
                    {lastInteractionAt
                      ? `Última interacción: ${formatRelativeDate(lastInteractionAt)}`
                      : 'Sin interacciones registradas'}
                  </div>
                </button>

                <div className={styles.cardBadgeRow}>
                  {openCount > 0 ? (
                    <span className={styles.badge} data-tone="warning">
                      {openCount} compromiso{openCount === 1 ? '' : 's'} abierto{openCount === 1 ? '' : 's'}
                    </span>
                  ) : (
                    <span className={styles.badge} data-tone="success">
                      Sin pendientes
                    </span>
                  )}

                  <SimpleDeleteConfirm
                    resourceLabel={vocabulary.person.toLowerCase()}
                    itemName={person.name}
                    ariaLabel={`Eliminar ${vocabulary.person.toLowerCase()}`}
                    onConfirm={() => handleDelete(person.id)}
                  />
                </div>
              </div>
            )
          })}
        </div>
      )}

      {openPerson && (
        <PersonDetailDialog
          key={openPerson.id}
          person={openPerson}
          isOpen
          onOpenChange={(open) => {
            if (!open) {
              setOpenPersonId(null)
              // Al cerrar se limpia el parámetro: si no, volver atrás
              // reabriría el mismo detalle.
              if (requestedPersonId) setSearchParams({}, { replace: true })
            }
          }}
          commitments={commitments.filter((c) => c.personId === openPerson.id)}
          tasks={tasks.filter((t) => t.personId === openPerson.id)}
          projects={projects.filter((p) => p.clientPersonId === openPerson.id)}
          notes={notes.filter((n) => n.personId === openPerson.id)}
          documents={documents.filter((d) => d.personId === openPerson.id)}
          resources={resources.filter((r) => r.personId === openPerson.id)}
          onCommitmentCreated={handleCommitmentCreated}
          onNoteCreated={handleNoteCreated}
          onResourceCreated={handleResourceCreated}
          onNoteUpdated={handleNoteUpdated}
          onTaskCreated={handleTaskCreated}
        />
      )}
    </AppShell>
  )
}
