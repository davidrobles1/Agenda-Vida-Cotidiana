# 33 — Auditoría cruzada de seguridad

**Alcance:** `21-security.md`, `24-security-checklist.md`, `11-auth-security.md` (y su decisión asociada en `22-decision-log.md`), verificados uno por uno contra el código real del backend, Android y Web — no contra lo que la documentación *dice* que existe. Cada afirmación tiene evidencia real (archivo/línea/comando) o queda marcada `DOCUMENTATION_CONFLICT`. No se resuelve ningún conflicto en silencio.

Severidades según la clasificación ya definida en `21-security.md` (CRÍTICA/ALTA/MEDIA/BAJA).

---

## 1. Hallazgos reales — requieren decisión del Product Owner

### 1.1 `DOCUMENTATION_CONFLICT` — protección contra fuerza bruta y recuperación de cuenta, declaradas pero no configuradas — **ALTA**

`11-auth-security.md` línea 4 y `22-decision-log.md` (ADR-008) afirman como parte de la misma DECISION: *"identidad delegada a Keycloak ... con soporte de passkeys/WebAuthn, MFA, gestión de sesiones, **recuperación de cuenta y protección contra fuerza bruta**"*.

Verificado contra el realm real (`infra/keycloak/realm-vida-cotidiana.json`, confirmado también contra el servidor Keycloak en ejecución vía Admin API):
```json
{"resetPasswordAllowed": false, "bruteForceProtected": false, "permanentLockout": false}
```

- **`bruteForceProtected: false`**: no hay bloqueo de cuenta tras intentos fallidos repetidos de login. Esto es un gap de seguridad real y activo hoy, no solo documental — un atacante puede intentar credenciales sin límite contra el realm real.
- **`resetPasswordAllowed: false`**: no existe recuperación de cuenta self-service (no hay "¿Olvidaste tu contraseña?" funcional), pese a que la documentación lo declara como ya resuelto.
- El `webAuthnPolicy*` sí está presente en el realm (scaffold estándar de Keycloak), pero ninguna authenticationFlow lo requiere u ofrece activamente — es la configuración por defecto sin usar, no una decisión de habilitarlo.

**No se corrige en silencio.** Activar `bruteForceProtected`/`resetPasswordAllowed` es una decisión de configuración de Keycloak de bajo riesgo técnico, pero **no le corresponde a esta auditoría decidir los parámetros** (umbral de intentos, tiempo de espera, si se habilita recuperación por email dado que el envío de correo real sigue `TBD` — DEC-009 pendiente, `22-decision-log.md`). Recomendación: el Product Owner decide si se cierra ahora o se documenta explícitamente como `FUTURE`.

### 1.2 Gap real (no conflicto de texto, pero sí de expectativa) — autorregistro no funciona en ningún cliente — **ALTA**

`02-roadmap.md` (V1 — Definition of Success, punto 1) y `FR-001` (`03-prd.md`) esperan que "el usuario pueda registrarse". Verificado:
- El realm tiene `registrationAllowed: false` — Keycloak rechaza el autorregistro.
- Ni `LoginScreen.kt` (Android) ni `LoginPage.tsx` (Web) tienen ningún botón/enlace de registro — solo "Log in".
- Todas las verificaciones reales de este proyecto hasta la fecha (Android/Web/iOS) usaron cuentas ya aprovisionadas por admin (`testuser`, `userb`), nunca un registro real de punta a punta.

**Corrección a `docs/development/05-v2-plan.md`:** esa auditoría anterior marcó el punto 1 de la Definition of Success como `DONE` basándose solo en evidencia de *login*, sin verificar *registro* por separado — un gap de la propia auditoría anterior, no solo del código. Se corrige explícitamente en la sección 3 de este documento.

### 1.3 Hallazgo real, severidad informativa — asimetría de renovación de sesión entre plataformas — **BAJA**

