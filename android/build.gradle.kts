// AND-001: bootstrap only — no auth, no real API consumption yet (AND-002+).
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20" apply false
    // AND-005: applied only in app/build.gradle.kts, gated on google-services.json existing there.
    id("com.google.gms.google-services") version "4.4.2" apply false
}
