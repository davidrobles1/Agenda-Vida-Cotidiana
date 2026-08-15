# 28 — V1 Decision Pack

Paquete de decisiones pendientes para cerrar V1. Contiene exclusivamente las 15 decisiones solicitadas — ninguna más. Para cada una se ofrecen opciones y una `RECOMMENDATION` técnica, pero **`DECISION` permanece `TBD` en todos los casos**: la resolución corresponde al Product Owner.

`AI-CONTEXT.md` no se actualizará hasta que estas decisiones queden aprobadas explícitamente (una a una o en bloque).

---

## DEC-001 — Estado de completado de recordatorios compartidos

**Contexto:** FR-009 dice que un colaborador puede "marcarlo como completado y deshacer **su propio** completado", lo que sugiere un estado de completado por persona. Pero `09-data-model.md` y `openapi.yaml` modelan `REMINDER.status` como un único campo global (`PENDING`/`COMPLETED`), igual que `AC-005` ("Estado cambia a COMPLETED"). Ambas cosas no pueden ser ciertas a la vez sin definir cuál prevalece.

**Opciones:**
- **A. Estado único global por recordatorio.** Cualquiera (propietario o colaborador) que lo complete cambia el mismo `status` para todos. Simple, pero "deshacer su propio completado" deja de tener sentido literal — cualquiera podría deshacer el completado de cualquiera.
- **B. Completado individual por colaborador.** Se añade una tabla de completado por usuario (p. ej. `REMINDER_COMPLETION(reminder_id, user_id, completed_at)`); el recordatorio "global" podría considerarse completado cuando el propietario lo marca, o mostrarse por persona en la UI. Más fiel al texto de FR-009, pero amplía el modelo de datos y la lógica de negocio.
- **C. Híbrido:** estado único global, pero solo el propietario puede revertir un completado ajeno; el colaborador solo puede alternar su propia acción de "marcar completado" sin que eso implique un estado independiente por persona (evita ambigüedad de UI sin tocar el esquema).

**RECOMMENDATION:** Opción A o C para V1 (estado único, más simple, consistente con el esquema ya escrito), reservando B para una versión futura si el negocio confirma que necesita completado independiente por colaborador (p. ej. lista de compras compartida donde cada quien marca su parte).

**Impacto técnico:** define si `REMINDER` necesita una tabla adicional o no. Cambiarlo después de implementado implica migración de datos.

**Impacto UX:** determina si la UI muestra "completado por X" o un único check compartido; afecta el copy de la pantalla de recordatorio compartido.

**Impacto seguridad:** ninguno directo, salvo que la opción B requiere autorización adicional (un colaborador no debería poder alterar el registro de completado de otro).

**Impacto datos:** B añade una entidad y relaciones nuevas (más superficie a auditar/retener).

**Impacto futuras versiones:** si se elige A/C ahora y B se pide después, requiere migración; elegir B ahora evita ese rework pero sobredimensiona V1 si no hay necesidad confirmada.

**DECISION: A — Estado único global por recordatorio (aprobada 2026-08-09).**

---

## DEC-002 — Comportamiento al eliminar un recordatorio con colaboradores

**Contexto:** `UC-05` sigue diciendo "se elimina o marca eliminado según decisión TBD" sin contemplar a los colaboradores. `09-data-model.md` no define el comportamiento `ON DELETE` de las FK `reminder_id` en `INVITATION`/`REMINDER_SHARE`. `openapi.yaml` no documenta ningún caso especial en `DELETE /reminders/{id}`.

**Opciones:**
- **A. Bloquear la eliminación** mientras existan colaboradores con acceso `ACTIVE` (el propietario debe revocarlos primero, luego eliminar).
- **B. Eliminación en cascada silenciosa:** se elimina el recordatorio y todas sus invitaciones/colaboraciones asociadas sin más trámite.
- **C. Eliminar y notificar:** se elimina inmediatamente, pero se genera una notificación push a los colaboradores activos informando que el recordatorio ya no existe.

**RECOMMENDATION:** Opción A para V1 — más simple de implementar y más segura frente a sorpresas de UX (el propietario ve explícitamente que debe revocar antes de eliminar). B es la opción de menor esfuerzo pero puede sorprender a colaboradores. C es la más completa mas también la de mayor esfuerzo (requiere que el evento de push sobreviva a la eliminación del recurso).

**Impacto técnico:** define el `ON DELETE` de las FK (`RESTRICT` para A, `CASCADE` para B, `CASCADE` + evento de dominio para C) y si hace falta una validación previa en el endpoint `DELETE /reminders/{id}`.

**Impacto UX:** A exige un paso adicional al propietario (revocar antes de eliminar); B es silenciosa para los colaboradores; C informa pero no evita la sorpresa.

