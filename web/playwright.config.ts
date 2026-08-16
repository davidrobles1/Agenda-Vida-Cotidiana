import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
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
