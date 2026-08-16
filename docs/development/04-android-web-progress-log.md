# 04 — Histórico de Progreso Android/Web

**Nota (2026-08-16):** por decisión explícita del usuario, iOS queda **pausado a partir de esta fecha** — no tocar `ios/` hasta nueva instrucción. Este documento existe para que, cuando iOS se retome, se pueda hacer un rebase informado de lo que Android/Web ya tienen (features, fixes, y — sobre todo — bugs reales encontrados cuya causa raíz es compartida entre plataformas, como una consulta de backend sin `ORDER BY`, o específica de un patrón que probablemente también exista en el código iOS equivalente aunque no se haya disparado todavía).

Cada entrada: qué se agregó, en qué plataforma(s), y el estado de iOS frente a esa misma pieza (**ya tiene** / **pendiente** / **riesgo no auditado**). Se sigue añadiendo una entrada nueva cada vez que Android/Web avancen — no hace falta pedirlo explícitamente, ya es práctica establecida de este documento.

---

## 2026-08-15 — AND-002/003, WEB-002/003: login (OIDC/PKCE) + reminders CRUD

**Qué:** autenticación real contra Keycloak (Authorization Code + PKCE) y CRUD de recordatorios (crear/listar/completar) contra el backend real.

**Plataformas:** Android (AppAuth-Android, Custom Tabs), Web (PKCE manual con `crypto.subtle`, sin librería).

**iOS:** **ya tiene** (IOS-002/003, verificado 2026-08-16 vía `ASWebAuthenticationSession` nativo + XCUITest en Simulador — ver `01-technical-backlog.md`).

**Bugs reales encontrados en esta entrega (Android):**
- `RedirectUriReceiverActivity` de AppAuth exige tema `Theme.AppCompat` — crasheaba al volver de Keycloak.
- `DefaultConnectionBuilder` de AppAuth rechaza HTTP no cifrado incondicionalmente — bloqueaba el intercambio de tokens contra el Keycloak de desarrollo.
- `MainActivity` sin `launchMode` se recreaba al volver del Custom Tab, dejando el estado de UI enganchado a una instancia obsoleta.
- Lista de recordatorios sin identificador único por fila — un botón "Complete" ambiguo entre corridas de test.

**Bug real encontrado en Web:** React StrictMode reintentaba el intercambio del authorization code (de un solo uso) — Keycloak rechazaba correctamente el segundo intento con 400.

**iOS — riesgo no auditado:** ninguno de estos cuatro afecta directamente al patrón de `ASWebAuthenticationSession`/SwiftUI usado en iOS (arquitectura distinta), pero no se ha verificado explícitamente que iOS esté libre de un análogo.

---

## 2026-08-16 — AND-004, WEB-004: Sharing/Invitations UI

**Qué:** panel de compartir por recordatorio (invitar por email/username, listar colaboradores + invitaciones pendientes, revocar/cancelar) y pantalla de "Invitaciones recibidas" (aceptar/rechazar). Contrato consumido tal cual, sin inventar endpoints.

**Plataformas:** Android (`ShareDialog`/`InvitationsScreen`, Compose), Web (`ShareDialog`/`InvitationsPage`, React).

**iOS:** **pendiente** (`IOS-004`, código UI ya escrito — `ShareView.swift`/`InvitationsView.swift` — pero sin verificar con un test real; pospuesto explícitamente el 2026-08-16 para priorizar Android/Web).

**Bugs reales encontrados durante esta entrega:**

1. **Backend, `ReminderRepository.findAccessibleTo` sin `ORDER BY`** — la consulta que lista "propios y compartidos" no tenía ningún orden explícito. Un recordatorio recién creado podía aparecer en cualquier posición de una lista ya larga y quedar fuera del viewport virtualizado (`LazyColumn` en Android; el mismo riesgo aplica a cualquier lista virtualizada). Fix: `ORDER BY r.createdAt DESC`.
   **iOS: ya tiene** el fix (es un cambio de backend, beneficia a las tres plataformas por igual sin tocar código de cliente) — pero la `List` de SwiftUI no virtualiza igual que `LazyColumn`, así que el síntoma nunca se habría manifestado idéntico ahí.

