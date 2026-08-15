# 00 — Development Baseline (V1 Kickoff)

Punto de partida formal del desarrollo de V1, posterior al gate `V1_READINESS_STATUS: READY` (`Documentacion/32-v1-development-gate-audit.md`). Este documento no redefine ninguna decisión aprobada; traduce la documentación ya cerrada en un plan de implementación verificable.

## 1. Estado inicial del código

Inventario real del repositorio al iniciar esta fase (verificado por inspección directa, no asumido por la documentación):

| Componente | Estado | Evidencia |
|---|---|---|
| Control de versiones (Git) | **DONE (2026-08-15)** | El `.git/index.lock` atascado (limitación de las sesiones de sandbox anteriores) se eliminó en una máquina real sin esa restricción; `git init -b main`/`add`/`commit` se ejecutaron con éxito (commit raíz `9f77ead`, 88 archivos). Ver `02-validation-report.md` §8.2. |
| Backend (Spring Boot) | **MISSING → PARTIAL (este ciclo)** | No existía ningún código antes de este ciclo. Este ciclo añade `backend/` con el primer vertical slice (ver §2 y `docs/development/01-technical-backlog.md`). |
| Android | **MISSING** | No existe carpeta `android/` ni proyecto Gradle/Kotlin. |
| iOS | **MISSING** | No existe carpeta `ios/` ni proyecto Xcode/Swift. |
| Web | **MISSING** | No existe carpeta `web/` ni proyecto React/TypeScript. |
| Configuración de entorno (`.env`, perfiles) | **MISSING → PARTIAL (este ciclo)** | Se añade `backend/src/main/resources/application.yml` con perfiles `local`/`test`; no existía nada antes. |
| Docker / infraestructura local | **DONE (2026-08-15)** | `docker-compose.yml` (PostgreSQL + Keycloak) en la raíz, validado: Keycloak 25 levantado vía `docker compose`; Postgres validado en un puerto alternativo por un conflicto de puerto local preexistente ajeno al proyecto (ver `02-validation-report.md` §8.7). |
| Base de datos / esquema | **MISSING → PARTIAL (este ciclo)** | Se añade la primera migración Flyway (`V1__init_schema.sql`) con las tablas `users` y `reminders` únicamente (alcance del vertical slice). `invitations`, `reminder_shares`, `device_push_tokens` quedan para el milestone de Sharing/Push (no se crean tablas para funcionalidad todavía no implementada). |
| Migraciones Flyway | **MISSING → PARTIAL (este ciclo)** | Ver arriba. |
| Tests | **DONE (2026-08-15)** | `ReminderServiceTest`, `ReminderControllerIntegrationTest`, `UserControllerIntegrationTest`: 19/19 ejecutados realmente vía `./gradlew clean test`, 0 failures/0 errors (JDK 21 + Docker/Testcontainers reales). Ver `02-validation-report.md` §8 y `03-milestone-1-gate.md`. |
| CI/CD | **MISSING** | No existe ningún workflow (`.github/workflows/`, etc.). Documentado conceptualmente en `19-cicd.md`, no implementado. |
| Keycloak (realm, clientes) | **MISSING** | No existe configuración de realm exportada ni instancia corriendo. `docker-compose.yml` añade un contenedor Keycloak en modo dev, pero el realm (`vida-cotidiana`) y sus clientes deben crearse manualmente o vía script de importación — no incluido en este ciclo. |
| Documentación (`Documentacion/`, `docs/`) | **DONE** | Completa y en estado `READY` según `32-v1-development-gate-audit.md`. Es la fuente de verdad de este ciclo. |

**Conclusión del inventario:** el proyecto arrancaba desde cero en código — la fase de documentación (fases anteriores) no había producido ningún artefacto de implementación. Esta fase es, literalmente, el primer código del proyecto.

## 2. Componentes existentes (al cierre de este ciclo)

