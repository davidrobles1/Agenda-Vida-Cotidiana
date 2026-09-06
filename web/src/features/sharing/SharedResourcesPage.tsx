import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Button, ToggleButton, ToggleButtonGroup, type Key } from 'react-aria-components'
import { Check, RotateCcw, X } from 'lucide-react'
import { AppShell } from '../../core/ui/layout/AppShell'
import {
  PART_DONE_LABELS,
  RESOURCE_LABELS,
  RESOURCE_ROUTES,
  listReceived,
  listSent,
  markPartDone,
  reopenPart,
  revokeShare,
  type ResourceShare,
} from './resourceShares'
import {
  acceptInvitation,
  listMyInvitations,
  rejectInvitation,
  type Invitation,
} from './api'
import styles from './SharedResourcesPage.module.css'

/**
 * Compartidos (ADR-025 §3).
 *
 * Punto central de la colaboración familiar, con las dos direcciones
 * claramente separadas: lo que otros me compartieron y lo que yo compartí. La
 * separación es una pestaña, no un matiz de color, porque confundir recibido
 * con enviado cambia quién tiene que actuar.
 *
 * NO DUPLICA RECURSOS: cada fila es la relación de compartición y enlaza al
 * módulo donde vive el original.
 *
 * Conserva además las invitaciones a RECORDATORIOS por correo, que es lo que
 * esta pantalla mostraba antes de ADR-025 (endpoints de V2, intactos). Son un
 * flujo distinto al familiar y no se ha retirado nada de él.
 */

type Tab = 'received' | 'sent'

function formatDate(iso?: string): string | null {
  if (!iso) return null
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return null
  return date.toLocaleDateString('es-MX', { day: 'numeric', month: 'short', year: 'numeric' })
}

