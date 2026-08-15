# 01 — Backlog Técnico de Implementación V1

Traducción de la documentación aprobada (`03-prd.md`, `04-use-cases.md`, `13-acceptance.md`, `openapi/openapi.yaml`, `09-data-model.md`, `26-v1-backlog.md`) en tareas técnicas ejecutables. Ninguna tarea introduce una funcionalidad no documentada. Cada tarea referencia su origen (`FR`/`UC`/`AC`/`US`).

Estado: `DONE` (código completo **y** verificado con build/test real), `PARTIAL` (código completo, verificado solo estáticamente — sin build/test real ejecutado, ver `02-validation-report.md`), `BLOCKED` (no se puede completar por una dependencia externa), `TODO` (siguiente).

**Nota de esta actualización (2026-08-15, ver `03-milestone-1-gate.md`, `MILESTONE_1_STATUS: READY`):** `./gradlew clean test` y `./gradlew build` se ejecutaron realmente (JDK 21 + Docker reales) con resultado `BUILD SUCCESSFUL` y 19/19 tests en verde (`02-validation-report.md` §8). Los ítems de backend de Milestone 1 (`BE-001` a `BE-013`, `BE-030` a `BE-032`) pasan de `PARTIAL` a `DONE` en esta actualización — código completo **y** verificado con build/test real, no solo estáticamente.

**Nota adicional (mismo día, tras la migración de build tool a Maven, ADR-013):** con el gate de Milestone 1 confirmado `READY` bajo Maven (`02-validation-report.md` §9, addendum en `03-milestone-1-gate.md`), arrancó Milestone 2 con `BE-014` (`PATCH /reminders/{id}`). `./mvnw clean test`: 26/26 en verde (19 heredados + 7 nuevos de `BE-014`); `./mvnw clean package`: `BUILD SUCCESS`.

## Backend (Java 21 / Spring Boot 3.3.4 / Modular Monolith)

