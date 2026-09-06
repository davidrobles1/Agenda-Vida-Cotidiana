import { useCallback, useEffect, useMemo, useState } from 'react'
import { Button } from 'react-aria-components'
import { Search, UserPlus, X } from 'lucide-react'
import { AppShell } from '../../core/ui/layout/AppShell'
import { useDebouncedValue } from '../../core/hooks/useDebouncedValue'
import {
  MIN_SEARCH_LENGTH,
  cancelInvitation,
  invite,
  listMembers,
  listSentInvitations,
  removeMember,
  searchUsers,
  type FamilyInvitation,
  type FamilyMember,
  type UserSearchResult,
} from './api'
import styles from './FamilyPage.module.css'

/**
 * Familia (ADR-025 §1).
 *
 * Deja de ser andamiaje: hasta ahora pintaba `core/mock/mockData.ts` y no
 * hablaba con ningún endpoint. Ahora lista integrantes reales, busca personas
 * por nombre de usuario y envía invitaciones.
 *
 * Las invitaciones RECIBIDAS no se responden aquí: viven en Ajustes, donde el
 * requisito §1 las pide. Aquí solo se ven las que uno mismo envió y siguen sin
 * respuesta, para poder cancelarlas.
 */

const RELATION_LABELS: Record<UserSearchResult['relation'], string | null> = {
  NONE: null,
  INVITATION_SENT: 'Invitación enviada',
  INVITATION_RECEIVED: 'Te invitó',
  FAMILY: 'Ya es de tu familia',
}

