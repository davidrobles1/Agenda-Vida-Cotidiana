# 22 — Architecture Decision Records

## ADR-001 Monolito modular
**Estado:** Accepted

**Contexto:** proyecto nuevo, equipo inicial pequeño, MVP.

**Decisión:** monolito modular con límites de dominio.

**Consecuencia:** menor complejidad operativa y posibilidad de extracción futura.

## ADR-002 Android nativo
**Estado:** Accepted

**Decisión:** Kotlin + Jetpack Compose.

**Razón:** plataforma inicial Android y ecosistema moderno estable.

## ADR-003 IA fuera de V1–V4
**Estado:** Accepted

**Razón:** reducir superficie de datos, coste y complejidad; primero validar producto.

## ADR-004 Finanzas fuera de V1–V4
**Estado:** Accepted

**Razón:** datos financieros elevan riesgo de seguridad, privacidad, cumplimiento y confianza; se tratarán como fase independiente.

## ADR-005 Plataformas V1: Android + iOS + Web
**Estado:** Accepted (2026-08-09)

**Contexto:** el draft inicial de `01-scope.md` excluía iOS y Web de V1 (solo Android), lo cual contradecía el requisito de CLAUDE.md de construir y documentar las tres plataformas de forma coherente. Se identificó como `DOCUMENTATION_CONFLICT` durante la auditoría inicial y se elevó al Product Owner.

**Decisión:** V1 cubre Android, iOS y Web desde el inicio, con el mismo backend/API/modelo de datos. No se asume código compartido entre plataformas.

**Alternativas consideradas:** (a) solo Android en V1, ampliar en V2/V3; (b) Android + Web en V1, iOS después; (c) las tres desde V1 (elegida).

**Consecuencias:** aumenta el esfuerzo de V1 respecto al draft original. Queda pendiente (no bloqueante) el análisis y recomendación del stack tecnológico definitivo de iOS y Web.

## ADR-006 Modelo de compartir recordatorios (owner + colaboradores)
**Estado:** Accepted (2026-08-09)

**Contexto:** el draft inicial asumía recordatorios estrictamente personales (`25-open-questions.md` Q9, sin decidir). El Product Owner definió que V1 debe soportar colaboración simple.

**Decisión:** un recordatorio tiene un propietario (`OWNER`, control total) y cero o más colaboradores (`COLLABORATOR`, 1:N) añadidos mediante invitación por email o username. La invitación requiere aceptación explícita (no hay acceso implícito), expira a los 7 días si no se responde, y el propietario puede revocar el acceso de un colaborador en cualquier momento con efecto inmediato. El colaborador solo puede ver y completar/deshacer completado.

**Alternativas consideradas:** (a) sin compartir en V1 (más simple, descartada por decisión de producto); (b) compartir 1:1 únicamente (descartada, se prefiere 1:N sin roles adicionales); (c) grupos/hogares con roles desde V1 (descartada por sobrearquitectura, se deja para V2/V3).

**Consecuencias:** añade los módulos/entidades `INVITATION` y `REMINDER_SHARE`, nuevos endpoints, nuevas reglas de autorización (SEC-001, SEC-002) y nuevas amenazas al threat model (enumeración de usuarios, escalación de privilegios sobre recurso compartido). El modelo de autorización se diseña extensible a roles/hogares futuros sin comprometerse a implementarlos en V1.

### Refinamientos cerrados el 2026-08-09 (`28-v1-decision-pack.md`)

- **DEC-001 (Estado de completado) → Opción A, Accepted.** `REMINDER.status` es un único estado global por recordatorio (`PENDING`/`COMPLETED`), compartido entre propietario y colaboradores. Cualquiera con acceso (owner o collaborator activo) puede marcarlo/desmarcarlo; no existe un estado de completado independiente por persona en V1. Esto corrige la ambigüedad de `FR-009` ("deshacer su propio completado"), que se reescribe para reflejar el estado único (ver `03-prd.md`).
- **DEC-002 (Eliminación de recordatorio compartido) → Opción C, Accepted.** Al eliminar un recordatorio, se elimina en cascada (`INVITATION` y `REMINDER_SHARE` asociadas) y se emite una notificación push a los colaboradores que tuvieran acceso `ACTIVE` en el momento de la eliminación, antes de que el recurso deje de existir. No se bloquea la eliminación ni se exige revocar primero.
- **DEC-003 (Lifecycle de INVITATION) → Opción A, Accepted.** `INVITATION.status` queda como `PENDING, ACCEPTED, REJECTED, EXPIRED, CANCELLED`. Se retira `REVOKED` de este nivel: la revocación de acceso vive únicamente en `REMINDER_SHARE.status` (`ACTIVE`/`REVOKED`), nunca en `INVITATION`.