| ID | Tarea | Origen | Estado | Clases / Tests |
|---|---|---|---|---|
| BE-001 | Bootstrap del proyecto backend (**Maven**, `pom.xml` + wrapper `./mvnw`; originalmente Gradle, migrado 2026-08-15 por decisión del Product Owner, ver ADR-013), estructura de paquetes por módulo (`shared/identity/user/reminder/sharing/notification/audit`) | `07-backend-architecture.md` | DONE | `pom.xml`, `mvnw`/`mvnw.cmd`/`.mvn/wrapper/` |
| BE-002 | `docker-compose.yml` local (PostgreSQL 16 + Keycloak 25 dev) | `18-dev-environment.md` | DONE | `docker-compose.yml` (Keycloak levantado vía `docker compose`; Postgres del compose validado en un puerto alternativo por conflicto de puerto local, ver `02-validation-report.md` §8.7) |
| BE-003 | Migración Flyway `V1__init_schema.sql` (`users`, `reminders`) | `09-data-model.md` | DONE | `V1__init_schema.sql` (aplicada realmente por Flyway contra un PostgreSQL 16 real; Hibernate validó el esquema sin discrepancias, `02-validation-report.md` §8.7) |
| BE-004 | Resource server OAuth2/OIDC (Spring Security, sin login propio) | `11-auth-security.md`, ADR-008 | DONE | `SecurityConfig`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler` (nuevas en este ciclo) |
| BE-005 | Sincronización de `USER` desde JWT (upsert por request autenticado) | UC-01/UC-02, `09-data-model.md` | DONE | `UserSyncFilter`, `UserSyncService`; `UserControllerIntegrationTest` (nueva en este ciclo) |
| BE-006 | `GET /me` | `openapi.yaml` | DONE | `UserController`; `UserControllerIntegrationTest` |
| BE-007 | `POST /reminders` (crear) | UC-03, AC-003, FR-004 | DONE | `ReminderController.create`; `ReminderControllerIntegrationTest.createListAndCompleteReminder_happyPath`, `.createReminder_blankTitleIsRejected` |
| BE-008 | `GET /reminders` (listar propios, paginado) | AC-004, FR-003/FR-004 | DONE | `ReminderController.list`; `ReminderControllerIntegrationTest.createListAndCompleteReminder_happyPath` |
| BE-009 | `GET /reminders/{id}` | AC-004 | DONE | `ReminderController.get`; `.getReminder_ownedByAnotherUserReturnsNotFound_neverForbidden`, `.getReminder_missingIdReturnsNotFound` |
| BE-010 | `POST /reminders/{id}/complete` (bloqueo optimista opcional) | UC-04, AC-005 | DONE | `ReminderController.complete`; `.completeReminder_mismatchedVersionReturns409`, `.completeReminder_omittedVersionSkipsConcurrencyCheck`; `ReminderServiceTest` (7 casos) |
| BE-011 | Manejo uniforme de errores (`Error` schema, `traceId`) | AC-006, NFR-006 | DONE | `GlobalExceptionHandler` + (nuevo) `RestAuthenticationEntryPoint`/`RestAccessDeniedHandler` para errores a nivel de filtro de seguridad, que antes NO pasaban por `GlobalExceptionHandler` (ver `03-milestone-1-gate.md` §Correcciones, hallazgo 1) |
| BE-012 | Tests unitarios `ReminderService` | `20-testing-qa.md` | DONE | `ReminderServiceTest` (8 tests: crear, acceso propietario/ajeno, completar, versión correcta/incorrecta/omitida) |
| BE-013 | Tests de integración (Testcontainers + MockMvc): crear/listar/completar/acceso ajeno/conflicto de versión | `20-testing-qa.md` | DONE | `ReminderControllerIntegrationTest` (8 tests), `UserControllerIntegrationTest` (3 tests, nueva) |
| BE-030 | Fix: filtros `TraceIdFilter`/`UserSyncFilter` se ejecutaban dos veces por request (auto-registro genérico de Spring Boot + registro explícito en `SecurityConfig`) | Hallazgo propio de esta validación | DONE | `SecurityConfig` (`FilterRegistrationBean` deshabilitando el auto-registro) |
| BE-031 | Fix: 401/403 generados dentro de la cadena de Spring Security no llevaban el envoltorio `Error` (`code`/`message`/`traceId`) | Hallazgo propio de esta validación, AC-006 | DONE | `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`; `ReminderControllerIntegrationTest.reminderEndpoints_requireAuthentication`, `UserControllerIntegrationTest.getCurrentUser_requiresAuthentication` |
| BE-032 | Fix: excepción no controlada dentro de `UserSyncFilter` (p. ej. token sin claim `email`) escapaba de `GlobalExceptionHandler` | Hallazgo propio de esta validación, AC-006 | DONE | `UserSyncFilter` (try/catch + respuesta `Error` uniforme) |
| BE-014 | `PATCH /reminders/{id}` (editar, bloqueo optimista obligatorio) | AC-004b, US-005 | DONE (2026-08-15, Milestone 2) | `ReminderController.update`, `ReminderService.edit`, `Reminder.applyEdit`, `UpdateReminderRequest`; `ReminderServiceTest` (+3 casos), `ReminderControllerIntegrationTest` (+4 casos). `./mvnw clean test`: 26/26 en verde; `./mvnw clean package`: `BUILD SUCCESS`. |
| BE-015 | `DELETE /reminders/{id}` (cascada + notificación a colaboradores activos) | UC-05, AC-013, DEC-002 | TODO (depende de BE-020 para colaboradores) |
| BE-016 | Migración Flyway `V2__sharing.sql` (`invitations`, `reminder_shares`) | `09-data-model.md` | TODO |
| BE-017 | `POST /reminders/{id}/shares` (crear invitación, sin enumeración, rate limit) | UC-07, AC-007, SEC-001 | TODO |
| BE-018 | `GET /reminders/{id}/shares`, `GET /me/invitations` (paginado) | `openapi.yaml` | TODO |
| BE-019 | `POST /invitations/{id}/accept` / `/reject` (transición atómica condicional) | UC-08/UC-09, AC-008/AC-009, HIG-003 | TODO |
| BE-020 | `DELETE /invitations/{invitationId}` (cancelar, UC-14) | AC-017 | TODO |
| BE-021 | `DELETE /reminders/{id}/shares/{shareId}` (revocar, efecto inmediato) | UC-10, AC-010 | TODO |
| BE-022 | Autorización `OWNER`/`COLLABORATOR` transversal sobre recordatorios compartidos | `11-auth-security.md` | TODO (depende de BE-016..021) |
| BE-023 | Migración Flyway `V3__push.sql` (`device_push_tokens`) | `09-data-model.md` | TODO |
| BE-024 | `GET/POST /me/devices`, `DELETE /me/devices/{deviceId}` | UC-12, AC-014, FR-012 | TODO |
| BE-025 | Puerto `PushNotificationSender` + adapter FCM | ADR-007, DEC-010 | TODO |
| BE-026 | Eventos de push (invitación recibida/aceptada/rechazada/cancelada, cambios, revocación, eliminación) | UC-11, AC-012, FR-011 | TODO (depende de BE-017..021, BE-025) |
| BE-027 | `DELETE /me` + `PENDING_DELETION` + job de purga a 30 días | UC-13, AC-015, DEC-015 | TODO |
| BE-028 | Job de purga de invitaciones sin cuenta asociada (retención corta) | AC-016, DEC-015 (A') | TODO |
| BE-029 | Auditoría de eventos de seguridad (creación/resolución de invitación, revocación) | `11-auth-security.md` §Auditoría | TODO |
| TEST-API-001 | Contract tests: validar respuestas reales contra `openapi.yaml` (schema `Error`/`PageMeta`) | `20-testing-qa.md` | TODO |
| DEVOPS-001 | Rate limiting (`429`) sobre creación de invitaciones | SEC-001 | TODO (junto con BE-017) |
| DEVOPS-002 | SAST/lint básico local (sin CI todavía) | `14-definition-of-done.md` | TODO |

## Android (Kotlin / Jetpack Compose)

| ID | Tarea | Origen | Estado |
|---|---|---|---|
| AND-001 | Bootstrap proyecto (Gradle, Compose, Hilt, Coroutines/Flow) | `08-android-architecture.md` | TODO — no iniciar antes de estabilizar BE-001..013 |
| AND-002 | Autenticación Keycloak (Authorization Code + PKCE, AppAuth) | UC-01/UC-02 | TODO |
| AND-003 | Reminders: crear/listar/completar (consume BE-007..010) | UC-03/UC-04 | TODO |
| AND-004 | Sharing/Invitations UI | UC-07..UC-10, UC-14 | TODO |
| AND-005 | Registro de push (FCM) | UC-12 | TODO |

## iOS (Swift / SwiftUI)

| ID | Tarea | Origen | Estado |
|---|---|---|---|
| IOS-001 | Bootstrap proyecto (SwiftUI, Swift Concurrency) | `08b-ios-architecture.md` | TODO — no iniciar antes de estabilizar backend |
| IOS-002 | Autenticación Keycloak (AppAuth-iOS) | UC-01/UC-02 | TODO |
| IOS-003 | Reminders: crear/listar/completar | UC-03/UC-04 | TODO |
| IOS-004 | Sharing/Invitations UI | UC-07..UC-10, UC-14 | TODO |
| IOS-005 | Registro de push (FCM vía puente APNs) | UC-12 | TODO |

## Web (React + TypeScript SPA)

| ID | Tarea | Origen | Estado |
|---|---|---|---|
| WEB-001 | Bootstrap proyecto (Vite/React/TS) | `08c-web-architecture.md` | TODO — no iniciar antes de estabilizar backend |
| WEB-002 | Autenticación Keycloak (OIDC/PKCE), patrón de almacenamiento de token (pendiente técnico ya documentado) | UC-01/UC-02, `11-auth-security.md` | TODO |
| WEB-003 | Reminders: crear/listar/completar | UC-03/UC-04 | TODO |
| WEB-004 | Sharing/Invitations UI | UC-07..UC-10, UC-14 | TODO |
| WEB-005 | Registro de push (Web Push), según alcance definido para V1 | UC-12 | TODO |

## Infraestructura

| ID | Tarea | Origen | Estado |
|---|---|---|---|
| INFRA-001 | `docker-compose.yml` local (Postgres + Keycloak) | `18-dev-environment.md` | DONE |
| INFRA-002 | Exportar/versionar realm de Keycloak (`vida-cotidiana`) para bootstrap reproducible | `11-auth-security.md` | TODO |
| INFRA-003 | CI (lint, tests, SAST, dependency scan, secret scan, build) | `19-cicd.md` | TODO |
| INFRA-004 | Entorno `staging` en AWS (RDS, despliegue) | `19-cicd.md`, DEC-008 | TODO |
| INFRA-005 | Amazon SES (adapter de email para invitaciones) | DEC-009 | TODO (junto con BE-017) |

## No hacer todavía (explícito, ver `26-v1-backlog.md` y reglas de la fase de kickoff)

Sharing/push/Android/iOS/Web no se inician hasta estabilizar el vertical slice base (BE-001..013) con build/tests reales en verde en un entorno con JDK 21 + Docker — ver `02-validation-report.md`.