- Web (`web/src/core/auth/authClient.ts`): renovación silenciosa real vía iframe oculto + `prompt=none` (`silentRenew`), confirmado en código — coincide con lo que documenta `21-security.md`.
- Android (`android/.../feature/auth/AuthManager.kt:94-101`): **no** refresca el token automáticamente al expirar — comentario propio en el código: *"refreshing it automatically on 401 is out of scope for this bootstrap (ASSUMPTION, not a decision...)"*. El token de acceso dura 5 minutos (mismo `accessTokenLifespan` que las demás plataformas); pasado ese tiempo, cada llamada falla con 401 hasta un nuevo login manual.
- **Verificado en vivo durante esta misma tarea** (no una inferencia): la verificación en dispositivo de `AND-007` chocó repetidamente con este límite real — cada intento de crear un recordatorio con fecha de vencimiento, varios minutos después del login, devolvía `HTTP 401` en pantalla, obligando a reiniciar sesión.
- No es una vulnerabilidad (fallar cerrado, exigiendo volver a iniciar sesión, es el lado seguro del fallo) — es un gap de completitud/UX real, no de seguridad. Queda como candidato de `05-v2-plan.md` §3, no se corrige aquí (fuera del alcance de esta auditoría, que es de seguridad, no de UX).

---

## 2. Controles verificados con evidencia real — sin conflicto

| Control | Evidencia real |
|---|---|
| Sin fuga de stack traces/SQL/nombres internos en errores | `GlobalExceptionHandler.java:75-80` — el handler genérico de `Exception.class` nunca devuelve `ex.getMessage()`, solo `"An unexpected error occurred."`; el detalle real solo existe en el log del servidor, correlacionado por `traceId`. |
| Rate limiting (`DEVOPS-001`) sigue activo | `InvitationRateLimiter.java` sin cambios, 10/hora por usuario; wireado y confirmado en `SharingService.java:78` (`invitationRateLimiter.checkAndRecord(callerUserId)`). |
| Auditoría de eventos (`BE-029`) cubre lo documentado | Los 6 eventos de `11-auth-security.md` §Auditoría están wireados: `INVITATION_CREATED`/`ACCEPTED`/`REJECTED`/`CANCELLED` y `SHARE_REVOKED` en `SharingService.java`, `INVITATION_EXPIRED` en `InvitationMaintenanceService.java`. |
| Autorización por objeto (BOLA/IDOR) | `ReminderControllerIntegrationTest`: `getReminder_ownedByAnotherUserReturnsNotFound_neverForbidden`, mismo patrón en update/delete — 404, nunca 403, diseño deliberado anti-enumeración. |
| Sin SQL/queries nativas (superficie de inyección) | `grep` sin resultados para `createNativeQuery`/`nativeQuery = true` en todo el backend — solo JPA/Hibernate parametrizado. |
| Correlación de requests (observabilidad) | `TraceIdFilter.java` — `traceId` real por request vía MDC, expuesto en header `X-Trace-Id` y en el campo `traceId` del envelope `Error`. |
| Almacenamiento de tokens — Android | `TokenStore.kt` — `EncryptedSharedPreferences` real (AES256-GCM, clave en Android Keystore), no `SharedPreferences` plano. |
| Almacenamiento de tokens — iOS | `KeychainTokenStore.swift` — `kSecClassGenericPassword` real vía Security framework, `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`. |
| Almacenamiento de tokens — Web | `authClient.ts` — variable de módulo en memoria (`let tokenSet`); `sessionStorage` se usa *solo* para el `code_verifier` PKCE transitorio, nunca para el token. Ningún uso de `localStorage` en todo `web/src`. |
| Android release sin excepción de cleartext | `network_security_config_debug.xml` vive en `src/debug/` (source set exclusivo de debug) — el manifest de `src/release/` no lo referencia; un build de release hereda el default seguro de Android (sin cleartext). |
| No se filtran tokens/contraseñas a logs | `grep` sin resultados para logging de `password`/`token` con contenido real en backend/Android (comprobación superficial, no exhaustiva — ver limitaciones abajo). |

