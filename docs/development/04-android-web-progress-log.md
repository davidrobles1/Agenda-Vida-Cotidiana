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

---

## 2026-08-16 — AND-006/WEB-006/INFRA-007: reporte de errores (Crashlytics + GlitchTip)

**Qué:** Firebase Crashlytics en Android (mismo proyecto Firebase que `AND-005`), GlitchTip autoalojado para Web (`docker-compose.yml`, reutiliza el servicio `postgres` existente + Redis, imagen oficial `glitchtip/glitchtip:v4.1.3`), `@sentry/react` como cliente Web (GlitchTip habla el protocolo de Sentry).

**Plataformas:** Android, Web. **Enfoque acotado explícitamente por el usuario** — nada de CI, Crashlytics/GlitchTip en un entorno real, ni auditoría de seguridad todavía; esta entrada es solo error-tracking de desarrollo local.

**iOS:** **pendiente**, ambas piezas (Firebase Crashlytics tiene un equivalente directo para iOS vía el mismo proyecto Firebase — `GoogleService-Info-apple.plist` ya está en la raíz del repo desde `AND-005`, sin usar; GlitchTip también aceptaría un cliente `@sentry/react-native` o el SDK nativo de Sentry para Swift si iOS alguna vez lo necesita, aunque eso no se ha evaluado). No se tocó `ios/` en esta sesión.

**Bugs/hallazgos reales durante esta verificación:**

1. **Android: header de `RemindersScreen` desbordado** — un `Row` con 4 botones (Invitations/Notifications/Debug ×2) no cabía en el ancho de pantalla; los dos botones de debug ni siquiera aparecían en el árbol de UI Automator (ausentes, no solo invisibles). Mismo patrón que el bug de `ReminderRow` en `AND-004`, esta vez sin necesitar un título largo — cualquier `Row` sin `weight`/límites de ancho con suficientes hijos puede reproducirlo. Fix: los botones de debug se movieron a su propio `Row`.
   **iOS: riesgo no auditado.** El `HStack` del header de `RemindersView.swift` en iOS tiene el mismo riesgo teórico si se le añaden más elementos — SwiftUI maneja el overflow distinto (envuelve/trunca en vez de comprimir a cero), pero no se ha verificado con una cantidad de botones similar.

2. **Android: `connectedAndroidTest` no observa un crash real de la misma forma que un tap manual vía `adb`** — `CrashlyticsCrashTest` (Compose Testing) reporta "passed" incluso cuando el botón de debug SÍ lanza la excepción y el proceso muere; la prueba real vino de `adb shell input tap` directo sobre la app instalada (fuera de cualquier arnés de test), que sí mostró el `FATAL EXCEPTION` genuino en `logcat` y el proceso muerto (`adb shell pidof` vacío). **Lección general:** un test instrumentado que pasa no siempre es evidencia suficiente para un escenario de crash real — cuando el objetivo es específicamente probar que "el proceso muere de verdad", el mecanismo del test framework puede interferir con la observación misma; documentado explícitamente en el Javadoc de `CrashlyticsCrashTest` en vez de confiar ciegamente en su resultado verde.
   **iOS: no aplica directamente** (XCTest tiene su propio manejo de crashes, no se ha investigado si comparte esta limitación).

3. **Entorno macOS, no un bug de la app:** el puerto host `5432` ya estaba ocupado por una instalación nativa de PostgreSQL 17 ajena a Docker — remapeado a `5434` en `docker-compose.yml` para el servicio `postgres`, sin afectar la comunicación interna contenedor-a-contenedor. Y Docker Desktop rechazó montar el script de init de GlitchTip con "operation not permitted" — restricción TCC de macOS para la carpeta `~/Documents`, resuelta con un permiso de sistema de una sola vez (Ajustes → Privacidad y Seguridad → Archivos y Carpetas → Docker → Carpeta Documentos). Documentado en el comentario del propio `docker-compose.yml` junto con un workaround (`docker exec ... psql -c "CREATE DATABASE glitchtip"`) por si se repite en otra máquina.

**Verificación real:**
- **Android:** con `-PcrashlyticsDebugEnabled=true`, un tap real (no automatizado) sobre "Debug: crash" produjo un `FATAL EXCEPTION` real con el stack trace exacto y mató el proceso de verdad; el SDK de Crashlytics se conectó de verdad a Firebase (tokens de autenticación genuinos en los logs); **el usuario confirmó visualmente en Firebase Console → Crashlytics que el evento llegó** — Crashlytics no tiene una API pública en tiempo real como GlitchTip, así que esta confirmación humana era la única forma honesta de cerrar el loop, tal como permitía la tarea.
- **Web:** `e2e/error-tracking.spec.ts` (Playwright real) hace clic en un botón de debug que lanza durante el render (capturado por un `Sentry.ErrorBoundary` nuevo en `main.tsx`), y **confirma programáticamente contra la propia API de GlitchTip** (`GET /api/0/projects/{org}/{project}/issues/`, compatible con la API de Sentry, con un `APIToken` real) que el issue llegó — no solo mirando la UI. Confirmado además manualmente con `curl` fuera del test.

