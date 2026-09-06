package com.vidacotidiana.app

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vidacotidiana.app.core.notifications.LocalReminderNotifier
import com.vidacotidiana.app.core.notifications.PushMessagingService
import com.vidacotidiana.app.core.notifications.PushTokenRegistrar
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VidaCotidianaApplication : Application() {

    @Inject
    lateinit var pushTokenRegistrar: PushTokenRegistrar

    override fun onCreate() {
        super.onCreate()

        // AND-006: always on in release; in debug, only if a dev explicitly opted in
        // at build time (BuildConfig.CRASHLYTICS_DEBUG_ENABLED, see app/build.gradle.kts)
        // — never hardcoded true, so a plain debug build never reports crashes silently.
        val collectionEnabled = !BuildConfig.DEBUG || BuildConfig.CRASHLYTICS_DEBUG_ENABLED
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(collectionEnabled)

        // AND-007: creating the channel is idempotent and cheap — safe to call on
        // every app start rather than only once at install time.
        LocalReminderNotifier.createChannel(this)

        // Canal propio para familia y compartidos: lo dispara otra persona, no
        // el reloj, y el usuario debe poder silenciar uno sin perder el otro.
        // Crearlo aqui —y no solo al recibir— hace que exista antes del primer
        // mensaje, que es cuando FCM lo necesita para dibujar en segundo plano.
        PushMessagingService.createChannel(this)

        // Empieza a observar la sesion para registrar el token de este
        // dispositivo en cuanto haya una. Sin esto el backend no tiene a donde
        // enviar y sus push se pierden aunque el adapter funcione.
        pushTokenRegistrar.start()
    }
}
