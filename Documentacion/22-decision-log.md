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

**Nota, 2026-08-17 (Task B — recuperación de cuenta, `33-security-cross-audit.md` §1.1):** DEC-009 **sigue explícitamente abierta**, sin decidir en esta tarea — se deja registrado el siguiente contexto para que no se pierda cuando el Product Owner la retome: usar el correo personal de Hotmail vía autenticación SMTP básica probablemente ya no es viable, dado el retiro de Microsoft de la autenticación básica en 2026. Como alternativa, se identificó Resend (con su nivel gratuito) como una opción viable a evaluar. Ninguna de las dos afirmaciones fue verificada de nuevo dentro de esta tarea — quedan como contexto de investigación previa, no como una decisión tomada.

## ADR-015 Contextos de uso Personal/Laboral (navegación dual + calendario agregado)
**Estado:** Accepted (2026-08-18)

**Contexto:** el Product Owner decidió ampliar el posicionamiento del producto para incluir profesionales con negocios pequeños (consultorios, bufetes, oficinas chicas) además de familias — sin agregar facturación ni módulos de negocio nuevos (rebrand a "Agenda Meraki", documentado por separado, aún no ejecutado). Para reflejar esto en la navegación sin duplicar la aplicación, el Product Owner definió que cada recordatorio pertenece a un **contexto**: Personal o Laboral, con una vista agregada (Calendario) que muestra ambos.

**Decisión:**
(a) en el registro se pregunta el propósito de uso mediante **selección múltiple** (checkboxes independientes) "Personal" y "Laboral", ambas opcionales — el usuario puede marcar una, las dos, o ninguna;
(b) el selector superior de navegación muestra siempre "Calendario" más **únicamente** los modos habilitados; un modo no marcado en el registro no aparece en el selector, pero puede activarse después desde Ajustes — al activarse, aparece en el selector (ver FR-016/UC-15);
(c) cada modo habilitado (Personal, Laboral) tiene su **propio navbar** (Inicio, Calendario del modo, Tareas, Compartidos — lista exacta de items TBD, ver `02-ux-ui/`);
(d) al abrir la aplicación, la vista por defecto es **Calendario** (general), no Inicio — esto reemplaza el destino post-login usado en el "Flujo principal" de `05-user-flows.md`, que queda superado en ese punto específico (ver nota agregada ahí, no se reescribe el diagrama original);
(e) el Calendario general agrega los recordatorios de todos los modos habilitados, **coloreados según su modo de origen** (no se inventa un tercer tema de color neutral para Calendario); admite vistas diario, semanal y mensual, seleccionables por el usuario.

**Alternativas consideradas:** (a) un solo espacio sin contexto, usando solo etiquetas/tags — descartada, el Product Owner pidió explícitamente navegación y tema visual distintos por modo, no solo una etiqueta; (b) forzar a elegir un único modo en el registro, sin permitir ambas ni ninguna — descartada, el Product Owner pidió selección múltiple opcional.

**Consecuencias:** requiere un campo de contexto en `REMINDER` y campos de modos habilitados en `USER` (ver `09-data-model.md`); requiere una nueva pantalla de onboarding (selección de propósito) y una nueva sección en Ajustes (activar modo adicional); requiere documentar la arquitectura de información (dos navbars + un selector superior condicional) en `02-ux-ui/`; no agrega facturación ni módulos de negocio nuevos — mismo límite de alcance que el resto de "Agenda Meraki".

**Implementación real (2026-08-18, Web — `BE-038`/`UX-012`):** (a)-(e) implementados y verificados de punta a punta contra el backend real: registro real → onboarding con validación visible → activar Laboral real desde Ajustes → crear un recordatorio real desde cada navbar → ambos visibles, coloreados por origen, en el Calendario general (`e2e/mode-navigation.spec.ts`, suite completa 19/19 en verde). Android/iOS sin cambios — desarrollo pausado hasta beta de Web (decisión ya registrada, `05-v2-plan.md`).

**TBD explícitos (no bloqueantes, pendientes de definición por el Product Owner):**
- ¿Un usuario puede **desactivar** un modo ya habilitado, o Ajustes solo permite activar (nunca revertir)?
- ¿Un recordatorio puede **cambiar de contexto** después de creado, o el contexto se fija permanentemente al crearlo?
- Lista exacta de items del navbar por modo, más allá de Inicio/Calendario/Tareas/Compartidos.

### Refinamientos cerrados el 2026-08-18

- **Versión → V3, Accepted.** Esta funcionalidad se documenta ahora como decisión aprobada, pero su implementación queda planificada para **V3** (después de estabilizar V2) — no forma parte del cierre de V1 ni se prioriza sobre los pendientes de V2 en curso (CI real, DEC-009, catch-up de iOS). Ver `02-roadmap.md`.
- **Registro con cero modos marcados → Opción B, Accepted.** El formulario de registro **exige marcar al menos una** casilla (Personal y/o Laboral) para completar el registro; no se permite continuar con ambas vacías.
- **Asignación de contexto con ambos modos habilitados → Opción A, Accepted.** El contexto de un recordatorio nuevo se **infiere del navbar de origen** (creado desde Personal → `PERSONAL`; creado desde Laboral → `LABORAL`). No se agrega un selector de contexto explícito en el formulario de creación.

