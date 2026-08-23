import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useModeContext } from '../../core/user/ModeContext'
import styles from './OnboardingPage.module.css'

/**
 * ADR-015 (Documentacion/22-decision-log.md) / FR-014 / UC-15 / CLAUDE.md
 * "Registro exige marcar al menos un modo".
 *
 * Shown right after a brand-new registration (CallbackPage.tsx routes here
 * only when authClient's `vc_just_registered` flag was set by register() —
 * see that file's comment for why: the backend doesn't expose a "this
 * account is brand new" signal on GET /me yet). Two independent checkboxes,
 * both optional individually but **at least one required** to continue —
 * "Refinamientos cerrados el 2026-08-18 → Opción B" in the ADR: the form
 * blocks submission with a real, visible error message, not a silently
 * disabled button (CLAUDE.md's explicit instruction).
 */
export function OnboardingPage() {
  const navigate = useNavigate()
  const { activateMode } = useModeContext()
  const [personal, setPersonal] = useState(false)
  const [laboral, setLaboral] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()

    if (!personal && !laboral) {
      setError('Selecciona al menos una opción (Personal o Laboral) para continuar.')
      return
    }

    setError(null)
    setSubmitting(true)
    try {
      // Sequential, not Promise.all: activateMode's mock fallback (see
      // ModeContext.tsx) mutates local state per call — parallel calls would
      // race that fallback's setState pair.
      if (personal) await activateMode('PERSONAL')
      if (laboral) await activateMode('LABORAL')
      navigate('/calendar', { replace: true })
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className={styles.container}>
      <div className={styles.card}>
        <h1 className={styles.title}>¿Cómo vas a usar Agenda?</h1>
        <p className={styles.subtitle}>
          Puedes elegir uno o los dos — más adelante puedes activar el que falte desde Ajustes.
        </p>

        <form onSubmit={handleSubmit} noValidate>
          <label className={styles.option}>
            <input
              type="checkbox"
              checked={personal}
              onChange={(e) => setPersonal(e.target.checked)}
            />
            <span>
              <strong>Personal</strong>
              <span className={styles.optionHint}>Tu vida cotidiana, tu hogar, tu familia.</span>
            </span>
          </label>

          <label className={styles.option}>
            <input
              type="checkbox"
              checked={laboral}
              onChange={(e) => setLaboral(e.target.checked)}
            />
            <span>
              <strong>Laboral</strong>
              <span className={styles.optionHint}>Tu consultorio, tu oficina, tu negocio.</span>
            </span>
          </label>

          {error && (
            <p role="alert" className={styles.error}>
              {error}
            </p>
          )}

          <button type="submit" disabled={submitting} className={styles.submitButton}>
            {submitting ? 'Guardando…' : 'Continuar'}
          </button>
        </form>
      </div>
    </main>
  )
}
