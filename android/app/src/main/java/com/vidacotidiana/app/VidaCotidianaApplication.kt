package com.vidacotidiana.app

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vidacotidiana.app.core.notifications.LocalReminderNotifier
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VidaCotidianaApplication : Application() {

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
    }
}