2. **Android, host de emulador filtrado de vuelta a `build.gradle.kts`** (`10.0.2.2` en vez de la IP LAN del Mac) — bloqueaba todo login/API contra el dispositivo físico. Fix: IP LAN explícita, igual que `AppConfig.swift` en iOS.
   **iOS: ya tiene** (`AppConfig.host` ya apuntaba a la IP LAN correcta desde `IOS-002`, no tuvo esta regresión).

3. **Android, `network_security_config_debug.xml` sin la IP LAN en la whitelist de cleartext** — al corregir (2), Android empezó a bloquear las peticiones HTTP del propio app (no las de Chrome), dando "Network error" sin que la petición llegara siquiera a Keycloak. Fix: se añadió el dominio.
   **iOS: no aplica** (iOS usa `NSAllowsLocalNetworking` en `Info.plist`, ya configurado desde `IOS-002`, sin el mismo mecanismo de whitelist por dominio).

4. **Android, race condition en `RemindersViewModel.refresh()`** — cada llamada lanzaba una corrutina independiente sin cancelar la anterior; el `refresh()` inicial de `init{}`, lento en un arranque en frío que compite con el login, podía resolver **después** del `refresh()` posterior a `createReminder()` y sobrescribir silenciosamente la lista con datos obsoletos. Fix: `Job` cancelado antes de relanzar.
   **iOS: riesgo no auditado — patrón idéntico presente en el código.** `RemindersView.swift`'s `refresh()` lanza dos `Task { }` independientes en cada llamada, sin cancelar los anteriores — el mismo patrón que causó el bug en Android. No se ha disparado porque `IOS-004` (que fue lo que lo expuso en Android, al combinar `createReminder` + `refresh` en sucesión rápida durante un test automatizado) todavía no se ha verificado en iOS. **Recomendación para cuando se retome iOS:** aplicar el mismo fix (cancelar el `Task` anterior) preventivamente antes de escribir el test de sharing.

5. **Android, botones con ancho cero en `ReminderRow`** — el `Row` no reservaba espacio para "Complete"/"Share"; un título largo los comprimía a 0px (presentes en el árbol de semántica pero no clicables). Fix: `Modifier.fillMaxWidth(0.6f)` + elipsis en el título.
   **iOS: riesgo no auditado, probablemente distinto.** El `HStack` de SwiftUI en `RemindersView.swift` no tiene el mismo comportamiento de compresión a ancho cero que un `Row` de Compose — un título largo ahí típicamente envuelve o trunca en vez de comprimir a los hermanos, pero no se ha verificado explícitamente con un título largo real.

6. **Android, `ShareDialog` llamaba `LocalContext.current` (función `@Composable`) dentro de un lambda de `remember { }`**, que no es un contexto `@Composable` — error de compilación. Fix: capturar `context` fuera de `remember`.
   **iOS: no aplica** (no existe un análogo directo al sistema de `CompositionLocal` de Compose en SwiftUI/`ShareView.swift`).

**Verificación real:** `SharingFlowTest` (Android, dispositivo físico) y `sharing.spec.ts` (Web, Playwright/Chromium) — ambos contra el backend real, confirmados por fuera consultando Postgres.

---

## 2026-08-16 — AND-005: registro de push (Firebase Cloud Messaging) — DONE

**Qué:** pantalla "Notifications" con botón "Enable notifications" → token FCM real → `POST /me/devices` real.

**Plataforma:** Android únicamente en esta entrada (Web se cierra en la siguiente entrada, el mismo día).

**iOS:** **pendiente**, bloqueado además por alcance (`IOS-005` pospuesto junto con `IOS-004`). `GoogleService-Info-apple.plist` (iOS) ya llegó del usuario durante esta sesión pero permanece sin mover/usar en la raíz del repo, tal como se pidió.

**Gatillo:** a mitad de esta sesión el usuario terminó de descargar `google-services.json` desde Firebase Console (proyecto `vida-cotidiana-6da30`) mientras se trabajaba en `AND-004` — se integró de inmediato en vez de esperar al cierre.

**Bugs reales encontrados:**

