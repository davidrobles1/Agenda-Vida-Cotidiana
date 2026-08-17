import { login, register } from '../../core/auth/authClient'
import styles from './LoginPage.module.css'

export function LoginPage() {
  return (
    <main className={styles.container}>
      <h1 className={styles.title}>Vida Cotidiana</h1>
      <button type="button" onClick={() => login()}>
        Log in
      </button>
      <button type="button" data-variant="secondary" className={styles.registerButton} onClick={() => register()}>
        Crear cuenta
      </button>
    </main>
  )
}
