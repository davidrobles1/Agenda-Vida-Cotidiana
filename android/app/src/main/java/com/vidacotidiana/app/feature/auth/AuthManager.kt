package com.vidacotidiana.app.feature.auth

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.vidacotidiana.app.BuildConfig
import com.vidacotidiana.app.core.security.TokenStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
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

    private val authService = AuthorizationService(context)

    fun isLoggedIn(): Boolean = tokenStore.load() != null

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

    /** Exchanges the authorization code from [resultIntent] for tokens and persists the session. */
    suspend fun handleLoginResult(resultIntent: Intent) {
        val response = AuthorizationResponse.fromIntent(resultIntent)
        val exception = AuthorizationException.fromIntent(resultIntent)
        if (response == null) {
            throw exception ?: IllegalStateException("No authorization response and no exception")
        }

        val tokenResponse = performTokenRequest(response)
        tokenStore.save(
            TokenStore.Session(
                accessToken = requireNotNull(tokenResponse.accessToken) { "No access_token in token response" },
                refreshToken = tokenResponse.refreshToken,
                idToken = tokenResponse.idToken,
                accessTokenExpirationMillis = tokenResponse.accessTokenExpirationTime,
            ),
        )
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
        tokenStore.clear()
    }

    /**
     * Current access token, or null if never logged in. Does not itself
     * refresh an expired token — the access token is short-lived (5 min,
     * same `accessTokenLifespan` as every other client in the realm) and
     * refreshing it automatically on 401 is out of scope for this bootstrap
     * (ASSUMPTION, not a decision: real refresh-on-401 belongs with the
     * rest of the token-lifecycle hardening, not this first vertical slice).
     */
    fun currentAccessToken(): String? = tokenStore.load()?.accessToken
}