### Refinamiento cerrado el 2026-08-18 (cambio de prioridad, no de diseño)

- **Versión → V3 diferida, SUPERADA.** El Product Owner decidió adelantar la implementación: ADR-015 avanza **en paralelo** con el resto del trabajo de V2 (vistas Semana/Día del Calendario, backend de Garantías/Mantenimiento), no después de cerrar V2. La documentación funcional (FR-014 a FR-019, casos de uso, modelo de datos) ya estaba completa antes de este cambio — no se requirió redefinir nada de diseño, solo el orden de ejecución. Ver `docs/development/05-v2-plan.md`.

### Refinamiento cerrado el 2026-08-18 (migración de backfill, `BE-038`)

- **Backfill de `REMINDER.context`/`USER.*_enabled` sobre datos ya existentes → Opción "PERSONAL + grandfather al dueño", Accepted.** Un TBD real y explícito, planteado antes de implementar la migración (no asumido): ¿qué `context` reciben los recordatorios ya existentes cuyo dueño no tenía ningún modo habilitado antes de esta migración (ambos flags nacen en `false`)? El Product Owner eligió: todo `REMINDER` preexistente pasa a `context = 'PERSONAL'`, y en la misma migración (`V6__adr015_context_modes.sql`) se fuerza `personal_enabled = true` para todo `USER` preexistente — antes de este ADR la app era de un solo contexto, implícitamente personal; se reconoce eso explícitamente en el dato en vez de dejar una inconsistencia entre los recordatorios de un usuario y su propio selector de navegación. Verificado real contra la base de datos de desarrollo (no solo Testcontainers): 4/4 usuarios y 158/158 recordatorios preexistentes migrados correctamente, cero nulos. Ver `09-data-model.md` y `01-technical-backlog.md` (`BE-038`).

## ADR-016 Módulo Laboral: entidades Persona/Proyecto/Compromiso (extiende ADR-015)
**Estado:** Accepted (2026-08-22)

**Contexto:** ADR-015 definió "Laboral" como un **contexto de navegación** — una etiqueta (`REMINDER.context`) y un tema visual, sin agregar "facturación ni módulos de negocio nuevos" (cita literal de ADR-015). El Product Owner solicitó y aprobó (2026-08-22) evolucionar ese modo hacia un espacio profesional universal — capaz de relacionar personas, proyectos, tareas, reuniones y compromisos, útil para cualquier profesión (consultoría, arquitectura, derecho, ventas, administración, salud, educación, servicios profesionales) sin plantillas por profesión — explícitamente **sin** convertirse en un ERP, CRM completo, Jira o Notion. Análisis completo, matriz por profesión y clasificación MVP/V4/Post-V4 en `34-laboral-module-proposal.md`.

Esta decisión **actualiza explícitamente** el límite de ADR-015 citado arriba — no lo contradice en silencio; se documenta aquí el cambio para que quede trazable (regla `DOCUMENTATION_CONFLICT` de `AI-CONTEXT.md`).

**Decisión:**
(a) se introducen tres entidades nuevas, exclusivas del contexto Laboral: `PERSON` (contacto profesional: cliente, colega, proveedor), `PROJECT` (agrupador de trabajo: proyecto, obra, caso u oportunidad, según el vocabulario de cada profesión) y `COMMITMENT` (compromiso direccional: `MINE` cuando el usuario debe actuar, `THEIRS` cuando el usuario espera una acción de otra persona). "Seguimientos" y "Esperando" **no son dos entidades**: son la misma `COMMITMENT`, filtrada por `direction` (ver Alternativas);
(b) `REMINDER` se extiende con `person_id` y `project_id` (FK nullable) y `location` (texto nullable). **No se crea una entidad EVENTO/REUNIÓN separada**: una reunión es un `REMINDER` con `context = 'LABORAL'`, `location` definido y participantes mediante el mecanismo ya existente `REMINDER_SHARE` (ADR-006), reutilizado tal cual, sin cambios;
(c) **no** se crea una entidad `ORGANIZATION` en esta fase — `PERSON.organization` es texto libre (RECOMMENDATION de minimización de datos, NFR-002/NFR-011; promovible a entidad propia en una versión posterior si aparece una razón concreta, p. ej. varias Personas de la misma organización);
(d) el vocabulario mostrado al usuario ("Proyecto" / "Obra" / "Caso" / "Oportunidad", "Persona" / "Cliente" / "Prospecto") es una capa de **presentación del cliente**, no un cambio de esquema — el modelo de datos es único e idéntico para cualquier perfil profesional (ver `02-ux-ui/design-system.md` UX-014);
(e) el Inbox de captura rápida no introduce una entidad nueva: reutiliza `NOTE` sin vínculo a `PERSON`/`PROJECT`/`REMINDER`; "clasificar" un ítem del Inbox significa asignarle esos vínculos (o convertirlo en `REMINDER`/`COMMITMENT`) después, nunca moverlo a otra tabla.

