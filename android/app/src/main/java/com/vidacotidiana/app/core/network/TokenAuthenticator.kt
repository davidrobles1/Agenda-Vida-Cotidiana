package com.vidacotidiana.app.core.network

import com.vidacotidiana.app.feature.auth.AuthManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AND-009 (Task B §3): when an API call fails with a real 401 (expired
 * access token — the access token lifespan is 5 minutes, same as every
 * other client in the realm, see 33-security-cross-audit.md §1.3), refresh
 * via AuthManager.getValidAccessToken(forceRefresh = true) — which performs
 * a real grant_type=refresh_token request through AppAuth's AuthState — and
 * retry the request once with the new token, instead of failing straight to
 * the caller and forcing a manual login. OkHttp calls Authenticator.authenticate
 * on its own dispatcher thread (never the Android main thread), so blocking
 * here with runBlocking on the suspend refresh call is safe and is the same
 * bridging pattern AppAuth's own samples use for this exact case.
 */
@Singleton
class TokenAuthenticator @Inject constructor(private val authManager: AuthManager) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Never retry more than once — a second 401 after a real refresh means
        // the refresh_token itself is no longer valid (expired/revoked), and
        // this must fail closed to a manual login rather than loop forever.
        if (responseCount(response) >= 2) return null

        // Don't attempt a refresh_token grant if there was never a session to
        // refresh in the first place (e.g. an unauthenticated 401 from a
        // misconfigured endpoint) — that's not this filter's job to recover.
        if (authManager.currentAccessToken() == null) return null

        val freshToken = runBlocking { authManager.getValidAccessToken(forceRefresh = true) } ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $freshToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}
