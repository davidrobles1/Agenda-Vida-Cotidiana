# 08b — Arquitectura iOS

**DECISION (DEC-006/ADR-010):** SwiftUI + Swift nativo. No se usa una solución cross-platform (Kotlin Multiplatform, Flutter, React Native), siguiendo el criterio de CLAUDE.md de priorizar soporte de Apple, mantenibilidad, seguridad, performance, integración nativa y longevidad por sobre el ahorro de código compartido.

**DECISION (DEC-012):** versión mínima soportada: **iOS 17**.

## Stack
- Swift.
- SwiftUI.
- Combine o `async`/`await` nativo para concurrencia (a definir en el bootstrap del proyecto).
- URLSession (o una librería ligera de networking) para consumo de la API REST `/api/v1`.
- Keychain para material sensible (tokens OIDC).
- AppAuth-iOS (o equivalente) para el flujo Authorization Code + PKCE contra Keycloak (DEC-004).
- Firebase SDK (FCM) para push (DEC-010).

## Patrón
Clean Architecture + MVVM, análogo al patrón usado en Android (`08-android-architecture.md`) para mantener coherencia conceptual entre plataformas, sin compartir código.

## Estructura (propuesta, a validar en el bootstrap del proyecto)
```text
App/
  Core/
    Network/
    Security/
    UI/
  Feature/
    Auth/
    Home/
    Reminders/
    Sharing/
    Settings/
  Navigation/
```

Flujo:
```text
SwiftUI View
 -> ViewModel (ObservableObject)
 -> UseCase
 -> Repository
 -> Remote/Local DataSource
```

## Seguridad
- no almacenar contraseñas (identidad delegada a Keycloak, DEC-004);
- tokens en Keychain, nunca en `UserDefaults`;
- no registrar tokens en logs;
- App Transport Security (ATS) sin excepciones salvo justificación explícita;
- no confiar en datos del cliente para autorización (igual que Android/Web).

## Push (DEC-005/DEC-010)
El cliente iOS obtiene el token de FCM (que internamente usa APNs) y lo registra contra el backend (`POST /me/devices`, FR-012) al iniciar sesión, y solicita su eliminación al cerrar sesión.

## TBD
- Gestor de dependencias (Swift Package Manager asumido por defecto; alternativa CocoaPods no descartada si una librería concreta lo requiere).
- Versión exacta de Xcode/Swift al hacer el bootstrap del proyecto (ver `17-dependencies.md`).
