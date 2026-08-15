# Milestone 1 — Backend Vertical Slice Gate

Fecha inicial: 2026-08-09 (ciclos 1–2, revisión estática, sin entorno real). **Actualizado 2026-08-15 (ciclo 3): ejecución real (`02-validation-report.md` §8), gate cerrado `READY`.** Fuente de verdad: `Documentacion/` (V1_READY, ver `32-v1-development-gate-audit.md`), `docs/development/00-development-baseline.md`, `01-technical-backlog.md`, `02-validation-report.md`, `Documentacion/openapi/openapi.yaml`. Ninguna decisión aprobada fue modificada en ningún ciclo; ningún endpoint ni regla de negocio fue inventado.

## Scope

Vertical slice mínimo definido en el kickoff de desarrollo: **Auth (resource server) → User sync → Create Reminder → List Reminders → Complete Reminder**, con manejo uniforme de errores y bloqueo optimista opcional. Explícitamente **fuera de alcance** de Milestone 1 (no implementado, no se debía implementar todavía): editar (`PATCH`)/eliminar (`DELETE`) recordatorio, sharing/invitations, push notifications, eliminación de cuenta, Android/iOS/Web, CI/CD.

En el ciclo 2 de validación se corrigieron 3 errores reales encontrados por inspección (ver `02-validation-report.md` §7.5) y se amplió la cobertura de tests (§7.6). En el ciclo 3 se ejecutó el build/test real por primera vez, se encontró y corrigió una incompatibilidad real `docker-java`/Docker Engine 29+ (`02-validation-report.md` §8.4), y se confirmaron los 19 tests en verde. Ninguna funcionalidad nueva fue agregada en ningún ciclo.

## Requirements

| FR | Cubierto | Nota |
|---|---|---|
| FR-001/FR-002 (registro/autenticación) | Parcial | Delegado a Keycloak (fuera del backend); el backend cubre el lado resource-server + sincronización de `USER`. |
| FR-004 (recordatorios: crear/consultar/completar) | Sí | Crear, listar, consultar por id, completar/revertir. |
| FR-004 (editar) | No | `PATCH` fuera de alcance de este milestone (BE-014). |
| NFR-001 (autorización por recurso) | Sí (alcance actual: solo `OWNER`) | `COLLABORATOR` no existe todavía (Milestone 2). |
| NFR-006 (manejo uniforme de errores) | Sí | Incluye ahora los 401/403 generados dentro de la cadena de Spring Security (corrección de este ciclo). |

## Use Cases

- UC-01/UC-02 (registro/login): cubiertos del lado del backend como resource server; el flujo de Keycloak en sí es externo.
- UC-03 (crear recordatorio): cubierto.
- UC-04 (completar recordatorio): cubierto, incluyendo bloqueo optimista opcional.
- UC-05, UC-06 a UC-14: no cubiertos en este milestone (fuera de alcance por diseño).

## API

Implementados y verificados contra `openapi.yaml` (ver `02-validation-report.md` §7.2):

- `GET /api/v1/me`
- `POST /api/v1/reminders`
- `GET /api/v1/reminders` (paginado)
- `GET /api/v1/reminders/{id}`
- `POST /api/v1/reminders/{id}/complete`

**No implementado:** `PATCH /api/v1/reminders/{id}` (definido en el contrato, pero deliberadamente fuera de Milestone 1 — ver `02-validation-report.md` §7.3 para la aclaración explícita de por qué no se dio por hecho).

## Data Model

- `users` (Flyway `V1__init_schema.sql`): `id`, `email` (UK), `username` (UK, nullable), `status`, `deletion_status` + campos de purga, timestamps. Corresponde a la entidad `User`.
- `reminders`: `id`, `owner_user_id` (FK), `title`, `description`, `due_at`, `status` (CHECK `PENDING`/`COMPLETED`), `version` (bloqueo optimista, `@Version`), timestamps. Corresponde a la entidad `Reminder`.
- Índice `ix_reminders_owner_user_id`. Constraints de unicidad en `users.email`/`users.username`.
- `invitations`, `reminder_shares`, `device_push_tokens`: no creadas todavía (correcto — no hay código que las use aún; se crean en `V2`/`V3` cuando se implementen sharing/push, ver `01-technical-backlog.md` BE-016/BE-023).

