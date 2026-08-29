import { useSyncExternalStore } from 'react'
import {
  dismissSessionNotice,
  getSessionNotice,
  login,
  register,
  subscribe,
} from '../../core/auth/authClient'
import {
  IconArrowRight,
  IconHeart,
  IconHome,
  IconLeaf,
  IconLock,
  IconSparkle,
  IconUser,
  IconUserPlus,
} from '../../core/ui/icons'
import styles from './LoginPage.module.css'
import logo from './assets/libro_hojas.jpeg'

/**
 * RETIRADA DE LA APLICACIÓN (UX-016, 2026-08-29, pedido explícito del
 * usuario: pasar de 3 pantallas de autenticación a 2).
 *
 * Esta portada era la pantalla intermedia: no autenticaba nada, solo tenía
 * dos botones que llevaban a Keycloak. Su identidad visual **no se perdió**:
 * se trasladó íntegra al theme de Keycloak
 * (`infra/keycloak/themes/vida-cotidiana-web/login`) — la fotografía del
 * libro con sus hojas giradas, la columna de eslóganes con los cuatro
 * valores, el divisor, la frase, "Meraki" en Alex Brush, la tarjeta, los
 * botones y el pie con el candado; y ahora también las tipografías reales
 * (Inter/Fraunces/Alex Brush servidas por el propio theme) y la fotografía
 * de fondo, que antes el theme solo aproximaba.
 *
 * `/` ya no la monta: `AuthGateway` manda directo al formulario de Keycloak
 * (ver `routes/AppRouter.tsx`). El archivo se conserva sin enrutar, como
 * `TareasPage`/`RemindersPage` en su momento: no se borran componentes.
 */
export function LoginPage() {
  // Si la sesión terminó de verdad, authClient deja escrito el motivo antes
  // de mandar aquí (ver getSessionNotice): caer al login sin una palabra es
  // indistinguible de que la app se haya roto.
  const notice = useSyncExternalStore(subscribe, getSessionNotice)

  return (
    <main className={styles.container}>

      {/* =====================================================
          COLUMN 1 — LIBRO / IMAGEN
          ===================================================== */}
      <section className={styles.bookSection}>
        <div className={styles.bookContent}>
          <div className={styles.heroLogoStack}>
            <img
              src={logo}
              alt="Agenda vida Cotidiana"
              className={styles.heroLogo}
            />
          </div>
        </div>
      </section>


      {/* =====================================================
          COLUMN 2 — ESLOGANES / IDENTIDAD
          ===================================================== */}
      <section className={styles.sloganSection}>
        <div className={styles.sloganContent}>

          <ul className={styles.valueRow}>

            <li>
              <IconHome className={styles.valueIcon} />
              <span>Tu hogar</span>
            </li>

            <li>
              <IconHeart className={styles.valueIcon} />
              <span>Tu esencia</span>
            </li>

            <li>
              <IconLeaf className={styles.valueIcon} />
              <span>Tu crecimiento</span>
            </li>

            <li>
              <IconSparkle className={styles.valueIcon} />
              <span>Tu ritmo</span>
            </li>

          </ul>

          <div className={styles.heroDivider} />

          <p className={styles.heroQuote}>
            “Cada día tiene su propia historia.”
          </p>

          <p className={styles.heroText}>
            Meraki
          </p>

          <p className={styles.heroTextUno}>
            el acto de poner el alma, la creatividad,
            <br />
            el amor y la pasión en todo lo que haces.
            <br />
            Es dejar una huella de tu esencia en tu trabajo.
          </p>

        </div>
      </section>


      {/* =====================================================
          COLUMN 3 — LOGIN
          ===================================================== */}
      <section className={styles.loginSection}>
        <div className={styles.loginCard}>

          <IconLeaf className={styles.brandIcon} />

          <div className={styles.brand}>
            <h2>Agenda</h2>
            <span>vida Cotidiana</span>
          </div>

          <div className={styles.welcome}>
            <h3>Bienvenido</h3>
            <p>Inicia sesión para continuar</p>
          </div>

          {notice && (
            <div className={styles.sessionNotice} role="status">
              <p className={styles.sessionNoticeText}>{notice}</p>
              <button
                type="button"
                className={styles.sessionNoticeDismiss}
                aria-label="Descartar aviso"
                onClick={dismissSessionNotice}
              >
                ×
              </button>
            </div>
          )}

          <div className={styles.actions}>

            <button
              type="button"
              className={styles.loginButton}
              onClick={() => login()}
            >
              <IconUser className={styles.buttonIcon} />
              <span>Iniciar sesión</span>
              <IconArrowRight className={styles.buttonIcon} />
            </button>

            <button
              type="button"
              className={styles.registerButton}
              onClick={() => register()}
            >
              <IconUserPlus className={styles.buttonIcon} />
              <span>Crear una cuenta</span>
              <IconArrowRight className={styles.buttonIcon} />
            </button>

          </div>

          <div className={styles.footerDivider} />

          <p className={styles.footerText}>
            <IconLock className={styles.footerIcon} />
            Tu hogar, tus momentos, tu agenda.
          </p>

        </div>
      </section>

    </main>
  )
}