// AND-001: bootstrap only. Retrofit/OkHttp/Kotlin Serialization are added
// now (already-decided stack, 08-android-architecture.md) but unused until
// AND-002 wires real API consumption + auth.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    // AND-005: real google-services.json now present (Firebase project vida-cotidiana-6da30).
    id("com.google.gms.google-services")
    // AND-006: same real Firebase project, Crashlytics product.
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.vidacotidiana.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vidacotidiana.app"
        minSdk = 30 // DEC-011
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        // AND-002: AppAuth's RedirectUriReceiverActivity is registered for this
        // scheme via manifest merge — must match the redirect_uri configured on
        // the android-app Keycloak client (Phase 1) and BuildConfig.OIDC_REDIRECT_URI below.
        manifestPlaceholders["appAuthRedirectScheme"] = "com.vidacotidiana.app"

        // Real emulator/device host resolution: 10.0.2.2 is the special alias the
        // Android emulator uses to reach the host machine's localhost. This
        // checkout is verified against a physical device on the same LAN, which
        // needs the Mac's real LAN IP instead (matches ios/App/Core/Network/AppConfig.swift
        // and the backend's currently-pinned OIDC_ISSUER — see README/CIERRE notes;
        // update if the Mac's DHCP lease changes).
        buildConfigField("String", "OIDC_ISSUER", "\"http://192.168.0.18:8081/realms/vida-cotidiana\"")
        buildConfigField("String", "OIDC_CLIENT_ID", "\"android-app\"")
        buildConfigField("String", "OIDC_REDIRECT_URI", "\"com.vidacotidiana.app://callback\"")
        buildConfigField("String", "API_BASE_URL", "\"http://192.168.0.18:8080/api/v1/\"")

        // AND-006: Crashlytics is always enabled in release (see buildTypes below);
        // in debug it stays off unless a dev explicitly opts in for local
        // verification (-PcrashlyticsDebugEnabled=true or gradle.properties),
        // so no debug build from any developer's machine reports crashes by
        // default. See VidaCotidianaApplication.kt for where this is read.
        buildConfigField(
            "boolean",
            "CRASHLYTICS_DEBUG_ENABLED",
            (project.findProperty("crashlyticsDebugEnabled") as String? ?: "false"),
        )

        testInstrumentationRunner = "com.vidacotidiana.app.HiltTestRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // AND-002: Authorization Code + PKCE against Keycloak (android-app client, Phase 1).
    implementation("net.openid:appauth:0.11.1")
    // AND-002: EncryptedSharedPreferences/Keystore for token storage (11-auth-security.md).
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // AND-005: real Firebase project (google-services.json present) — token registration only,
    // no other Firebase product enabled.
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // AND-002/AND-003 real-device instrumented test: UI Automator drives the
    // Custom Tabs login page (a separate process/app — Espresso alone can't
    // reach across app boundaries), Compose Testing drives the app's own screens.
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.51.1")
}