**Alternativas consideradas:**
(a) modelar "Seguimiento" y "Esperando" como dos entidades independientes — descartada: un compromiso cambia de dirección con frecuencia (p. ej. tras que la otra persona responde, el turno de acción se invierte) y dos tablas obligarían a migrar filas entre ellas cada vez que eso ocurre; una sola entidad con un campo `direction` refleja que es el mismo hecho de negocio visto desde ambos lados, sin esa migración;
(b) crear una entidad `EVENT`/`MEETING` separada de `REMINDER` — descartada: `REMINDER` ya soporta fecha/hora, `context` y colaboradores (`REMINDER_SHARE`); duplicar esa capacidad en una tabla nueva violaría la regla de no sobrearquitecturar sin una razón concreta (`AI-CONTEXT.md`, "Reglas de código");
(c) construir un módulo CRM completo (pipeline con etapas, oportunidades, forecast, scoring) desde esta fase — descartada explícitamente por el Product Owner ("no queremos un ERP/Salesforce/Jira/Notion"); el pipeline queda `FUTURE`/Post-V4, ver `34-laboral-module-proposal.md` §10/§13.

**Consecuencias:** requiere las tablas nuevas `PERSON`/`PROJECT`/`COMMITMENT` y las columnas nuevas de `REMINDER` (ver `09-data-model.md`, migración Flyway `V7`, aún no ejecutada — `TBD` técnico de cuándo se implementa, ver `docs/development/08-laboral-module-plan.md`); nuevos endpoints REST (`/people`, `/projects`, `/commitments`, `TBD` en `openapi.yaml`); nuevos casos de uso (`UC-17` a `UC-21`, `05-user-flows.md`) y requerimientos (`FR-021` a `FR-028`, `NFR-011`, `SEC-004`). No cambia el modelo de autorización: mismo patrón dueño-únicamente que `REMINDER`/`WARRANTY`/`MAINTENANCE_RECORD` (`owner_user_id`, sin colaboradores sobre `PERSON`/`PROJECT`/`COMMITMENT` en V1 de este módulo — compartir una reunión sigue viviendo en `REMINDER_SHARE`, ya existente). No agrega facturación, pagos ni integración bancaria — sigue expresamente fuera de alcance (ADR-004).

**TBD explícitos (no bloqueantes, pendientes de definición por el Product Owner):**
- ¿`PROJECT` admite más de una `PERSON` como cliente (N:N) o una sola (1:N, elegido aquí por simplicidad — ver Alternativas)?
- ¿`COMMITMENT` puede existir sin `PERSON` asociada (p. ej. "algo que debo hacer" sin una contraparte clara) o siempre requiere una?
- Vocabulario exacto por perfil profesional más allá de los 4 ejemplos ya documentados (Consultor tecnológico, Arquitecto, Abogado, Vendedor) — ver `34-laboral-module-proposal.md` §9.
- Mecanismo y umbral exactos de "última interacción derivada" (candidato V4): ¿cuenta abrir el registro de `PERSON`, o solo un `REMINDER`/`COMMITMENT` vinculado con esa persona?
- ¿Ajustes permite desactivar el módulo Laboral por completo (heredado de ADR-015, sigue sin decidir) y qué pasa con `PERSON`/`PROJECT`/`COMMITMENT` si eso ocurre?

**Versión:** V3 para el núcleo (a)-(e), continuando el trabajo de ADR-015 adelantado en paralelo con V2. Clasificación completa (qué es V3, qué queda V4, qué queda Post-V4) en `34-laboral-module-proposal.md` §14 y `02-roadmap.md`.

**Implementación real (2026-08-22, Backend — `BE-039`):** (a)-(e) implementados y verificados: `PERSON`/`PROJECT`/`COMMITMENT` (migración `V11__adr016_laboral_module.sql`), `REMINDER` extendido con `personId`/`projectId`/`location`, endpoints `/people*`/`/projects*`/`/commitments*` con el mismo patrón dueño-únicamente que `warranty`/`maintenance`. `./mvnw clean test`: **217/217 en verde**.

**Implementación real (2026-08-22, Fase 3a — `BE-040`/`WEB-011`, incremento aislado, sin reabrir Fases 1/2):** `NOTE` extendida con `personId`/`projectId` opcionales (candidato V4, FR-029) — migración `V16__adr016_notes_person_project_links.sql`, mismo patrón aditivo (columnas nullable, overloads) que `REMINDER` en la Fase 1. Sección "Notas" + alta embebida en `PersonDetailDialog`/`ProjectDetailDialog`; `RemindersPage.tsx`/`CreateReminderDialog.tsx`/Personal no se tocaron. `./mvnw test -Dtest=NoteControllerIntegrationTest`: 14/14 en verde (12 heredados + 2 nuevos). Verificado con Playwright real desde Persona y desde Proyecto, cero errores de consola.

**Refuerzo del hallazgo anterior (2026-08-28):** este mismo problema volvió a morder, esta vez al revés y con un síntoma distinto. Al verificar 3e1 se encontró el servidor Vite corriendo con los *defaults* (`localhost:8081`), y se reinició el backend a `localhost` para alinearlo — lo cual funcionó para la verificación manual, pero **rompió 18 tests de la suite Playwright**, porque `web/playwright.config.ts` levanta su propio `webServer` con `VITE_OIDC_ISSUER`/`VITE_API_BASE_URL` apuntando a la **IP LAN**: los tokens que emitía Keycloak para la suite no validaban contra un backend configurado para `localhost`, y toda llamada autenticada daba 401. **Regla operativa para no repetirlo: la IP LAN es la configuración canónica de este entorno** (es la que está fijada en `playwright.config.ts` y la que usan Android/iOS); si un servidor Vite se encuentra corriendo con otros valores, hay que reiniciar *ese* servidor, no adaptar el backend. `reuseExistingServer: true` en el config hace que Playwright reutilice un Vite ya levantado, así que uno arrancado a mano con defaults contamina la suite entera.

