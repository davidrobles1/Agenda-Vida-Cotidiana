import styles from './SessionRestoring.module.css'

/**
 * Pantalla de espera mientras `restoreSession()` (authClient.ts) comprueba
 * en silencio si la sesión SSO sigue viva.
 *
 * Existe por una razón concreta: sin ella, el guard de rutas tendría que
 * decidir "autenticado / no autenticado" antes de saberlo, y en cada
 * recarga se vería un parpadeo al login incluso cuando la sesión sí estaba
 * viva. Dura lo que tarde el iframe oculto — normalmente unas décimas.
 */
export function SessionRestoring() {
  return (
    <div className={styles.container} role="status" aria-live="polite">
      <span className={styles.dot} aria-hidden="true" />
      <p className={styles.label}>Restaurando tu sesión…</p>
    </div>
  )
}