- `backend/` — proyecto Gradle (Kotlin DSL) Java 21 / Spring Boot 3.3.4, módulos `shared`, `identity`, `user`, `reminder` (los únicos necesarios para el vertical slice; `sharing`, `notification`, `audit` quedan como paquetes vacíos a crear cuando se implementen, para no anticipar código sin uso).
- Migración Flyway `V1__init_schema.sql` (`users`, `reminders`).
- Resource server OAuth2/OIDC contra Keycloak (sin login propio, DEC-004/ADR-008).
- Sincronización de `USER` desde el JWT autenticado (upsert en cada request autenticado).
- `Reminder`: crear, listar (propias), completar/revertir (con bloqueo optimista opcional), con autorización por propietario.
- Manejo uniforme de errores (`Error` schema: `code`/`message`/`traceId`), alineado con `openapi.yaml`.
- Tests unitarios (servicio) y de integración (Testcontainers + MockMvc) del vertical slice.
- `docker-compose.yml` (PostgreSQL 16 + Keycloak 25, modo dev local).

## 3. Componentes faltantes (explícitamente fuera de este ciclo)

- Edición de recordatorio (`PATCH /reminders/{id}`, AC-004b) y eliminación (`DELETE /reminders/{id}`, UC-05).
- Todo el módulo `sharing`: invitaciones, `REMINDER_SHARE`, revocación, cancelación (UC-07 a UC-10, UC-14).
- Todo el módulo `notification`: `DEVICE_PUSH_TOKEN`, `PushNotificationSender`/adapter FCM.
- Eliminación de cuenta (`DELETE /me`, `PENDING_DELETION`, job de purga).
- Auditoría (`audit`) como módulo explícito (por ahora, logging estructurado sin tabla dedicada).
- Android, iOS, Web — ningún cliente.
- CI/CD, entornos `staging`/`production`, despliegue AWS.
- Configuración real de Keycloak (realm/clientes/roles) — el `docker-compose.yml` levanta el servidor, no el realm.
- Contract tests automatizados contra `openapi.yaml` (se deja como backlog técnico, ver `01-technical-backlog.md`, ítem `TEST-API-001`).

Ninguno de estos faltantes es una regresión: son exactamente los siguientes milestones, ya priorizados en la Sección 5 de la tarea encomendada y en `01-technical-backlog.md`.

## 4. Dependencias técnicas

- **Java 21 LTS**, **Spring Boot 3.3.4** (versión exacta fijada en este ciclo; `17-dependencies.md` la marcaba como `TBD al bootstrap` — este es ese momento; no es una decisión de negocio, es la resolución de un TBD técnico explícitamente delegado a la implementación).
- **Gradle 8.9** (Kotlin DSL) — `17-dependencies.md` indicaba "Gradle preferido... TBD"; se fija Gradle como build tool de este ciclo en adelante.
- **PostgreSQL 16** (imagen Docker `postgres:16-alpine` para desarrollo local).
- **Flyway** (integrado vía `spring-boot-starter-data-jpa` + `org.flywaydb:flyway-database-postgresql`).
- **Spring Security OAuth2 Resource Server** (`spring-boot-starter-oauth2-resource-server`) contra Keycloak (imagen `quay.io/keycloak/keycloak:25.0` en modo `start-dev` para local).
- **Testcontainers** (`postgresql` module) para tests de integración con una base de datos real, no H2 — evita falsos positivos de compatibilidad SQL.
- **JUnit 5 + Mockito + Spring Boot Test + MockMvc**.

No se introduce ninguna dependencia fuera de lo ya documentado en `07-backend-architecture.md`/`17-dependencies.md`.

## 5. Orden recomendado de implementación

Se seguido el orden solicitado explícitamente en la tarea de kickoff, que es consistente con `12-traceability.md` y con la regla de "vertical slice primero":

