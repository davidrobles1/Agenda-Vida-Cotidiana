# 02 — Validation Report (V1 Kickoff, Vertical Slice 1)

Este documento registra, con honestidad, qué se pudo validar en el entorno de esta sesión y qué requiere ejecutarse en un entorno real antes de dar el milestone por cerrado. No se declara nada "hecho" simplemente porque el código fue escrito (regla explícita de la fase: "No marques una tarea como DONE solamente porque compile").

## 1. Limitaciones reales del entorno de esta sesión (verificadas por inspección directa)

| Herramienta requerida | Estado en este entorno | Evidencia |
|---|---|---|
| JDK 21 | **Ausente** | Solo `openjdk-11-jre-headless` (JRE, sin `javac`) instalado. `java -version` → `11.0.31`. |
| Gradle | **Ausente** | `gradle`/`gradlew` no disponibles; no se pudo generar el wrapper (requiere descargar un binario, ver §3). |
| Docker | **Ausente** | `docker` no es un comando disponible en el sandbox. |
| Acceso de red a repositorios de artefactos (Maven Central, `services.gradle.org`) | **Bloqueado** | `curl` a `services.gradle.org` y `repo.maven.apache.org` devuelve `403 Forbidden (blocked-by-allowlist)`. |
| Git (operaciones atómicas de archivo) | **Bloqueado en esta sesión** | `git init` funcionó, pero `git add -A`/`git commit` fallan: el punto de montaje de la carpeta de trabajo no permite `unlink`/rename de archivos internos de `.git` (`Operation not permitted`), dejando un `.git/index.lock` que tampoco se pudo eliminar desde esta sesión. Ver §5. |

**Conclusión:** no fue posible compilar, ejecutar, ni testear el backend en este entorno, ni completar la inicialización de Git. Esto se declara explícitamente como limitación de la sesión, no como resultado de validación del código.

## 2. Lo que sí se verificó en este entorno

- **Estructura y sintaxis por inspección manual línea por línea** de los 26 archivos Java del vertical slice contra la sintaxis de Java 21 / Spring Boot 3.3.x / Spring Security 6.x / Spring Data JPA / Testcontainers, comparando cada import y cada firma de método contra las APIs reales de esas versiones (conocimiento verificado, no generado a ciegas).
- **Balance estructural automático**: script que cuenta llaves `{}` y paréntesis `()` en los 26 archivos `.java` — todos balanceados, ninguna discrepancia.
- **Resolución de imports internos**: script que extrae cada `import com.vidacotidiana.*` de cada archivo y confirma que la clase referenciada existe en la ruta de paquete correspondiente — 100% resueltos, ningún import roto.
- **Coincidencia paquete↔directorio**: los 26 archivos declaran un `package` que coincide exactamente con su ruta de carpeta bajo `src/main/java`/`src/test/java`.
- **Validación de YAML**: `application.yml`, `application-test.yml` y `docker-compose.yml` parsean correctamente con un parser YAML real (`PyYAML`).
- **Consistencia esquema JPA ↔ migración Flyway**: se verificó columna por columna que las entidades `User`/`Reminder` (nombre, nulabilidad, tipo) coinciden con `V1__init_schema.sql`.
- **Consistencia API ↔ contrato**: se verificó que cada endpoint implementado (`GET /me`, `POST/GET /reminders`, `GET /reminders/{id}`, `POST /reminders/{id}/complete`) coincide en método, path, request/response con `Documentacion/openapi/openapi.yaml`, incluyendo el envoltorio de paginación (`page`/`size`/`totalElements`/`totalPages`/`items`) y el manejo de `version` (bloqueo optimista).
- **Corrección de un riesgo real encontrado durante esta verificación**: la migración inicial incluía `CREATE EXTENSION IF NOT EXISTS "pgcrypto"` para `gen_random_uuid()`; se confirmó que esa función es nativa de PostgreSQL 13+ (el motor objetivo es PostgreSQL 16) y se retiró la dependencia de la extensión para no introducir un fallo potencial si la imagen usada no incluyera el módulo `pgcrypto`.

## 3. Lo que NO se pudo verificar (requiere entorno real)