`./mvnw clean test`: 110/110 en verde (sin cambios de este bloque). `npm run build`: éxito real. `./gradlew assembleDebug`/`assembleRelease`: ambos `BUILD SUCCESSFUL`.

---

## 2026-08-16 — UX-005: tema visual de Keycloak por cliente (mobile/web)

**Qué:** dos temas de login de Keycloak (`vida-cotidiana-mobile`, `vida-cotidiana-web`), construidos como `parent=keycloak.v2` con el mínimo diff posible sobre una copia real de los `.ftl` extraída del propio contenedor en ejecución — un solo panel dividido para Web, columna única para mobile. Ver `Documentacion/02-ux-ui/login-theme.md` para el detalle completo.

**Plataformas:** Keycloak (server-side, `infra/keycloak/themes/`), aplica automáticamente a Android y Web vía `attributes.login_theme` por cliente — sin tocar código de app en ninguna de las dos.

**iOS:** **ya tiene** (no bloqueado, no requiere trabajo) — `ios-app` ya apunta a `vida-cotidiana-mobile` en el realm, configuración de servidor pura. Sin verificar visualmente en un dispositivo/Simulador iOS todavía (no se tocó `ios/` en esta sesión, sigue pausado).

**Bugs reales encontrados:**

1. **Admin REST API de Keycloak: `loginTheme` no es un campo de nivel superior.** El primer intento de fijar el tema por cliente vía la API admin asumió `ClientRepresentation.loginTheme` (así lo describía la documentación de un provider de Terraform/Pulumi consultada antes de escribir código) — la API real, en ejecución, lo rechazó con `Unrecognized field "loginTheme"`. El campo real es `attributes.login_theme`. Corregido tanto en la llamada admin como en `realm-vida-cotidiana.json` (que había quedado con el campo equivocado del mismo primer intento). **iOS: no aplica** (es un detalle del backend/Admin API de Keycloak, no de ningún cliente).

2. **CSS: el panel dividido de Web no cuadraba con el grid declarado.** `grid-template-columns: min(45vw,480px) 1fr` debería sumar 480+960=1440 en un viewport de 1440px, pero `getComputedStyle` reportaba `480px 700px` y `.pf-v5-c-login__main` no llenaba la altura del viewport. Causa real: el tema base `keycloak.v2` trae su propio `column-gap` de 64px y padding en `.pf-v5-c-login__container` (para un grid responsive interno de PatternFly que no usamos) y no fija `box-sizing:border-box` en ningún lado. Confirmado con `getComputedStyle`/`boundingBox()` en un script de Playwright desechable — no reinspeccionando la captura de pantalla — antes de dar el fix por bueno. Fix: reset explícito de `gap`/`padding`/`margin`/`box-sizing` bajo el scope `vc-theme-web`, más `align-self:stretch` en la columna del formulario. **iOS: no aplica** (mismo tema `vida-cotidiana-mobile` de Android, columna única, sin grid dividido).

**Verificación real:** Android — login real por Custom Tab en un dispositivo físico Samsung, capturas antes/después (`screenshots/android-kc-login-{before,after}.png`, tema desactivado temporalmente vía Admin API para la captura "before" y restaurado después — misma técnica de "revertir, capturar, restaurar" que el `git stash` de `UX-004`, aquí a nivel de configuración de Keycloak en vez de código); `LoginAndRemindersFlowTest`/`SharingFlowTest` (instrumentados reales, mismo dispositivo) en verde contra la página con tema. Web — capturas reales `screenshots/web-kc-login-{before,after}.png` (Playwright/Chrome real); `sharing.spec.ts`/`notifications.spec.ts`/`error-tracking.spec.ts` (cada uno inicia sesión por esta página real como parte de su propio setup) en verde contra la página con tema. El contenedor de desarrollo (`vc-dev-keycloak`) se parcheó en caliente (`docker cp` + Admin API) en vez de recrearse desde el realm JSON actualizado — recrearlo habría borrado `testuser`/`userb` (usuarios reales creados fuera del JSON versionado, referenciados por columnas `owner` en los datos de Postgres); el JSON versionado queda consistente para que una importación limpia futura reproduzca el mismo resultado.

**Adición posterior, misma fecha:** panel hero de Web con un ornamento SVG inline abstracto (círculos superpuestos, sin ícono ni logo — ninguno aprobado en `02-ux-ui/`, no se inventó uno). Sin imagen de referencia real disponible en este entorno para replicar con fidelidad — declarado explícitamente como límite en `login-theme.md` §8 en vez de forzar una réplica no verificada. Mobile queda sin ornamento a propósito (viewport angosto de Custom Tab). `web-kc-login-after.png` recapturado; `sharing`/`notifications`/`error-tracking.spec.ts` reverificados en verde.
