import 'dotenv/config'
import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  // Task B §1 real finding: with Keycloak's bruteForceProtected now on
  // (realm-vida-cotidiana.json), running these 4 specs in parallel (the
  // default) has all of them log in as the same real `testuser` account
  // within the same ~1s window — reproduced for real, not assumed: 3
  // separate 4-worker runs, 2 of them had one spec fail on Keycloak's own
  // login page with "Invalid username or password" for the objectively
  // correct password, while every serial (1-worker) run and every run with
  // bruteForceProtected temporarily reverted to false passed cleanly. A
  // single shared account authenticating from several concurrent sessions
  // inside Keycloak's quickLoginCheckMilliSeconds window (1000ms, an
  // untouched Keycloak default) isn't a realistic single-user login pattern
  // this control needs to tolerate, so the fix belongs here — serialize the
  // suite — rather than loosening the security control to make parallel
  // load-testing-shaped test traffic pass.
  workers: 1,
  use: {
    baseURL: 'http://localhost:5173',
    // WEB-005: real Web Push subscriptions need Google's proprietary GCM/FCM
    // sender registration, which is only present in official Google Chrome —
    // Playwright's bundled open-source Chromium always fails push
    // subscriptions with "AbortError: Registration failed - permission
    // denied" (found for real, not a bug in this app's code).
    channel: 'chrome',
  },
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: true,
    env: {
      // `localhost`, igual que los defaults de `core/auth/config.ts`.
      //
      // Antes esto fijaba la IP LAN (`192.168.0.18`), y como
      // `reuseExistingServer` es true, la suite reutilizaba cualquier Vite ya
      // levantado — de modo que el issuer efectivo dependía de con qué
      // variables se hubiera arrancado ese servidor. La consecuencia real
      // (2026-08-29): tras dejar el backend apuntando a la IP LAN para una
      // corrida de la suite, la sesión del navegador quedó devolviendo 401 en
      // TODAS las llamadas autenticadas, porque Keycloak emitía el token con
      // `iss=localhost` y el backend esperaba la IP.
      //
      // Playwright corre en esta misma máquina, así que `localhost` le vale y
      // coincide con lo que usa el navegador a diario. La IP LAN sigue siendo
      // necesaria, pero solo para dispositivos físicos Android/iOS, que la
      // fijan en sus propias configuraciones (`build.gradle.kts`,
      // `AppConfig.swift`) y no dependen de este archivo.
      VITE_OIDC_ISSUER: 'http://localhost:8081/realms/vida-cotidiana',
      VITE_API_BASE_URL: 'http://localhost:8080/api/v1',
    },
  },
})
