# AI-CONTEXT.md — Constitución del proyecto para agentes de IA

IMPORTANT:
Before modifying code, read this file and the relevant documentation under `Documentacion/`.
Never invent requirements.
Never bypass security controls.
Never introduce a dependency without justification.
Never change an architectural decision silently.

Este documento es contexto obligatorio y frecuente. No duplica el PRD completo; para detalle ver los documentos referenciados.

## Proyecto
**Nombre (provisional):** Vida Cotidiana. Nombre definitivo: `TBD` (ver `25-open-questions.md`).

## Objetivo
Plataforma (Android, iOS, Web) para organizar información y actividades de la vida cotidiana. V1 se limita a cuenta de usuario, recordatorios/tareas (con opción de compartirlos) y notificaciones.

## Estado actual
Fase de documentación pre-desarrollo. No existe código de producto todavía. Las 15 decisiones bloqueantes de V1 fueron aprobadas por el Product Owner el 2026-08-09 (`28-v1-decision-pack.md`) y quedan reflejadas en todo el repositorio de documentación. Ver `29-v1-final-readiness.md` para el estado de preparación y los TBDs no bloqueantes restantes.

## Arquitectura
- Backend: **monolito modular** (Clean Architecture + DDD pragmático), módulos: `shared`, `identity`, `user`, `reminder`, `sharing`, `notification`, `audit`. No microservicios en V1 (ADR-001). Hospedado en un **servidor propio alquilado (self-hosted)** (DEC-008/ADR-014; corrige la decisión previa de AWS, ver `22-decision-log.md`).
- Frontend:
  - Android: Kotlin + Jetpack Compose, Clean Architecture + MVVM, `minSdk 30` (Android 11) (ADR-002/DEC-011).
  - iOS: Swift + SwiftUI nativo, Clean Architecture + MVVM, mínimo iOS 17 (ADR-010/DEC-012). Ver `08b-ios-architecture.md`.
  - Web: React + TypeScript, SPA (sin SSR/Next.js en V1) (ADR-011/DEC-007). Ver `08c-web-architecture.md`. Navegadores soportados: últimas 2 versiones mayores de Chrome/Edge/Firefox, Safari desktop/iOS (actual y anterior), Chrome Android (actual) (DEC-013).
  - No se asume código compartido entre plataformas.
- Todas las plataformas consumen el mismo backend/API REST (`/api/v1`) y comparten modelo de datos y reglas de autorización.
- Autorización por recurso: roles `OWNER` (control total) y `COLLABORATOR` (ver + completar/marcar pendiente sobre un **estado único global** de completado, DEC-001) sobre un recordatorio compartido (ADR-006). Deny-by-default, nunca confiar en `userId` del cliente.
- Eliminar un recordatorio compartido: se elimina en cascada (`INVITATION`/`REMINDER_SHARE`) y se notifica por push a los colaboradores `ACTIVE` (DEC-002).
- Lifecycle de `INVITATION`: `PENDING, ACCEPTED, REJECTED, EXPIRED, CANCELLED` — la revocación de acceso vive únicamente en `REMINDER_SHARE` (`ACTIVE`/`REVOKED`), nunca en `INVITATION` (DEC-003).
- Notificaciones: locales (cliente) + push vía **Firebase Cloud Messaging (FCM)** unificado, detrás de `PushNotificationSender` (ADR-007/DEC-010), con `DEVICE_PUSH_TOKEN` multi-dispositivo por usuario (DEC-005).
- Identidad: **Keycloak** (OIDC/OAuth 2.1 self-hosted) con passkeys/MFA (ADR-008/DEC-004). La aplicación **no expone login propio** (`POST /auth/login`); el backend es resource server, los clientes autentican directamente contra Keycloak (Authorization Code + PKCE). Verificación de email delegada a Keycloak, sin sistema propio (DEC-014).
- Retención/eliminación de cuenta: soft delete con 30 días de gracia (`PENDING_DELETION` → purga); emails de invitados sin cuenta se purgan tras expiración/rechazo/cancelación de la invitación (ADR-012/DEC-015).