**Impacto seguridad:** ninguno crítico; C requiere emitir el evento de auditoría/push antes de que el recurso deje de existir (orden de operaciones).

**Impacto datos:** determina si `INVITATION`/`REMINDER_SHARE` quedan huérfanas, se recrean como registros históricos, o se eliminan también.

**Impacto futuras versiones:** si V2/V3 añaden hogares/grupos, este comportamiento debería generalizarse; conviene documentar la decisión como principio reutilizable, no solo como caso puntual.

**DECISION: C — Eliminar el recordatorio y notificar a los colaboradores activos (aprobada 2026-08-09).**

---

## DEC-003 — Lifecycle de INVITATION

**Contexto:** El enum actual de `INVITATION.status` es `PENDING, ACCEPTED, REJECTED, EXPIRED, REVOKED`. Sin embargo: (a) `FR-007` y el endpoint `DELETE /invitations/{invitationId}` permiten **cancelar** una invitación pendiente, y no existe un estado `CANCELLED`; (b) `REVOKED` a nivel de `INVITATION` es conceptualmente incorrecto, porque la revocación (`FR-010`) actúa sobre `REMINDER_SHARE` (colaboración ya aceptada), no sobre la invitación en sí.

**Opciones:**
- **A. Corregir el enum:** `PENDING, ACCEPTED, REJECTED, EXPIRED, CANCELLED` (se retira `REVOKED` de `INVITATION`; la revocación vive únicamente en `REMINDER_SHARE.status`).
- **B. Mantener `REVOKED` en `INVITATION`** además de añadir `CANCELLED`, usándolo para casos donde el propietario "revoca" una invitación ya aceptada retroactivamente sin pasar por `REMINDER_SHARE` (duplicaría semántica con la revocación de share).
- **C. Unificar todo en una sola tabla** (fusionar `INVITATION` y `REMINDER_SHARE` en una única entidad con más estados), evitando la distinción entre "invitación" y "colaboración activa".

**RECOMMENDATION:** Opción A — es la corrección mínima y más consistente con el resto del diseño ya aprobado (ADR-006), sin fusionar conceptos ni duplicar semántica.

**Impacto técnico:** cambio de enum antes de la primera migración; si ya hubiera datos con `REVOKED` en `INVITATION` habría que migrarlos (no aplica todavía, no hay datos).

**Impacto UX:** ninguno directo; es un detalle de modelo interno.

**Impacto seguridad:** una máquina de estados clara reduce el riesgo de lógica de autorización inconsistente (p. ej. tratar por error una invitación `REVOKED` como si tuviera el mismo significado que una `REMINDER_SHARE` revocada).

**Impacto datos:** define el esquema definitivo de la tabla `INVITATION` antes de crearla.

**Impacto futuras versiones:** una máquina de estados limpia facilita añadir en V2/V3 nuevos motivos de cierre (p. ej. "expulsado del hogar") sin volver a rediseñar el enum base.

**DECISION: A — `PENDING, ACCEPTED, REJECTED, EXPIRED, CANCELLED`; la revocación pertenece únicamente a `REMINDER_SHARE` (aprobada 2026-08-09).**

---

## DEC-004 — Proveedor OIDC

**Contexto:** ADR-008 (Proposed) delega la identidad a un proveedor OIDC/OAuth 2.1 self-hosted con passkeys/MFA. Se comparó: Keycloak, Zitadel, Authentik, Ory (Kratos+Hydra). Los cuatro son OIDC-compliant; la integración con Spring Boot (Spring Security OAuth2/OIDC) y con clientes Android/iOS/Web es equivalente en los cuatro. La diferencia real está en madurez, ligereza operativa, alcance de features y licencia.

**Opciones:**
- **A. Keycloak** — el más maduro y extenso; passkeys nativos desde v25; mayor complejidad operativa (tuning/clustering); Apache 2.0.
- **B. Zitadel** — arquitectura event-sourced en Go, más ligera de operar; passwordless/WebAuthn "de fábrica"; pensado para multi-tenant; Apache 2.0 (algunas features enterprise aparte).
- **C. Authentik** — buena experiencia de desarrollador, WebAuthn GA, incluye SAML/LDAP/RADIUS; más orientado a SSO/proxy que a apps nativas; MIT.
- **D. Ory (Kratos+Hydra)** — requiere ensamblar dos servicios (identidad + OAuth/OIDC); mayor complejidad operativa; Apache 2.0.

**RECOMMENDATION:** Keycloak (A) como proveedor de arranque, por madurez y documentación, sin descartar migrar a Zitadel si la carga operativa de Keycloak resulta excesiva para el equipo una vez en producción (el contrato OIDC estándar hace esa migración menos costosa que con una solución propietaria).

