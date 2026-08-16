import { login } from '../../core/auth/authClient'
import styles from './LoginPage.module.css'

export function LoginPage() {
  return (
    <div className={styles.container}>
      <h1 className={styles.title}>Vida Cotidiana</h1>
      <button type="button" onClick={() => login()}>
        Log in
      </button>
    </div>
  )
}
