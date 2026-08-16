package com.vidacotidiana.app.core.network

import android.net.Uri
import net.openid.appauth.connectivity.ConnectionBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * AND-002, debug builds only (this file lives under src/debug, never compiled
 * into release): AppAuth's DefaultConnectionBuilder hard-refuses any
 * non-HTTPS connection ("only https connections are permitted") — correct
 * for production, but it blocks the token exchange against the local dev
 * Keycloak, which is plain HTTP by design (no TLS cert for a LAN IP/
 * 10.0.2.2). Found for real on a physical device: login reached Keycloak,
 * authenticated, redirected back, and then the token exchange crashed here.
 * Mirrors DefaultConnectionBuilder's timeouts/redirect policy exactly, minus
 * the HTTPS-only check.
 */
object DebugConnectionBuilder : ConnectionBuilder {
    private val CONNECTION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(15).toInt()
    private val READ_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10).toInt()

    override fun openConnection(uri: Uri): HttpURLConnection {
        val connection = URL(uri.toString()).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECTION_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.instanceFollowRedirects = false
        return connection
    }
}
