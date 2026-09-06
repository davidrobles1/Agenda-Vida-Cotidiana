import { useCallback, useEffect, useState } from 'react'
import { Button } from 'react-aria-components'
import {
  acceptInvitation,
  listReceivedInvitations,
  rejectInvitation,
  type FamilyInvitation,
} from '../family/api'
import styles from './SettingsPage.module.css'

/**
 * Invitaciones familiares recibidas (ADR-025 §1).
 *
 * Va en Ajustes por requisito explícito: "como parte fundamental de la sección
 * Configuración, debe existir un apartado donde pueda consultar las
 * invitaciones que otros usuarios me hayan enviado".
 *
 * Es componente aparte y no más JSX dentro de `SettingsPage` porque tiene
 * estado propio y carga por su cuenta: mezclarlo habría hecho que un fallo al
 * pedir invitaciones se llevara por delante el resto de los ajustes.
 *
 * El apartado se OCULTA cuando no hay ninguna pendiente. Un bloque permanente
 * que casi siempre dice "no hay nada" es ruido en una pantalla de ajustes; en
 * cuanto llega una invitación aparece solo.
 */
export function FamilyInvitationsSection() {
  const [invitations, setInvitations] = useState<FamilyInvitation[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    try {
      setInvitations(await listReceivedInvitations())
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudieron cargar tus invitaciones.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  async function run(id: string, action: () => Promise<void>) {
    setBusyId(id)
    try {
      await action()
      await refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo responder la invitación.')
    } finally {
      setBusyId(null)
    }
  }

  // Sin pendientes y sin error, el apartado no existe. Con error sí se muestra:
  // callar un fallo sería afirmar que no hay invitaciones sin saberlo.
  if (loading || (invitations.length === 0 && !error)) {
    return null
  }

  return (
    <section className={styles.profileSection} aria-labelledby="family-invitations-heading">
      <h2 id="family-invitations-heading" className={styles.sectionTitle}>
        Invitaciones a una familia
      </h2>
      <p className={styles.sectionHint}>
        Alguien quiere compartir contigo su día a día. Al aceptar podréis compartiros tareas, pagos,
        mantenimientos, garantías, artículos y documentos.
      </p>

      {error && (
        <p role="alert" className={styles.invitationError}>
          {error}
        </p>
      )}

      <ul className={styles.invitationList}>
        {invitations.map((invitation) => (
          <li key={invitation.id} className={styles.invitationRow}>
            <span className={styles.invitationAvatar} aria-hidden="true">
              {(invitation.counterpartUsername ?? '?').charAt(0).toUpperCase()}
            </span>
            <span className={styles.invitationText}>
              <strong>{invitation.counterpartUsername ?? 'Alguien'}</strong> te invitó a su familia
            </span>
            <span className={styles.invitationState}>Pendiente</span>
            <span className={styles.invitationActions}>
              <Button
                className={styles.invitationAccept}
                isDisabled={busyId === invitation.id}
                onPress={() => void run(invitation.id, () => acceptInvitation(invitation.id))}
              >
                Aceptar
              </Button>
              <Button
                className={styles.invitationReject}
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
  )
}