**Hallazgo operativo, no de código (mismo incremento):** el entorno de desarrollo compartido usa la IP LAN de la máquina (`192.168.0.18`), no `localhost`, como issuer real de Keycloak — reiniciar el backend con `OIDC_ISSUER=http://localhost:8081/...` produce `401` en *todo* endpoint autenticado ("the iss claim is not valid"), aunque `/actuator/health` y los endpoints anónimos respondan bien. Documentado aquí para que no se repita: cualquier reinicio de este backend compartido debe usar `OIDC_ISSUER=http://192.168.0.18:8081/realms/vida-cotidiana` (y `DB_URL` apuntando a `vc-dev-postgres`, puerto `5433`, no el `docker-compose.yml` de este repo, que corre en `5434` y no es el que este entorno usa).

**Implementación real (2026-08-22, Fase 3b — `BE-041`/`WEB-013`, incremento aislado):** `DOCUMENT` (módulo `document`, de otra sesión, ya estable — sin ediciones en las ~7 horas previas a este incremento) extendido con `personId`/`projectId` opcionales (candidato V4, FR-030) — migración `V18__adr016_documents_person_project_links.sql`, mismo patrón aditivo (overloads en `upload()`/`edit()`/constructor/`applyEdit`). No se tocó ninguna otra parte de `document` (categorías, compartir, visibilidad, `DocumentsPage.tsx`). `./mvnw test -Dtest=DocumentControllerIntegrationTest`: 11/11 en verde (9 heredados + 2 nuevos). Web: sección "Documentos" de **solo lectura** en `PersonDetailDialog`/`ProjectDetailDialog` — no se construyó una pantalla de subida vinculada en este incremento (ver FR-030, TBD explícito). Verificado con Playwright real: documento subido vía API con `personId` real aparece de inmediato en el detalle de Carlos Martínez tras refrescar, cero errores de consola. `openapi.yaml` no se tocó para `document` — ese módulo nunca se documentó ahí (gap preexistente de la otra sesión, no corregido aquí por exceder el alcance de este incremento).

**Implementación real (2026-08-22, Web — `WEB-010`, mismo día):** navbar de Laboral (`AppShell.tsx`) reemplazado por las 7 secciones núcleo (Hoy/Agenda/Tareas/Personas/Proyectos/Seguimientos/Inbox), confirmado explícitamente por el usuario que esto es *solo* un cambio de menú — Vision Board/Compartidos conservan sus módulos/rutas/componentes/datos intactos (siguen usándose sin cambios en Personal), solo pierden su enlace en el navbar de Laboral. Verificado con Playwright contra el backend/Keycloak/Postgres reales: creación real de Persona→Proyecto→Tarea→Compromiso, pestañas Mías/Esperando, Inbox, cero errores de consola; navbar de Personal reverificado sin cambios. Detalle completo en `docs/development/08-laboral-module-plan.md` y `01-technical-backlog.md` (`WEB-010`).

**TBD resuelto pragmáticamente, no por el Product Owner (`BE-039`):** "¿`COMMITMENT` puede existir sin `PERSON` asociada?" se implementó como **`person_id NOT NULL`** — una **ASSUMPTION técnica**, no una decisión de negocio confirmada (ver javadoc de `Commitment`, `09-data-model.md`). Se eligió NOT NULL por ser el caso que cubren todos los ejemplos de `34-laboral-module-proposal.md` y por ser el cambio más simple de relajar después (pasar a nullable no rompe ningún consumidor existente). El resto de los TBD explícitos de arriba **siguen abiertos**, no se resolvieron en esta implementación.

**Decisión de alcance (2026-08-22, Fase 3e — Objetivos/Rutinas/Lugares/Recursos, sin implementar todavía):** el Product Owner aprobó la definición funcional mínima de cuatro entidades nuevas de contexto, previamente `BLOCKED` por falta de alcance (`08-laboral-module-plan.md`). Se dividen en cuatro incrementos aislados (3e1-3e4), cada uno con su propio FR/UC/AC (`FR-031`-`FR-034`, `UC-24`-`UC-27`, `AC-018`-`AC-021`) y modelo (`09-data-model.md`, `OBJECTIVE`/`ROUTINE`/`PLACE`/`RESOURCE`):
- **3e1 — `OBJECTIVE`:** meta laboral independiente, progreso manual (`currentValue`/`targetValue`), sin vínculo a `PROJECT`/`PERSON`.
- **3e2 — `ROUTINE`:** actividad recurrente marcada manualmente; **decisión explícita: no genera `REMINDER`/`COMMITMENT`**, para no solaparse con 3d ("Automatizaciones simples"), que sigue `BLOCKED` de forma independiente.
- **3e3 — `PLACE`:** catálogo de ubicaciones reutilizables, integrado con `CreateTaskDialog` solo como autocompletado de texto sobre `REMINDER.location` ya existente — **no** se agrega `REMINDER.place_id`.
- **3e4 — `RESOURCE`:** material/referencia reutilizable (tipo + referencia de texto), vinculable opcionalmente a `PERSON`/`PROJECT` — **no** sustituye a `DOCUMENT` (Fase 3b): sin archivo real, sin versionado.