**Impacto técnico:** define qué servidor de identidad se despliega, cómo se configuran los realms/clientes de Android, iOS y Web, y qué SDK/librería OIDC se usa en cada plataforma.

**Impacto UX:** todos soportan passkeys/WebAuthn y MFA, por lo que el impacto en la experiencia de login debería ser similar independientemente de la opción elegida; puede variar la velocidad de las pantallas de login hospedadas si se usan flujos redirigidos.

**Impacto seguridad:** los cuatro cubren OAuth 2.1/OIDC/passkeys; la diferencia de riesgo está más en la superficie operativa (parcheo, configuración) que en el protocolo. Keycloak, al ser el más usado, tiene más historial de CVEs conocidas y también más parches/documentación de hardening.

**Impacto datos:** ninguno de los cuatro requiere cambios en el modelo de datos de la aplicación (identidad vive en el IdP, la app solo guarda `user_id`/`email`).

**Impacto futuras versiones:** cambiar de proveedor más adelante es factible por ser todos OIDC estándar, pero implica migrar usuarios/credenciales del IdP elegido — mejor decidir con la mayor certeza posible ahora.

**DECISION: A — Keycloak como proveedor OIDC/OAuth 2.1 self-hosted (aprobada 2026-08-09).**

---

## DEC-005 — Modelo de almacenamiento de dispositivos/tokens push

**Contexto:** ADR-007/FR-011 requieren notificaciones push, pero ni `09-data-model.md` ni `openapi.yaml` tienen una entidad para guardar tokens de dispositivo. Sin esto no se puede implementar el envío de push.

**Opciones:**
- **A. Tabla `DEVICE_PUSH_TOKEN`** (`id`, `user_id` FK, `platform` [ANDROID/IOS/WEB], `token`, `created_at`, `last_seen_at`), con múltiples filas por usuario (multi-dispositivo desde el día uno).
- **B. Un único token por usuario** (columna en `USER` en vez de tabla aparte) — más simple, pero no soporta que un usuario tenga la app instalada en varios dispositivos/navegadores a la vez (un login nuevo pisaría el token anterior).
- **C. Delegar el registro de dispositivos a un servicio externo** (p. ej. un proveedor de push todo-en-uno tipo OneSignal que gestione sus propios tokens), sin tabla propia más allá de un identificador de referencia.

**RECOMMENDATION:** Opción A — es la que sostiene correctamente "multi-dispositivo" (ya listado como capacidad futura en FR-011/FUTURE) sin sobrearquitecturar, y evita atarse a un proveedor externo de terceros para un dato sensible como el token de push.

**Impacto técnico:** nueva tabla y su repositorio en el módulo `notification`; el cliente debe registrar/actualizar su token en cada arranque de sesión.

**Impacto UX:** ninguno visible directamente, salvo que sin esto no hay push (afecta la fiabilidad percibida de "avisos" en recordatorios compartidos).

**Impacto seguridad:** los tokens de push son sensibles (permiten enviar notificaciones al dispositivo); deben tratarse como dato semi-secreto: no loguear, invalidar al cerrar sesión/logout, limpiar tokens obsoletos.

**Impacto datos:** añade una entidad con su propia política de retención (tokens inactivos deberían purgarse).

**Impacto futuras versiones:** con A, agregar preferencias de notificación por dispositivo (FUTURE, ya anticipado en FR-011) no requiere rediseño.

**DECISION: A — Tabla `DEVICE_PUSH_TOKEN` con múltiples dispositivos por usuario (aprobada 2026-08-09).**

---

## DEC-006 — Stack tecnológico definitivo de iOS

**Contexto:** ADR-005 confirmó iOS como plataforma V1, pero el stack sigue `TBD`. CLAUDE.md pide evitar cross-platform "solo para ahorrar código" y priorizar estabilidad, soporte de Apple, mantenibilidad, seguridad, performance, integración nativa y longevidad.

**Opciones:**
- **A. SwiftUI + Swift nativo.** Framework moderno de Apple, mismo patrón conceptual que Compose en Android (declarativo), soporte oficial de Apple, mejor longevidad e integración con passkeys/WebAuthn/APNs nativos.
- **B. UIKit + Swift.** Más maduro/estable en apps muy complejas o con necesidades de compatibilidad con versiones muy antiguas de iOS, pero más código boilerplate y peor alineación con el patrón declarativo usado en Android.
- **C. Cross-platform (Kotlin Multiplatform, Flutter, React Native, etc.).** Permite compartir lógica entre plataformas, pero CLAUDE.md explícitamente pide no elegir esto solo por ahorro de código, y añade una capa de abstracción adicional a mantener.