- Compilación real (`./gradlew compileJava compileTestJava`).
- Ejecución de los tests unitarios (`ReminderServiceTest`) y de integración (`ReminderControllerIntegrationTest`, que requiere Docker para levantar el contenedor Testcontainers de PostgreSQL).
- Arranque real de la aplicación (`./gradlew bootRun`) contra PostgreSQL/Keycloak reales.
- Validación de que Hibernate (`ddl-auto: validate`) acepta el esquema generado por Flyway sin discrepancias (revisado manualmente, no ejecutado).
- Comportamiento real del filtro de sincronización de usuario (`UserSyncFilter`) y del resource server contra un token JWT real emitido por Keycloak (los tests de integración usan `SecurityMockMvcRequestPostProcessors.jwt()`, que **no** ejercita la validación real de firma/emisor).
- Análisis estático (SAST/lint) — no configurado todavía, ver `01-technical-backlog.md` (`DEVOPS-002`).

## 4. Comandos exactos para completar la validación (a ejecutar por un desarrollador, JDK 21 + Docker)

```bash
cd backend
gradle wrapper --gradle-version 8.9   # una sola vez, genera gradlew/gradlew.bat/gradle-wrapper.jar
./gradlew compileJava compileTestJava # valida que el código compila
./gradlew test                        # unitarios + integración (requiere Docker corriendo, para Testcontainers)
docker compose up -d postgres keycloak  # (desde la raíz del repo) para bootRun manual/pruebas exploratorias
./gradlew bootRun
```

Antes de `bootRun` contra Keycloak real, es necesario crear el realm `vida-cotidiana` y su cliente (ver `docs/development/00-development-baseline.md` §3, `INFRA-002` en `01-technical-backlog.md`) — el `docker-compose.yml` de este ciclo levanta el servidor Keycloak, no el realm.

## 5. Git — no se pudo completar la inicialización en esta sesión

Se ejecutó `git init -b main` con éxito, y `git add -A` (83 archivos, todo el estado actual del repositorio: documentación, artefactos generados y el nuevo backend). Sin embargo, `git commit` falló: el punto de montaje de esta sesión no permite las operaciones de `unlink`/rename que Git necesita sobre su propio directorio `.git/` (probablemente la misma protección que impide borrar/renombrar archivos en la carpeta de trabajo conectada, aplicada también a nivel de sistema de archivos). Quedó un `.git/index.lock` que tampoco se pudo eliminar desde esta sesión.

**Esto no es un problema del código ni de la configuración de Git** — es una restricción del entorno de esta sesión de trabajo. **Acción recomendada para el usuario:** en su propio equipo (Finder/Terminal, fuera de esta sesión), eliminar manualmente la carpeta `.git/` generada (o al menos `.git/index.lock`) y ejecutar de nuevo:

```bash
git init -b main
git add -A
git commit -m "chore: bootstrap Vida Cotidiana repository (docs + backend vertical slice)"
```

Esto no tiene ningún efecto sobre el contenido de `Documentacion/`, `docs/`, ni `backend/` — todos los archivos ya están escritos correctamente en el sistema de archivos real del usuario; solo falta el historial de Git.

## 6. Resultado de esta validación

**No se declara el vertical slice "Done" en el sentido estricto de `14-definition-of-done.md`** (compilación limpia, CI verde, tests ejecutados en verde) porque el entorno de esta sesión no permite ejecutar esas verificaciones. Se declara en su lugar:

**CODE_STATUS: WRITTEN_AND_STATICALLY_VERIFIED — PENDING_REAL_BUILD**

El código sigue estrictamente el contrato aprobado (`openapi.yaml`, `09-data-model.md`, `11-auth-security.md`) y pasó toda la verificación estática posible sin un JDK/Gradle/Docker reales. El siguiente paso obligatorio, antes de continuar con el Milestone 2, es que un desarrollador con un entorno completo ejecute los comandos de §4 y reporte el resultado real. Ningún hallazgo de esta limitación oculta un problema de diseño: es exclusivamente una restricción de herramientas del entorno de ejecución de esta sesión.

---

## 7. Ciclo de validación 2 — cierre de gate Milestone 1