**Implementación real (2026-08-28, Fases 3e1 y 3e3 — `BE-042`/`WEB-015`, `BE-044`/`WEB-017`, incrementos aislados):** `OBJECTIVE` (migración `V19__adr016_objectives.sql`) y `PLACE` (`V20__adr016_places.sql`) implementados con el mismo patrón exacto que `person` (domain/application/api, dueño-únicamente, 404 nunca 403, bloqueo optimista). Dos decisiones de diseño confirmadas al implementar, ambas coherentes con lo aprobado y ninguna nueva: (a) marcar un Objetivo cumplido es un `PATCH` ordinario, **no** un `POST /{id}/complete` como `Reminder` — AC-018 lo define como un campo que fija el dueño, sin regla de visibilidad entre colaboradores detrás; (b) `reminders.place_id` **no se creó** y `REMINDER` no se tocó en absoluto — elegir un Lugar copia su texto al campo `location` ya existente (FR-033/FR-024). Verificado con Playwright real contra el backend/Keycloak reales, incluido el caso que más importaba de AC-018: subir `currentValue` hasta igualar `targetValue` **no** autocompleta el Objetivo (confirmado server-side `completed=false currentValue=2 targetValue=3`). Cero errores de consola.

**Implementación real (2026-08-28, Fases 3e2 y 3e4 — `BE-043`/`WEB-016`, `BE-045`/`WEB-018`):** `ROUTINE` (`V21`) y `RESOURCE` (`V22`) completadas tras resolverse sus dos TBD. `ROUTINE` no tiene campo `completed` (FR-032: se completa repetidamente; su estado permanente es `active`) y no tiene FK a `reminders`/`commitments` ni job alguno — la prohibición de FR-032 quedó expresada en el propio esquema, no solo en documentación. `RESOURCE` no acepta archivos por ningún endpoint: `DOCUMENT` (FR-030) sigue siendo la única entidad responsable de documentos reales.

**Dos TBD elevados al Product Owner y resueltos (2026-08-28):** ambos se detectaron al implementar, se plantearon como decisión en vez de adivinarse (regla fundamental de `CLAUDE.md`), y se resolvieron el mismo día:
- **3e2, avance de `nextExecutionDate` → opción B:** se calcula desde la **fecha programada original**, nunca desde "ahora" — la cadencia sobrevive a una ejecución tardía. Verificado con un test dedicado y con navegador real: una rutina semanal atrasada 3 días avanzó a *programada+7d*, no a *hoy+7d*.
- **3e4, forma de `reference` → opción A:** un **único campo de texto libre**, no `url` validada + campo aparte, porque varios tipos aprobados (`MANUAL`, `PLANTILLA`, `HERRAMIENTA`) a menudo no tienen URL.

Con esto **las cuatro sub-fases de 3e (Objetivos, Rutinas, Lugares, Recursos) quedan implementadas y verificadas**.

**Implementación real (2026-08-28, Fase 3d — `BE-047`/`WEB-019`): última sub-fase de la Fase 3.** El Product Owner definió la regla que la tuvo `BLOCKED` desde el inicio: (1) el disparador es **manual** — un botón "Sugerir tarea" en la nota, **sin** detección automática por palabras clave, job ni heurística; (2) la sugerencia se convierte en Tarea o se descarta; (3) una vez resuelta, no vuelve a ofrecerse. Implementado como una columna aditiva `NOTE.task_suggestion_resolved` (`V23`) y `POST /notes/{id}/resolve-task-suggestion`. Convertir crea un `REMINDER` normal con el endpoint de siempre — no se introdujo ninguna ruta de creación de Tareas paralela. **Nada automático quedó en el código**, lo que mantiene esta fase dentro de lo permitido por `CLAUDE.md`/ADR-003: la automatización proactiva basada en patrones sigue siendo Post-V4 y prohibida. Verificado con navegador real (convertir vs. descartar, y una sesión nueva que confirma que la sugerencia no vuelve). Suite completa: **298/298 en verde**.

**Con 3d cerrada, la Fase 3 completa está terminada** — y con ella todo el alcance V4 documentado en `02-roadmap.md` §V4. **Fase 4 (Post-V4) permanece sin alcance implementable**, no por falta de detalle sino por decisiones ya tomadas: el pipeline/CRM fue descartado explícitamente por el Product Owner (Alternativa (c) de esta misma ADR); las verticales (Casos/Obras/Clases) no tienen definición funcional; e Insights/Asistente/automatización proactiva están prohibidos por la regla de IA. Análisis completo en `docs/development/08-laboral-module-plan.md` §"Fase 4".