**RECOMMENDATION:** Opción A (SwiftUI + Swift) — nativo, alineado con el criterio de CLAUDE.md, con mejor soporte a largo plazo de Apple y paridad conceptual con la arquitectura Android (Clean Architecture + MVVM, igual que en `08-android-architecture.md`).

**Impacto técnico:** define estructura de proyecto, patrón de arquitectura del cliente iOS (recomendado: Clean Architecture + MVVM, análogo a Android) y las librerías de red/DI a usar.

**Impacto UX:** SwiftUI facilita seguir las convenciones nativas de iOS (Human Interface Guidelines) manteniendo coherencia conceptual con Android vía el sistema de diseño compartido.

**Impacto seguridad:** SwiftUI/Swift tienen mejor integración nativa con Keychain, passkeys/WebAuthn y App Transport Security que alternativas cross-platform.

**Impacto datos:** ninguno directo sobre el backend; el cliente consumirá la misma API REST que Android y Web.

**Impacto futuras versiones:** una base nativa facilita adoptar rápidamente nuevas capacidades de iOS (widgets, Live Activities, etc.) si se necesitan en V2/V3.

**DECISION: A — Swift + SwiftUI para iOS (aprobada 2026-08-09).**

---

## DEC-007 — Stack tecnológico definitivo de Web

**Contexto:** ADR-005 confirmó Web como plataforma V1, pero el stack sigue `TBD`. V1 requiere sesión autenticada (poco valor de SEO público); CLAUDE.md pide priorizar accesibilidad, performance, SEO cuando corresponda, seguridad, mantenibilidad y responsive design.

**Opciones:**
- **A. React + TypeScript (SPA pura).** Suficiente para una app autenticada sin necesidad de SEO público; ecosistema maduro, buen soporte de accesibilidad si se implementa con cuidado.
- **B. Next.js (React + TypeScript con SSR/SSG).** Aporta SSR/SEO y mejor rendimiento de carga inicial, útil si en el futuro se necesitan páginas públicas (landing, invitación pública a compartir, etc.); más complejidad operativa (requiere un runtime Node o despliegue específico) que una SPA estática.
- **C. Otro framework** (Vue, Svelte, Angular) — variantes viables pero sin ninguna señal en la documentación de por qué apartarse de React/TypeScript, que es la opción explícitamente sugerida a evaluar en CLAUDE.md.

**RECOMMENDATION:** Opción A (React + TypeScript, SPA) para V1, dado que no hay necesidad de SEO confirmada; dejar Next.js como camino de migración natural si V2/V3 requiere páginas públicas indexables (p. ej. una landing de marketing, o una página pública de "aceptar invitación" antes de loguearse).

**Impacto técnico:** define el patrón de build/despliegue (estático vs. servidor Node) y afecta directamente a DEC-004/manejo de tokens OIDC en el navegador (ver `27-v1-readiness-review.md`, riesgo de arquitectura sobre tokens Web).

**Impacto UX:** SPA pura puede tener una carga inicial algo más lenta que SSR, pero es aceptable para una app autenticada tipo "dashboard".

**Impacto seguridad:** una SPA pública que maneje tokens OIDC en el navegador tiene más riesgo que un patrón con backend-for-frontend; esta decisión está relacionada pero no sustituye a definir el patrón de manejo de tokens (pendiente, ver `27-v1-readiness-review.md`).

**Impacto datos:** ninguno directo sobre el backend.

**Impacto futuras versiones:** migrar de SPA pura a Next.js más adelante es factible pero no trivial; conviene confirmar pronto si el "compartir por invitación pública" (aceptar sin login previo) es un escenario a soportar, porque eso favorecería Next.js desde ahora.

**DECISION: A — React + TypeScript como SPA para Web (aprobada 2026-08-09).**

---

## DEC-008 — Cloud provider

**Contexto:** CLAUDE.md propone AWS como objetivo por defecto "salvo que la documentación existente justifique otra decisión". La documentación del proyecto nunca lo ratificó; sigue `TBD` en `01-scope.md`, `AI-CONTEXT.md` y `25-open-questions.md`.

**Opciones:**
- **A. AWS** — el propuesto por CLAUDE.md; ecosistema más amplio, mayor cantidad de servicios gestionados (RDS PostgreSQL, S3, SES, SNS/push).
- **B. GCP** — buena integración nativa si se elige Firebase Cloud Messaging para push (DEC-010) y Cloud SQL para PostgreSQL.
- **C. Azure** — viable, con AKS/App Service, Azure Database for PostgreSQL.
- **D. Proveedor self-hosted / VPS genérico** (p. ej. Hetzner, DigitalOcean) — menor costo inicial y menor lock-in, coherente con la preferencia general del proyecto por soluciones self-hosted (ver DEC-004), pero exige operar más piezas manualmente (backups, TLS, escalado).