## Stack tecnológico
- Backend: Java 21 LTS, Spring Boot 3.x, Spring Security, Spring Data JPA/Hibernate, PostgreSQL, Flyway, Testcontainers.
- Android: Kotlin, Jetpack Compose, Hilt, Coroutines/Flow, Retrofit. `minSdk 30`.
- iOS: Swift, SwiftUI, Keychain, AppAuth-iOS, Firebase SDK (push). Mínimo iOS 17.
- Web: React, TypeScript, SPA, librería OIDC para SPA, Firebase SDK (Web Push).
- Object Storage S3-compatible: preparado conceptualmente, no implementado en V1 (sin archivos/adjuntos).
- Cloud: **servidor propio alquilado (self-hosted)**, proveedor de hosting concreto `TBD` (DEC-008/ADR-014). Correo transaccional: `TBD` (DEC-009 reabierta; adapter actual no-op/log-only). Identidad: **Keycloak**. Push: **FCM**.

## Estructura de documentación
Archivos numerados en `Documentacion/` (00 a 26) + `openapi/openapi.yaml`. Ver `README.md` e `INDEX` implícito en la numeración. No usar la estructura de carpetas de CLAUDE.md hasta una fase de reorganización explícita — mientras tanto, los documentos numerados son la fuente de verdad.

## Versiones
- **V1 (MVP):** cuenta, login, Home, CRUD + completar recordatorios, compartir recordatorio (owner + colaboradores 1:N vía invitación), notificaciones locales y push, base de seguridad/privacidad/auditoría/observabilidad. Plataformas: Android, iOS, Web.
- **V2:** estabilización, hardening, UX, sincronización robusta, recuperación de errores, más pruebas, observabilidad.
- **V3:** funcionalidades adicionales (sin especificar), rendimiento, escalabilidad, colaboración avanzada (grupos/hogares) *si se aprueba*.
- **V4:** versión madura, alcance funcional exacto `TBD`.
- **POST-V4:** IA, Finanzas — explícitamente fuera de V1–V4 (ADR-003, ADR-004).

## Funcionalidades fuera de alcance (V1–V4 salvo indicación)
IA, Finanzas/integración bancaria/estados de cuenta, marketplace, afiliados, publicidad, microservicios/arquitectura distribuida compleja, archivos/adjuntos, grupos/hogares/equipos/roles granulares entre colaboradores (más allá de OWNER/COLLABORATOR simple), búsqueda social/libreta de contactos.

## Decisiones arquitectónicas importantes (ver `22-decision-log.md` para detalle completo)
- ADR-001 Monolito modular (Accepted).
- ADR-002 Android nativo, Kotlin + Compose (Accepted).
- ADR-003 IA fuera de V1–V4 (Accepted).
- ADR-004 Finanzas fuera de V1–V4 (Accepted).
- ADR-005 Plataformas V1 = Android + iOS + Web (Accepted).
- ADR-006 Modelo de compartir: owner + colaboradores 1:N vía invitación; estado de completado único global (DEC-001); eliminación en cascada con notificación (DEC-002); lifecycle de INVITATION corregido (DEC-003) (Accepted).
- ADR-007 Push notifications: interfaz propia `PushNotificationSender`, proveedor **FCM** unificado (DEC-010), entidad `DEVICE_PUSH_TOKEN` (DEC-005) (Accepted).
- ADR-008 Proveedor OIDC self-hosted: **Keycloak** (DEC-004); verificación de email delegada (DEC-014) (Accepted).
- ADR-009 Cloud provider: **AWS** (DEC-008) (Accepted 2026-08-09, **superado por ADR-014 el 2026-08-15** — ver abajo).
- ADR-010 Stack iOS: **Swift + SwiftUI** (DEC-006) (Accepted).
- ADR-011 Stack Web: **React + TypeScript SPA** (DEC-007) (Accepted).
- ADR-012 Política de retención/eliminación de cuenta: soft delete 30 días + purga de invitaciones sin cuenta (DEC-015) (Accepted).
- ADR-013 Build tool: **Maven**, sustituye el bootstrap inicial en Gradle (Accepted, 2026-08-15).
- ADR-014 Cloud/infra provider: **servidor propio alquilado (self-hosted)**, sustituye AWS (DEC-008 corregida; DEC-009/correo reabierta como `TBD`) (Accepted, 2026-08-15).

