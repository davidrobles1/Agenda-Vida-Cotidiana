import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listMembers, type FamilyMember } from '../family/api'
import {
  listForResource,
  supportsResponsibility,
  type PendingShare,
  type SharedResourceType,
} from './resourceShares'
import styles from './ShareWithFamily.module.css'

/**
 * Control de compartición para los formularios de alta y edición (ADR-025 §2).
 *
 * MISMO COMPONENTE PARA CREAR Y PARA EDITAR. Nunca llama a la API al marcar
 * una casilla: acumula la intención y el formulario la aplica al guardar, con
 * {@link applyShares}. Es lo que hace que funcione igual en un alta —donde el
 * recurso todavía no tiene id— que en una edición, y evita el efecto raro de
 * que marcar una casilla ya comparta aunque luego se cierre sin guardar.
 *
 * Si no hay familia todavía, el control lo dice y enlaza a la sección Familia
 * en vez de mostrar una lista vacía sin explicación.
 */

interface ShareWithFamilyProps {
  type: SharedResourceType
  /** `null` mientras el recurso no existe (alta). */
  resourceId: string | null
  value: PendingShare[]
  onChange: (next: PendingShare[]) => void
  /** Se avisa cuando termina de sembrar el estado inicial desde el servidor. */
  onLoaded?: () => void
}

export function ShareWithFamily({ type, resourceId, value, onChange, onLoaded }: ShareWithFamilyProps) {
  const [members, setMembers] = useState<FamilyMember[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const withResponsibility = supportsResponsibility(type)

  useEffect(() => {
    let cancelled = false

    async function load() {
      try {
        const nextMembers = await listMembers()
        if (cancelled) return
        setMembers(nextMembers)

        if (resourceId) {
          // En edición, el estado inicial es lo que YA está compartido: sin
          // esto, guardar sin tocar nada revocaría todo lo anterior.
          const existing = await listForResource(type, resourceId)
          if (cancelled) return
          onChange(
            existing.map((share) => ({
              userId: share.counterpartUserId ?? '',
              username: share.counterpartUsername ?? '',
              responsibility: share.responsibility,
            })),
          )
        }
        setError(null)
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : 'No se pudo cargar tu familia.')
      } finally {
        if (!cancelled) {
          setLoading(false)
          onLoaded?.()
        }
      }
    }

    void load()
    return () => {
      cancelled = true
    }
    // `value`/`onChange` quedan fuera a propósito: esto siembra el estado UNA
    // vez por recurso. Incluirlos lo volvería a sembrar en cada pulsación y
    // borraría lo que el usuario acaba de marcar.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [type, resourceId])

  function toggleMember(member: FamilyMember) {
    const already = value.some((entry) => entry.userId === member.userId)
    onChange(
      already
        ? value.filter((entry) => entry.userId !== member.userId)
        : [...value, { userId: member.userId, username: member.username, responsibility: false }],
    )
  }

  function toggleResponsibility(userId: string) {
    onChange(
      value.map((entry) =>
        entry.userId === userId ? { ...entry, responsibility: !entry.responsibility } : entry,
      ),
    )
  }

  if (loading) {
    return <p className={styles.note}>Cargando tu familia…</p>
  }

  if (error) {
    return (
      <p role="alert" className={styles.error}>
        {error}
      </p>
    )
  }

  if (members.length === 0) {
    return (
      <p className={styles.note}>
        Todavía no tienes familia. <Link to="/family">Invita a alguien</Link> para poder compartir.
      </p>
    )
  }

  return (
    <ul className={styles.list}>
      {members.map((member) => {
        const entry = value.find((row) => row.userId === member.userId)
        const shared = !!entry

        return (
          <li key={member.userId} className={styles.row}>
            <label className={styles.memberLabel}>
              <input
                type="checkbox"
                className={styles.checkbox}
                checked={shared}
                onChange={() => toggleMember(member)}
              />
              <span className={styles.username}>{member.username}</span>
            </label>

            {/* La responsabilidad solo aparece donde significa algo. Un
                artículo se posee y un documento se consulta: no hay una parte
                que hacer, así que no se ofrece la opción (requisito §5/§6). */}
            {shared && withResponsibility && (
              <label className={styles.responsibility}>
                <input
                  type="checkbox"
                  className={styles.checkbox}
                  checked={entry?.responsibility ?? false}
                  onChange={() => toggleResponsibility(member.userId)}
                />
                <span>Se compromete con su parte</span>
              </label>
            )}
          </li>
        )
      })}
    </ul>
  )
}