**RECOMMENDATION:** Opción A (AWS), siguiendo la propuesta de CLAUDE.md, salvo que el equipo tenga una razón concreta (costo, experiencia previa, preferencia por self-hosted) para elegir otra — en cuyo caso debe registrarse como decisión justificada, no como omisión.

**Impacto técnico:** define los servicios gestionados disponibles (base de datos, object storage, colas, IaC) y el pipeline de despliegue en `19-cicd.md`.

**Impacto UX:** ninguno directo, salvo latencia según región elegida.

**Impacto seguridad:** cada proveedor tiene su propio modelo de IAM/red; la elección afecta cómo se implementan cifrado at rest, gestión de secretos y aislamiento de entornos (`06-devops`/`09-devops` conceptual).

**Impacto datos:** define dónde residen físicamente los datos (relevante para futuras consideraciones de privacidad/regulación según el mercado inicial, aún `TBD`).

**Impacto futuras versiones:** cambiar de proveedor cloud más adelante es costoso; conviene decidirlo antes de escribir IaC o pipelines de despliegue reales.

**DECISION: A — AWS como cloud provider (aprobada 2026-08-09).**

---

## DEC-009 — Proveedor de correo

**Contexto:** V1 necesita enviar correo al menos para invitaciones a personas sin cuenta (FR-007) y, si se confirma DEC-014, para verificación de email. `01-scope.md` y `25-open-questions.md` lo marcan `TBD`.

**Opciones:**
- **A. Amazon SES** — coherente si se elige AWS (DEC-008); costo bajo, requiere gestión de reputación de dominio propia.
- **B. SendGrid / Postmark / Mailgun** — proveedores especializados en email transaccional, con mejor deliverability "out of the box" y paneles de seguimiento, independientes del cloud provider elegido.
- **C. Proveedor self-hosted (p. ej. Postal, Mailu)** — coherente con la preferencia general por self-hosted, pero la reputación de IP/dominio para deliverability es más difícil de mantener por cuenta propia.

**RECOMMENDATION:** Si se elige AWS en DEC-008, Amazon SES (A) por integración natural y costo; si no, un especialista transaccional tipo Postmark (B) por su enfoque en deliverability para emails transaccionales (que es exactamente el caso de uso: invitaciones y verificación, no marketing masivo).

**Impacto técnico:** define el adapter de envío de correo en el módulo `sharing`/`identity` y su configuración de dominio (SPF/DKIM/DMARC).

**Impacto UX:** afecta directamente si la invitación por email llega de forma confiable y rápida (deliverability), lo cual es crítico para FR-007.

**Impacto seguridad:** el correo transaccional no debe incluir información sensible más allá de lo necesario (p. ej. no incluir datos del recordatorio en el cuerpo del email más allá de lo imprescindible).

**Impacto datos:** ninguno sobre el modelo de datos de la aplicación; sí implica compartir emails de usuarios/invitados con un proveedor externo (relevante para la política de privacidad).

**Impacto futuras versiones:** verificación de email (DEC-014) y futuras notificaciones por correo (digest, recuperación de cuenta) dependen de esta elección.

**DECISION: A — Amazon SES para correo transaccional (aprobada 2026-08-09).**

---

## DEC-010 — Proveedores de push

**Contexto:** ADR-007 exige notificaciones push abstraídas detrás de una interfaz propia (`PushNotificationSender`), con adapters concretos por proveedor, dado que V1 cubre Android, iOS y Web.

**Opciones:**
- **A. Firebase Cloud Messaging (FCM) unificado** — FCM entrega nativamente a Android, y puede entregar a iOS (vía su puente con APNs) y a navegadores (vía el protocolo Web Push estándar), reduciendo la integración a un solo proveedor.
- **B. Nativo por plataforma:** FCM para Android, APNs directo para iOS, Web Push (VAPID) directo para navegadores — más control y menor dependencia de un tercero, pero triplica la integración y el mantenimiento.
- **C. Agregador de terceros** (p. ej. OneSignal) — reduce el esfuerzo de integración aún más que A, pero añade un proveedor externo adicional que procesará contenido de las notificaciones (relevante para minimización de datos, NFR-002).

**RECOMMENDATION:** Opción A (FCM unificado) — es el mejor equilibrio entre esfuerzo de integración (uno solo, coherente con ADR-007) y minimización de dependencias de terceros frente a un agregador comercial adicional (C).

**Impacto técnico:** define los adapters concretos detrás de `PushNotificationSender`; con A, un solo SDK/servidor de envío para las tres plataformas.

**Impacto UX:** ninguna diferencia perceptible para el usuario entre opciones; todas entregan notificaciones nativas del sistema operativo/navegador.