**Fecha:** 2026-08-09 (misma sesión de trabajo, fase posterior explícita de "validación real" solicitada por el Product Owner).
**Entorno:** idéntico al de esta sesión de Cowork (sandbox Linux aislado), reverificado desde cero al inicio de este ciclo.

### 7.1 Reverificación del entorno (comandos ejecutados, salida real)

```text
$ java -version
openjdk version "11.0.31" 2026-04-21
OpenJDK Runtime Environment (build 11.0.31+11-post-1ubuntu1-22.04.2-Ubuntu)

$ which javac
(sin salida — no instalado; solo JRE, no JDK)

$ which gradle
(sin salida — no instalado)

$ which docker
bash: line 1: docker: command not found

$ curl -sI https://services.gradle.org --max-time 5
HTTP/1.1 403 Forbidden (X-Proxy-Error: blocked-by-allowlist)

$ curl -sI https://repo.maven.apache.org --max-time 5
HTTP/1.1 403 Forbidden (X-Proxy-Error: blocked-by-allowlist)
```

**Conclusión, sin ambigüedad:** este entorno sigue sin JDK 21, sin Gradle, sin Docker y sin acceso de red a repositorios de artefactos. Es imposible ejecutar `./gradlew clean test`, `./gradlew build`, o levantar `docker compose` en esta sesión. Esto se declara explícitamente, tal como exige la tarea ("No simules resultados. No declares tests como PASS.").

### 7.2 Revisión de coherencia OpenAPI ↔ Controller ↔ DTO ↔ Service ↔ Entity ↔ Flyway

Revisados campo por campo, endpoint por endpoint:

| Endpoint | OpenAPI | Controller | DTO | Service | Entity/Flyway | Resultado |
|---|---|---|---|---|---|---|
| `GET /api/v1/me` | `components.schemas.User` (id, email, username, deletionStatus) | `UserController.getCurrentUser` | `UserResponse` (4 campos, coinciden) | — (lectura directa de `UserRepository`) | `User`/`users` (columnas coinciden) | Coherente |
| `POST /api/v1/reminders` | `CreateReminderRequest` (title requerido ≤200, description ≤2000, dueAt) → `Reminder` (201) | `ReminderController.create` | `CreateReminderRequest`/`ReminderResponse` | `ReminderService.create` | `Reminder`/`reminders` | Coherente |
| `GET /api/v1/reminders` | Paginado: `page`/`size` query, `PageMeta` + `items[Reminder]` | `ReminderController.list` | `PageResponse<ReminderResponse>` (page/size/totalElements/totalPages/items — igual forma que `allOf` en el contrato) | `ReminderService.listOwnedBy` | `ReminderRepository.findByOwnerUserId` (paginado) | Coherente |
| `GET /api/v1/reminders/{id}` | `Reminder` (200), 401/403/404 | `ReminderController.get` | `ReminderResponse` | `ReminderService.getAccessible` | — | Coherente; ver nota sobre 403 vs. 404 en §7.4 |
| `POST /api/v1/reminders/{id}/complete` | `CompleteReminderRequest` (version opcional) → `Reminder`, 401/403/404/409 | `ReminderController.complete` | `CompleteReminderRequest`/`ReminderResponse` | `ReminderService.toggleCompletion` | `Reminder.version` (`@Version`) | Coherente |
| `PATCH /api/v1/reminders/{id}` | Definido en el contrato (`UpdateReminderRequest`, version requerido) | **No implementado** | — | — | — | **Fuera de alcance de Milestone 1** — ver §7.3 |

No se encontró ninguna divergencia entre lo implementado y `openapi.yaml` para los cinco endpoints que sí forman parte de Milestone 1. `PATCH` está definido en el contrato pero deliberadamente no forma parte de este milestone (ver `docs/development/00-development-baseline.md` §3 y `01-technical-backlog.md` BE-014, ambos ya redactados en el ciclo anterior, antes de esta validación).

### 7.3 Aclaración sobre el punto 7 de la tarea (endpoints "ya implementados")

