# 08 — Arquitectura Android

**DECISION (DEC-011):** `minSdk = 30` (Android 11).

## Stack
- Kotlin.
- Jetpack Compose.
- ViewModel.
- Coroutines/Flow.
- Navigation Compose.
- Hilt.
- Retrofit/OkHttp.
- Kotlin Serialization.
- Room solo cuando la V1 necesite persistencia local.
- Android Keystore para material sensible.

## Patrón
Clean Architecture + MVVM + unidirectional data flow.

MVI no será obligatorio globalmente. Se usará donde el estado complejo lo justifique.

## Estructura
```text
app/
  core/
    network/
    security/
    ui/
  feature/
    auth/
    home/
    reminders/
    sharing/
    settings/
  navigation/
```

Flujo:
```text
Compose UI
 -> ViewModel
 -> UseCase
 -> Repository
 -> Remote/Local DataSource
```

## Seguridad
- no almacenar contraseñas;
- tokens en almacenamiento seguro;
- no registrar tokens;
- permisos mínimos;
- Network Security Config;
- no confiar en datos del cliente para autorización.

## Push (DEC-005/DEC-010)
El cliente Android obtiene el token de FCM y lo registra contra el backend (`POST /me/devices`, FR-012) al iniciar sesión, y solicita su eliminación (`DELETE /me/devices/{deviceId}`) al cerrar sesión.
