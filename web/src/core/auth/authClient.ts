import { authConfig } from './config'
import { generateCodeChallenge, generateCodeVerifier } from './pkce'

// WEB-002/08c-web-architecture.md: Option 1 (the only one coherent with DEC-007/SPA) —
// the token lives in memory only (this module-level variable), never in
// localStorage/sessionStorage. sessionStorage is used below only for the PKCE
// code_verifier, which is single-use flow state, not a credential.

interface TokenSet {
  accessToken: string
  idToken: string
  refreshToken?: string
  expiresAt: number
}

let tokenSet: TokenSet | null = null
let renewTimer: number | undefined
let listeners: Array<() => void> = []

/** Reintentos con espera creciente ante fallos transitorios de renovación
    (ver `silentRenew`): cubren un reinicio de Keycloak o un corte de red
    corto sin que el usuario note nada, porque su token sigue siendo válido
    mientras tanto. */
const RENEW_RETRY_DELAYS_MS = [3_000, 15_000, 45_000]
let renewRetries = 0

/**
 * Pedido explícito del usuario (2026-08-23): "¿por qué el aplicativo me
 * regresa al login después de cierto tiempo?".
 *
 * La causa principal era que el token vive SOLO en memoria (decisión de
 * seguridad de WEB-002, no se toca) y no existía ningún intento de
 * recuperar la sesión al arrancar: AppRouter preguntaba `isAuthenticated()`,
 * que era `false` porque la memoria está vacía tras cualquier recarga, y
 * mandaba al login aunque la sesión SSO de Keycloak siguiera viva (dura 4
 * horas). Una F5, cerrar y abrir la pestaña o un hot-reload bastaban.
 *
 * `restoring` existe para que el guard de rutas NO decida "no autenticado"
 * mientras la comprobación silenciosa está en vuelo — sin este estado, el
 * usuario vería un parpadeo al login antes de volver a entrar.
 */
export type AuthStatus = 'restoring' | 'authenticated' | 'anonymous'

let status: AuthStatus = 'restoring'

function setStatus(next: AuthStatus): void {
  if (status === next) return
  status = next
  notify()
}

export function getAuthStatus(): AuthStatus {
  return status
}

/**
 * Tercer arreglo del mismo pedido: cuando la sesión sí termina de verdad,
 * decirlo. Antes se caía al login sin una sola palabra, y desde fuera eso
 * es indistinguible de un fallo de la app.
 *
 * Se refleja en `sessionStorage` porque el cierre por inactividad
 * (`expireByInactivity`) sale de la app hacia el endpoint de logout de
 * Keycloak y vuelve: sin persistirlo, el aviso se perdería justo en el
 * único caso en que más falta hace. No es una credencial — es una frase
 * para el usuario.
 */
const SESSION_NOTICE_KEY = 'vc_session_notice'

function readStoredNotice(): string | null {
  try {
    return sessionStorage.getItem(SESSION_NOTICE_KEY)
  } catch {
    return null
  }
}

let sessionNotice: string | null = readStoredNotice()

function setSessionNotice(notice: string | null): void {
  sessionNotice = notice
  try {
    if (notice) sessionStorage.setItem(SESSION_NOTICE_KEY, notice)
    else sessionStorage.removeItem(SESSION_NOTICE_KEY)
  } catch {
    /* Modo privado o storage bloqueado: el aviso sigue en memoria. */
  }
}

export function getSessionNotice(): string | null {
  return sessionNotice
}

export function dismissSessionNotice(): void {
  if (sessionNotice === null) return
  setSessionNotice(null)
  notify()
}

const CODE_VERIFIER_KEY = 'vc_pkce_code_verifier'
// ADR-015/FR-014: CallbackPage needs to know whether this login just came
// from Keycloak's *registration* form (→ show onboarding) vs. an ordinary
// login (→ go straight to Calendario). The backend doesn't expose a
// "brand-new account" signal on GET /me, so this session-scoped flag is the
// bridge: register() sets it right before the redirect, CallbackPage
// consumes (reads + clears) it once, right after the token exchange.
const JUST_REGISTERED_KEY = 'vc_just_registered'

function notify() {
  for (const listener of listeners) listener()
}