## ADR-007 Notificaciones push abstraídas detrás de una interfaz propia
**Estado:** Accepted (2026-08-09)

**Contexto:** el draft inicial solo contemplaba notificaciones locales (`25-open-questions.md` Q10). El Product Owner requiere sincronización entre dispositivos/usuarios para eventos de compartición, lo cual exige push desde backend, y quiere evitar acoplarse a un proveedor específico dado que V1 cubre tres plataformas.

**Decisión:** el backend define un puerto `PushNotificationSender` (interfaz propia) con adapters concretos por proveedor. Las notificaciones locales (sin necesidad de red) se mantienen resueltas en el cliente.

**Estado histórico previo (referencia, ya no vigente):** en la versión original de este ADR, el proveedor exacto por plataforma quedaba `TBD` (candidatos entonces: FCM para Android, APNs para iOS, Web Push para navegadores). Esto fue resuelto y reemplazado por la decisión **DEC-010** (ver "Refinamientos cerrados" más abajo): **FCM unificado** para las tres plataformas. Cualquier mención de "APNs" o "Web Push" como proveedores directos/separados en documentación anterior queda superada por DEC-010.

**Alternativas consideradas:** (a) solo notificaciones locales (más simple, insuficiente para eventos de compartición entre usuarios); (b) acoplar directamente a un proveedor concreto (más rápido pero rígido); (c) interfaz propia con adapters (elegida).

**Consecuencias:** el fallo del proveedor de push debe ser best-effort y no debe romper la operación principal (AC-012). Añade una nueva amenaza (abuso/spoofing del canal push) al threat model.

### Refinamientos cerrados el 2026-08-09 (`28-v1-decision-pack.md`)

- **DEC-010 (Proveedor de push) → Opción A, Accepted.** Se adopta **Firebase Cloud Messaging (FCM)** como proveedor unificado detrás de `PushNotificationSender`, entregando a Android de forma nativa y a iOS/Web a través de sus puentes con APNs y Web Push respectivamente. Un solo adapter concreto para las tres plataformas en V1.
- **DEC-005 (Almacenamiento de tokens de dispositivo) → Opción A, Accepted.** Se añade la entidad `DEVICE_PUSH_TOKEN` (`user_id`, `platform`, `token`, `created_at`, `last_seen_at`) al modelo de datos, soportando múltiples dispositivos activos por usuario desde V1 (ver `09-data-model.md` y `FR-012`).

## ADR-008 Proveedor de identidad OIDC self-hosted
**Estado:** Accepted (2026-08-09, `DEC-004` en `28-v1-decision-pack.md`) — proveedor: **Keycloak**.

**Actualización:** el Product Owner aprobó Keycloak como proveedor OIDC/OAuth 2.1 self-hosted para V1. Se mantiene la comparación original como registro histórico de las alternativas evaluadas.

**Consecuencia adicional (DEC-014, Accepted):** la verificación de email se delega al flujo estándar de Keycloak ("Verify Email" required action); la aplicación **no implementa un sistema propio de verificación de email**. Ver `FR-001` y `11-auth-security.md`.

**Estado histórico previo (referencia):** Proposed — Pending (no bloquea el resto del diseño; el contrato es OIDC estándar)

**Contexto:** el Product Owner quiere evitar implementar almacenamiento/verificación de contraseñas propio y solicitó comparar alternativas self-hosted/open-source antes de decidir: Keycloak, Zitadel, Authentik, Ory (Kratos+Hydra).

**Comparación (resumen, ver `11-auth-security.md`):**
- **Keycloak:** el más maduro y extenso; soporte de passkeys nativo desde v25; mayor complejidad operativa (tuning/clustering); licencia Apache 2.0.
- **Zitadel:** arquitectura event-sourced en Go, más ligera de operar; passwordless/WebAuthn de fábrica; pensado para multi-tenant; licencia Apache 2.0 (con algunas features enterprise aparte).
- **Authentik:** buena experiencia de desarrollador, WebAuthn GA, incluye SAML/LDAP/RADIUS; más orientado a SSO/proxy que a apps nativas; licencia MIT.
- **Ory (Kratos+Hydra):** requiere ensamblar dos servicios (identidad + OAuth/OIDC), mayor complejidad operativa; licencia Apache 2.0.
- Los cuatro son OIDC-compliant: la integración con Spring Boot (Spring Security OAuth2/OIDC) y con clientes Android/iOS/Web (vía librerías OIDC estándar) es equivalente en los cuatro casos; la diferencia real está en madurez, ligereza operativa y alcance de features.