export function SharedResourcesPage() {
  const [tab, setTab] = useState<Tab>('received')
  const [received, setReceived] = useState<ResourceShare[]>([])
  const [sent, setSent] = useState<ResourceShare[]>([])
  const [reminderInvitations, setReminderInvitations] = useState<Invitation[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    try {
      const [nextReceived, nextSent, nextInvitations] = await Promise.all([
        listReceived(),
        listSent(),
        // Si este falla no debe tumbar la pantalla entera: es el flujo antiguo
        // y es independiente del familiar.
        listMyInvitations().catch(() => [] as Invitation[]),
      ])
      setReceived(nextReceived)
      setSent(nextSent)
      setReminderInvitations(nextInvitations)
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar los compartidos.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  async function run(id: string, action: () => Promise<unknown>) {
    setBusyId(id)
    try {
      await action()
      await refresh()
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo completar la acción.')
    } finally {
      setBusyId(null)
    }
  }

  const rows = tab === 'received' ? received : sent

  return (
    <AppShell
      title="Compartidos"
      subtitle="Lo que tu familia comparte contigo y lo que tú compartes con ella."
    
      actions={<><ToggleButtonGroup
          className={styles.tabs}
          selectionMode="single"
          disallowEmptySelection
          selectedKeys={new Set<Key>([tab])}
          onSelectionChange={(keys) => {
            const next = [...keys][0]
            if (next === 'received' || next === 'sent') setTab(next)
          }}
          aria-label="Dirección de la compartición"
        >
          <ToggleButton id="received" className={styles.tab}>
            Me compartieron
          </ToggleButton>
          <ToggleButton id="sent" className={styles.tab}>
            Yo compartí
          </ToggleButton>
        </ToggleButtonGroup></>}
      actionsHint={loading
            ? undefined
            : `${received.length} ${received.length === 1 ? 'recibido' : 'recibidos'} · ${sent.length} ${
                sent.length === 1 ? 'compartido' : 'compartidos'
              }`}
    >
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      {/* Invitaciones a recordatorios (flujo por correo previo a ADR-025). */}
      {reminderInvitations.length > 0 && (
        <section className={styles.panel} aria-label="Invitaciones a recordatorios">
          <h2 className={styles.panelTitle}>Invitaciones a recordatorios</h2>
          <ul className={styles.list}>
            {reminderInvitations.map((invitation) => (
              <li key={invitation.id} className={styles.row}>
                <span className={styles.rowMain}>
                  <span className={styles.rowLabel}>{invitation.invitedEmail}</span>
                  <span className={styles.rowMeta}>Invitación pendiente</span>
                </span>
                <span className={styles.rowActions}>
                  <Button
                    className={styles.primaryButton}
                    isDisabled={busyId === invitation.id}
                    onPress={() => void run(invitation.id, () => acceptInvitation(invitation.id))}
                  >
                    Aceptar
                  </Button>
                  <Button
                    className={styles.ghostButton}
                    isDisabled={busyId === invitation.id}
                    onPress={() => void run(invitation.id, () => rejectInvitation(invitation.id))}
                  >
                    Rechazar
                  </Button>
                </span>
              </li>
            ))}
          </ul>
        </section>
      )}

      <section
        className={styles.panel}
        aria-label={tab === 'received' ? 'Recursos que me compartieron' : 'Recursos que compartí'}
      >
        <h2 className={styles.panelTitle}>
          {tab === 'received' ? 'Me compartieron' : 'Yo compartí'}
        </h2>

        {loading ? (
          <p className={styles.note}>Cargando…</p>
        ) : error ? (
          /* Un vacío afirma un hecho sobre los datos del usuario; si la carga
             falló no sabemos nada de ellos (ADR-021 k). */
          null
        ) : rows.length === 0 ? (
          <p className={styles.note}>
            {tab === 'received'
              ? 'Todavía nadie de tu familia ha compartido algo contigo.'
              : 'Todavía no has compartido nada. Al crear o editar un recurso puedes elegir con quién.'}
          </p>
        ) : (
          <ul className={styles.list}>
            {rows.map((share) => {
              const partLabel = PART_DONE_LABELS[share.resourceType]
              const date = formatDate(share.resourceDate)
              const done = !!share.partDoneAt

              return (
                <li key={share.id} className={styles.row} data-done={done || undefined}>
                  <span className={styles.type}>{RESOURCE_LABELS[share.resourceType]}</span>

                  <span className={styles.rowMain}>
                    <Link className={styles.rowLabel} to={RESOURCE_ROUTES[share.resourceType]}>
                      {share.resourceLabel}
                    </Link>
                    <span className={styles.rowMeta}>
                      {tab === 'received' ? 'De ' : 'Con '}
                      <strong>{share.counterpartUsername ?? 'alguien de tu familia'}</strong>
                      {date ? ` · ${date}` : ''}
                    </span>
                  </span>

                  {/* Estado de la responsabilidad — el mismo hecho contado desde
                      cada lado: quien recibe ve qué le toca, quien comparte ve
                      si ya se hizo. */}
                  {share.responsibility && (
                    <span className={done ? styles.badgeDone : styles.badgePending}>
                      {done
                        ? tab === 'received'
                          ? 'Hiciste tu parte'
                          : 'Ya hizo su parte'
                        : tab === 'received'
                          ? 'Te toca'
                          : 'Pendiente'}
                    </span>
                  )}

                  <span className={styles.rowActions}>
                    {tab === 'received' && share.responsibility && partLabel && (
                      done ? (
                        <Button
                          className={styles.ghostButton}
                          isDisabled={busyId === share.id}
                          onPress={() => void run(share.id, () => reopenPart(share.id))}
                        >
                          <RotateCcw width={14} height={14} aria-hidden="true" />
                          Deshacer
                        </Button>
                      ) : (
                        <Button
                          className={styles.primaryButton}
                          isDisabled={busyId === share.id}
                          onPress={() => void run(share.id, () => markPartDone(share.id))}
                        >
                          <Check width={14} height={14} aria-hidden="true" />
                          {partLabel}
                        </Button>
                      )
                    )}

                    {tab === 'sent' && (
                      <Button
                        className={styles.ghostButton}
                        aria-label={`Dejar de compartir ${share.resourceLabel}`}
                        isDisabled={busyId === share.id}
                        onPress={() => void run(share.id, () => revokeShare(share.id))}
                      >
                        <X width={14} height={14} aria-hidden="true" />
                        Dejar de compartir
                      </Button>
                    )}
                  </span>
                </li>
              )
            })}
          </ul>
        )}
      </section>
    </AppShell>
  )
}