1. Backend base + PostgreSQL + Flyway — **hecho en este ciclo**.
2. Seguridad/Keycloak (resource server) — **hecho en este ciclo** (config; realm/clientes reales quedan pendientes, ver §3).
3. User/profile synchronization — **hecho en este ciclo**.
4. Reminder CRUD — **parcial en este ciclo**: create + list + complete. Edit/delete quedan para el siguiente incremento inmediato (mismo módulo, sin bloqueadores nuevos).
5. Optimistic locking — **hecho en este ciclo** (aplicado a `complete`; se reutiliza igual en `edit` cuando se implemente).
6. Sharing/Invitation — pendiente (siguiente milestone grande).
7. ReminderShare/revocation — pendiente (parte del mismo milestone que 6).
8. Device push tokens — pendiente.
9. Push notifications — pendiente.
10. Account deletion — pendiente.
11. Android — pendiente (no debe empezar antes de estabilizar el backend completo del vertical slice base, por instrucción explícita).
12. iOS — pendiente.
13. Web — pendiente.
14. CI/CD/staging hardening — pendiente.

No se justifica ningún cambio a este orden: la estructura real del repositorio (vacía) no impone ninguna restricción distinta a la ya definida.

## 6. Riesgos técnicos

- **Riesgo cerrado (2026-08-15):** el código se compiló y ejecutó en un entorno real (JDK 21 + Docker), confirmando la revisión estática de los ciclos 1–2 sin errores de sintaxis/imports/tipos. Ver §11 (Validación) y `02-validation-report.md` §8.
- El wrapper de Gradle (`gradlew`/`gradle-wrapper.jar`) fue generado en este ciclo (`gradle wrapper --gradle-version 8.9`, usando un binario de Gradle 8.9 temporal solo para ese paso) y ya está commiteado en el repositorio; ningún desarrollador necesita repetir este paso.
- El realm de Keycloak no está preconfigurado; sin él, el resource server no tiene un `issuer-uri` válido contra el cual validar tokens. Es un paso manual de un desarrollador con Keycloak corriendo.
- La política exacta de `AC-004` ("otro usuario recibe 404 o 403 según política definida") se resolvió como **404 uniforme** para cualquier recordatorio no accesible (propio o no), consistente con la descripción ya existente de `NotFound` en `openapi.yaml` ("never distinguishes the two, to avoid leaking existence"). No es una decisión nueva, es la aplicación directa de una regla ya documentada.
- El mapeo `USER.id = Keycloak `sub` claim` (ambos UUID) es una decisión de implementación no explicitada en `09-data-model.md`. Se documenta aquí como nota de implementación, no como cambio al modelo de datos aprobado — evita una columna `keycloak_id` redundante y mantiene una única fuente de identidad.

## 7. Primer milestone

**Milestone 1: "Reminder core, sin compartir."** — **MILESTONE_1_STATUS: READY** (2026-08-15, ver `docs/development/03-milestone-1-gate.md`).
Alcance: autenticación vía Keycloak, sincronización de usuario, crear/listar/completar recordatorios propios, con bloqueo optimista y manejo de errores uniforme.
Criterio de cierre real cumplido: tests unitarios y de integración en verde contra una base de datos real (Testcontainers), 19/19, `./gradlew build` `BUILD SUCCESSFUL` — ver §11 y `02-validation-report.md` §8.

**Milestone 2 (siguiente, no iniciado):** editar/eliminar recordatorio (cierra el CRUD completo, US-005/US-007) + inicio del módulo `sharing` (invitaciones).

## 8. Definition of Done aplicable a este ciclo

Se aplicó `14-definition-of-done.md` en lo que el entorno permite verificar:

| Criterio DoD | Estado en este ciclo |
|---|---|
| Cumple criterios de aceptación | Sí — verificado con tests ejecutados realmente (19/19 en verde, `02-validation-report.md` §8.5). |
| Código revisado | No aplica todavía (no hay proceso de PR sin repositorio remoto). |
| Pruebas unitarias/integración relevantes | Escritas y **ejecutadas realmente** (`./gradlew clean test`, JDK 21 + Docker/Testcontainers reales). |
| Compilación limpia | **Verificada** (`./gradlew build` → `BUILD SUCCESSFUL`, jar ejecutable generado). |
| Análisis estático limpio | No configurado todavía (queda en backlog, `DEVOPS` — SAST/lint). |
| No existen secretos | Verificado por inspección manual: `application.yml` no contiene credenciales reales, usa variables de entorno. |
| Dependencias revisadas | Sí — todas ya estaban documentadas en `07-backend-architecture.md`/`17-dependencies.md`; ninguna nueva sin justificar. |
| Documentación/API actualizada | `openapi.yaml` ya describía este contrato desde el gate anterior; no requirió cambios. |
| Migraciones incluidas | Sí, `V1__init_schema.sql`. |
| Logs sin información sensible | Verificado por inspección: no se loguean tokens ni contraseñas. |
| Pruebas de autorización para recursos | Incluidas en `ReminderControllerIntegrationTest` (acceso a recordatorio ajeno → 404). |
| CI verde | No aplica (no existe CI todavía). |
| Evidencia de QA disponible | Este documento + `01-technical-backlog.md` + resultado de validación en §11. |
| Ticket enlazado al PR | No aplica (no hay tracker conectado; backlog vive en Markdown). |

**Conclusión:** el vertical slice cumple el DoD documental, de diseño, y de ejecución (compilación/tests reales en verde, 2026-08-15). Queda pendiente únicamente CI (no existe todavía como pipeline automatizado) y análisis estático (backlog `DEVOPS-002`).

## 9. Relación con requisitos / UC / AC

| Requisito | UC | AC | Implementado en este ciclo |
|---|---|---|---|
| FR-001/FR-002 (registro/autenticación) | UC-01/UC-02 | AC-001/AC-002 | Delegado a Keycloak; el backend implementa el lado resource-server (validación de token) y la sincronización de `USER`. El flujo de registro/login en sí ocurre en Keycloak, fuera del backend. |
| FR-004 (recordatorios: crear/consultar/completar) | UC-03/UC-04 | AC-003/AC-004/AC-005 | Sí — `POST /reminders`, `GET /reminders`, `GET /reminders/{id}`, `POST /reminders/{id}/complete`. |
| FR-004 (editar) | — | AC-004b | No — siguiente milestone. |
| NFR-001 (autorización por recurso) | — | AC-006 (parcial) | Sí — solo el propietario accede a su recordatorio; sin propiedad → 404. |
| NFR-006 (manejo uniforme de errores) | — | AC-006 | Sí — `Error` schema (`code`/`message`/`traceId`), sin stack traces. |
| Bloqueo optimista (`RECOMMENDATION`, `09-data-model.md`) | — | AC-004b/AC-005 | Sí, en `complete`. |

## 10. Qué NO debe modificarse porque ya está decidido

Por instrucción explícita de esta fase, y confirmado en `32-v1-development-gate-audit.md`:

- Ninguno de los ADR (`22-decision-log.md`) ni de las decisiones `DEC-001` a `DEC-015` (`28-v1-decision-pack.md`).
- El lifecycle de `INVITATION` (`PENDING/ACCEPTED/REJECTED/EXPIRED/CANCELLED`, sin `REVOKED`) — no se toca en este ciclo porque `sharing` ni siquiera se implementa todavía.
- El proveedor de identidad (Keycloak) y el patrón resource-server-only (no login propio).
- El proveedor de push unificado (FCM) — no se toca en este ciclo.
- El estado único global de `Reminder` (`PENDING`/`COMPLETED`, sin estado por colaborador).
- El contrato `openapi.yaml` — el código de este ciclo se ajustó al contrato existente; no se modificó el contrato para que el código encajara.

## 11. Validación de este ciclo

Ver `docs/development/02-validation-report.md` para el detalle completo, comandos exactos y resultado de cada verificación. Resumen: el código se revisó manualmente línea por línea contra el contrato (`openapi.yaml`, `09-data-model.md`, `11-auth-security.md`) en los ciclos 1–2, y en el ciclo 3 (2026-08-15) se compiló, testeó (`./gradlew clean test`, 19/19 en verde) y empaquetó (`./gradlew build`) realmente contra JDK 21 + Docker/Testcontainers reales, más una validación manual de `bootRun` contra PostgreSQL y Keycloak reales. `MILESTONE_1_STATUS: READY` (`docs/development/03-milestone-1-gate.md`).