**Decisión:** Keycloak. Aprobada por el Product Owner el 2026-08-09.

**Consecuencias:** el backend se integra como resource server de Keycloak (Spring Security OAuth2/OIDC Resource Server); los clientes Android/iOS/Web obtienen tokens directamente de Keycloak (Authorization Code + PKCE) y nunca implementan su propio almacenamiento/verificación de contraseñas. Esto también resuelve el `DOCUMENTATION_CONFLICT` señalado en `27-v1-readiness-review.md` §4.1: la aplicación **no expone un endpoint propio `POST /auth/login`**; el login ocurre contra Keycloak y el backend solo valida el token recibido (ver `10-api-openapi.md` y `openapi.yaml`).

**Nota (UX-005, 2026-08-16):** el login de Keycloak recibió un tema visual personalizado por cliente (`vida-cotidiana-mobile`/`vida-cotidiana-web`, `Documentacion/02-ux-ui/login-theme.md`, `infra/keycloak/themes/`). No es una nueva decisión arquitectónica ni un ADR — es una capa visual (FreeMarker + CSS, mecanismo estándar de Keycloak) sobre este mismo ADR-008; el flujo Authorization Code + PKCE y el nivel de seguridad no cambian.

## ADR-009 Cloud provider: AWS
**Estado:** Accepted (2026-08-09, `DEC-008`)

**Estado histórico previo (superado, ver ADR-014):** esta decisión (AWS como cloud provider) fue revertida por decisión explícita posterior del Product Owner el 2026-08-15. V1 se despliega en un servidor propio alquilado (self-hosted), no en AWS ni en ningún servicio gestionado de AWS. Se conserva el contexto y razonamiento original de este ADR como registro histórico; no describe el estado vigente. Ver ADR-014.

**Contexto:** CLAUDE.md proponía AWS como objetivo por defecto; la documentación del proyecto lo mantenía como `TBD` sin ratificar.

**Decisión:** AWS es el cloud provider de V1.

**Alternativas consideradas:** GCP, Azure, proveedor self-hosted/VPS genérico (ver `28-v1-decision-pack.md` DEC-008).

**Consecuencias:** habilita el uso de RDS (PostgreSQL), S3 (object storage futuro), y IAM/red de AWS para el diseño de `09-devops`/CI-CD. Amazon SES (DEC-009) se elige de forma coherente con esta decisión.

## ADR-010 Stack iOS: Swift + SwiftUI nativo
**Estado:** Accepted (2026-08-09, `DEC-006`)

**Contexto:** ADR-005 confirmó iOS como plataforma V1; el stack quedaba `TBD`. CLAUDE.md pide priorizar estabilidad, soporte de Apple, seguridad, performance, integración nativa y longevidad, evitando cross-platform solo por ahorro de código.

**Decisión:** SwiftUI + Swift nativo, con Clean Architecture + MVVM (análogo al patrón usado en Android).

**Alternativas consideradas:** UIKit + Swift; cross-platform (Kotlin Multiplatform/Flutter/React Native) — descartadas por CLAUDE.md y por el criterio de longevidad/integración nativa.

**Consecuencias:** requiere un documento de arquitectura iOS propio (`08b-ios-architecture.md`); no se comparte código con Android/Web.

## ADR-011 Stack Web: React + TypeScript (SPA)
**Estado:** Accepted (2026-08-09, `DEC-007`)

**Contexto:** ADR-005 confirmó Web como plataforma V1; el stack quedaba `TBD`. V1 requiere sesión autenticada, sin necesidad confirmada de SEO público.

**Decisión:** React + TypeScript como SPA (sin SSR/Next.js en V1).

**Alternativas consideradas:** Next.js (SSR/SSG) — queda como camino de evolución si en V2/V3 se necesitan páginas públicas indexables; otros frameworks (Vue, Svelte, Angular) — sin justificación para apartarse de React/TypeScript.

**Consecuencias:** al ser una SPA pública, el manejo de tokens OIDC en el navegador requiere una estrategia de almacenamiento segura (ver **nota de seguridad pendiente** en `11-auth-security.md` — no forma parte de esta decisión, sigue abierta como consideración técnica de implementación, no bloquea V1).

## ADR-012 Política de retención y eliminación de cuenta
**Estado:** Accepted (2026-08-09, `DEC-015`)