export function subscribe(listener: () => void): () => void {
  listeners.push(listener)
  return () => {
    listeners = listeners.filter((l) => l !== listener)
  }
}

export function getAccessToken(): string | null {
  return tokenSet?.accessToken ?? null
}

export function isAuthenticated(): boolean {
  return tokenSet !== null
}

function authorizeUrl(params: Record<string, string>): string {
  return `${authConfig.issuer}/protocol/openid-connect/auth?${new URLSearchParams(params)}`
}

/** Arranca un flujo de autorización a nivel de PESTAÑA (no en el iframe):
    guarda el `code_verifier` de PKCE y navega. Lo comparten el login
    manual y la reautenticación silenciosa de `escalateToTopLevelReauth`. */
async function beginTopLevelAuth(extraParams: Record<string, string> = {}): Promise<void> {
  const verifier = generateCodeVerifier()
  const challenge = await generateCodeChallenge(verifier)
  sessionStorage.setItem(CODE_VERIFIER_KEY, verifier)

  window.location.assign(
    authorizeUrl({
      client_id: authConfig.clientId,
      response_type: 'code',
      redirect_uri: authConfig.redirectUri,
      scope: authConfig.scope,
      code_challenge: challenge,
      code_challenge_method: 'S256',
      ...extraParams,
    }),
  )
}

export async function login(): Promise<void> {
  await beginTopLevelAuth()
}

// WEB-008 (Task B §2): Keycloak's real self-registration form
// (protocol/openid-connect/registrations) — same client, same PKCE flow, same
// redirect_uri as login(), only the endpoint path differs. On submit, Keycloak
// creates the account and redirects back to /callback with an authorization
// code exactly like a normal login, so CallbackPage's existing exchange logic
// handles it unmodified.
function registrationUrl(params: Record<string, string>): string {
  return `${authConfig.issuer}/protocol/openid-connect/registrations?${new URLSearchParams(params)}`
}

/**
 * UX-016: se mantiene **intacto** por pedido explícito del usuario, aunque
 * desde 2026-08-29 ninguna pantalla de la aplicación lo llame: la portada
 * que tenía el botón "Crear una cuenta" se retiró, y ahora al registro se
 * llega desde el enlace que el propio formulario de Keycloak ya renderiza
 * (`#kc-registration`, activado por `registrationAllowed` en el realm),
 * estilado como el mismo botón que había en el portal.
 *
 * Sigue siendo la vía correcta si alguna pantalla necesita mandar a alguien
 * directo al alta sin pasar por el formulario de acceso.
 */
export async function register(): Promise<void> {
  const verifier = generateCodeVerifier()
  const challenge = await generateCodeChallenge(verifier)
  sessionStorage.setItem(CODE_VERIFIER_KEY, verifier)
  sessionStorage.setItem(JUST_REGISTERED_KEY, '1')

  window.location.assign(
    registrationUrl({
      client_id: authConfig.clientId,
      response_type: 'code',
      redirect_uri: authConfig.redirectUri,
      scope: authConfig.scope,
      code_challenge: challenge,
      code_challenge_method: 'S256',
    }),
  )
}

