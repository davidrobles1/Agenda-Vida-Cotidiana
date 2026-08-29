import { useEffect } from 'react'
import {
  dismissSessionNotice,
  getSessionNotice,
  isLoggingOut,
  login,
  subscribe,
} from '../../core/auth/authClient'
import { useSyncExternalStore } from 'react'
import styles from './AuthGateway.module.css'

/**
 * UX-016 — de 3 pantallas de autenticación a 2.
 *
 * Antes, "/" mostraba `LoginPage`: una pantalla propia de la aplicación con
 * dos botones que **solo** llevaban a Keycloak. Era una escala intermedia
 * sin función propia: el usuario veía tres pantallas (portada del portal →
 * formulario de Keycloak → formulario de registro) para dos acciones.
 *
 * Ahora "/" no dibuja nada: manda directo al formulario de Keycloak, que
 * lleva la identidad visual del portal en su propio theme
 * (`infra/keycloak/themes/vida-cotidiana-web`), incluidas las columnas del
 * libro y los eslóganes que vivían en `LoginPage`. Desde ahí, el enlace
 * "Crear una cuenta" —que Keycloak ya renderizaba— lleva al registro. Dos
 * pantallas, las dos de Keycloak, las dos con la cara del portal.
 *
 * La arquitectura de autenticación NO cambia: se sigue llamando al mismo
 * `login()` (Authorization Code + PKCE), el mismo `/callback`, el mismo
 * manejo de tokens en memoria, la misma renovación y el mismo logout. La
 * aplicación sigue sin ver jamás una contraseña.
 *
 * ÚNICA EXCEPCIÓN al redirect inmediato: cuando la sesión terminó sola
 * (expiró, o no se pudo renovar) authClient deja un motivo escrito. Saltar
 * a Keycloak en silencio borraría ese aviso, que existe precisamente para
 * que el usuario no interprete un cierre de sesión como un fallo de la app.
 * En ese caso —y solo en ese— se muestra el motivo con un botón para
 * continuar. No es una tercera pantalla del flujo normal: es el estado de
 * error de este mismo paso.
 */
export function AuthGateway() {
  const notice = useSyncExternalStore(subscribe, getSessionNotice)

  useEffect(() => {
    if (notice) return
    // Un cierre de sesión en curso ya pidió su propia navegación al endpoint
    // de logout de Keycloak. Lanzar aquí un `login()` la pisaría —y eso es
    // exactamente lo que hacía que el botón "cerrar sesión" no cerrara nada:
    // ganaba el último `location.assign`, el usuario acababa en el endpoint
    // de autorización y Keycloak, con la cookie SSO aún viva, lo devolvía
    // dentro al instante.
    if (isLoggingOut()) return
    void login()
  }, [notice])

  if (!notice) {
    return (
      <div className={styles.container} role="status" aria-live="polite">
        <span className={styles.dot} aria-hidden="true" />
        <p className={styles.label}>Llevándote a iniciar sesión…</p>
      </div>
    )
  }

  return (
    <main className={styles.container}>
      <div className={styles.card}>
        <p className={styles.notice}>{notice}</p>

        <button
          type="button"
          className={styles.continueButton}
          onClick={() => {
            dismissSessionNotice()
            void login()
          }}
        >
          Volver a iniciar sesión
        </button>
      </div>
    </main>
  )
}