**Hallazgo real, gap preexistente corregido (`BE-046`):** un body JSON no deserializable (enum desconocido, fecha malformada) devolvía `500 INTERNAL_ERROR` en vez de `400` — `HttpMessageNotReadableException` no tenía handler en `GlobalExceptionHandler`. Lo encontró el test de `frequency` inválido de 3e2, pero **no era específico de Routine**: el mismo `500` ocurría en `POST /commitments` con un `direction` inválido, caso que ningún test cubría. Corregido en el punto único de la política de errores (AC-006/NFR-006); 44/44 tests de los módulos afectados en verde tras el cambio.

Las cuatro mantienen el mismo patrón de autorización dueño-únicamente que `PERSON`/`PROJECT`/`COMMITMENT` (sin colaboradores), consistente con esta ADR. Ningún endpoint de IA ni financiero se introduce (regla `CLAUDE.md`/ADR-003/ADR-004). **TBD explícitos que siguen abiertos** (fórmula de avance de `ROUTINE.nextExecutionDate`, si `RESOURCE.reference` es uno o dos campos, punto de entrada exacto en la UI de cada entidad más allá de lo ya decidido): ver `09-data-model.md` §"V4 candidato — Fase 3e" y `08-laboral-module-plan.md` Fase 3e para el desglose completo por incremento y la recomendación de orden de implementación (3e1 primero). **Esta es una decisión de alcance, no una implementación** — ningún código, migración ni endpoint se creó en esta tarea.

## ADR-017 Expiración de sesión por inactividad (2 h)
**Estado:** Accepted (2026-08-28)

**Contexto:** el Product Owner solicitó (2026-08-28) que la sesión del portal expire tras **2 horas sin actividad**. Hasta ahora la política real era otra y además involuntaria: Keycloak tenía `ssoSessionIdleTimeout = 4 h`, pero la SPA renovaba el token en silencio (`prompt=none`) cada ~4,5 min **de forma incondicional**, y cada renovación reinicia ese contador. Con la app abierta, el contador de inactividad de Keycloak no llegaba a avanzar nunca: la sesión sobrevivía hasta el tope absoluto de `ssoSessionMaxLifespan = 10 h`. Es decir, "inactividad" no estaba siendo medida por nadie.

**Decisión:**
(a) el plazo de inactividad es **2 h (7200 s)**, y se aplica en dos capas complementarias, no redundantes:
  - **cliente Web** (`web/src/core/auth/authClient.ts`, `IDLE_TIMEOUT_MS`) mientras la app está abierta — mide **interacción real** del usuario y deja de renovar al vencer el plazo;
  - **Keycloak** (`ssoSessionIdleTimeout`, `infra/keycloak/realm-vida-cotidiana*.json`) mientras la app está cerrada — si nadie renueva, la sesión SSO caduca sola en el mismo plazo;
(b) al vencer el plazo, el cliente **termina la sesión SSO** llamando al endpoint de logout de Keycloak, no solo suelta el token de memoria: mientras la cookie SSO siguiera viva, el siguiente `prompt=none` volvería a autenticar solo y la expiración sería aparente, no real;
(c) "actividad" = interacción real del usuario (`mousedown`, `keydown`, `touchstart`, `scroll`). Que la pestaña esté abierta, visible, o haciendo peticiones en segundo plano **no** cuenta (**ASSUMPTION**, no la definió el Product Owner);
(d) la marca de última actividad se comparte entre pestañas del mismo navegador vía `localStorage` — es **una marca de tiempo, no una credencial**; WEB-002/DEC-007 (token solo en memoria, nunca en `localStorage`) se mantiene sin excepción;
(e) `ssoSessionMaxLifespan` (10 h) y `accessTokenLifespan` (5 min) **no cambian**: son topes independientes del de inactividad;
(f) aplica a la SPA Web. Android/iOS quedan **TBD** — no se asume que el plazo deba ser el mismo en móvil.

**Alternativas consideradas:**
(a) configurar únicamente `ssoSessionIdleTimeout = 2 h` en Keycloak, sin tocar el cliente — **descartada porque no funciona**: la renovación silenciosa reinicia ese contador cada ~4,5 min, así que con la app abierta el plazo no vencería jamás. Es exactamente el fallo que motivó esta ADR;
(b) enforcement solo en el cliente, sin tocar Keycloak — descartada: no cubre el caso de la app cerrada (o de un cliente manipulado), y dejaría la sesión SSO viva hasta las 10 h del tope absoluto;
(c) cerrar sesión soltando el token del navegador sin llamar al logout de Keycloak — descartada por (b) de la Decisión: la expiración sería solo aparente;
(d) contar como actividad cualquier petición HTTP de la app — descartada: la app renueva y consulta sola en segundo plano, así que eso equivaldría a no expirar nunca (el problema original con otro nombre).

**Consecuencias:** el usuario ve en la pantalla de login el motivo del cierre ("Tu sesión se cerró tras 2 horas sin actividad") en vez de un rebote silencioso. Cambia `ssoSessionIdleTimeout` en los dos realms (`vida-cotidiana` y `vida-cotidiana-test`) — los archivos de import solo surten efecto al recrear el contenedor de Keycloak, así que el valor se aplicó además sobre el Keycloak de desarrollo ya en marcha vía `kcadm`; en cualquier otro entorno hay que aplicarlo igual. No afecta al backend: sigue siendo un resource server sin estado que valida JWT de 5 min (la política vive en la emisión del token, no en la validación). Documentación afectada: `11-auth-security.md` §Sesiones (DEC-016).