---

## 3. Cobertura OWASP (evaluación de alto nivel, no un pentest formal)

Referencia: `CLAUDE.md`/`21-security.md` piden ASVS, Top 10, API Security Top 10 — sin declarar cumplimiento formal (correcto, no se declara aquí tampoco).

- **A01 Broken Access Control**: mitigado y probado (ver tabla arriba).
- **A02 Cryptographic Failures**: tokens cifrados en reposo en Android/iOS; TLS/HTTPS en tránsito queda `TBD` para el entorno real (depende de `INFRA-004`, servidor propio todavía no existe) — no es un gap nuevo, ya documentado como tal.
- **A04 Insecure Design**: transiciones de estado atómicas (`WHERE status = 'PENDING'`), bloqueo optimista (`REMINDER.version`) — diseño real, no solo declarado.
- **A05 Security Misconfiguration**: `bruteForceProtected: false` es una instancia real de esta categoría — ver hallazgo 1.1.
- **A07 Identification and Authentication Failures**: hallazgos 1.1 (sin lockout) y 1.2 (sin registro funcional) caen aquí directamente.
- **A08 Software and Data Integrity Failures**: sin `createNativeQuery`, contratos validados contra `openapi.yaml` real (`TEST-API-001`).
- **A09 Security Logging and Monitoring Failures**: auditoría de eventos real (tabla arriba); pero **alerting sigue explícitamente `TBD`** (ya declarado así en el propio `24-security-checklist.md`, confirmado, no es un hallazgo nuevo) y Crashlytics/GlitchTip son solo de desarrollo local (`AND-006`/`WEB-006`, ya documentado).
- **A10 SSRF**: sin superficie aplicable identificada en V1 (no hay funcionalidad que resuelva URLs proporcionadas por el usuario).

---

## 4. `24-security-checklist.md` — estado real ítem por ítem

| Sección | Ítem | Estado real |
|---|---|---|
| Identity | proveedor de identidad seleccionado | ✅ Keycloak, ADR-008 |
| Identity | MFA/passkey strategy definida | ⚠️ Disponible como capacidad de Keycloak, no activada/requerida — no es una estrategia definida, es el default sin tocar |
| Identity | recuperación de cuenta definida | ❌ `resetPasswordAllowed: false` — ver hallazgo 1.1 |
| Identity | expiración/revocación definida | ⚠️ Expiración sí (accessTokenLifespan); revocación real (logout) sí; pero sin refresh automático en Android — ver hallazgo 1.3 |
| API | HTTPS | 🕓 TBD para el entorno real, correcto como TBD (sin servidor propio todavía) |
| API | validation | ✅ Bean Validation + `maxLength` en DTOs |
| API | authorization por recurso | ✅ ver tabla §2 |
| API | rate limiting | ✅ solo sobre invitaciones (`DEVOPS-001`) — no hay rate limiting general de API |
| API | payload limits | ⚠️ implícito vía defaults de Spring Boot/Tomcat, sin política explícita configurada — aceptable para V1, no es una vulnerabilidad real hoy |
| API | error handling | ✅ ver tabla §2 |
| API | OpenAPI | ✅ `TEST-API-001` |
| Data | minimización | ✅ estructural, sin campos superfluos en el modelo |
| Data | cifrado at rest | 🕓 TBD, correctamente diferido a `INFRA-004` (sin servidor propio todavía, nada que contradiga) |
| Data | backups | 🕓 TBD, mismo motivo |
| Data | retention definida | ✅ documentado (SEC-003, `11-auth-security.md`) |
| Data | eliminación definida | ✅ `AccountDeletionService`/`AccountDeletionIntegrationTest` |
| Mobile | secure token storage | ✅ ver tabla §2 |
| Mobile | no secrets | ✅ ver §5 (gitleaks real) |
| Mobile | no sensitive logs | ✅ comprobación superficial, ver limitaciones |
| Mobile | least privilege | ✅ permisos del manifest revisados uno por uno, todos justificados |
| Mobile | network security config | ✅ ver tabla §2 |
| CI/CD | SAST | ⚠️ existe (`DEVOPS-002`, SpotBugs) pero solo manual, sin CI real |
| CI/CD | SCA | ⚠️ `dependabot.yml` configurado pero nunca corrió (sin repo remoto) |
| CI/CD | secret scanning | ✅ corrido de verdad en esta tarea (`gitleaks detect --source . --log-opts="--all"`) — ver §5 |
| CI/CD | dependency update process | ⚠️ configurado, nunca ejecutado en la práctica |
| CI/CD | branch protection | ❌ requiere repo remoto, no existe todavía (ya declarado así) |
| Observability | request correlation | ✅ ver tabla §2 |
| Observability | metrics | ❌ solo `/actuator/health` expuesto, deliberadamente (superficie mínima, `INFRA-006`) — no hay métricas |
| Observability | errors | ✅ `AND-006`/`WEB-006`, solo desarrollo local |
| Observability | audit events | ✅ ver tabla §2 |
| Observability | alerting | 🕓 explícitamente `TBD` (ya lo dice el propio checklist) |

