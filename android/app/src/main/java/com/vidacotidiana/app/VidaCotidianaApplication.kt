package com.vidacotidiana.app

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
    }
}