## Reglas de seguridad
- HTTPS siempre; autorización por recurso verificada server-side; deny-by-default; mínimo privilegio.
- No implementar criptografía/autenticación propia; delegar a Keycloak. No implementar verificación de email propia (DEC-014).
- No enumerar usuarios vía invitaciones (SEC-001); rate limiting sobre creación de invitaciones.
- Revocación de acceso compartido es inmediata, sin ventana de gracia; vive solo en `REMINDER_SHARE`, nunca en `INVITATION` (SEC-002/DEC-003).
- Retención: soft delete de cuenta (30 días de gracia) y purga de emails de invitados sin cuenta tras expiración/rechazo/cancelación (SEC-003/DEC-015).
- Cliente Web (SPA): tokens OIDC en memoria + renovación silenciosa, nunca en `localStorage` (consideración técnica pendiente de finalizar, no bloqueante — ver `08c-web-architecture.md`).
- No secretos en código ni en logs; no tokens en logs; SAST/SCA/secret scanning en CI.
- Sin cumplimiento formal declarado (OWASP/NIST/ISO son marcos de referencia, no certificaciones).

## Reglas de código
- Java: constructor injection, records para DTOs, excepciones de dominio controladas, no capturar `Exception` genérica sin propósito.
- Kotlin: null-safety, coroutines, immutable state, sin lógica de negocio en composables.
- `domain` no depende de `infrastructure`; controllers delgados; no exponer entidades JPA directamente.
- No abstraer antes de tener una razón concreta (anti-sobrearquitectura).

## Reglas de documentación
- Etiquetar siempre: `DECISION`, `ASSUMPTION`, `RECOMMENDATION`, `TBD`, `FUTURE`.
- IDs estables y no reutilizables: `FR-xxx`, `NFR-xxx`, `SEC-xxx`, `UC-xxx`, `AC-xxx`, `ADR-xxx`.
- Nunca inventar requerimientos de negocio; decisiones no definidas quedan `TBD`.
- Contradicciones entre documentos se marcan `DOCUMENTATION_CONFLICT` y se elevan, no se resuelven en silencio.

## Reglas de Git
- Trunk-based simplificado, PR hacia `main`, sin merge directo. CI obligatoria antes de merge.
- Conventional Commits (`feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `security`).
- Rama `develop`: `TBD` (preferencia inicial: evitarla).

## Reglas de testing
- Pirámide: unit (dominio/casos de uso) → integration (API + PostgreSQL vía Testcontainers) → API (contract/OpenAPI) → E2E (solo flujos críticos).
- Tests de autorización obligatorios en todo endpoint que reciba un identificador de recurso, incluyendo diferenciación OWNER vs. COLLABORATOR.

## Convenciones de nombres
- Base path API: `/api/v1`. IDs: UUID. Timestamps: ISO-8601 UTC.
- Paquetes backend: `com.vidacotidiana.<módulo>.{api,application,domain,infrastructure}`.
- Migraciones Flyway: `V<n>__descripcion.sql`, nunca editar una ya ejecutada.

## TBDs abiertos (no exhaustivo, ver `25-open-questions.md` y `29-v1-final-readiness.md`)
Nombre del producto, mercado inicial, primer grupo de validación, licencia/repo, límite de colaboradores por recordatorio, formato de username, notificación al editar un recordatorio compartido, auto-vinculación de invitación pendiente si el invitado se registra después, reversión de `PENDING_DELETION`, modo offline/cuenta real vs. local, dispositivos soportados más allá de versión de SO, patrón final de manejo de tokens en la SPA Web.

## Prohibiciones
No implementar IA ni Finanzas. No crear microservicios en V1. No implementar autenticación/criptografía propia ni login propio (`POST /auth/login` no existe; el backend es resource server de Keycloak). No implementar verificación de email propia (delegada a Keycloak). No exponer stack traces, SQL, nombres internos o secretos en respuestas/logs. No sobrearquitecturar V1 (grupos/hogares/roles granulares quedan para V2/V3; completado por colaborador individual queda descartado para V1, DEC-001). No usar `REVOKED` en `INVITATION` (vive solo en `REMINDER_SHARE`). No decidir silenciosamente sobre un `DOCUMENTATION_CONFLICT` o un `TBD` de negocio.
