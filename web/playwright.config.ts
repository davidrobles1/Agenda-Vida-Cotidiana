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
      // Matches whatever host the currently-running backend's OIDC_ISSUER is
      // pinned to (see CIERRE notes) — the app itself is still served from
      // localhost:5173 (matches Keycloak's web-spa redirectUri/webOrigins
      // and the backend's CORS allowed-origins), only the API/issuer targets
      // move.
      VITE_OIDC_ISSUER: 'http://192.168.0.18:8081/realms/vida-cotidiana',
      VITE_API_BASE_URL: 'http://192.168.0.18:8080/api/v1',
    },
  },
})