La tarea de esta fase listó `PATCH /api/v1/reminders/{id}` entre los endpoints a verificar como "ya implementados". **No lo está.** Esto no es un hallazgo nuevo: `01-technical-backlog.md` ya registraba `PATCH` como `BE-014 — TODO`, explícitamente fuera del vertical slice de Milestone 1 (que se definió como Auth → User → Create → List → Complete, sin editar/eliminar). Se documenta aquí para no dar una falsa impresión de cobertura: **no se implementó `PATCH` en este ciclo** para no expandir el alcance de Milestone 1 sin instrucción explícita de hacerlo (regla de la tarea: "no agregues funcionalidades nuevas"). Queda como el primer ítem de Milestone 2.

### 7.4 Autorización — 401 vs. 403 vs. 404

- **401** — ausencia/invalidez de token: correcto y ahora con envoltorio `Error` uniforme (ver hallazgo 1 abajo).
- **403** — el contrato (`openapi.yaml`) documenta `403` como posible respuesta en `GET/PATCH/DELETE /reminders/{id}` y en los endpoints de `shares`, pero la descripción del propio `404` ("never distinguishes the two, to avoid leaking existence, SEC-001") ya indica que, para lectura, la ambigüedad entre "no existe" y "no autorizado" se resuelve intencionalmente hacia `404`. En Milestone 1 **no existe todavía el concepto de `COLLABORATOR`/`REMINDER_SHARE`** (eso es Milestone 2) — el único rol posible hoy es `OWNER`. Por lo tanto, en este slice, cualquier acceso de alguien que no es el propietario responde **404**, nunca 403, y el código de `403` queda reservado para cuando exista un colaborador autenticado con conocimiento legítimo del recurso (p. ej. un colaborador revocado) — un escenario que el modelo actual no puede producir todavía. Esto no es una contradicción del contrato: es una interpretación válida dentro del rango que el contrato ya permite (AC-004: "según política definida"), y **no requiere ni justifica cambiar `openapi.yaml`**.
- **404** — verificado con test (`getReminder_ownedByAnotherUserReturnsNotFound_neverForbidden`, `getReminder_missingIdReturnsNotFound`): mismo código (`REMINDER_NOT_FOUND`) para "no existe" y "existe pero no es mío", sin distinguir, tal como exige AC-004/SEC-001.
- **409** — verificado con test (`completeReminder_mismatchedVersionReturns409`).

### 7.5 Errores reales encontrados y corregidos en este ciclo

**Hallazgo 1 (real, corregido) — 401/403 generados por Spring Security no llevaban el envoltorio `Error`.**
Los fallos de autenticación/autorización que ocurren dentro de la cadena de filtros de Spring Security (antes de llegar a `DispatcherServlet`) nunca pasan por `@RestControllerAdvice` (`GlobalExceptionHandler`). Antes de esta corrección, un `401` por token ausente/ inválido habría devuelto la respuesta por defecto de Spring Security (sin cuerpo JSON con `code`/`message`/`traceId`), violando AC-006 ("toda respuesta de error incluye traceId") y la referencia `$ref: '#/components/responses/Unauthorized'` de `openapi.yaml`, que apunta al schema `Error`.
**Corrección:** se añadieron `identity.infrastructure.RestAuthenticationEntryPoint` y `RestAccessDeniedHandler`, registrados en `SecurityConfig` vía `.exceptionHandling(...)`, que escriben el mismo envoltorio `Error` que `GlobalExceptionHandler`. Verificado con nuevos tests (`reminderEndpoints_requireAuthentication`, `getCurrentUser_requiresAuthentication`) que comprueban `$.code` y `$.traceId` en la respuesta 401.

**Hallazgo 2 (real, corregido) — `TraceIdFilter` y `UserSyncFilter` se ejecutaban dos veces por request.**
Ambos son `@Component` que extienden `OncePerRequestFilter`. Sin una exclusión explícita, Spring Boot registra automáticamente **cualquier** bean `Filter` como un filtro genérico del contenedor servlet — además de la instancia que `SecurityConfig` ya añade explícitamente a la cadena de Spring Security vía `.addFilterBefore/After(...)`. Resultado: cada uno corría dos veces por request (una vez como filtro genérico de Boot, otra como parte de la cadena de seguridad). Para `UserSyncFilter` esto significaba dos upserts de `USER` por request (funcionalmente casi inofensivo por ser idempotente, pero incorrecto y con orden relativo indefinido frente al resto de la cadena de seguridad); para `TraceIdFilter`, un `MDC.put`/`remove` duplicado que podía dejar el `traceId` en un estado inconsistente entre ambas ejecuciones.
**Corrección:** se añadieron dos `@Bean FilterRegistrationBean<Filter>` en `SecurityConfig` con `setEnabled(false)`, indicando a Spring Boot que no debe auto-registrar esas instancias como filtros genéricos — quedan exclusivamente bajo el control de la cadena de Spring Security, en el orden ya definido.

