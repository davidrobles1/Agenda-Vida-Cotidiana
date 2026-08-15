# Milestone 1 — Backend Vertical Slice Gate

Fecha: 2026-08-09. Fuente de verdad: `Documentacion/` (V1_READY, ver `32-v1-development-gate-audit.md`), `docs/development/00-development-baseline.md`, `01-technical-backlog.md`, `02-validation-report.md`, `Documentacion/openapi/openapi.yaml`. Ninguna decisión aprobada fue modificada en este ciclo; ningún endpoint ni regla de negocio fue inventado.

## Scope

Vertical slice mínimo definido en el kickoff de desarrollo: **Auth (resource server) → User sync → Create Reminder → List Reminders → Complete Reminder**, con manejo uniforme de errores y bloqueo optimista opcional. Explícitamente **fuera de alcance** de Milestone 1 (no implementado, no se debía implementar todavía): editar (`PATCH`)/eliminar (`DELETE`) recordatorio, sharing/invitations, push notifications, eliminación de cuenta, Android/iOS/Web, CI/CD.

En este ciclo de validación se corrigieron 3 errores reales encontrados por inspección (ver `02-validation-report.md` §7.5) y se amplió la cobertura de tests (§7.6); no se agregó ninguna funcionalidad nueva.

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

Todos **escritos y revisados estáticamente**; **ninguno ejecutado realmente** en este entorno (sin JDK 21/Docker, ver `02-validation-report.md` §7.1).

| Clase | Casos | Cubre |
|---|---|---|
| `reminder.application.ReminderServiceTest` | 8 | Crear, acceso propietario, acceso ajeno → `NotFoundException`, id inexistente → `NotFoundException`, completar (toggle ida/vuelta), version correcta, version incorrecta → `VersionConflictException`, version omitida, no-propietario → `NotFoundException`. |
| `reminder.api.ReminderControllerIntegrationTest` | 8 | Flujo feliz crear/listar/completar, validación 400 (título vacío), 404 uniforme para ajeno (nunca 403), 409 por version incorrecta, version omitida sin chequeo, 404 por id inexistente, 401 con envoltorio `Error` completo. |
| `user.api.UserControllerIntegrationTest` | 3 | Creación de `USER` en el primer request autenticado, actualización (no duplicación) al cambiar el email del claim, 401 uniforme sin autenticación. |

**Resultado real de ejecución: NOT_EXECUTED.** No se declara ningún test como PASS.

## Security

- Resource server OAuth2/OIDC contra Keycloak, sin login propio (DEC-004/ADR-008) — confirmado por inspección de `SecurityConfig`.
- 401 (sin token/token inválido): ahora con envoltorio `Error` uniforme (corregido en este ciclo, ver hallazgo 1 en `02-validation-report.md`).
- 403: reservado para escenarios que este milestone todavía no puede producir (no existe `COLLABORATOR`); el `AccessDeniedHandler`/`GlobalExceptionHandler` ya están listos para cuando exista.
- 404: acceso a un recordatorio ajeno y a un recordatorio inexistente responden igual (`REMINDER_NOT_FOUND`), sin distinguir — cumple AC-004/SEC-001.
- 409: conflicto de versión (`REMINDER_VERSION_CONFLICT`) verificado con test.
- Filtros de seguridad (`TraceIdFilter`, `UserSyncFilter`) corregidos para ejecutarse exactamente una vez por request (hallazgo 2) y para no dejar escapar excepciones sin el envoltorio `Error` (hallazgo 3).

## Known Limitations

Solo limitaciones reales, ninguna oculta:

1. **No se ejecutó ningún build ni test real** en esta sesión — el entorno no tiene JDK 21 (solo JRE 11 headless, sin `javac`), no tiene Gradle, no tiene Docker, y no tiene acceso de red a Maven Central/`services.gradle.org`. Ver comandos exactos en `02-validation-report.md` §4.
2. El wrapper de Gradle (`gradlew`) no existe todavía en el repositorio — debe generarse una vez con `gradle wrapper --gradle-version 8.9` en un equipo con Gradle instalado.
3. No existe realm de Keycloak configurado — `docker-compose.yml` levanta el servidor, no el realm/cliente `vida-cotidiana` (tarea manual, `INFRA-002`).
4. `git commit` no se pudo completar en esta sesión por una restricción del punto de montaje sobre operaciones de archivo internas de `.git/` — ver `02-validation-report.md` §5 para el comando de recuperación en el equipo del usuario.
5. `PATCH /api/v1/reminders/{id}` está definido en `openapi.yaml` pero no implementado — por diseño, es el primer ítem de Milestone 2, no un defecto de Milestone 1.

## Gate Result

**MILESTONE_1_STATUS: NOT_READY**

**BUILD_STATUS: NOT_EXECUTED**
**TEST_STATUS: NOT_EXECUTED**
**BLOCKERS: 1**

El único bloqueador es de entorno, no de diseño ni de código: **no fue posible ejecutar `./gradlew clean test`/`./gradlew build` en esta sesión** (sin JDK 21, Gradle ni Docker disponibles, y sin acceso de red para instalarlos). El código fue revisado exhaustivamente por inspección estática (coherencia OpenAPI↔Controller↔DTO↔Service↔Entity↔Flyway, balance sintáctico, resolución de imports, tipos y constraints), y se corrigieron 3 errores reales encontrados durante esa revisión (ver `02-validation-report.md` §7.5). No se declara `MILESTONE_1_STATUS: READY` porque eso exigiría haber visto realmente `BUILD SUCCESSFUL` y los tests en verde, y esta sesión no puede producir esa evidencia — solo un desarrollador con un entorno completo puede.

## Recommendation

**No procede iniciar Milestone 2 todavía.** Antes de continuar:

1. En un equipo con JDK 21 + Docker, ejecutar los comandos de `02-validation-report.md` §4 (`gradle wrapper`, `./gradlew clean test`, `./gradlew build`, `docker compose up`, prueba manual de `bootRun`).
2. Si todo pasa en verde: actualizar `MILESTONE_1_STATUS` a `READY` en este mismo documento (sin reabrir ninguna otra sección) y recién entonces **proceder a Milestone 2** (editar/eliminar recordatorio + inicio de `sharing`, ver `01-technical-backlog.md` BE-014 en adelante).
3. Si algo falla: reportar el error real exacto (stack trace/mensaje del compilador o del test runner) para corregirlo — no se debe adivinar la causa sin verlo.
