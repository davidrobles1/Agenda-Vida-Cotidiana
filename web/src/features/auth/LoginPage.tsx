import { login } from '../../core/auth/authClient'

export function LoginPage() {
  return (
    <div>
      <h1>Vida Cotidiana</h1>
      <button type="button" onClick={() => login()}>
        Log in
      </button>
    </div>
  )
}
