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

---

## 8. Ciclo de validación 3 — ejecución real (cierre efectivo del gate)

**Fecha:** 2026-08-15. **Entorno:** máquina real del usuario (macOS, fuera del sandbox de las sesiones anteriores). A diferencia de los ciclos 1 y 2, este ciclo **sí** dispuso de JDK 21, Docker y acceso de red reales, por lo que aquí se ejecutaron de verdad los comandos que en `§4`/`§7.1` solo estaban documentados como pendientes.

### 8.1 Entorno real (comandos ejecutados, salida real)

```text
$ java -version
openjdk version "21.0.10" 2026-01-20 LTS
OpenJDK Runtime Environment (build 21.0.10+8-LTS-217)

$ docker --version
Docker version 29.6.2, build dfc4efb

$ docker compose version
Docker Compose version v5.3.1
```

El daemon de Docker no estaba corriendo al inicio de este ciclo (`docker ps` fallaba con `no such file or directory` en el socket); Docker Desktop ya estaba abierto por el usuario y el daemon quedó disponible sin más acción.

### 8.2 Git — resuelto

El `.git/index.lock` que había quedado atascado en el ciclo 1 (bloqueo del entorno de esa sesión, no del repositorio) se eliminó sin incidentes (`rm -f .git/index.lock`) porque en esta máquina real no existe la restricción de montaje que afectaba a las sesiones de sandbox anteriores. Se ejecutó `git add -A` (88 archivos, incluyendo los cambios sin confirmar de los hallazgos 1–3 del ciclo 2 que habían quedado como *unstaged*/*untracked*) y `git commit`, con éxito. Commit raíz `9f77ead`.

### 8.3 Gradle wrapper — generado

No había `gradle` instalado como binario del sistema (`which gradle` → vacío) ni vía Homebrew utilizable sin `sudo chown` (que no se ejecutó, por ser una operación irreversible sobre `/opt/homebrew` fuera del alcance de esta tarea). Se descargó la distribución binaria oficial de Gradle 8.9 (`https://services.gradle.org/distributions/gradle-8.9-bin.zip`, verificado con red real, sin bloqueo de allowlist) a un directorio temporal fuera del repositorio, y se usó ese binario **una sola vez** para ejecutar `gradle wrapper --gradle-version 8.9` dentro de `backend/`. Resultado: `backend/gradlew`, `backend/gradlew.bat`, `backend/gradle/wrapper/gradle-wrapper.jar` y `gradle-wrapper.properties` (`distributionUrl` → `gradle-8.9-bin.zip`) generados y confirmados con `BUILD SUCCESSFUL`. El binario temporal no forma parte del repositorio ni de la imagen de build; todo build subsecuente usa exclusivamente `./gradlew`.

### 8.4 `./gradlew clean test` — primer intento: fallo real, causa raíz identificada y corregida

Primer intento (`./gradlew clean test --no-daemon`): `compileJava`/`compileTestJava`/`classes` **compilaron limpio a la primera** (confirma la revisión estática manual de los ciclos 1–2: cero errores de sintaxis, imports o tipos). `ReminderServiceTest` (9 tests, no requiere Docker) pasó. Los dos test classes que usan Testcontainers (`ReminderControllerIntegrationTest`, `UserControllerIntegrationTest`) fallaron en `initializationError` con:

```
java.lang.IllegalStateException: Could not find a valid Docker environment.
  UnixSocketClientProviderStrategy: failed with exception BadRequestException (Status 400: {...campos vacíos...})
  DockerDesktopClientProviderStrategy: failed with exception BadRequestException (Status 400: {...campos vacíos...})
```

**Causa raíz (real, no hipotética):** incompatibilidad conocida entre `docker-java` (cliente HTTP interno de Testcontainers) y Docker Engine 29+, que en 2026 elevó su versión mínima de API de `1.24` a `1.40` (este daemon reporta `MinAPIVersion: 1.40`, `ApiVersion: 1.55` vía `docker version`). `testcontainers-bom` estaba fijado en `1.20.1` (`build.gradle.kts`), cuyo `docker-java` interno sondea por defecto una versión de API anterior al mínimo aceptado, y el daemon responde `400` con un cuerpo `SystemInfo` vacío en vez de un error claro. Se confirmó reproduciendo el mismo cuerpo vacío con `curl --unix-socket /var/run/docker.sock http://localhost/v1.24/info` (200, pero todos los campos vacíos) contra `http://localhost/info` sin versión (200, campos completos).

**Corrección aplicada (código, no el contrato):**
1. `backend/build.gradle.kts`: `testcontainers-bom` `1.20.1` → `1.21.4` (última versión de la línea `1.x`, sin saltar a la `2.x` que cambia coordenadas de artefacto). Por sí sola no resolvió el problema — mismo síntoma.
2. `backend/src/test/resources/docker-java.properties` (archivo nuevo): `api.version=1.44`, fijando explícitamente la versión de API que `docker-java` debe negociar con el daemon, dentro del rango que este acepta (`1.40`–`1.55`). Esta es la solución documentada por el propio proyecto Testcontainers para este incompatibilidad conocida con Docker Desktop/Engine 29+ en versiones `1.21.x` (la alternativa sin este archivo es saltar a `testcontainers` `2.x`, que sí resuelve el problema de raíz pero es un cambio de versión mayor no justificado para este ciclo).

Ninguna de las dos correcciones toca `openapi.yaml`, el modelo de datos, ni ninguna decisión aprobada — es exclusivamente una fijación de versión/configuración de una herramienta de test, exactamente el tipo de ajuste técnico que esta fase de validación está autorizada a hacer.

### 8.5 `./gradlew clean test` — segundo intento: verde real

```
> Task :clean
> Task :compileJava
> Task :processResources
> Task :classes
> Task :compileTestJava
> Task :processTestResources
> Task :testClasses
> Task :test

BUILD SUCCESSFUL in 35s
6 actionable tasks: 6 executed
```

Resultado real por clase (XML de `build/test-results/test/`, generados en esta ejecución, no reutilizados de ciclos anteriores):

| Clase | tests | failures | errors |
|---|---|---|---|
| `reminder.application.ReminderServiceTest` | 9 | 0 | 0 |
| `reminder.api.ReminderControllerIntegrationTest` | 7 | 0 | 0 |
| `user.api.UserControllerIntegrationTest` | 3 | 0 | 0 |
| **Total** | **19** | **0** | **0** |

**TEST_STATUS: PASSED (19/19, ejecución real, Testcontainers con PostgreSQL 16 real vía Docker, no simulado).**

### 8.6 `./gradlew build` — verde real

```
> Task :compileJava UP-TO-DATE
> Task :bootJar
> Task :jar
> Task :assemble
> Task :test UP-TO-DATE
> Task :check UP-TO-DATE
> Task :build

BUILD SUCCESSFUL in 4s
8 actionable tasks: 3 executed, 5 up-to-date
```

Artefactos generados y confirmados: `backend/build/libs/vida-cotidiana-backend-0.1.0-SNAPSHOT.jar` (55.9 MB, jar ejecutable Spring Boot) y `...-plain.jar`.

**BUILD_STATUS: SUCCESSFUL (ejecución real de `./gradlew build`, no simulado).**

### 8.7 Validación manual adicional: `docker compose` + `bootRun` contra Postgres/Keycloak reales

`docker compose up -d postgres keycloak` (desde la raíz del repo) levantó Keycloak 25 sin problema (`http://localhost:8081`), pero el servicio `postgres` del compose falló al enlazar el puerto `5432`: **ya existe un PostgreSQL 17 nativo corriendo en esa máquina fuera de este proyecto** (`/Library/PostgreSQL/17/bin/postgres`, servicio del sistema preexistente, no relacionado con Vida Cotidiana). Esto no es un defecto de `docker-compose.yml` ni del proyecto — es un conflicto de puerto con software ya instalado en el equipo del usuario, fuera del alcance de esta tarea modificar o detener.

Para completar la validación manual de `bootRun` sin tocar el Postgres nativo del sistema ni cambiar el `docker-compose.yml` versionado, se levantó un contenedor Postgres 16 temporal ad hoc en el puerto alternativo `15432` (mismas credenciales que `docker-compose.yml`: `vidacotidiana`/`vidacotidiana`), se ejecutó `DB_URL=jdbc:postgresql://localhost:15432/vidacotidiana ./gradlew bootRun` y se verificó en caliente:

- Arranque limpio: Flyway aplicó `V1__init_schema.sql` contra el Postgres 16 real (`Successfully applied 1 migration ... now at version v1`), Hibernate validó el esquema (`ddl-auto: validate`) sin discrepancias, Tomcat arrancó en el puerto 8080, aplicación lista en 2.585s.
- `GET /actuator/health` → `200 {"status":"UP"}`.
- `GET /api/v1/me` sin token → `401 {"code":"UNAUTHORIZED","message":"Authentication is required to access this resource.","traceId":"..."}`.
- `GET /api/v1/reminders` sin token → mismo envoltorio `401` uniforme.

Esto confirma en caliente, contra un proceso real (no un test), la corrección del Hallazgo 1 del ciclo 2 (`RestAuthenticationEntryPoint` devolviendo el envoltorio `Error` uniforme para 401 generados por Spring Security). No se validó el flujo completo de login/token real contra Keycloak (crear el realm `vida-cotidiana` sigue siendo un paso manual pendiente, `INFRA-002`, fuera del alcance de este ciclo) — la validación se limitó a confirmar que la aplicación arranca, conecta a una base de datos real, aplica migraciones reales, y responde con el contrato de errores correcto sin autenticación.

Tras la validación, se detuvo el proceso `bootRun` y se eliminaron los contenedores temporales (`vc-validation-postgres`, y el `docker compose down` de `keycloak`). No queda ningún contenedor ni proceso de este ciclo corriendo en el equipo del usuario.

### 8.8 Resultado de este ciclo

- `BUILD_STATUS: SUCCESSFUL` (visto realmente, `./gradlew build` con `BUILD SUCCESSFUL`).
- `TEST_STATUS: PASSED` (19/19, visto realmente, incluyendo Testcontainers con Docker real).
- `CODE_STATUS: BUILD_AND_TEST_VERIFIED` (supera el estado `PENDING_REAL_BUILD` de los ciclos 1–2: ya no falta evidencia de ejecución real).
- Se corrigió una causa raíz real y no anticipada (incompatibilidad `docker-java`/Docker Engine 29+, `§8.4`), documentada con su causa exacta y su fix, no adivinada.

Ver `docs/development/03-milestone-1-gate.md` para la actualización del gate a `READY` con esta evidencia.

---

## 9. Migración de build tool — Gradle → Maven (2026-08-15)

**Decisión:** el Product Owner decidió cambiar la herramienta de build del backend de Gradle a Maven, en adelante, antes de iniciar Milestone 2. Es una decisión de tooling, no de arquitectura — no toca `openapi.yaml`, el modelo de datos, ni ninguna decisión aprobada (DEC-001 a DEC-015, ADRs). Resuelve el `TBD` que `17-dependencies.md` dejaba abierto ("Gradle preferido para build: TBD"); ver ADR-013 en `Documentacion/22-decision-log.md`.

**Importante:** todo lo documentado en §7–§8 de este mismo archivo (ciclos 1–3, incluyendo `./gradlew clean test`, `./gradlew build`, la incompatibilidad `docker-java`/Docker Engine 29+ y su fix) sigue siendo evidencia real y válida de *lo que se ejecutó con Gradle en su momento*. No se reescribe ni se invalida retroactivamente — queda como historial superado por el cambio de herramienta, no como un resultado falso.

### 9.1 Qué se migró

- `backend/build.gradle.kts` → `backend/pom.xml`: mismas dependencias exactas (Spring Boot 3.3.4 como parent, Java 21, `spring-boot-starter-{web,validation,data-jpa,oauth2-resource-server,security,actuator}`, `flyway-core` + `flyway-database-postgresql`, `postgresql` runtime; test: `spring-boot-starter-test`, `spring-security-test`, `testcontainers-bom` **1.21.4** — se mantuvo la versión del fix real de `§8.4`, no se bajó a `1.20.1` — más `testcontainers` `junit-jupiter` y `postgresql`).
- `backend/settings.gradle.kts` y `backend/gradle/` (wrapper de Gradle + `README.md` histórico) — eliminados.
- `backend/gradlew`/`gradlew.bat` — eliminados (quedaban no funcionales sin `gradle/wrapper/gradle-wrapper.jar`).
- Wrapper de Maven generado con `mvn -N wrapper:wrapper` (Maven 3.9.9, `only-script`): `backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/maven-wrapper.properties`.
- `backend/src/test/resources/docker-java.properties` (`api.version=1.44`) — **conservado sin cambios**; es el mismo fix real de compatibilidad con Docker Engine 29+ de `§8.4`, no depende de la herramienta de build.
- `backend/.gitignore` y `.gitignore` (raíz): `build/`/`.gradle/` → `target/`.

### 9.2 Revalidación real bajo Maven (JDK 21 + Docker reales, misma máquina)

```text
$ JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw clean test
...
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- in com.vidacotidiana.reminder.api.ReminderControllerIntegrationTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 -- in com.vidacotidiana.reminder.application.ReminderServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in com.vidacotidiana.user.api.UserControllerIntegrationTest
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**19/19 tests en verde, a la primera** — sin necesidad de repetir ningún ajuste adicional: el `docker-java.properties` conservado de `§8.4` funcionó igual bajo Maven Surefire que bajo Gradle Test, confirmando que era un fix de la herramienta de test (Testcontainers/docker-java), no de Gradle.

```text
$ JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw clean package
...
[INFO] Replacing main artifact .../target/vida-cotidiana-backend-0.1.0-SNAPSHOT.jar with repackaged archive, adding nested dependencies in BOOT-INF/.
[INFO] BUILD SUCCESS
```

Jar ejecutable generado: `backend/target/vida-cotidiana-backend-0.1.0-SNAPSHOT.jar` (55.9 MB).

**Validación manual adicional** (equivalente a `§8.7`, repetida bajo Maven): se levantó un PostgreSQL 16 temporal (puerto `15432`, mismo motivo que `§8.7` — el Postgres nativo del sistema en `5432` sigue presente en esta máquina) y Keycloak vía `docker compose up -d keycloak`, y se ejecutó `DB_URL=jdbc:postgresql://localhost:15432/vidacotidiana ./mvnw spring-boot:run`:

- Arranque limpio en 3.653s, Flyway aplicó `V1__init_schema.sql` contra el Postgres real, mismo comportamiento que con `bootRun` de Gradle.
- `GET /actuator/health` → `200 {"status":"UP"}`.
- `GET /api/v1/me` y `GET /api/v1/reminders` sin token → `401` con el mismo envoltorio `Error` uniforme (`code`/`message`/`traceId`).

Idéntico resultado al validado con Gradle en `§8.7`. Se detuvo el proceso y se eliminaron los contenedores temporales al terminar; no queda nada corriendo.

### 9.3 Resultado de la migración

- `BUILD_STATUS: SUCCESSFUL` (Maven, visto realmente).
- `TEST_STATUS: PASSED (19/19)` (Maven, visto realmente, mismos test classes, sin tocar lógica de negocio).
- Ningún error de migración encontrado (plugins, exclusiones o versiones resueltas distinto por Maven) — no fue necesario corregir nada más allá de traducir `build.gradle.kts` a `pom.xml`.
- Ver `docs/development/03-milestone-1-gate.md` (addendum) para la confirmación de que `MILESTONE_1_STATUS` sigue `READY` bajo Maven.

---

## 10. Milestone 2 — BE-014 (`PATCH`) y BE-015 (`DELETE`) (2026-08-15)

Mismo día, tras confirmar el gate `READY` bajo Maven (§9). Se implementaron y validaron en real dos incrementos de Milestone 2 sobre el mismo entorno (JDK 21 + Docker reales, `docker ps` operativo).

### 10.1 BE-014 — `PATCH /reminders/{id}` (AC-004b)

Código nuevo: `ReminderController.update`, `ReminderService.edit`, `Reminder.applyEdit`, DTO `UpdateReminderRequest` (`version` obligatorio, a diferencia del `version` opcional de `/complete`). Tests nuevos: `ReminderServiceTest` (+3: edición con versión correcta, `VersionConflictException` con versión incorrecta sin aplicar el cambio, no-propietario → `NotFoundException`), `ReminderControllerIntegrationTest` (+4: edición parcial exitosa, `409` por versión incorrecta, `400` por versión ausente, no-propietario → `404`).

```text
$ JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw clean test
...
[INFO] Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**26/26 en verde a la primera** (19 heredados + 7 nuevos). `./mvnw clean package` → `BUILD SUCCESS`.

### 10.2 BE-015 — `DELETE /reminders/{id}` (AC-013/DEC-002, alcance reducido)

`openapi.yaml` describe este endpoint como "cascades to INVITATION/REMINDER_SHARE and notifies active collaborators before deletion (DEC-002)". `REMINDER_SHARE`/`INVITATION` (`BE-016..021`) y el envío de push (`BE-025`) siguen sin implementar — no hay nada que cascadear ni nadie que notificar todavía. Se implementó exactamente el subconjunto válido con lo que existe hoy: borrado del `REMINDER` propio, mismo criterio de autorización que el resto del slice (no-propietario → `404`, nunca `403`, sin revelar existencia), `204` sin cuerpo. Documentado explícitamente en el código (`ReminderService.delete`) y en `01-technical-backlog.md` — no es un pendiente oculto, es la consecuencia directa de que sharing/push no existen todavía. La cascada real y la notificación se completan cuando se implementen `BE-016..026` (ya cubierto por `BE-022`/`BE-026`, sin ID nuevo).

Código nuevo: `ReminderController.delete` (`@DeleteMapping`, `204 No Content`), `ReminderService.delete`. Tests nuevos: `ReminderServiceTest` (+3: propietario borra y se verifica `repository.delete` invocado, no-propietario → `NotFoundException` sin invocar `delete`, id inexistente → `NotFoundException`), `ReminderControllerIntegrationTest` (+4: happy path `DELETE` → `204` seguido de `GET` sobre el mismo id → `404`, no-propietario → `404` sin que el recordatorio del dueño se vea afectado, id inexistente → `404`, sin autenticación → `401` con el envoltorio `Error` uniforme).

```text
$ JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw clean test
...
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 -- in com.vidacotidiana.reminder.api.ReminderControllerIntegrationTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 -- in com.vidacotidiana.reminder.application.ReminderServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in com.vidacotidiana.user.api.UserControllerIntegrationTest
[INFO] Tests run: 33, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**33/33 en verde a la primera** (26 heredados + 7 nuevos de `BE-015`). `./mvnw clean package` → `BUILD SUCCESS`, jar ejecutable regenerado.

### 10.3 Resultado

- `BUILD_STATUS: SUCCESSFUL` (Maven, visto realmente, ambos incrementos).
- `TEST_STATUS: PASSED (33/33)` (visto realmente; ningún test simulado ni declarado en verde sin ejecutarlo).
- Ningún cambio a `openapi.yaml`: ambos endpoints implementan exactamente el contrato ya definido (BE-015 dentro del subconjunto hoy alcanzable, declarado explícitamente, no silenciosamente).
- Ver `docs/development/01-technical-backlog.md` (BE-014/BE-015) y `Documentacion/12-traceability.md` (fila `FR-004`) para la propagación de este resultado.

---

## 11. Corrección de infraestructura (AWS → servidor propio) y sharing completo — BE-016..022 (2026-08-15)

Mismo día. Antes de implementar sharing, se corrigió una decisión de infraestructura superada: el Product Owner determinó que V1 no usa AWS ni ningún servicio gestionado de AWS — el backend y PostgreSQL se despliegan en un servidor propio alquilado (self-hosted). Ver `ADR-014` en `Documentacion/22-decision-log.md` para el detalle completo; `DEC-009` (proveedor de correo, que dependía de `DEC-008`/AWS) quedó reabierta como `TBD`, sin sustituir por un proveedor específico sin instrucción explícita. Esta sección documenta solo la parte de **código y build/test real**; la lista completa de documentos corregidos (ADR-009 marcado histórico, `28-v1-decision-pack.md`, `01-scope.md`, `06-c4.md`, `07-backend-architecture.md`, `25-open-questions.md`, `AI-CONTEXT.md`, `README.md`, `00-development-baseline.md`, `01-technical-backlog.md`) está en los commits correspondientes, no repetida aquí.

### 11.1 Consecuencia en código: adapter de correo no-op

`sharing.application.EmailSender` (puerto) + `sharing.infrastructure.NoOpEmailSender` (único adapter): registra en log que un email de invitación "se enviaría", sin enviar nada real y sin ningún cliente SMTP/API. El proveedor real se conecta detrás de esta misma interfaz cuando `DEC-009` se resuelva.

### 11.2 Sharing completo: BE-016 a BE-022

Migración `V2__sharing.sql` (`invitations`, `reminder_shares`, con `ON DELETE CASCADE`, `UNIQUE(reminder_id, collaborator_user_id)`, índice único parcial `WHERE status = 'PENDING'`, e índices de `invited_email`/`(status, expires_at)`/`collaborator_user_id` — exactamente los obligatorios de `09-data-model.md`, ninguno adicional).

Código nuevo: paquete `sharing` completo (`domain`: `Invitation`, `ReminderShare`, `InvitationRepository` con la transición atómica condicional `resolveIfPending`, `ReminderShareRepository`; `application`: `SharingService`, `EmailSender`; `infrastructure`: `NoOpEmailSender`; `api`: `ReminderShareController`, `InvitationController`, DTOs). Tres excepciones de dominio nuevas en `shared.domain` (`ConflictException` → 409, `GoneException` → 410, `ValidationException` → 400), registradas en `GlobalExceptionHandler`. `ReminderService` extendido (BE-022): `getOwnedOrThrow` (owner-only, reutilizado por `SharingService`) vs. `requireOwnerOrActiveCollaborator` (lectura/completar); `ReminderRepository.findAccessibleTo` amplía `GET /reminders` a propios + compartidos `ACTIVE`.

**Transición atómica condicional (AC-008/SEC-002), verificada de verdad, no solo escrita:** `InvitationRepository.resolveIfPending` es un `@Modifying @Query("UPDATE Invitation i SET i.status = :newStatus, i.resolvedAt = CURRENT_TIMESTAMP WHERE i.id = :id AND i.status = ...PENDING")`. `SharingFlowIntegrationTest.acceptInvitation_alreadyResolved_returns410` ejecuta dos requests secuenciales de aceptación sobre la misma invitación (simulando el lado perdedor de una concurrencia real, tal como autoriza la tarea) y confirma que la segunda responde `410` sin crear un segundo `REMINDER_SHARE` — no hay una ventana "leer, comprobar en Java, guardar" en la que dos requests pudieran ambas tener éxito.

### 11.3 `./mvnw clean test` — verde a la primera

```text
$ JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw clean test
...
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0 -- in com.vidacotidiana.sharing.api.SharingFlowIntegrationTest
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0 -- in com.vidacotidiana.sharing.application.SharingServiceTest
[INFO] Tests run: 15, Failures: 0, Errors: 0, Skipped: 0 -- in com.vidacotidiana.reminder.api.ReminderControllerIntegrationTest
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0 -- in com.vidacotidiana.reminder.application.ReminderServiceTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 -- in com.vidacotidiana.user.api.UserControllerIntegrationTest
[INFO] Tests run: 72, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**72/72 en verde** (33 heredados de Milestone 1/BE-014/BE-015 + 39 nuevos: 16 en `SharingFlowIntegrationTest`, 19 en `SharingServiceTest`, 4 casos de autorización de colaborador añadidos a `ReminderServiceTest` para BE-022). Ningún test falló en el primer intento — no fue necesario ningún fix de compatibilidad esta vez (a diferencia de la migración Gradle→Maven, §8.4). Los logs de Flyway confirman `Migrating schema "public" to version "2 - sharing"` en cada test de integración, contra un PostgreSQL 16 real vía Testcontainers.

Cobertura real por escenario (no solo "compila"): invitación por email con/sin cuenta existente (misma forma de respuesta, SEC-001), por username existente/inexistente (400), invitación duplicada pendiente (409), creación por no-propietario (404), listados paginados con autorización owner-only, aceptar/rechazar (happy path + `410` en la segunda resolución), cancelar (owner/no-inviter/ya-resuelta), revocar con efecto inmediato (colaborador revocado → `404` en el siguiente request, sin ventana de gracia), y el nuevo alcance de autorización: un colaborador activo puede `GET`/completar pero recibe `404` (nunca 403) en `PATCH`/`DELETE`/invitar, y `GET /reminders` ahora incluye recordatorios compartidos activos.

### 11.4 `./mvnw clean package` — verde

```text
[INFO] Tests run: 72, Failures: 0, Errors: 0, Skipped: 0
...
[INFO] BUILD SUCCESS
```

Jar ejecutable regenerado.

### 11.5 Resultado

- `BUILD_STATUS: SUCCESSFUL`.
- `TEST_STATUS: PASSED (72/72)`.
- Cero cambios a `openapi.yaml`/`09-data-model.md`: BE-016..022 implementan exactamente el contrato ya definido, incluyendo los estados/campos exactos de `INVITATION`/`REMINDER_SHARE`.
- `DEVOPS-001` (rate limiting sobre creación de invitaciones) deliberadamente **no** incluido en este incremento, tal como pedía la tarea — sigue `TODO`.
- Ver `docs/development/01-technical-backlog.md` (BE-016..022) y `Documentacion/12-traceability.md` (filas `FR-007`..`FR-010`) para la propagación de este resultado.