## Tests

Escritos, revisados estáticamente en los ciclos 1–2, y **ejecutados realmente en el ciclo 3** (`02-validation-report.md` §8) contra JDK 21 + Docker reales, con Testcontainers levantando un PostgreSQL 16 real.

| Clase | Casos | Cubre |
|---|---|---|
| `reminder.application.ReminderServiceTest` | 9 | Crear, acceso propietario, acceso ajeno → `NotFoundException`, id inexistente → `NotFoundException`, completar (toggle ida/vuelta), version correcta, version incorrecta → `VersionConflictException`, version omitida, no-propietario → `NotFoundException`. |
| `reminder.api.ReminderControllerIntegrationTest` | 7 | Flujo feliz crear/listar/completar, validación 400 (título vacío), 404 uniforme para ajeno (nunca 403), 409 por version incorrecta, version omitida sin chequeo, 404 por id inexistente, 401 con envoltorio `Error` completo. |
| `user.api.UserControllerIntegrationTest` | 3 | Creación de `USER` en el primer request autenticado, actualización (no duplicación) al cambiar el email del claim, 401 uniforme sin autenticación. |
| **Total** | **19** | |

**Resultado real de ejecución: `./gradlew clean test` → BUILD SUCCESSFUL, 19/19 PASSED, 0 failures, 0 errors** (visto realmente el 2026-08-15, `02-validation-report.md` §8.5). `./gradlew build` también `BUILD SUCCESSFUL` (§8.6), generando el jar ejecutable. Adicionalmente se validó `bootRun` manual contra un PostgreSQL 16 real (Flyway aplicó la migración, Hibernate validó el esquema) y Keycloak levantado vía `docker compose`, confirmando `GET /actuator/health` → `200` y `401` uniforme sin token en `/api/v1/me` y `/api/v1/reminders` (§8.7).

## Security

- Resource server OAuth2/OIDC contra Keycloak, sin login propio (DEC-004/ADR-008) — confirmado por inspección de `SecurityConfig`.
- 401 (sin token/token inválido): ahora con envoltorio `Error` uniforme (corregido en este ciclo, ver hallazgo 1 en `02-validation-report.md`).
- 403: reservado para escenarios que este milestone todavía no puede producir (no existe `COLLABORATOR`); el `AccessDeniedHandler`/`GlobalExceptionHandler` ya están listos para cuando exista.
- 404: acceso a un recordatorio ajeno y a un recordatorio inexistente responden igual (`REMINDER_NOT_FOUND`), sin distinguir — cumple AC-004/SEC-001.
- 409: conflicto de versión (`REMINDER_VERSION_CONFLICT`) verificado con test.
- Filtros de seguridad (`TraceIdFilter`, `UserSyncFilter`) corregidos para ejecutarse exactamente una vez por request (hallazgo 2) y para no dejar escapar excepciones sin el envoltorio `Error` (hallazgo 3).

## Known Limitations

Solo limitaciones reales, ninguna oculta:

1. No se configuró el realm de Keycloak (`vida-cotidiana`) en esta validación — `docker-compose.yml` levanta el servidor, no el realm/cliente (tarea manual, `INFRA-002`). Por eso no se validó un login/token real de extremo a extremo; sí se validó que el resource server arranca y protege los endpoints correctamente sin token (`02-validation-report.md` §8.7).
2. El `docker-compose.yml` versionado no se pudo levantar tal cual en la máquina de esta validación porque el puerto `5432` ya está ocupado por un PostgreSQL 17 nativo preexistente en ese equipo, ajeno al proyecto. No es un defecto del compose ni del código — se documenta como hecho del entorno local del validador (`02-validation-report.md` §8.7); cualquier otro desarrollador sin ese conflicto de puerto podrá levantar `docker compose up -d postgres keycloak` sin ajustes.
3. `PATCH /api/v1/reminders/{id}` está definido en `openapi.yaml` pero no implementado — por diseño, es el primer ítem de Milestone 2, no un defecto de Milestone 1.
4. Se fijó `testcontainers-bom` a `1.21.4` (antes `1.20.1`) y se añadió `backend/src/test/resources/docker-java.properties` (`api.version=1.44`) para resolver una incompatibilidad real entre `docker-java` y Docker Engine 29+ descubierta durante esta validación (`02-validation-report.md` §8.4). Es un ajuste de versión/configuración de herramienta de test, no un cambio de contrato ni de decisión aprobada.