---

## 5. `gitleaks` real contra todo el historial

`gitleaks detect --source . --log-opts="--all"` (17 commits escaneados) — 3 hallazgos:

1. **Real, corregido en esta misma tarea**: `web/e2e/error-tracking.spec.ts` tenía un token real de API de GlitchTip hardcodeado, con un comentario incorrecto que lo describía como "no un secreto real" por analogía con las API keys de Firebase. Esa analogía no aplica — un token de API de GlitchTip/Sentry es una credencial real (a diferencia de una API key pública de Firebase). Movido a `web/.env` (gitignorado) vía `dotenv`, con `.env.example` documentando el nombre sin valor. **El commit histórico donde se introdujo (`42904b3`) sigue conteniendo el valor viejo** — no se reescribió el historial sin pedir permiso primero, tal como se indicó explícitamente. Si este repositorio se comparte o se hace público alguna vez, ese token debería rotarse (es local-dev-only hoy, sin exposición a internet).
2. y 3. **Falsos positivos, no son secretos reales**: la API key pública de Firebase Web (`web/public/firebase-messaging-sw.js`, `android/app/google-services.json`). Confirmado contra la documentación oficial de Google (no asumido): *"API keys restricted to Firebase services do not need to be treated as secrets, and it's safe to include them in your code or configuration files"* ([Learn about and manage API keys for Firebase](https://firebase.google.com/docs/projects/api-keys)) — el control de acceso real es Firebase Security Rules/App Check, no la confidencialidad de esta key. Este proyecto además ni siquiera usa Firebase para nada más que Crashlytics/FCM (sin Firestore/Realtime Database), así que no aplica ni la excepción de "otras APIs de Google Cloud" que la misma documentación menciona.

## 6. Limitaciones de esta auditoría (declaradas explícitamente, no ocultas)

- La búsqueda de logging sensible fue un `grep` dirigido (patrones `password`/`token` cerca de llamadas de log), no una revisión línea por línea de cada punto de logging del proyecto — no es una garantía exhaustiva.
- No se hizo un pentest dinámico (fuzzing, ataques reales contra el backend en ejecución) — todo lo anterior es revisión de código + configuración real, más los hallazgos que ya se dieron durante la verificación funcional de `AND-007`/`WEB-007` en la misma tarea.
- La cobertura OWASP de la sección 3 es una evaluación de alto nivel por categoría, no un checklist ASVS línea por línea — eso queda como trabajo mayor para una auditoría dedicada futura si el Product Owner lo prioriza.
