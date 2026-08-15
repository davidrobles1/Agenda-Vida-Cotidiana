# Vida Cotidiana — Android (AND-001, bootstrap)

Bootstrap scaffold only — no auth, no real API consumption. That starts at AND-002.

Stack (08-android-architecture.md): Kotlin, Jetpack Compose, Hilt, Coroutines/Flow,
Navigation Compose, Retrofit/OkHttp, Kotlin Serialization. `minSdk = 30` (DEC-011).

**ASSUMPTION:** exact AGP/Kotlin/Compose BOM/Hilt versions (AGP 8.5.2, Kotlin 2.0.20,
Compose BOM 2024.09.00, Hilt 2.51.1, Gradle 8.9) are a technical bootstrap choice, not
an approved business decision — resolves the "versiones exactas TBD al crear el
proyecto" note in `17-dependencies.md`.

## Build status (real, 2026-08-15)

`./gradlew assembleDebug` was run for real in the environment this scaffold was
created in. Gradle configuration succeeds (plugins resolve, Compose Compiler plugin
wired, dependencies declared) — the build only fails at
`:app:compileDebugJavaWithJavac` with **"SDK location not found"**, because no
Android SDK is installed there (no `ANDROID_HOME`, no `sdkmanager`/`adb`, no
`~/Library/Android/sdk`). This is **BLOCKED_BY_ENVIRONMENT**, not a project defect.

To build on a machine with Android Studio / the Android SDK installed:

```
sdkmanager "platforms;android-34" "build-tools;34.0.0"
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleDebug
```

## Structure

```
app/src/main/java/com/vidacotidiana/app/
  core/{network,security,ui}
  feature/{auth,home,reminders,sharing,settings}
  navigation/
```

One minimal Compose screen (`MainActivity`) renders "Vida Cotidiana". Everything else
is an empty package (`.gitkeep`) reserved for AND-002+.