1. **`RegisterDeviceRequest.platform` con valor por defecto (`"ANDROID"`)** — `kotlinx.serialization`'s `Json` (con `encodeDefaults = false`, el default de la librería) omite del JSON cualquier campo cuyo **valor actual** coincida con su default declarado, sin importar si el llamador lo pasó explícitamente o no. El campo `platform` desaparecía del body en cada request y la validación `@NotBlank` del backend lo rechazaba con 400. Fix: quitar el default — el campo pasa a ser obligatorio en el constructor.
   **iOS: no aplica directamente** (Swift's `Codable`/`JSONEncoder` no tiene este comportamiento — siempre serializa todas las propiedades declaradas de un `struct`, sin omisión por "valor igual al default"). **Pero sí es una lección aplicable:** cualquier DTO con valores por defecto en Android/Kotlin debe auditarse contra este mismo patrón antes de asumir que "pasar el valor explícitamente" arregla algo — no lo hace.

2. **Falso positivo de test** — una fila de prueba insertada manualmente vía `curl` durante el diagnóstico del bug anterior (usando las credenciales de `testuser`, para reproducir el mismo contexto de autenticación que la app) quedó en la base de datos y satisfacía la aserción laxa del test aunque el registro real seguía fallando con 400. Fix: aserción adicional de "no debe verse ningún error HTTP visible" además de la fila esperada — y limpieza de la fila de prueba. **Lección general para cualquier plataforma:** una aserción de "aparece algo que coincide" no es suficiente cuando el estado persiste entre corridas contra un backend real sin reset — hay que descartar activamente el estado de error, no solo confirmar el estado de éxito.

**Verificación real:** `NotificationsFlowTest` (Android, dispositivo físico) — token FCM real obtenido de Firebase, registrado contra el backend real, confirmado por fuera consultando Postgres (`device_push_tokens`, token con el formato real `xxxx:APA91b...`). Sin cuenta de servicio de Firebase en este checkout, la entrega real de una notificación de punta a punta no se probó — solo el registro del token.

---

## 2026-08-16 — WEB-005: registro de push (Firebase Cloud Messaging Web Push) — DONE

**Qué:** misma pantalla/flujo que `AND-005`, para Web: `firebase/messaging`, Service Worker (`public/firebase-messaging-sw.js`), token real de Web Push → `POST /me/devices` real.

**Plataforma:** Web.

**iOS:** no aplica (Web Push es un mecanismo específico de navegador, sin equivalente directo en iOS nativo — iOS usa APNs vía el mismo backend `FcmPushNotificationSender`).

**Gatillo:** el usuario proveyó la config real de Firebase Web (mismo proyecto `vida-cotidiana-6da30`) inmediatamente después de `AND-005`, y más tarde la VAPID key (Firebase Console → Cloud Messaging → certificados push web) que solo él podía generar.

**Bug/limitación real encontrada durante la verificación (no es un bug de código):** el registro automatizado vía Playwright falla consistentemente en `pushManager.subscribe()` con `AbortError: Registration failed - permission denied` — reproducido igual en Chromium empaquetado y en Chrome real, headless y headed, siempre con el permiso de notificaciones correctamente concedido y el Service Worker correctamente registrado. El backend de push de Google rechaza la suscripción específicamente cuando detecta que el navegador está controlado por WebDriver — una medida anti-abuso del lado de Google, no algo corregible desde este código. **Confirmado pidiéndole al usuario un clic manual real** (mouse, Chrome no automatizado, sirviendo la app con `npm run dev` expuesto en la LAN): el dispositivo `WEB` se registró de verdad, confirmado por fuera consultando Postgres (`device_push_tokens`, `platform='WEB'`, `2026-08-16 08:28:32`, token con el formato real `xxxx:APA91b...`).
   **iOS: no aplica**, pero **lección general aplicable:** cualquier flujo de permisos de navegador/push que se intente verificar con Playwright/Selenium en el futuro (Web o, si alguna vez se investiga WebKit-driven testing en iOS/Safari) debe anticipar el mismo tipo de rechazo anti-automatización del lado del proveedor — la verificación automatizada puede quedar estructuralmente limitada a "todo lo previo al último paso", con el último paso requiriendo confirmación manual humana, documentada explícitamente en vez de forzar una aserción que nunca podrá pasar bajo WebDriver.

**Verificación real:** `e2e/notifications.spec.ts` (Playwright) cubre automatizadamente todo lo alcanzable bajo WebDriver (login real, `GET /me/devices` real, botón "Enable notifications" habilitado); el registro del token en sí quedó verificado por el clic manual del usuario + confirmación en Postgres, documentado explícitamente en el propio test y en `01-technical-backlog.md` — no simulado, no asumido.