**TBD:** plazo de inactividad en Android/iOS; si el plazo debe ser configurable por el usuario o distinto para el contexto Laboral (no solicitado, no asumido).

## ADR-018 Alertas de fecha derivadas (Garantías, Mantenimiento, Suscripciones → Calendario)
**Estado:** Accepted (2026-08-28)

**Contexto:** el Product Owner solicitó (2026-08-28) que los módulos que manejan fechas alimenten el Calendario automáticamente, con avisos escalonados por importancia: Garantías 1 mes/15 días/día de expiración (media, media, alta); Mantenimiento 7/3/0 días (baja, media, alta); Suscripciones 5/2/0 días (baja, media, alta). Y con una restricción explícita y repetida: **las alertas NO son tareas** — no se convierten en `REMINDER`, no ofrecen crear tareas, no aparecen entre los pendientes del usuario. Además: deben distinguirse visualmente por importancia, quedar vinculadas a su módulo y registro de origen, **actualizarse si cambia la fecha del registro original** y **no duplicarse** para el mismo evento y fecha.

**Decisión:**
(a) las alertas se **DERIVAN, no se almacenan**: son una función pura del estado de los tres módulos (`web/src/features/calendar/alerts/dateAlerts.ts`). No hay entidad, tabla, endpoint ni migración de alertas;
(b) cada alerta lleva un id determinista `origen:registro:fechaDelEvento:díasAntes`, y la generación colapsa por ese id — el mecanismo antiduplicados;
(c) se reutiliza el mecanismo existente: `useCalendarData` ya cargaba Garantías y Mantenimientos para el Calendario; solo se le suma Suscripciones. **No se creó una arquitectura paralela**, tal como pidió el Product Owner;
(d) los marcadores del mes de Garantías/Mantenimiento dejan de añadirse por su cuenta y pasan a venir de las alertas — mantener ambos duplicaría el punto del día de vencimiento;
(e) severidad → tono existente del calendario: alta=`error`, media=`warning`, baja=`info`. La severidad **también se escribe en texto** ("Alta"/"Media"/"Baja") y cambia el grosor del filete: el color no es el único portador de la información (accesibilidad);
(f) la periodicidad se persiste (**migración V24**, `maintenance_records.interval_months`, nullable): sin ella, "usar la frecuencia de ¿Cada cuánto? para calcular las próximas fechas" era imposible — el intervalo era un cálculo de UI que se descartaba tras crear el registro. Se proyectan hasta 12 ocurrencias futuras, y lo mismo con el `billingCycle` ya existente de Suscripciones;
(g) Personal→Inicio y Laboral→Hoy muestran "Alertas próximas" (30 días, mayor importancia primero), que es donde el usuario aterriza al entrar.

**Alternativas consideradas:**
(a) materializar las alertas como filas en base de datos — descartada: obligaría a resolver a mano las cuatro condiciones del Product Owner (resincronizar al cambiar la fecha, evitar duplicados, limpiar las huérfanas al borrar el registro, migrar las existentes). Derivar las cumple las cuatro por construcción y sin migración;
(b) crear las alertas como `REMINDER` con una marca especial — **descartada por prohibición explícita**: acabarían en la lista de tareas del usuario, que es justo lo que se pidió evitar;
(c) generarlas en el backend con un job programado — descartada por innecesaria en V1: no hay ningún consumidor fuera del cliente (las notificaciones push no entran en este alcance) y añadiría un componente operativo sin resolver ningún problema nuevo;
(d) no persistir `interval_months` y proyectar solo la próxima fecha — descartada: incumple la petición explícita sobre la frecuencia.

**Consecuencias:** una columna nueva nullable (`V24`) y tres campos nuevos opcionales en la API de Mantenimiento (`intervalMonths` en create/update/response, `openapi.yaml` actualizado); ningún cambio en Garantías ni Suscripciones. El Calendario carga ahora también `GET /subscriptions`. Al cambiar la fecha de un registro, sus alertas cambian en la siguiente lectura sin ninguna acción adicional. **Riesgo asumido y declarado:** las alertas solo existen mientras el cliente está abierto — no hay recordatorio push ni correo asociado; si en el futuro se quiere notificar fuera de la app, esa sí sería una decisión nueva (candidata a job en backend) y debe registrarse aparte.

**TBD:** horizonte de proyección (hoy 12 ocurrencias, elegido por el equipo técnico, no por el Product Owner); si el usuario debe poder silenciar una alerta concreta; si las alertas deben aparecer también en el contexto Laboral del calendario o solo en Personal (hoy aparecen en ambos, igual que Garantías/Mantenimiento antes).

## ADR-019 Aislamiento de recursos por módulo (Personal / Laboral)
**Estado:** Accepted (2026-08-28)

**Contexto:** el Product Owner solicitó (2026-08-28) que todo recurso que alimenta el Calendario pertenezca exclusivamente al módulo desde el que se creó, que no interfiera con el otro módulo (ni en visualización ni en CRUD, estados, filtros, conteos ni información derivada), y que el Calendario respete el módulo activo. Con una condición explícita: **"no soluciones esto únicamente ocultando elementos mediante filtros visuales; el aislamiento debe existir también en la lógica de negocio y en las consultas/fuentes de datos"**.

