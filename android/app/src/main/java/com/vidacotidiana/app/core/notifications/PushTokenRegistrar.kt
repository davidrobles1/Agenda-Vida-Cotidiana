package com.vidacotidiana.app.core.notifications

import com.google.firebase.messaging.FirebaseMessaging
import com.vidacotidiana.app.core.network.DeviceApi
import com.vidacotidiana.app.core.network.RegisterDeviceRequest
import com.vidacotidiana.app.feature.auth.AuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lleva el token de este dispositivo al backend.
 *
 * `DeviceApi` existía y Hilt lo proveía, pero NADIE lo llamaba: el token nunca
 * se pedía a FCM ni se registraba, así que el backend no tenía a dónde enviar
 * y sus push se perdían aunque el adapter funcionara. Este es el eslabón que
 * faltaba.
 *
 * SE REGISTRA CUANDO HAY SESIÓN, no al arrancar. `POST /me/devices` es un
 * endpoint autenticado: llamarlo sin token de acceso devuelve 401 y el
 * registro se perdería. Por eso observa la sesión —el mismo flujo que ya usa
 * `AppViewModel`— y actúa en cuanto se abre. Así funciona igual si el usuario
 * ya venía identificado al abrir la app que si inicia sesión tres pantallas
 * después.
 */
@Singleton
class PushTokenRegistrar @Inject constructor(
    private val deviceApi: DeviceApi,
    private val authManager: AuthManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** El último token conocido, para poder reenviarlo al abrirse la sesión. */
    @Volatile
    private var pendingToken: String? = null

    /**
     * Empieza a observar la sesión. Lo arranca la propia `Application`, una
     * sola vez.
     */
    fun start() {
        scope.launch {
            authManager.isLoggedInFlow.collect { loggedIn ->
                if (loggedIn) registerCurrentToken()
            }
        }
    }

    /**
     * FCM rotó el token. Si hay sesión se manda ya; si no, se guarda para
     * mandarlo en cuanto la haya — descartarlo dejaría el dispositivo mudo
     * hasta la siguiente rotación, que puede tardar semanas.
     */
    fun onTokenRefreshed(token: String) {
        pendingToken = token
        if (authManager.isLoggedIn()) {
            scope.launch { register(token) }
        }
    }

    private suspend fun registerCurrentToken() {
        val token = pendingToken ?: runCatching { FirebaseMessaging.getInstance().token.await() }
            .getOrNull()
            ?: return
        pendingToken = token
        register(token)
    }

    private suspend fun register(token: String) {
        // Best-effort, igual que el envío en el backend (AC-012): que el
        // registro falle no debe romper nada de lo que el usuario esté
        // haciendo. Sin red, se reintenta en la próxima apertura de sesión.
        runCatching { deviceApi.registerDevice(RegisterDeviceRequest(platform = PLATFORM, token = token)) }
    }

    private companion object {
        /** Uno de los tres valores de `DevicePlatform` en el backend. */
        const val PLATFORM = "ANDROID"
    }
}
