package com.vidacotidiana.app.feature.auth

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.vidacotidiana.app.BuildConfig
import com.vidacotidiana.app.core.network.AppAuthConfigProvider
import com.vidacotidiana.app.core.security.TokenStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * AND-002: Authorization Code + PKCE against the android-app Keycloak client
 * (Phase 1) via AppAuth-Android — AppAuth generates and verifies the PKCE
 * code_verifier/code_challenge itself (S256), and the redirect comes back to
 * the app through the appAuthRedirectScheme registered in build.gradle.kts,
 * never through a WebView (Custom Tabs, per AppAuth's own security guidance).
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStore: TokenStore,
) {
    private val serviceConfig = AuthorizationServiceConfiguration(
        "${BuildConfig.OIDC_ISSUER}/protocol/openid-connect/auth".toUri(),
        "${BuildConfig.OIDC_ISSUER}/protocol/openid-connect/token".toUri(),
    )

    // AND-008 (Task B §2): Keycloak's real self-registration endpoint accepts the
    // exact same OAuth/PKCE query params as /auth (client_id, redirect_uri,
    // response_type, scope, code_challenge*) and redirects back with an
    // authorization code the same way — so reusing AuthorizationServiceConfiguration/
    // AuthorizationRequest here gets PKCE generation and the Custom Tab launch for
    // free, with zero duplicated crypto/browser code. Only the authorizationEndpoint
    // differs; tokenEndpoint (used by the exchange below) is the same for both.
    private val registrationServiceConfig = AuthorizationServiceConfiguration(
        "${BuildConfig.OIDC_ISSUER}/protocol/openid-connect/registrations".toUri(),
        "${BuildConfig.OIDC_ISSUER}/protocol/openid-connect/token".toUri(),
    )

    // AppAuthConfigProvider is variant-specific (src/debug vs src/release): debug
    // allows plain HTTP so this can reach the local dev Keycloak; release stays
    // HTTPS-only (AppAuth's own default) — see AppAuthConfigProvider/DebugConnectionBuilder.
    private val authService = AuthorizationService(context, AppAuthConfigProvider.config)

    // AND-009 (Task B §3): the in-memory AuthState is the single source of
    // truth for the session — loaded once from TokenStore, mutated in place
    // by every login/refresh, and re-persisted after each mutation. Using
    // AppAuth's own AuthState (rather than four separate token fields) is
    // what makes performActionWithFreshTokens below possible without
    // hand-rolling a refresh_token grant call.
    @Volatile
    private var authState: AuthState = tokenStore.loadAuthState() ?: AuthState(serviceConfig)

    fun isLoggedIn(): Boolean = authState.isAuthorized

    fun buildLoginIntent(): Intent {
        val request = AuthorizationRequest.Builder(
            serviceConfig,
            BuildConfig.OIDC_CLIENT_ID,
            ResponseTypeValues.CODE,
            BuildConfig.OIDC_REDIRECT_URI.toUri(),
        )
            .setScope("openid profile email")
            .build() // PKCE (S256) is generated automatically by the builder.

        return authService.getAuthorizationRequestIntent(request)
    }

    fun buildRegisterIntent(): Intent {
        val request = AuthorizationRequest.Builder(
            registrationServiceConfig,
            BuildConfig.OIDC_CLIENT_ID,
            ResponseTypeValues.CODE,
            BuildConfig.OIDC_REDIRECT_URI.toUri(),
        )
            .setScope("openid profile email")
            .build()

        return authService.getAuthorizationRequestIntent(request)
    }

    /** Exchanges the authorization code from [resultIntent] for tokens and persists the session. */
    suspend fun handleLoginResult(resultIntent: Intent) {
        val response = AuthorizationResponse.fromIntent(resultIntent)
        val exception = AuthorizationException.fromIntent(resultIntent)
        if (response == null) {
            throw exception ?: IllegalStateException("No authorization response and no exception")
        }

        val newAuthState = AuthState(response, exception)
        val tokenResponse = performTokenRequest(response)
        newAuthState.update(tokenResponse, null)
        authState = newAuthState
        tokenStore.saveAuthState(authState)
    }

    private suspend fun performTokenRequest(response: AuthorizationResponse): TokenResponse =
        suspendCancellableCoroutine { continuation ->
            authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, ex ->
                when {
                    tokenResponse != null -> continuation.resume(tokenResponse)
                    ex != null -> continuation.resumeWithException(ex)
                    else -> continuation.resumeWithException(IllegalStateException("Token exchange returned neither a response nor an error"))
                }
            }
        }

    fun logout() {
        authState = AuthState(serviceConfig)
        tokenStore.clear()
    }

    /**
     * Current access token, or null if never logged in. Does not itself
     * refresh — for that, see [getValidAccessToken] (used by
     * TokenAuthenticator on a real 401, not on every request: AppAuth's own
     * expiry check already covers the common case, retry-on-401 is the
     * backstop for clock skew / a token the server considers expired sooner
     * than our local copy thinks).
     */
    fun currentAccessToken(): String? = authState.accessToken

    /**
     * AND-009 (Task B §3): AppAuth's documented refresh mechanism —
     * `AuthState.performActionWithFreshTokens` checks the token's own
     * tracked expiry and, if it's expired (or [forceRefresh] says to treat
     * it as such regardless — used after a real 401, since the server's
     * clock is authoritative over our local guess), performs a real
     * `grant_type=refresh_token` request against the same token endpoint
     * before invoking the callback with a fresh access token. On failure
     * (e.g. the refresh_token itself is expired/revoked), returns null —
     * the caller falls back to requiring a manual login, the same
     * fail-closed behavior as before this task, just no longer the *first*
     * response to an expired access token.
     */
    suspend fun getValidAccessToken(forceRefresh: Boolean = false): String? {
        if (!authState.isAuthorized) return null
        if (forceRefresh) authState.needsTokenRefresh = true

        return suspendCancellableCoroutine { continuation ->
            authState.performActionWithFreshTokens(authService) { accessToken, _, ex ->
                if (ex != null || accessToken == null) {
                    continuation.resume(null)
                } else {
                    tokenStore.saveAuthState(authState)
                    continuation.resume(accessToken)
                }
            }
        }
    }
}