## Gate Result

**MILESTONE_1_STATUS: READY**

**BUILD_STATUS: SUCCESSFUL**
**TEST_STATUS: PASSED (19/19)**
**BLOCKERS: 0**

Ejecutado y visto realmente el 2026-08-15 (`02-validation-report.md` §8): `./gradlew clean test` → `BUILD SUCCESSFUL`, 19 tests, 0 failures, 0 errors, incluyendo los dos test classes de integración con Testcontainers levantando un PostgreSQL 16 real vía Docker. `./gradlew build` → `BUILD SUCCESSFUL`, jar ejecutable generado. Validación manual adicional (`bootRun` contra Postgres real + Keycloak real vía `docker compose`) confirmó arranque limpio, migración Flyway aplicada, validación de esquema Hibernate sin discrepancias, `/actuator/health` en `200`, y el envoltorio `Error` uniforme en `401` sin token. No se simuló ningún resultado; no se declaró nada como PASS sin haberlo visto ejecutar.

## Recommendation

**Milestone 1 cerrado. Procede iniciar Milestone 2** (editar/eliminar recordatorio + inicio de `sharing`, ver `01-technical-backlog.md` BE-014 en adelante), siguiendo el mismo estándar de esta validación: escribir el código, ejecutar el build/test real antes de declarar cualquier tarea `DONE`, y no modificar `openapi.yaml`/decisiones aprobadas salvo necesidad real y justificada.

Pendiente no bloqueante para cuando se necesite `bootRun`/pruebas exploratorias con Keycloak real: crear el realm `vida-cotidiana` y su cliente (`INFRA-002`, ver `docs/development/00-development-baseline.md` §3).

---

## Addendum (2026-08-15) — Migración de build tool Gradle → Maven

Antes de iniciar Milestone 2, el Product Owner decidió migrar el build del backend de Gradle a Maven (decisión de tooling, no de arquitectura — ver ADR-013 en `Documentacion/22-decision-log.md` y `02-validation-report.md` §9 para el detalle completo de la migración y su revalidación).

**Esto no invalida el resultado de este gate.** Toda la evidencia de arriba (`BUILD_STATUS: SUCCESSFUL`, `TEST_STATUS: PASSED (19/19)`) fue real y correcta *en el momento en que se generó, bajo Gradle*. Queda explícitamente como **evidencia histórica, superada por el cambio de herramienta** — no como un resultado falso ni retroactivamente inválido.

Se revalidó el mismo scope, con los mismos 19 test classes/casos (sin tocar lógica de negocio), bajo Maven:

- `./mvnw clean test` → `BUILD SUCCESS`, **19/19 tests, 0 failures, 0 errors** — verde a la primera, sin necesidad de repetir ningún fix.
- `./mvnw clean package` → `BUILD SUCCESS`, jar ejecutable generado.
- Validación manual (`./mvnw spring-boot:run` contra PostgreSQL real + Keycloak real): arranque limpio, Flyway aplicó la migración, `/actuator/health` → `200`, `401` uniforme sin token en `/api/v1/me` y `/api/v1/reminders` — idéntico al comportamiento validado bajo Gradle.

**MILESTONE_1_STATUS: READY (confirmado bajo Maven, 2026-08-15).**
**BUILD_STATUS: SUCCESSFUL (Maven).**
**TEST_STATUS: PASSED (19/19, Maven).**
**BLOCKERS: 0.**

A partir de este addendum, todos los comandos de desarrollo del backend usan `./mvnw` (ver `Documentacion/18-dev-environment.md`). `./gradlew` ya no existe en el repositorio.
