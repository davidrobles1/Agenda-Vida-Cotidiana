// WEB-002. Defaults match Phase 1's Keycloak client (web-spa) and local dev backend.
export const authConfig = {
  issuer: import.meta.env.VITE_OIDC_ISSUER ?? 'http://localhost:8081/realms/vida-cotidiana',
  clientId: import.meta.env.VITE_OIDC_CLIENT_ID ?? 'web-spa',
  redirectUri: import.meta.env.VITE_OIDC_REDIRECT_URI ?? 'http://localhost:5173/callback',
  scope: 'openid profile email',
}

export const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'
