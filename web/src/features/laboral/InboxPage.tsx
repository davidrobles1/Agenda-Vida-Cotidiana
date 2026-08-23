import { useEffect, useState, type FormEvent } from 'react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { ListSectionCard } from '../../core/ui/components/ListSectionCard'
import { IconPlus } from '../../core/ui/icons'
import { createReminder } from '../reminders/api'
import { createCommitment } from '../commitments/api'
import { listPeople, type Person } from '../people/api'
import { addInboxItem, loadInboxItems, removeInboxItem, type InboxItem } from './inboxStorage'
import styles from './InboxPage.module.css'

/**
 * ADR-016/FR-028. Captura sin fricción, clasifica después. El backend NOTE
 * no se extendió con vínculos opcionales en la Fase 1 (ver
 * `inboxStorage.ts` para el razonamiento completo) — este Inbox vive
 * enteramente en `localStorage`, por dispositivo, y "convertir" crea una
 * Tarea/Compromiso real vía la API antes de borrar el ítem local.
 */
export function InboxPage() {
  const [items, setItems] = useState<InboxItem[]>([])
  const [people, setPeople] = useState<Person[]>([])
  const [quickCapture, setQuickCapture] = useState('')
  const [convertingId, setConvertingId] = useState<string | null>(null)
  const [commitmentPersonId, setCommitmentPersonId] = useState('')
  const [commitmentDueAt, setCommitmentDueAt] = useState('')
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setItems(loadInboxItems())
    listPeople()
      .then((page) => setPeople(page.items))
      .catch(() => {
        /* el selector de Persona solo se necesita al convertir a Seguimiento — no bloquea el Inbox */
      })
  }, [])

  function handleAdd(event: FormEvent) {
    event.preventDefault()
    if (!quickCapture.trim()) return
    setItems(addInboxItem(quickCapture.trim()))
    setQuickCapture('')
  }

  function handleDiscard(id: string) {
    setItems(removeInboxItem(id))
  }

  async function handleConvertToTask(item: InboxItem) {
    setError(null)
    try {
      await createReminder({ title: item.text, context: 'LABORAL' })
      setItems(removeInboxItem(item.id))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo convertir a tarea.')
    }
  }

  function startConvertToCommitment(item: InboxItem) {
    setConvertingId(item.id)
    setCommitmentPersonId(people[0]?.id ?? '')
    setCommitmentDueAt('')
    setError(null)
  }

  async function handleConvertToCommitment(event: FormEvent, item: InboxItem) {
    event.preventDefault()
    if (!commitmentPersonId || !commitmentDueAt) return
    setError(null)
    try {
      await createCommitment({
        personId: commitmentPersonId,
        description: item.text,
        direction: 'MINE',
        dueAt: new Date(commitmentDueAt).toISOString(),
      })
      setItems(removeInboxItem(item.id))
      setConvertingId(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo convertir a seguimiento.')
    }
  }

  return (
    <AppShell title="Inbox" subtitle="Captura sin decidir. Clasifica cuando tengas tiempo.">
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      <ListSectionCard title="Captura rápida">
        <form className={styles.captureForm} onSubmit={handleAdd}>
          <input
            className={styles.captureInput}
            value={quickCapture}
            onChange={(e) => setQuickCapture(e.target.value)}
            placeholder="Escribe algo que no quieras olvidar…"
          />
          <button type="submit">
            <IconPlus width={16} height={16} /> Agregar
          </button>
        </form>
      </ListSectionCard>

      <ListSectionCard title={`Sin clasificar (${items.length})`}>
        {items.length === 0 && <p className={styles.emptyHint}>Inbox vacío.</p>}
        {items.map((item) => (
          <div key={item.id} className={styles.item}>
            <div className={styles.itemRow}>
              <span className={styles.itemText}>{item.text}</span>
              <div className={styles.itemActions}>
                <button type="button" onClick={() => void handleConvertToTask(item)}>
                  Tarea
                </button>
                <button type="button" data-variant="secondary" onClick={() => startConvertToCommitment(item)}>
                  Seguimiento
                </button>
                <button type="button" data-variant="destructive" onClick={() => handleDiscard(item.id)}>
                  Descartar
                </button>
              </div>
            </div>

            {convertingId === item.id && (
              <form className={styles.convertForm} onSubmit={(e) => void handleConvertToCommitment(e, item)}>
                <select value={commitmentPersonId} onChange={(e) => setCommitmentPersonId(e.target.value)}>
                  {people.length === 0 && <option value="">Sin personas creadas todavía</option>}
                  {people.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
                <input type="date" value={commitmentDueAt} onChange={(e) => setCommitmentDueAt(e.target.value)} />
                <button type="submit" disabled={!commitmentPersonId || !commitmentDueAt}>
                  Guardar
                </button>
                <button type="button" data-variant="secondary" onClick={() => setConvertingId(null)}>
                  Cancelar
                </button>
              </form>
            )}
          </div>
        ))}
      </ListSectionCard>
    </AppShell>
  )
}