**Contexto:** `25-open-questions.md` dejaba la política de retención/eliminación de cuenta como `TBD`; el modelo de compartir (ADR-006) añadió el caso de emails de personas invitadas sin cuenta.

**Decisión:** soft delete de cuenta con periodo de gracia de 30 días antes del borrado definitivo (opción B); los emails de invitados sin cuenta se purgan tras expiración, rechazo o cancelación de la invitación, siguiendo una política de retención corta (opción A').

**Alternativas consideradas:** hard delete inmediato; retención mínima solo para auditoría anonimizada; conservación indefinida de emails de invitados (ver `28-v1-decision-pack.md` DEC-015).

**Consecuencias:** requiere un job de purga diferida para cuentas marcadas para eliminación y otro para invitaciones resueltas/expiradas; añade estado de "pendiente de eliminación" a `USER` (ver `09-data-model.md`).

## ADR-013 Build tool: Maven (sustituye el bootstrap inicial en Gradle)
**Estado:** Accepted (2026-08-15)

**Contexto:** `17-dependencies.md` dejaba "Gradle preferido para build: TBD"; el bootstrap de Milestone 1 fijó Gradle 8.9 (Kotlin DSL) como resolución provisional de ese TBD (`docs/development/00-development-baseline.md`). Antes de iniciar Milestone 2, el Product Owner decidió explícitamente cambiar a Maven para todo el backend en adelante. Es una decisión de tooling, no de arquitectura — no afecta `openapi.yaml`, el modelo de datos, ni ninguna decisión aprobada (DEC-001 a DEC-015, otros ADRs).

**Decisión:** Maven (`./mvnw`, wrapper Maven 3.9.9) reemplaza a Gradle como build tool del backend.

**Alternativas consideradas:** mantener Gradle (statu quo, sin justificación en contra salvo preferencia explícita del Product Owner).

**Consecuencias:** `backend/build.gradle.kts`, `settings.gradle.kts`, `gradle/` y `gradlew`/`gradlew.bat` se eliminan, reemplazados por `pom.xml` y `mvnw`/`mvnw.cmd`/`.mvn/`. Revalidado con build/test real (`./mvnw clean test` → 19/19 tests, `./mvnw clean package` → jar ejecutable); ver `docs/development/02-validation-report.md` §9 y el addendum de `03-milestone-1-gate.md`. `18-dev-environment.md` actualizado (`./mvnw` en vez de `./gradlew` para el backend; Android sigue usando Gradle, sin relación con este cambio).

## ADR-014 Cloud/infra provider: servidor propio alquilado (self-hosted)
**Estado:** Accepted (2026-08-15)

**Contexto:** ADR-009/DEC-008 habían fijado AWS como cloud provider de V1. El Product Owner decidió explícitamente, antes de continuar con Milestone 2, no usar AWS ni ningún servicio gestionado de AWS (RDS, S3, SES, SNS). La infraestructura será un servidor propio alquilado (self-hosted), donde se alojan el backend y PostgreSQL, gestionado directamente por el equipo. Esto no es una decisión de arquitectura del backend en sí — el monolito modular (ADR-001), PostgreSQL, Flyway, Keycloak (ADR-008) y el patrón resource-server-only siguen exactamente igual; es una corrección de dónde se despliega, no de cómo está construido.

**Decisión:**
(a) se descarta AWS y cualquier servicio gestionado de AWS (RDS, S3, SES, SNS) para V1;
(b) el backend y PostgreSQL se despliegan en un servidor alquilado por el equipo, gestionado directamente (no managed);
(c) detalles concretos del proveedor de hosting, especificaciones de servidor y región quedan `TBD` — el Product Owner solo fijó "no AWS, servidor propio", no el proveedor específico.

**Alternativas consideradas:** mantener AWS (ADR-009, statu quo, descartado explícitamente por el Product Owner); GCP/Azure (no evaluadas, fuera de lo que el Product Owner pidió).

**Consecuencias:** DEC-009 (proveedor de correo), que dependía explícitamente de DEC-008 ("si se elige AWS, Amazon SES; si no, un especialista tipo Postmark" — `28-v1-decision-pack.md`), queda reabierta como `TBD` — ninguna de las opciones originales fue elegida todavía, y no se sustituye AWS por un proveedor de correo específico sin instrucción del Product Owner. El módulo `sharing` expone un puerto `EmailSender` (mismo patrón que `PushNotificationSender`, ADR-007) con un adapter no-op/log-only mientras DEC-009 siga abierta.