**Impacto seguridad:** cualquier opción expone el contenido de la notificación al proveedor de push elegido; con C (agregador de terceros) esa exposición se da además a un proveedor no directamente relacionado con Google/Apple.

**Impacto datos:** el payload de la notificación push (p. ej. título de un recordatorio) transita por el proveedor elegido — debe evaluarse qué tan sensible es ese contenido y si conviene mandar solo un identificador genérico ("tienes una nueva invitación") en vez del contenido real.

**Impacto futuras versiones:** la interfaz `PushNotificationSender` (ya decidida en ADR-007) permite cambiar de proveedor más adelante sin rediseñar el dominio, independientemente de qué opción se elija ahora.

**DECISION: A — FCM como proveedor unificado de push detrás de `PushNotificationSender` (aprobada 2026-08-09).**

---

## DEC-011 — Versión mínima soportada de Android

**Contexto:** `NFR-008` deja la versión mínima de Android como `TBD`. Datos de mercado (agosto 2026): la cobertura acumulada llega a ~86.9% desde API 30 (Android 11) y ~68.9% desde API 33 (Android 13); Android 11/12 en conjunto aún representan una porción no despreciable de dispositivos activos.

**Opciones:**
- **A. minSdk 26 (Android 8.0, Oreo)** — cobertura muy amplia (estimada >95% del parque activo), a cambio de no poder usar algunas APIs modernas de forma directa (canales de notificación sí están disponibles desde 26; otras APIs de seguridad más recientes requerirían compatibilidad hacia atrás vía Jetpack).
- **B. minSdk 30 (Android 11)** — cobertura ~87%, acceso a APIs de seguridad y privacidad más modernas (scoped storage obligatorio, mejoras de permisos) con menos necesidad de código de compatibilidad.
- **C. minSdk 33 (Android 13)** — cobertura ~69%, la más moderna en APIs (permiso granular de notificaciones, etc.) pero deja fuera a una porción considerable de usuarios con dispositivos más antiguos.

**RECOMMENDATION:** Opción A o B. Dado que el proyecto prioriza seguridad pero también alcance de usuarios en un MVP, B (minSdk 30) es un punto medio razonable: cubre la gran mayoría del parque activo y evita mantener compatibilidad con versiones muy antiguas sin sacrificar demasiado alcance.

**Impacto técnico:** determina qué APIs de Jetpack/Compose/seguridad se pueden usar directamente vs. cuáles requieren *backport*/comprobaciones de versión en tiempo de ejecución.

**Impacto UX:** cuanto más alto el mínimo, más usuarios con dispositivos antiguos quedan excluidos del MVP.

**Impacto seguridad:** versiones más recientes de Android traen mejoras de seguridad del sistema operativo (permisos, sandboxing) que reducen la superficie de ataque disponible para la app.

**Impacto datos:** ninguno directo.

**Impacto futuras versiones:** subir el mínimo más adelante es sencillo (se hace progresivamente); bajarlo después de lanzar es más difícil si ya se usó una API exclusiva de versiones nuevas.

**DECISION: B — Android mínimo API 30 / Android 11 (aprobada 2026-08-09).**

---

## DEC-012 — Versión mínima soportada de iOS

**Contexto:** `NFR-008` deja la versión mínima de iOS como `TBD`. Datos de mercado (junio 2026): iOS 26 concentra ~70-79% de los iPhones activos, iOS 18 ronda 14-18%, y el resto de versiones se reparten porcentajes menores al 4% cada una.

**Opciones:**
- **A. iOS 17** — cobertura muy amplia (iOS 17+18+26 cubren, según los datos anteriores, más del 90% del parque activo), a cambio de mantener compatibilidad con algunas APIs algo más antiguas.
- **B. iOS 18** — cobertura ligeramente menor pero aún amplia (~85-93% combinando 18 y 26), con acceso a APIs algo más recientes.
- **C. iOS 26 (solo la versión actual)** — máximo acceso a APIs nuevas, pero deja fuera entre el 20-30% del parque activo (todo lo que no sea iOS 26), lo cual es agresivo para un MVP que busca validar producto con el mayor alcance posible.

**RECOMMENDATION:** Opción A (iOS 17) — maximiza el alcance sin renunciar a APIs modernas de seguridad (passkeys/WebAuthn ya están disponibles desde iOS 16), coherente con el enfoque de "no sobrearquitecturar ni restringir alcance sin necesidad" de V1.

**Impacto técnico:** determina qué versión mínima de SwiftUI y qué APIs de seguridad (Keychain, passkeys, App Attest) se pueden asumir disponibles sin comprobaciones adicionales.

**Impacto UX:** cuanto más bajo el mínimo, mayor alcance de usuarios, a cambio de tener que respetar comportamientos/estilos de versiones de SwiftUI algo más antiguas.