**Auditoría previa (lo que ya existía, y lo que no):**

| Recurso | Contexto persistido | Filtro en servidor | Filtro en cliente |
|---|---|---|---|
| `REMINDER` | Sí (ADR-015, nullable) | **No** | Sí — y **con fuga**: dejaba pasar los de `context = NULL` en los DOS módulos |
| `WARRANTY` | No | No | No |
| `MAINTENANCE_RECORD` | No | No | No |
| `SUBSCRIPTION` | No | No | No |
| `DAY_NOTE_ELEMENT` | No | No | No |

Es decir: el requisito **no existía**, salvo un filtro de cliente en recordatorios que además era exactamente el tipo de solución que el Product Owner descartó.

**Decisión:**
(a) columna `context VARCHAR(16) NOT NULL DEFAULT 'PERSONAL'` en `warranties`, `maintenance_records`, `subscriptions` y `day_note_elements` (**migración V25**), más `UPDATE reminders SET context='PERSONAL' WHERE context IS NULL` para cerrar la fuga descrita arriba. Índices `(owner_user_id, context)` en las cinco tablas;
(b) el enum vive en `shared.domain.ModuleContext` porque lo comparten cuatro agregados sin relación entre sí. `reminder.domain.ReminderContext` **no se toca**: ya existía y ya está persistido; son dos representaciones del mismo concepto que conviven sin coste;
(c) el filtro baja **hasta la consulta**: `findByOwnerUserIdAndContext(...)` en cada repositorio y una variante de `findAccessibleTo` con contexto para recordatorios. Los recursos del otro módulo no se leen de la base de datos;
(d) `?context=` es **opcional** en los listados. Ausente ⇒ sin filtrar, que es exactamente lo que necesita el **Calendario general** (el tercer modo, que existe para ver Personal y Laboral juntos — ADR-015(b)). El cliente lo envía cuando hay módulo activo y lo omite cuando no;
(e) en el alta, `context` ausente ⇒ **PERSONAL**, nunca "sin módulo". Esto implementa la regla 4 del pedido y cubre además Garantías/Mantenimiento/Suscripciones, cuyas pantallas cuelgan del menú de Personal pero viven en rutas planas donde `useActiveMode()` es `null`;
(f) el contexto **se fija al crear y no cambia**: ningún `applyEdit` lo toca;
(g) los recordatorios **compartidos** también se filtran por contexto: el contexto es del recurso, no de quien lo mira, así que una tarea laboral que alguien comparte contigo sigue siendo laboral;
(h) el anti-solapamiento de las notas del día compara solo contra notas del mismo módulo — dos módulos no comparten lienzo, así que una nota Personal no puede bloquear el movimiento de una Laboral;
(i) el filtro de cliente de `CalendarPage` se conserva como segunda barrera, pero **estricto** (`context ?? 'PERSONAL'`), no como mecanismo principal.

**Alternativas consideradas:**
(a) filtrar solo en el cliente — **descartada por prohibición explícita** del Product Owner, y además ya se había demostrado insuficiente: el filtro existente tenía una fuga real con los contextos nulos;
(b) reutilizar `ReminderContext` en los cuatro agregados nuevos — descartada: acoplaría cuatro módulos al módulo `reminder` sin ninguna ganancia; un enum compartido en `shared` es la dependencia correcta en un monolito modular (ADR-001);
(c) hacer `?context=` obligatorio — descartada: rompería el Calendario general, que es una funcionalidad existente que el pedido manda mantener intacta;
(d) inferir el contexto de la pantalla en el servidor — imposible: el servidor no sabe desde qué navbar se llamó; el cliente es el único que conoce el módulo activo.

**Consecuencias:** una columna nueva en cuatro tablas y un backfill en `reminders` (V25, sin pérdida de datos: todo lo existente queda en Personal, como pidió la regla 4). Un parámetro opcional `context` en cinco listados y en cuatro altas; `context` expuesto en las respuestas para que el cliente pueda verificarlo. Las alertas de fecha (ADR-018) heredan el aislamiento sin cambios propios, porque se derivan de recursos ya filtrados. `NOTE` (`features/calendar/notes`) queda **fuera de alcance**: pese al nombre de su carpeta no lo consume el Calendario, sino Personas/Proyectos.

**Límite declarado, no implementado:** el aislamiento cubre lectura y alta. Una **mutación dirigida por id** (completar/borrar un recurso del otro módulo conociendo su UUID) sigue siendo posible para el propio dueño, porque no hay frontera de seguridad entre los módulos de un mismo usuario — son la misma cuenta. En la práctica es inalcanzable: ninguna pantalla del módulo contrario muestra ese recurso. Si se quisiera cerrar también, habría que exigir el contexto en cada mutación y rechazar los desajustes; **TBD**, no se asume.

**TBD:** si el usuario debe poder mover un recurso de un módulo a otro (hoy el contexto es inmutable por decisión (f)); si Documentos/Inventario/Familia —que no alimentan el Calendario— deben adoptar la misma regla.
