import { login, register } from '../../core/auth/authClient'
import styles from './LoginPage.module.css'

export function LoginPage() {
  return (
    <main className={styles.container}>
      <section className={styles.hero}>
        <div className={styles.heroContent}>
          <div className={styles.homeIcon}>⌂</div>

          <h1>Agenda</h1>
          <p className={styles.script}>vida Cotidiana</p>

          <div className={styles.heroDivider} />

          <p className={styles.heroText}>
            Un lugar para organizar tus días,
            <br />
            tus momentos y tu hogar.
          </p>

          <div className={styles.houseIllustration}>
            <span>⌂</span>
          </div>

          <p className={styles.heroQuote}>
            “Cada día tiene su propia historia.”
          </p>
        </div>
      </section>

      <section className={styles.loginSection}>
        <div className={styles.loginCard}>
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
              Iniciar sesión
            </button>

            <button
              type="button"
              className={styles.registerButton}
              onClick={() => register()}
            >
              Crear una cuenta
            </button>
          </div>

          <p className={styles.footerText}>
            Tu hogar, tus momentos, tu agenda.
          </p>
        </div>
      </section>
    </main>
  )
}