async function exchangeCode(code: string, verifier: string): Promise<TokenSet> {
  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    code,
    redirect_uri: authConfig.redirectUri,
    client_id: authConfig.clientId,
    code_verifier: verifier,
  })
  const response = await fetch(`${authConfig.issuer}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })
  if (!response.ok) throw new Error(`Token exchange failed: ${response.status}`)
  const json = await response.json()
  return {
    accessToken: json.access_token,
    idToken: json.id_token,
    refreshToken: json.refresh_token,
    expiresAt: Date.now() + json.expires_in * 1000,
  }
}

/** See JUST_REGISTERED_KEY above — reads and clears the flag; single-use,
    same reasoning as the PKCE code_verifier. */
export function consumeJustRegistered(): boolean {
  const value = sessionStorage.getItem(JUST_REGISTERED_KEY)
  sessionStorage.removeItem(JUST_REGISTERED_KEY)
  return value === '1'
}

export async function handleCallback(): Promise<void> {
  const params = new URLSearchParams(window.location.search)
  const code = params.get('code')
  const error = params.get('error')

  // Volver con `error` en vez de código solo puede pasar tras la
  // reautenticación a nivel de pestaña de `escalateToTopLevelReauth` (un
  // login normal muestra el formulario en vez de contestar esto). Y esta vez
  // la respuesta SÍ es concluyente: la petición llevaba la cookie de sesión
  // como cualquier navegación, así que `login_required` aquí significa que
  // la sesión terminó de verdad.
  if (error) {
    clearReauthAttempt()
    sessionStorage.removeItem(CODE_VERIFIER_KEY)
    endSession(
      error === 'login_required' || error === 'interaction_required' || error === 'consent_required'
        ? 'Tu sesión expiró. Vuelve a iniciar sesión para continuar.'
        : 'No se pudo restablecer tu sesión. Vuelve a iniciar sesión.',
    )
    throw new Error(`Authorization failed: ${error}`)
  }

  const verifier = sessionStorage.getItem(CODE_VERIFIER_KEY)
  if (!code || !verifier) throw new Error('Missing authorization code or PKCE code_verifier')

  tokenSet = await exchangeCode(code, verifier)
  clearReauthAttempt()
  sessionStorage.removeItem(CODE_VERIFIER_KEY)
  renewRetries = 0
  setSessionNotice(null)
  // Un login recién hecho es el punto cero del plazo de inactividad.
  markActivity()
  scheduleSilentRenew()
  attachActivityListeners()
  setStatus('authenticated')
  notify()
}

function scheduleSilentRenew(): void {
  if (renewTimer) window.clearTimeout(renewTimer)
  if (!tokenSet) return
  const msUntilRenew = Math.max(tokenSet.expiresAt - Date.now() - 30_000, 5_000)
  renewTimer = window.setTimeout(silentRenew, msUntilRenew)
}

// Pedido explícito del usuario (2026-08-21): "me saca el aplicativo... que
// empiece a contar mientras haya navegación que no me saque" — el único
// timer que existía (scheduleSilentRenew, arriba) es un `setTimeout` de
// ~4.5 minutos que los navegadores retrasan o pausan por completo mientras
// la pestaña está en segundo plano o el equipo duerme (una pestaña
// "congelada" así es la causa real de "me saca" pese a haber estado
// activo: el timer simplemente no llega a dispararse a tiempo, y para
// cuando la pestaña vuelve a primer plano, el token ya expiró y la sesión
// de Keycloak puede haber superado su propio `ssoSessionIdleTimeout`
// mientras tanto — ver infra/keycloak/realm-vida-cotidiana*.json, ahora
// extendido). Esto no reemplaza ese timer — lo complementa: cualquier
// interacción real (clic, tecla, toque, scroll) o que la pestaña vuelva a
// ser visible dispara una verificación inmediata; si el token ya venció o
// está a punto de hacerlo, renueva ahí mismo en vez de esperar al
// `setTimeout` que pudo haberse retrasado. Mientras haya navegación real,
// esto mantiene la sesión viva de forma continua; solo la inactividad
// genuina (sin estos eventos, con la pestaña visible o no) deja que la
// sesión expire con normalidad.
const ACTIVITY_EVENTS = ['mousedown', 'keydown', 'touchstart', 'scroll'] as const
let activityListenersAttached = false

/**
 * SESIÓN: EXPIRACIÓN POR INACTIVIDAD (pedido explícito del usuario,
 * 2026-08-28 — ADR-017, `Documentacion/11-auth-security.md` §Sesiones).
 *
 * Dos horas sin interacción real del usuario cierran la sesión.
 *
 * Ojo con por qué esto necesita código y no basta con la configuración de
 * Keycloak: la renovación silenciosa de arriba se dispara sola cada ~4,5
 * min pase lo que pase, y cada renovación REINICIA el contador de
 * inactividad de Keycloak (`ssoSessionIdleTimeout`). Con la app abierta,
 * ese contador no llegaba a avanzar nunca — la sesión se mantenía viva
 * indefinidamente hasta el tope absoluto de 10 h. Así que la inactividad
 * se mide aquí, sobre interacción real, y la renovación se corta cuando se
 * cumple el plazo.
 *
 * Las dos mitades de la política, para que no se contradigan:
 *   - Con la app abierta manda este módulo (mide interacción real).
 *   - Con la app cerrada manda Keycloak, ahora también con 2 h de
 *     `ssoSessionIdleTimeout` (infra/keycloak/realm-vida-cotidiana*.json):
 *     si nadie renueva, la sesión SSO caduca sola en el mismo plazo.
 */
export const IDLE_TIMEOUT_MS = 2 * 60 * 60 * 1000

const IDLE_NOTICE = 'Tu sesión se cerró tras 2 horas sin actividad. Vuelve a iniciar sesión.'

/** Cada cuánto se comprueba el plazo con la app abierta. Los navegadores
    limitan los temporizadores de pestañas en segundo plano a ~1/min, así
    que bajar de aquí no compraría precisión real. */
const IDLE_CHECK_INTERVAL_MS = 60_000

/**
 * La marca de actividad se comparte entre pestañas del mismo navegador:
 * sin esto, una segunda pestaña abierta en segundo plano se creería
 * inactiva y cerraría la sesión de la pestaña en la que el usuario está
 * trabajando. Es una marca de tiempo, no una credencial — la regla de
 * WEB-002 sobre no guardar el token en `localStorage` sigue intacta.
 */
const LAST_ACTIVITY_KEY = 'vc_last_activity'
/** Escribir en cada evento sería escribir en cada scroll; con 30 s basta
    para una ventana de 2 h. */
const ACTIVITY_WRITE_THROTTLE_MS = 30_000

/** Arranca en 0 a propósito: al cargar la página, lo único que sabe cuánto
    tiempo lleva el usuario sin tocar nada es la marca guardada, no esta
    variable recién creada. Se pone en hora al abrir sesión. */
let lastActivityAt = 0
let lastActivityWriteAt = 0
let idleWatchTimer: number | undefined

function markActivity(): void {
  const now = Date.now()
  lastActivityAt = now
  if (now - lastActivityWriteAt < ACTIVITY_WRITE_THROTTLE_MS) return
  lastActivityWriteAt = now
  try {
    localStorage.setItem(LAST_ACTIVITY_KEY, String(now))
  } catch {
    /* Modo privado: se sigue midiendo en memoria, solo se pierde el
       reparto entre pestañas. */
  }
}

function clearStoredActivity(): void {
  try {
    localStorage.removeItem(LAST_ACTIVITY_KEY)
  } catch {
    /* idem */
  }
}

/** La más reciente entre esta pestaña y cualquier otra. */
function lastActivity(): number {
  let shared = 0
  try {
    shared = Number(localStorage.getItem(LAST_ACTIVITY_KEY)) || 0
  } catch {
    shared = 0
  }
  return Math.max(lastActivityAt, shared)
}

function idleForMs(): number {
  return Date.now() - lastActivity()
}

/** Cierra la sesión si se cumplió el plazo. Devuelve `true` cuando lo hizo,
    para que quien llame no siga adelante renovando algo ya caducado. */
function enforceIdleTimeout(): boolean {
  if (!tokenSet) return false
  if (idleForMs() < IDLE_TIMEOUT_MS) return false

  // Igual que en `logout()`: la URL se arma con el token todavía vivo.
  const url = logoutUrl(tokenSet?.idToken)
  loggingOut = true

  // Se limpia ANTES de salir: si la marca vieja siguiera ahí, al volver del
  // logout `restoreSession()` volvería a verla caducada y redirigiría otra
  // vez — un bucle.
  clearStoredActivity()
  endSession(IDLE_NOTICE)

  // No basta con soltar el token de esta pestaña: mientras la cookie SSO
  // de Keycloak siga viva, el siguiente `prompt=none` volvería a entrar
  // solo, y "expiró por inactividad" sería mentira. Esto la termina de
  // verdad y devuelve al usuario al login.
  window.location.assign(url)
  return true
}

function handleActivity(): void {
  // El orden importa: si el usuario vuelve después de tres horas, su primer
  // clic NO debe revivir la sesión.
  if (enforceIdleTimeout()) return
  markActivity()
  renewIfDueOrExpired()
}

function renewIfDueOrExpired(): void {
  if (!tokenSet) return
  if (Date.now() >= tokenSet.expiresAt - 30_000) void silentRenew()
}

function attachActivityListeners(): void {
  if (activityListenersAttached) return
  activityListenersAttached = true
  for (const event of ACTIVITY_EVENTS) {
    window.addEventListener(event, handleActivity, { passive: true })
  }
  document.addEventListener('visibilitychange', () => {
    // Volver a mirar la pestaña no es interacción, así que aquí NO se marca
    // actividad: solo se comprueba si el plazo venció mientras no se veía.
    if (document.visibilityState !== 'visible') return
    if (enforceIdleTimeout()) return
    renewIfDueOrExpired()
  })

  if (idleWatchTimer === undefined) {
    idleWatchTimer = window.setInterval(() => {
      enforceIdleTimeout()
    }, IDLE_CHECK_INTERVAL_MS)
  }
}

/**
 * Segundo arreglo del pedido: distinguir POR QUÉ falló la renovación.
 *
 * El `catch` anterior trataba todos los fallos igual y tiraba el token al
 * instante, pero `prompt=none` falla por dos motivos que no se parecen en
 * nada:
 *
 *   - `login_required` — Keycloak contesta que ya no hay sesión SSO. Es un
 *     cierre de sesión real y no tiene vuelta atrás.
 *   - cualquier otra cosa — Keycloak reiniciándose, el iframe bloqueado por
 *     la política de cookies de terceros del navegador (el front corre en
 *     :5173 y Keycloak en :8081, así que su cookie SSO es "de tercero"), la
 *     red caída, o el timeout de 10 s de abajo. Nada de esto significa que
 *     el usuario haya dejado de estar autenticado.
 *
 * Cerrar la sesión por lo segundo es precisamente lo que sacaba al usuario
 * "después de cierto tiempo" sin motivo real.
 */
type RenewFailureReason = 'login_required' | 'transient'

class SilentRenewError extends Error {
  readonly reason: RenewFailureReason

  constructor(message: string, reason: RenewFailureReason) {
    super(message)
    this.name = 'SilentRenewError'
    this.reason = reason
  }
}

function failureReasonOf(error: unknown): RenewFailureReason {
  // Todo lo que no venga clasificado (fallo de red del token endpoint, un
  // throw inesperado) se trata como transitorio a propósito: el peor caso
  // de equivocarse aquí es reintentar de más, no sacar a nadie de más.
  return error instanceof SilentRenewError ? error.reason : 'transient'
}

// prompt=none in a hidden iframe: renews the token without ever showing the
// login UI or touching localStorage — the refresh token stays server-side in
// the Keycloak SSO session cookie.
function silentAuthorize(url: string): Promise<string> {
  return new Promise((resolve, reject) => {
    const iframe = document.createElement('iframe')
    iframe.style.display = 'none'
    const cleanup = () => iframe.remove()
    const timeoutId = window.setTimeout(() => {
      cleanup()
      reject(new SilentRenewError('Silent renew timed out', 'transient'))
    }, 10_000)

    iframe.onload = () => {
      window.clearTimeout(timeoutId)

      let href: string | undefined
      try {
        href = iframe.contentWindow?.location.href
      } catch {
        // Leer la URL lanza cuando el iframe sigue en el origen de Keycloak
        // en vez de haber vuelto al redirect_uri: se quedó en una página de
        // error o el navegador bloqueó su cookie. No es un cierre de sesión.
        cleanup()
        reject(new SilentRenewError('Silent renew: iframe stayed on the identity provider', 'transient'))
        return
      }
      cleanup()

      const params = href ? new URL(href).searchParams : null
      const code = params?.get('code') ?? null
      if (code) {
        resolve(code)
        return
      }

      // Cuando `prompt=none` no puede renovar sin interacción del usuario,
      // Keycloak vuelve al redirect_uri con ?error=… (OIDC Core §3.1.2.6).
      const error = params?.get('error') ?? 'no_authorization_code'
      const reason: RenewFailureReason =
        error === 'login_required' || error === 'interaction_required' || error === 'consent_required'
          ? 'login_required'
          : 'transient'
      reject(new SilentRenewError(`Silent renew failed: ${error}`, reason))
    }

    iframe.onerror = () => {
      window.clearTimeout(timeoutId)
      cleanup()
      reject(new SilentRenewError('Silent renew: iframe failed to load', 'transient'))
    }

    iframe.src = url
    document.body.appendChild(iframe)
  })
}

/** El flujo `prompt=none` completo: autoriza en el iframe y canjea el
    código. Lo comparten la renovación periódica y la restauración al
    arrancar, que solo se diferencian en qué hacen cuando falla. */
async function fetchTokenSilently(): Promise<TokenSet> {
  const verifier = generateCodeVerifier()
  const challenge = await generateCodeChallenge(verifier)
  const code = await silentAuthorize(
    authorizeUrl({
      client_id: authConfig.clientId,
      response_type: 'code',
      redirect_uri: authConfig.redirectUri,
      scope: authConfig.scope,
      code_challenge: challenge,
      code_challenge_method: 'S256',
      prompt: 'none',
    }),
  )
  return exchangeCode(code, verifier)
}

function endSession(notice: string | null): void {
  tokenSet = null
  renewRetries = 0
  if (renewTimer) window.clearTimeout(renewTimer)
  renewTimer = undefined
  setSessionNotice(notice)
  setStatus('anonymous')
  notify()
}

/**
 * DEFECTO REAL ENCONTRADO (2026-08-28, reportado por el usuario): la app
 * seguía sacándolo con el aviso "Tu sesión expiró", y al pulsar "Iniciar
 * sesión" entraba **sin escribir credenciales**. Eso último es la prueba:
 * la sesión SSO de Keycloak estaba perfectamente viva y el cliente la dio
 * por muerta.
 *
 * El motivo es que la renovación silenciosa ocurre dentro de un IFRAME, y
 * un iframe puede no llevar la cookie de sesión de Keycloak aunque la
 * pestaña sí la lleve (bloqueo de cookies de terceros del navegador,
 * particionado de almacenamiento, ITP). Sin cookie, Keycloak responde
 * exactamente lo mismo que si la sesión hubiera terminado:
 * `error=login_required`. Los dos casos son indistinguibles DESDE EL
 * IFRAME — así que interpretarlo como "sesión terminada" era una conclusión
 * que el cliente no podía sacar.
 *
 * La comprobación que sí distingue es la que hacía el usuario a mano: pedir
 * la autorización a nivel de pestaña. Si la sesión vive, Keycloak devuelve
 * un código al instante y sin formulario (por eso "entraba sin iniciar
 * sesión"); si de verdad terminó, contesta `login_required` otra vez y
 * entonces sí se acabó. Eso es lo que hace esto, automáticamente, con
 * `prompt=none` para que nunca aparezca un formulario inesperado.
 *
 * `REAUTH_ATTEMPT_KEY` es el freno anti-bucle: solo se escala una vez por
 * ciclo. Si se vuelve del intento a nivel de pestaña con `login_required`,
 * la sesión terminó de verdad (`handleCallback`).
 */
const REAUTH_ATTEMPT_KEY = 'vc_reauth_attempt'
/** Adónde volver tras la reautenticación: el usuario estaba trabajando en
    una pantalla concreta y no tiene por qué acabar en otra. */
const POST_LOGIN_PATH_KEY = 'vc_post_login_path'

function escalateToTopLevelReauth(): boolean {
  let alreadyTried = false
  try {
    alreadyTried = sessionStorage.getItem(REAUTH_ATTEMPT_KEY) === '1'
  } catch {
    /* Sin sessionStorage no hay freno anti-bucle: no se escala. */
    return false
  }
  if (alreadyTried) return false

  try {
    sessionStorage.setItem(REAUTH_ATTEMPT_KEY, '1')
    sessionStorage.setItem(POST_LOGIN_PATH_KEY, window.location.pathname + window.location.search)
  } catch {
    return false
  }

  void beginTopLevelAuth({ prompt: 'none' })
  return true
}

function clearReauthAttempt(): void {
  try {
    sessionStorage.removeItem(REAUTH_ATTEMPT_KEY)
  } catch {
    /* nada que limpiar */
  }
}

/** Devuelve la ruta guardada antes de una reautenticación, si la hay.
    Consumo único, igual que el `code_verifier`. */
export function consumePostLoginPath(): string | null {
  try {
    const path = sessionStorage.getItem(POST_LOGIN_PATH_KEY)
    sessionStorage.removeItem(POST_LOGIN_PATH_KEY)
    // "/" y "/callback" no son destinos: llevarían de vuelta al login o a
    // un intercambio de código ya consumido.
    if (!path || path === '/' || path.startsWith('/callback')) return null
    return path
  } catch {
    return null
  }
}

/**
 * Sin este guardia, cada evento de actividad podía lanzar SU PROPIA
 * renovación mientras otra seguía en vuelo: varias renovaciones en paralelo
 * consumían el presupuesto de reintentos (`renewRetries`) de golpe y una
 * sola incidencia pasajera bastaba para cerrar la sesión. Un scroll largo
 * con el token a punto de vencer era suficiente para provocarlo.
 */
let renewInFlight: Promise<void> | null = null

function silentRenew(): Promise<void> {
  if (renewInFlight) return renewInFlight
  renewInFlight = runSilentRenew().finally(() => {
    renewInFlight = null
  })
  return renewInFlight
}

async function runSilentRenew(): Promise<void> {
  // Renovar es exactamente lo que mantenía viva una sesión inactiva (ver el
  // bloque de inactividad arriba): si el plazo ya venció, se cierra aquí en
  // vez de pedir un token nuevo.
  if (enforceIdleTimeout()) return

  try {
    tokenSet = await fetchTokenSilently()
    renewRetries = 0
    setSessionNotice(null)
    clearReauthAttempt()
    scheduleSilentRenew()
    setStatus('authenticated')
    notify()
  } catch (error) {
    if (failureReasonOf(error) === 'transient' && renewRetries < RENEW_RETRY_DELAYS_MS.length) {
      const delay = RENEW_RETRY_DELAYS_MS[renewRetries]
      renewRetries += 1
      if (renewTimer) window.clearTimeout(renewTimer)
      // El token vigente se conserva a propósito: mientras no expire, la app
      // sigue funcionando y el reintento pasa desapercibido.
      renewTimer = window.setTimeout(() => void silentRenew(), delay)
      return
    }

    // Aquí está el arreglo del defecto descrito arriba: el iframe NO puede
    // concluir que la sesión terminó. Se comprueba a nivel de pestaña, que
    // es la única forma de saberlo de verdad, y solo si eso también falla
    // se cierra la sesión.
    if (escalateToTopLevelReauth()) return

    endSession(
      failureReasonOf(error) === 'login_required'
        ? 'Tu sesión expiró. Vuelve a iniciar sesión para continuar.'
        : 'No se pudo renovar tu sesión porque el servidor de identidad no respondió. Vuelve a iniciar sesión.',
    )
  }
}

/**
 * Primer arreglo, y el que de verdad respondía a la pregunta: al arrancar,
 * intentar recuperar la sesión antes de dar por sentado que no hay ninguna.
 *
 * El token nunca se guarda en disco (WEB-002), así que tras una recarga la
 * memoria está vacía — pero la sesión SSO de Keycloak sigue viva en su
 * cookie durante horas. Un `prompt=none` la convierte otra vez en un token
 * sin mostrar nada al usuario. Sin esto, cualquier F5, reapertura de
 * pestaña o hot-reload de Vite mandaba al login teniendo sesión válida.
 */
let restorePromise: Promise<void> | null = null

export function restoreSession(): Promise<void> {
  if (restorePromise) return restorePromise

  // `silentAuthorize` carga la app entera dentro de su iframe oculto por un
  // instante antes de quitarlo; relanzar el flujo desde ahí sería recursivo.
  const insideSilentFrame = window.self !== window.top
  // /callback ya tiene su propio código que canjear (CallbackPage): no hay
  // nada que restaurar y competir con él solo daría dos flujos en paralelo.
  const onCallback = window.location.pathname === '/callback'

  if (insideSilentFrame || onCallback || tokenSet) {
    setStatus(tokenSet ? 'authenticated' : 'anonymous')
    restorePromise = Promise.resolve()
    return restorePromise
  }

  // La app estuvo cerrada, así que aquí no hay nada que "medir": lo único
  // que dice cuánto tiempo pasó es la marca compartida entre pestañas. Si
  // ya venció el plazo, no se restaura — da igual que la cookie SSO siga
  // viva, porque el usuario lleva 2 h sin tocar nada.
  if (lastActivity() > 0 && idleForMs() >= IDLE_TIMEOUT_MS) {
    clearStoredActivity()
    endSession(IDLE_NOTICE)
    restorePromise = Promise.resolve()
    loggingOut = true
    // El logout de Keycloak termina la cookie SSO: sin esto, "Iniciar
    // sesión" volvería a entrar sin pedir credenciales. Aquí no hay
    // `id_token_hint` que ofrecer (la app acaba de arrancar sin token), así
    // que Keycloak pedirá confirmación; es el único camino en que eso pasa.
    window.location.assign(logoutUrl())
    return restorePromise
  }

  restorePromise = (async () => {
    try {
      tokenSet = await fetchTokenSilently()
      renewRetries = 0
      markActivity()
      scheduleSilentRenew()
      attachActivityListeners()
      setSessionNotice(null)
      setStatus('authenticated')
      notify()
    } catch (error) {
      // Mismo razonamiento que en `silentRenew`: el iframe no puede concluir
      // que no hay sesión. Pero aquí solo se escala si HAY indicio de que la
      // había — la marca de actividad compartida —; en una primera visita
      // real no tiene sentido pagar un redirect para descubrir lo obvio.
      if (lastActivity() > 0 && escalateToTopLevelReauth()) return

      // Arrancar sin sesión SSO es lo normal (primera visita, sesión ya
      // cerrada): no es un error y no se avisa nada. Solo se avisa cuando
      // el motivo fue que no se pudo hablar con Keycloak, porque ahí el
      // login puede fallar igual y conviene saber por qué.
      endSession(
        failureReasonOf(error) === 'login_required'
          ? null
          : 'No se pudo contactar al servidor de identidad. Si el problema sigue, revisa tu conexión.',
      )
    }
  })()

  return restorePromise
}

/**
 * RP-initiated logout de Keycloak: termina la cookie SSO, no solo el token
 * de esta pestaña. Lo comparten el cierre manual y el cierre por inactividad
 * (`enforceIdleTimeout`).
 *
 * `id_token_hint` importa: sin él, Keycloak 18+ no cierra la sesión de
 * inmediato — muestra una página de confirmación ("¿quieres cerrar
 * sesión?"), y si el usuario no la confirma la sesión SSO sigue viva. Con el
 * hint, Keycloak identifica la sesión, la cierra y vuelve directo al
 * `post_logout_redirect_uri`, sin pantalla intermedia.
 *
 * OJO AL ORDEN: hay que construir esta URL **antes** de limpiar el estado,
 * porque el id_token sale del `tokenSet` que `endSession` borra.
 */
function logoutUrl(idToken?: string): string {
  const params: Record<string, string> = {
    client_id: authConfig.clientId,
    post_logout_redirect_uri: window.location.origin,
  }
  if (idToken) params.id_token_hint = idToken
  return `${authConfig.issuer}/protocol/openid-connect/logout?${new URLSearchParams(params)}`
}

/**
 * DEFECTO REAL (2026-08-29, reportado por el usuario: "al dar logout en el
 * botón del portal no me cierra la sesión").
 *
 * Causa: `logout()` pide la navegación al endpoint de cierre, pero antes
 * llama a `endSession`, que notifica a React. El re-render es SÍNCRONO y
 * ocurre antes de que el navegador procese esa navegación: `RequireAuth` ve
 * "anonymous", manda a "/", y `AuthGateway` —que desde UX-016 redirige solo
 * al formulario de acceso— dispara `login()` con OTRO
 * `window.location.assign`. Gana el último: el navegador iba al endpoint de
 * autorización en vez de al de cierre, Keycloak veía la cookie SSO todavía
 * viva y devolvía un código al instante. Resultado: el usuario volvía a
 * entrar sin haber salido nunca.
 *
 * Esta bandera corta esa carrera: mientras un cierre está en vuelo, nada
 * más puede iniciar un login.
 */
let loggingOut = false

export function isLoggingOut(): boolean {
  return loggingOut
}

export function logout(): void {
  // La URL se construye con el token todavía en memoria (ver logoutUrl).
  const url = logoutUrl(tokenSet?.idToken)
  loggingOut = true
  clearStoredActivity()
  endSession(null)
  window.location.assign(url)
}