**Impacto seguridad:** versiones más recientes de iOS traen mejoras de seguridad del sistema; un mínimo muy bajo obliga a mantener rutas de compatibilidad más largas.

**Impacto datos:** ninguno directo.

**Impacto futuras versiones:** igual que en Android, subir el mínimo después es sencillo; bajarlo es más costoso.

**DECISION: A — iOS mínimo 17 (aprobada 2026-08-09).**

---

## DEC-013 — Navegadores mínimos soportados (Web)

**Contexto:** `NFR-008` deja los navegadores soportados como `TBD`. La práctica de la industria en 2026 favorece un enfoque de "navegadores evergreen" (Chrome, Edge, Firefox, Safari, que se actualizan automáticamente) en vez de fijar números de versión concretos, apoyándose en el concepto de "Baseline: widely available" del W3C/MDN (funcionalidades con soporte estable en los cuatro motores durante 30+ meses).

**Opciones:**
- **A. Últimas 2 versiones mayores de Chrome, Edge, Firefox y Safari** (desktop), más la versión actual y anterior de Safari en iOS, y la versión actual de Chrome en Android — enfoque recomendado por prácticas actuales de la industria (evergreen + Baseline widely available).
- **B. Enfoque más conservador:** soportar además navegadores más antiguos o poco usados (p. ej. versiones ESR de Firefox, navegadores corporativos desactualizados) — mayor alcance potencial en entornos corporativos, mayor costo de pruebas y de renunciar a funcionalidades modernas del estándar web.
- **C. Enfoque más agresivo:** solo la última versión de cada navegador evergreen, sin margen — menor carga de pruebas, pero puede excluir usuarios que tardan unos días/semanas en actualizar.

**RECOMMENDATION:** Opción A — es el estándar de facto de la industria en 2026 (navegadores evergreen + Baseline widely available), balancea alcance real con la posibilidad de usar funcionalidades modernas de la plataforma web sin polyfills excesivos.

**Impacto técnico:** define la configuración de `browserslist`/target de build del cliente Web (DEC-007) y qué funcionalidades de la plataforma web se pueden usar sin polyfill.

**Impacto UX:** un enfoque más conservador (B) exige más pruebas de compatibilidad visual/funcional; uno más agresivo (C) puede degradar la experiencia de una minoría de usuarios con navegadores desactualizados.

**Impacto seguridad:** navegadores desactualizados carecen de mitigaciones de seguridad recientes (p. ej. políticas de cookies, CSP moderno); restringir a evergreen reduce ese riesgo del lado del cliente.

**Impacto datos:** ninguno directo.

**Impacto futuras versiones:** un enfoque evergreen (A) requiere revisar periódicamente la matriz de soporte, pero no exige una decisión formal costosa cada vez.

**DECISION: A — últimas 2 versiones mayores de Chrome, Edge, Firefox y Safari, más Safari iOS (actual y anterior) y Chrome Android (actual) (aprobada 2026-08-09).**

---

## DEC-014 — Verificación de email

**Contexto:** `FR-001` deja explícitamente como `TBD` si se requiere verificación de email en V1. Esto también condiciona el flujo de invitación por email (FR-007): si no hay verificación, cualquier email introducido en el registro podría no pertenecer realmente a quien lo usa.

**Opciones:**
- **A. Verificación obligatoria antes de poder usar la cuenta** (flujo clásico: registro → email de verificación → confirmación → acceso pleno).
- **B. Verificación opcional/diferida:** el usuario puede usar la app sin verificar, pero ciertas acciones (p. ej. recibir invitaciones de compartición, aparecer como destinatario válido de invitación por email) requieren tener el email verificado.
- **C. Sin verificación en V1:** se confía en el email introducido, delegando la validación de identidad completamente al proveedor OIDC elegido (DEC-004), que puede ya exigir verificación como parte de su propio flujo de registro (varios proveedores OIDC verifican email por defecto).

**RECOMMENDATION:** Opción C si el proveedor OIDC elegido en DEC-004 ya verifica email como parte de su flujo estándar (evita duplicar lógica); si no, Opción A por ser la más simple de razonar y la que evita ambigüedad en el flujo de invitaciones (FR-007) sobre a quién se le está compartiendo realmente un recordatorio.

**Impacto técnico:** si se delega al proveedor OIDC (C), no hay trabajo adicional en el backend propio; si se implementa a mano (A/B), requiere flujo de envío de correo (DEC-009) y estado de verificación en `USER`.

**Impacto UX:** A añade fricción al onboarding (un paso extra antes de poder usar la app); C es la más fluida.

