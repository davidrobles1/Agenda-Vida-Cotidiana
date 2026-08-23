import { login, register } from '../../core/auth/authClient'
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

export function LoginPage() {
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