export function FamilyPage() {
  const [members, setMembers] = useState<FamilyMember[]>([])
  const [sent, setSent] = useState<FamilyInvitation[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [term, setTerm] = useState('')
  const [results, setResults] = useState<UserSearchResult[]>([])
  const [searching, setSearching] = useState(false)
  const [searchError, setSearchError] = useState<string | null>(null)
  const [busyUserId, setBusyUserId] = useState<string | null>(null)

  // Requisito §1: ni una consulta por cada tecla. El valor solo "se asienta"
  // cuando pasan 350 ms sin escribir.
  const settledTerm = useDebouncedValue(term, 350)
  const enoughCharacters = settledTerm.trim().length >= MIN_SEARCH_LENGTH

  const refresh = useCallback(async () => {
    try {
      const [nextMembers, nextSent] = await Promise.all([listMembers(), listSentInvitations()])
      setMembers(nextMembers)
      setSent(nextSent)
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo cargar tu familia.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  useEffect(() => {
    if (!enoughCharacters) {
      // Por debajo del mínimo no se consulta NADA y se limpia lo anterior, para
      // que no queden resultados de una búsqueda que ya no corresponde.
      setResults([])
      setSearchError(null)
      setSearching(false)
      return
    }

    let cancelled = false
    setSearching(true)
    searchUsers(settledTerm)
      .then((found) => {
        if (!cancelled) {
          setResults(found)
          setSearchError(null)
        }
      })
      .catch((e: unknown) => {
        if (!cancelled) setSearchError(e instanceof Error ? e.message : 'No se pudo buscar.')
      })
      .finally(() => {
        if (!cancelled) setSearching(false)
      })

    return () => {
      cancelled = true
    }
  }, [settledTerm, enoughCharacters])

  async function handleInvite(user: UserSearchResult) {
    setBusyUserId(user.userId)
    try {
      await invite(user.userId)
      // El resultado pasa a "Invitación enviada" sin volver a buscar.
      setResults((prev) =>
        prev.map((row) => (row.userId === user.userId ? { ...row, relation: 'INVITATION_SENT' } : row)),
      )
      await refresh()
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo enviar la invitación.')
    } finally {
      setBusyUserId(null)
    }
  }

  async function handleCancel(invitation: FamilyInvitation) {
    try {
      await cancelInvitation(invitation.id)
      await refresh()
      setResults((prev) =>
        prev.map((row) =>
          row.userId === invitation.counterpartUserId ? { ...row, relation: 'NONE' } : row,
        ),
      )
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo cancelar la invitación.')
    }
  }

  async function handleRemove(member: FamilyMember) {
    try {
      await removeMember(member.userId)
      await refresh()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo quitar a esa persona.')
    }
  }

  const remaining = useMemo(
    () => Math.max(0, MIN_SEARCH_LENGTH - term.trim().length),
    [term],
  )

  return (
    <AppShell
      title="Familia"
      subtitle="Las personas con las que compartes tu día a día."
      actionsHint={
        members.length > 0
          ? `${members.length} ${members.length === 1 ? 'integrante' : 'integrantes'}`
          : undefined
      }
      actions={
        <label className={styles.searchField}>
          <Search width={15} height={15} aria-hidden="true" />
          <input
            type="search"
            className={styles.searchInput}
            placeholder="Buscar por nombre de usuario…"
            aria-label="Buscar personas por nombre de usuario"
            value={term}
            onChange={(event) => setTerm(event.target.value)}
          />
        </label>
      }
    >
      {error && (
        <p role="alert" className={styles.error}>
          {error}
        </p>
      )}

      {/* --- Búsqueda --- */}
      {term.trim().length > 0 && (
        <section className={styles.panel} aria-label="Resultados de la búsqueda">
          {!enoughCharacters ? (
            <p className={styles.note}>
              Escribe {remaining} {remaining === 1 ? 'carácter' : 'caracteres'} más para buscar.
            </p>
          ) : searching ? (
            <p className={styles.note}>Buscando…</p>
          ) : searchError ? (
            <p role="alert" className={styles.error}>
              {searchError}
            </p>
          ) : results.length === 0 ? (
            <p className={styles.note}>Nadie coincide con «{settledTerm.trim()}».</p>
          ) : (
            <ul className={styles.list}>
              {results.map((user) => {
                const label = RELATION_LABELS[user.relation]
                return (
                  <li key={user.userId} className={styles.row}>
                    <span className={styles.avatar} aria-hidden="true">
                      {(user.username ?? '?').charAt(0).toUpperCase()}
                    </span>
                    <span className={styles.name}>{user.username}</span>
                    {label ? (
                      <span className={styles.state}>{label}</span>
                    ) : (
                      <Button
                        className={styles.inviteButton}
                        isDisabled={busyUserId === user.userId}
                        onPress={() => void handleInvite(user)}
                      >
                        <UserPlus width={14} height={14} aria-hidden="true" />
                        {busyUserId === user.userId ? 'Enviando…' : 'Invitar'}
                      </Button>
                    )}
                  </li>
                )
              })}
            </ul>
          )}
        </section>
      )}

      {/* --- Invitaciones que envié --- */}
      {sent.length > 0 && (
        <section className={styles.panel} aria-label="Invitaciones enviadas">
          <h2 className={styles.panelTitle}>Invitaciones enviadas</h2>
          <ul className={styles.list}>
            {sent.map((invitation) => (
              <li key={invitation.id} className={styles.row}>
                <span className={styles.avatar} aria-hidden="true">
                  {(invitation.counterpartUsername ?? '?').charAt(0).toUpperCase()}
                </span>
                <span className={styles.name}>{invitation.counterpartUsername}</span>
                <span className={styles.state}>Pendiente de respuesta</span>
                <Button
                  className={styles.ghostButton}
                  aria-label={`Cancelar la invitación a ${invitation.counterpartUsername}`}
                  onPress={() => void handleCancel(invitation)}
                >
                  <X width={14} height={14} aria-hidden="true" />
                  Cancelar
                </Button>
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* --- Integrantes --- */}
      <section className={styles.panel} aria-label="Integrantes de tu familia">
        <h2 className={styles.panelTitle}>Tu familia</h2>
        {loading ? (
          <p className={styles.note}>Cargando…</p>
        ) : error ? (
          /* No se afirma "no tienes familia" cuando lo que pasó es que la
             carga falló: sería afirmar algo que no sabemos (ADR-021 k). */
          null
        ) : members.length === 0 ? (
          <p className={styles.note}>
            Todavía no hay nadie. Busca a alguien por su nombre de usuario e invítalo.
          </p>
        ) : (
          <ul className={styles.list}>
            {members.map((member) => (
              <li key={member.userId} className={styles.row}>
                <span className={styles.avatar} aria-hidden="true">
                  {(member.username ?? '?').charAt(0).toUpperCase()}
                </span>
                <span className={styles.name}>{member.username}</span>
                <Button
                  className={styles.ghostButton}
                  aria-label={`Quitar a ${member.username} de tu familia`}
                  onPress={() => void handleRemove(member)}
                >
                  <X width={14} height={14} aria-hidden="true" />
                  Quitar
                </Button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </AppShell>
  )
}