**Impacto seguridad:** sin verificación, aumenta el riesgo de cuentas con emails ajenos o inválidos, lo cual afecta la confiabilidad del flujo de invitación (¿a quién se le está compartiendo realmente el recordatorio?).

**Impacto datos:** si se implementa, añade un campo de estado (`email_verified`) y posiblemente un token de verificación en el modelo de `USER`.

**Impacto futuras versiones:** la política de verificación es más fácil de endurecer después (pasar de C/B a A) que de relajar.

**DECISION: C — la verificación de email se delega al proveedor OIDC (Keycloak); no se implementa un sistema paralelo en la aplicación (aprobada 2026-08-09).**

---

## DEC-015 — Política de eliminación/retención de cuenta

**Contexto:** `25-open-questions.md` deja como `TBD` la política de retención/eliminación de cuenta. Además, el modelo de compartir (ADR-006) introduce un caso adicional no cubierto originalmente: qué pasa con los emails de personas invitadas que nunca llegaron a crear una cuenta (dato de un tercero sin consentimiento explícito de la plataforma, ver riesgo de privacidad en `27-v1-readiness-review.md`).

**Opciones:**
- **A. Eliminación completa e inmediata** al solicitarla el usuario (hard delete de sus datos, incluyendo recordatorios propios y su participación como colaborador), sin período de gracia.
- **B. Soft delete con período de gracia** (p. ej. 30 días) antes del borrado definitivo, permitiendo revertir la eliminación si fue accidental.
- **C. Retención mínima por motivos de auditoría/seguridad** (p. ej. conservar únicamente los registros de auditoría de seguridad ya anonimizados, eliminando el resto de datos personales de inmediato).

Para las invitaciones a personas sin cuenta (el email queda huérfano si nunca se registran o si la invitación expira/es rechazada):
- **A'. Purgar el email tras expiración/rechazo** pasado un plazo corto (p. ej. 30-90 días).
- **B'. Conservarlo indefinidamente** como parte del historial de auditoría del recordatorio.

**RECOMMENDATION:** Opción B (soft delete con periodo de gracia corto, p. ej. 30 días) para cuentas, por ser el estándar más amigable con el usuario sin renunciar a poder revertir errores; y Opción A' (purga tras expiración/rechazo) para emails de invitados sin cuenta, alineado con el principio de minimización de datos (NFR-002) ya adoptado en el proyecto.

**Impacto técnico:** B requiere un job/proceso de purga diferida y un estado "pendiente de eliminación" en `USER`; A' requiere un job periódico de limpieza de invitaciones resueltas/expiradas.

**Impacto UX:** B permite al usuario arrepentirse de eliminar su cuenta dentro del plazo de gracia; A elimina esa posibilidad.

**Impacto seguridad:** una política de retención clara reduce la superficie de datos personales expuestos ante una eventual brecha (menos datos acumulados de personas que ya no usan el servicio o que nunca lo usaron).

**Impacto datos:** define el ciclo de vida completo de `USER`, `REMINDER`, `INVITATION` y `REMINDER_SHARE` tras una baja, y debe quedar reflejado en `09-data-model.md` una vez decidido.

**Impacto futuras versiones:** una política de retención bien definida ahora evita rediseños cuando la base de usuarios crezca y la eliminación masiva/selectiva se vuelva más delicada operativamente.

**DECISION: B + A' — Soft delete de cuenta con período de gracia de 30 días y purga posterior; los emails de invitados sin cuenta se purgan tras expiración/rechazo/cancelación siguiendo una política de retención corta (aprobada 2026-08-09).**

---

## Nota final

Las 15 decisiones fueron aprobadas por el Product Owner el 2026-08-09 con los valores indicados en cada `DECISION`. Quedaron reflejadas en `22-decision-log.md` (ADR-006/007/008 actualizados; ADR-009 a ADR-012 nuevos), `09-data-model.md`, `openapi/openapi.yaml`, `03-prd.md`, `04-use-cases.md`, `13-acceptance.md`, `12-traceability.md`, `06-c4.md`, `07-backend-architecture.md`, `08-android-architecture.md`, `08b-ios-architecture.md` (nuevo), `08c-web-architecture.md` (nuevo), `11-auth-security.md`, `21-security.md`, `01-scope.md`, `README.md`, `26-v1-backlog.md` y `25-open-questions.md`. Detalle completo de la auditoría cruzada en `29-v1-final-readiness.md`.

No se agregó ninguna decisión fuera de las 15 solicitadas. Donde una opción dependía de otra decisión de este mismo paquete (p. ej. DEC-009 dependía de DEC-008; DEC-014 dependía de DEC-004), se resolvió de forma consistente con la decisión de la que dependía.

`AI-CONTEXT.md` fue actualizado con estas 15 decisiones aprobadas.