**Hallazgo 3 (real, corregido) — excepción no controlada dentro de `UserSyncFilter` escapaba de `GlobalExceptionHandler`.**
`UserSyncFilter` ejecuta lógica (lectura de claims del JWT, upsert en base de datos) **antes** de invocar `filterChain.doFilter(...)`, es decir, antes de que la petición llegue a `DispatcherServlet`. Cualquier excepción ahí (p. ej. un token de Keycloak sin el claim `email` esperado, o un error transitorio de base de datos) se propagaba fuera de la cadena de filtros sin pasar por `GlobalExceptionHandler`, produciendo previsiblemente una página de error genérica del contenedor en vez de un `Error` JSON uniforme — mismo tipo de violación de AC-006 que el Hallazgo 1.
**Corrección:** se envolvió la llamada a `userSyncService.syncFromToken(...)` en un `try/catch` que registra el error (sin exponer detalles al cliente) y escribe la misma respuesta `Error` uniforme con `code: INTERNAL_ERROR`, status 500.

**No se encontraron más errores reales** en la revisión de `ReminderService`, `ReminderController`, `Reminder`/`User` (entidades), `V1__init_schema.sql`, ni en la configuración de `application.yml`/`application-test.yml`/`docker-compose.yml`.

### 7.6 Cobertura de tests ampliada en este ciclo

Nuevos tests añadidos (sin agregar funcionalidad nueva, solo cobertura de comportamiento ya implementado, tal como exige la tarea):

- `ReminderControllerIntegrationTest.reminderEndpoints_requireAuthentication` — ampliado para verificar `$.code`/`$.traceId` en el 401 (antes solo verificaba el status).
- `ReminderControllerIntegrationTest.completeReminder_omittedVersionSkipsConcurrencyCheck` — nuevo, cubre explícitamente la rama de AC-005 "si se omite [version], la operación se aplica sin verificación de concurrencia" a nivel HTTP (ya existía a nivel de servicio en `ReminderServiceTest`, faltaba a nivel de integración).
- `ReminderControllerIntegrationTest.getReminder_missingIdReturnsNotFound` — nuevo, cubre el caso "no existe" (antes solo se cubría "existe pero es de otro usuario").
- `user.api.UserControllerIntegrationTest` (nueva clase, 3 tests) — `GET /me`: creación de la fila `USER` en el primer request autenticado, actualización de la misma fila (no duplicación) cuando cambia el claim `email` en un request posterior con el mismo `sub`, y 401 uniforme sin autenticación.

Todos estos tests están **escritos y revisados estáticamente**, no ejecutados (mismas limitaciones de entorno que el resto de este documento).

### 7.7 Resultado de este ciclo

- `BUILD_STATUS: NOT_EXECUTED` (sin JDK 21/Gradle disponibles en esta sesión).
- `TEST_STATUS: NOT_EXECUTED` (sin JDK 21/Docker disponibles en esta sesión).
- `CODE_STATUS: WRITTEN_AND_STATICALLY_VERIFIED — PENDING_REAL_BUILD` (se mantiene; no se pudo mejorar a `BUILD_AND_TEST_VERIFIED` porque eso exigiría haber ejecutado realmente `./gradlew clean test`, cosa que este entorno no permite).

Se corrigieron 3 errores reales encontrados por inspección (no hipotéticos: los tres son violaciones concretas de AC-006 y del comportamiento esperado de los filtros, detectables por lectura de código sin necesitar ejecutarlo). Ver `docs/development/03-milestone-1-gate.md` para el resultado consolidado del gate.
