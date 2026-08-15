# 32 — V1 Development Gate Audit

Auditoría final de cierre documental de V1, posterior a `29-v1-final-readiness.md` (READY, 2026-08-09) y a `30-documentation-consistency-review.md`/`31-documentation-pack-audit.md` (drift menor, sin bloqueadores). Objetivo: validar un informe de hallazgos externo (Gemini, 22 hallazgos con IDs `BLK-*`/`DOC-CF-*`/`HIG-*`) contra la jerarquía de autoridad del proyecto (decisiones aprobadas > requisitos > arquitectura/modelo de datos/API > casos de uso > backlog/trazabilidad > documentos ejecutivos), corregir directamente en la fuente lo que resultara real, y no aceptar ciegamente ningún hallazgo externo por venir de una auditoría de IA.

Regla aplicada en todo el ciclo: **ningún hallazgo se aceptó o rechazó sin verificarlo contra `22-decision-log.md`, `28-v1-decision-pack.md` y el resto de la documentación normativa.** Ninguna corrección introdujo una decisión de negocio nueva; los ajustes técnicos (bloqueo optimista, transición atómica, constraint de unicidad, healthcheck) se etiquetan explícitamente como `RECOMMENDATION (técnica, no es decisión de negocio)`, no como `DECISION`.

## 1. Clasificación de los 22 hallazgos de Gemini

| ID | Hallazgo (resumen) | Clasificación | Detalle |
|---|---|---|---|
| BLK-001 | Lifecycle de `INVITATION` inconsistente | **REAL** | `07-backend-architecture.md` (párrafo `DECISION (ADR-006)`) seguía listando `REVOKED` como estado de `INVITATION`. Corregido: ahora lista `PENDING/ACCEPTED/REJECTED/EXPIRED/CANCELLED (DEC-003)` con nota aclaratoria de que `REVOKED` vive solo en `REMINDER_SHARE`. |
| BLK-002 | Contradicción `DELETE /me` vs `PENDING_DELETION` | **FALSO POSITIVO** | Verificado en `09-data-model.md`, `openapi.yaml`, `10-api-openapi.md`, `03-prd.md`: los cuatro documentos son consistentes — `DELETE /me` → `202` → `deletion_status = PENDING_DELETION` → purga a los 30 días. No hay borrado inmediato documentado en ningún lugar. |
| BLK-003 | Ambigüedad OIDC/PKCE vs. `/auth/*` | **REAL** | `10-api-openapi.md` seguía documentando `POST /api/v1/auth/logout` como endpoint propio, inexistente en `openapi.yaml`. Por instrucción explícita ("no introducir un proxy de autenticación custom para evitar eliminar documentación antigua"), se **eliminó** el endpoint fantasma en vez de inventar uno: el logout es responsabilidad del cliente (descartar tokens + `end_session_endpoint` opcional de Keycloak) más `DELETE /me/devices/{deviceId}` para dar de baja el push token. `openapi.yaml` no define ningún `/auth/*`, confirmado por auditoría independiente. |
| BLK-004 | Falta bloqueo optimista (`version`) en `Reminder` | **REAL** | Añadido `REMINDER.version` (modelo de datos, `09-data-model.md`), `openapi.yaml` (`UpdateReminderRequest.version` requerido, `CompleteReminderRequest.version` opcional, `409` documentado), y regla de concurrencia en `07-backend-architecture.md`. Etiquetado `RECOMMENDATION (técnica)`, no decisión de negocio. |
| BLK-005 | Falta endpoint para revocar `REMINDER_SHARE` | **FALSO POSITIVO** | `DELETE /reminders/{id}/shares/{shareId}` ya existía en `openapi.yaml` y `10-api-openapi.md` desde el ciclo de decisiones anterior (DEC-003/UC-10/AC-010). |
| BLK-006 | Constraint `UNIQUE(user_id)` en `DEVICE_PUSH_TOKEN` | **PARCIAL / REQUIERE DECISIÓN → resuelto sin reabrir DEC-005** | La sugerencia literal de Gemini (`UNIQUE(user_id)`) es **incorrecta por sí misma**: violaría DEC-005 (multi-dispositivo por usuario), que exige múltiples filas por `user_id`. El gap real subyacente sí existía: no había ninguna constraint de unicidad sobre `token`. Corregido con `UNIQUE(token)` + semántica de upsert (si el token ya pertenece a otro `user_id`, se reasigna al usuario autenticado actual — comportamiento normal cuando un dispositivo cambia de cuenta). No se tocó DEC-005. |
| DOC-CF-01 | `INVITATION` `CANCELLED` vs `REVOKED` | **REAL, YA CORREGIDO** (mismo fix que BLK-001) | Ver BLK-001. |
| DOC-CF-02 | Keycloak OIDC vs. `/auth/login` | **REAL, YA CORREGIDO en ciclo anterior; reverificado sin regresión** | `28-v1-decision-pack.md`/`29-v1-final-readiness.md` ya habían resuelto esto (ADR-008). Esta auditoría confirmó que ningún documento reintrodujo `/auth/login`. |
| DOC-CF-03 | FCM unificado en el modelo de datos/arquitectura vs. mención de APNs directo en documentos ejecutivos | **PARCIAL** | Los documentos ejecutivos (DOCX/PPTX) **nunca** mencionaron APNs como proveedor directo/separado — siempre lo describieron correctamente como "FCM vía puente FCM–APNs para iOS" (verificado línea por línea en `build_docx.js`/`build_pptx.js`). El problema real estaba en la fuente: `05-user-flows.md` tenía el nodo `"Adapter de push: FCM/APNs/Web Push - TBD por plataforma"` en el diagrama de flujo, además de faltar los eventos "cancelada" y "eliminación de recordatorio compartido". Corregido: un único adapter FCM con ramas explícitas Android nativo / iOS vía puente FCM-APNs / Web vía Web Push, y los eventos añadidos. |
| DOC-CF-04 | `REMINDER.status` boolean/`completed` vs. `ARCHIVED` | **FALSO POSITIVO** | El modelo de datos usa un único estado global de tipo enumerado (no boolean) conforme a DEC-001; no existe ni se menciona ningún estado `ARCHIVED` en ningún documento normativo. |
| DOC-CF-05 | Eliminación de cuenta: 30 días vs. borrado directo | **FALSO POSITIVO** | Ver BLK-002; mismo resultado. |
| DOC-CF-06 | IDs de historias de usuario ausentes en trazabilidad/backlog | **REAL** | `12-traceability.md` no referenciaba ningún ID de `26-v1-backlog.md` (`US-001`...`US-017`). Reconstruida la matriz completa con columna `Backlog` poblada para todos los requisitos. |
| DOC-CF-07 | Healthcheck inconsistente / no documentado | **REAL** | No existía mención de un healthcheck operacional en ningún documento. Añadido: Spring Boot Actuator `/actuator/health`, explícitamente **fuera** del contrato versionado `/api/v1` (no forma parte de `openapi.yaml`, es infraestructura de orquestación/monitoreo, no funcionalidad de negocio). Documentado en `07-backend-architecture.md` y en los documentos ejecutivos. |
| HIG-001 | Falta paginación en endpoints de listado | **REAL** | Añadida paginación (`page`/`size`, schema `PageMeta`) a `GET /reminders`, `GET /reminders/{id}/shares`, `GET /me/invitations` en `openapi.yaml` y `10-api-openapi.md`. |
| HIG-002 | Timezone / formato de fecha no especificado | **FALSO POSITIVO** | `AI-CONTEXT.md`, `10-api-openapi.md` y `openapi.yaml` ya especifican timestamps ISO-8601 en UTC de forma consistente. |
| HIG-003 | Condición de carrera al aceptar una invitación | **REAL** | Añadida regla de transición atómica condicional (`UPDATE ... WHERE status = 'PENDING'`) para toda salida de `PENDING` en `INVITATION`, documentada en `09-data-model.md`, `07-backend-architecture.md`, `11-auth-security.md` (SEC-002) y como caso de test explícito en `20-testing-qa.md`. Etiquetada `RECOMMENDATION (técnica)`. |
| HIG-004 | Falta rate limiting | **FALSO POSITIVO** | `SEC-001` ya cubre rate limiting sobre la creación de invitaciones; `openapi.yaml` ya documentaba la respuesta `429`. |
| HIG-005 | Push token no se elimina al hacer logout | **FALSO POSITIVO** | `DELETE /me/devices/{deviceId}` en logout ya estaba documentado en `UC-06`/`UC-12`/`FR-012` desde el ciclo de decisiones anterior. |
| HIG-006 | Formato de error (`error envelope`) no formalizado | **PARCIAL** | El formato `{code, message, traceId}` ya se describía en prosa, pero no existía como `schema` reutilizable en `openapi.yaml`. Añadido `components.schemas.Error` y referenciado (`$ref`) desde `components.responses.{BadRequest,Unauthorized,Forbidden,NotFound,Gone,TooManyRequests}`, usados en todas las operaciones. También se formalizó la disambiguación del `409` vía el campo `code` (`INVITATION_ALREADY_PENDING` vs. `REMINDER_VERSION_CONFLICT`). |
| HIG-007 | Falta purga de invitaciones sin cuenta asociada | **FALSO POSITIVO** | Ya documentado como DEC-015 (A') en `09-data-model.md` desde el ciclo de decisiones anterior. |
| HIG-008 | Seguridad de tokens OIDC en la SPA Web | **FALSO POSITIVO** | Ya documentado (`08c-web-architecture.md`, `21-security.md`): almacenamiento en memoria (no `localStorage`), CSP estricta, renovación silenciosa de tokens de vida corta. |

**Resumen:** de 22 hallazgos, **8 fueron reales y se corrigieron** (BLK-001/003/004, DOC-CF-01/06/07, HIG-001/003), **2 fueron parciales y se cerraron** (BLK-006 con una solución distinta a la sugerida, HIG-006), **1 fue parcial con la mitad ya correcta** (DOC-CF-03), **2 ya estaban corregidos de un ciclo anterior y se reverificaron sin regresión** (DOC-CF-02, y BLK-001/DOC-CF-01 que son el mismo hallazgo), y **9 fueron falsos positivos** verificados contra la documentación existente (BLK-002/005, DOC-CF-04/05, HIG-002/004/005/007/008).

## 2. Nuevos hallazgos de la auditoría independiente (no estaban en el informe de Gemini)

Heredados de `30-documentation-consistency-review.md` (drift ya identificado, ahora corregido en este ciclo) y hallazgos adicionales de esta pasada:

1. **ADR-007 sin marcar como histórico** — el párrafo original ("proveedor exacto por plataforma queda TBD") podía leerse como vigente. Corregido con una subsección "Estado histórico previo (referencia, ya no vigente)", replicando el patrón ya usado en ADR-008.
2. **`UC-02` con redacción pre-Keycloak** ("Backend crea sesión") — contradecía el patrón resource-server-only de ADR-008/DEC-004. Corregido para describir Authorization Code + PKCE contra Keycloak, sin sesión propia del backend.
3. **Autocontradicción en `01-scope.md`** — un párrafo `RECOMMENDATION` afirmaba que el stack de iOS/Web "todavía no se ha analizado formalmente... TBD", contradiciendo DEC-006/DEC-007 (ya aprobadas). Corregido con una nota histórica que reconoce que DEC-006/DEC-007 ya resolvieron esto.
4. **`08-android-architecture.md` sin módulo `sharing`** en la estructura de carpetas `feature/`, inconsistente con backend/iOS/Web (que sí lo listan). Añadido.
5. **Falta de UC/AC propios para "cancelar invitación pendiente"** (ya señalado como brecha G en `30-documentation-consistency-review.md`, sin caso de uso ni criterio de aceptación ni fila de trazabilidad). Corregido: nuevo `UC-14`, nuevo `AC-017`, filas de `FR-007`/`FR-010` actualizadas en `12-traceability.md`.
6. **`FR-011`/`FR-012`/`FR-013` en orden físico no ascendente en `03-prd.md`** (hallazgo H de `30-documentation-consistency-review.md`) — **NO corregido en este ciclo** (cosmético, no afecta contenido ni IDs). Se deja como TBD de estilo, ver §5.
7. **Localización incompleta en los documentos ejecutivos "en inglés" (`build_docx.js`/`build_pptx.js`)** — descubierto durante la verificación visual de esta fase: las tablas de datos (Casos de Uso, Criterios de Aceptación, Requisitos, ADRs, Decisiones, Trazabilidad, Endpoints, Modelo de Datos, Amenazas, Códigos HTTP) del documento "en inglés" están redactadas en español; solo los encabezados de sección y los párrafos narrativos están en inglés. Esto es un defecto preexistente de generación (anterior a este ciclo de auditoría, no introducido por él) y afecta **únicamente a los artefactos ejecutivos generados** (`docs/generated/*-EN.docx`/`*-EN.pptx`), no a la documentación normativa en `Documentacion/`, que es la fuente de verdad y está enteramente en español. No se corrigió en este ciclo (requeriría traducir manualmente más de 100 filas de tablas, fuera del alcance de "estabilizar la documentación contractual"), y se reporta explícitamente en vez de ocultarse.

## 3. Contradicciones eliminadas (resumen)

- `REVOKED` como estado fantasma de `INVITATION` en `07-backend-architecture.md`.
- Endpoint fantasma `POST /auth/logout` en `10-api-openapi.md`.
- Redacción "TBD por plataforma" para el proveedor de push en `05-user-flows.md` (ya resuelto como FCM unificado por DEC-010).
- Redacción "backend crea sesión" en `UC-02` (contradice el patrón resource-server-only).
- Autocontradicción sobre el stack iOS/Web en `01-scope.md`.
- ADR-007 legible como "aún sin decidir" pese a estar superado por DEC-010.

## 4. Decisiones aprobadas — verificación de propagación

Se reverificaron las 15 decisiones de `28-v1-decision-pack.md` (DEC-001 a DEC-015) contra todos los documentos normativos listados en `29-v1-final-readiness.md` §5, más los archivos tocados en este ciclo. **Ninguna decisión aprobada quedó sin propagar** tras las correcciones de este ciclo (el hallazgo BLK-001/DOC-CF-01 era precisamente un caso de propagación incompleta de DEC-003, ya cerrado). No se registró ninguna decisión nueva de negocio: todos los cambios de este ciclo son correcciones de consistencia editorial (referencias caducas) o adiciones técnicas explícitamente etiquetadas `RECOMMENDATION`.

## 5. TBDs restantes — no bloquean V1

| TBD | Por qué no bloquea |
|---|---|
| Orden físico de `FR-011`/`FR-012`/`FR-013` en `03-prd.md` | Cosmético; los IDs son estables, no se reutilizó ni se perdió ninguno; no afecta la trazabilidad ni la implementación. |
| Localización de las tablas de datos en el DOCX/PPTX "en inglés" | Afecta solo al artefacto ejecutivo derivado, no a la documentación normativa (`Documentacion/`), que es la fuente de verdad y ya está completa y correcta en español. |
| Reversión exacta de `PENDING_DELETION` (UX) | Ya listado en `25-open-questions.md`/`29-v1-final-readiness.md` como TBD de producto no bloqueante. |
| Patrón final de tokens OIDC en la SPA Web | Ya documentado como consideración técnica pendiente en `08c-web-architecture.md`, con mitigación provisional ya definida (memoria + CSP). |
| Entidad `AUDIT_EVENT` explícita | Mencionada solo conceptualmente; no bloquea el diseño de V1 del resto de entidades. |
| Nombre de producto, mercado inicial, límites de UX (máx. colaboradores, formato de username, etc.) | TBDs de producto/negocio explícitamente fuera del alcance de esta auditoría técnica (regla fundamental: no inventar requisitos de negocio). |

Ninguno de estos TBDs oculta una contradicción conocida ni una decisión aprobada sin propagar.

## 6. Segunda y tercera auditoría (Fases 11–12)

Tras aplicar las correcciones de la Fase 3–9, se ejecutó una segunda pasada independiente (grep dirigido, no basado en memoria de los cambios propios) sobre todo `Documentacion/` buscando: referencias residuales a `REVOKED` en `INVITATION`, `/auth/login`/`/auth/logout`, `APNs` como proveedor directo, `"TBD por plataforma"`, y confirmando la presencia de `UC-14`/`AC-017` en todos los documentos que deberían referenciarlos (`04-use-cases.md`, `13-acceptance.md`, `12-traceability.md`, `10-api-openapi.md`, `05-user-flows.md`, `11-auth-security.md`, `20-testing-qa.md`). Resultado: **todas las coincidencias restantes de esos términos están en documentos históricos de auditoría (`27-v1-readiness-review.md`, `28-v1-decision-pack.md`, `29-v1-final-readiness.md`, `30-documentation-consistency-review.md`, `31-documentation-pack-audit.md`) describiendo el estado *antes* de la corrección, correctamente contextualizadas como historial — ninguna aparece en un documento normativo vigente como si describiera el estado actual.**

Se revalidó también `openapi.yaml` estructuralmente (12 `paths`, 12 `schemas`, cero operaciones sin `responses`, todas las referencias `$ref` resuelven), la constraint de unicidad de `DEVICE_PUSH_TOKEN.token`, el campo `REMINDER.version`, y la referencia cruzada `12-traceability.md` ↔ `26-v1-backlog.md` (IDs `US-001`–`US-017` existentes y coherentes).

No se encontró ningún bloqueador nuevo en esta segunda pasada, por lo que **no fue necesaria una tercera ronda de corrección** — la tercera pasada de la Fase 12 se redujo a esta misma verificación confirmatoria, sin cambios adicionales.

## 7. Documentos ejecutivos (Fase 10)

Se actualizaron y regeneraron los cuatro artefactos derivados, en este orden (fuente Markdown primero, artefacto después):

- `build_docx.js` / `build_docx_es.js` → `V1-Software-Architecture-and-Product-Specification-{EN,ES}.docx/.pdf`
- `build_pptx.js` / `build_pptx_es.js` → `V1-Product-and-Architecture-Overview-{EN,ES}.pptx`

Cambios de contenido aplicados a los cuatro: `UC-14`/`AC-017` añadidos; tabla de endpoints con paginación, `version` (bloqueo optimista) y transiciones atómicas; modelo de datos con `REMINDER.version` y `DEVICE_PUSH_TOKEN.token` único; dos amenazas nuevas (condición de carrera, actualizaciones perdidas); sección de arquitectura de API con mención de `securitySchemes`/`Error`/paginación; nueva sección de healthcheck operacional; sección de trazabilidad actualizada para reflejar la cadena completa reconstruida; sección de preparación de V1 con un resumen de esta auditoría; control de documento e historial de cambios actualizados. Verificados visualmente (conversión a PDF + inspección página por página / diapositiva por diapositiva) sin errores de formato ni desbordamiento de texto. Copiados a `docs/generated/` reemplazando las versiones anteriores.

## 8. V1_READINESS_STATUS

## V1_READINESS_STATUS: READY

- **BLOCKERS: 0**
- **CRITICAL_CONTRADICTIONS: 0**
- **UNPROPAGATED_APPROVED_DECISIONS: 0**

Todos los criterios de la Sección "Objetivo" se cumplen: no quedan bloqueadores reales; ninguna contradicción crítica entre PRD, casos de uso, arquitectura, modelo de datos, API, seguridad o UX; las 15 decisiones aprobadas están propagadas en todos los documentos afectados; la trazabilidad cierra `Requirement → Use Case → Acceptance Criteria → API → Data Model → Architecture → Backlog → Test` para todos los `FR`/`NFR`; `openapi.yaml` es coherente con los casos de uso y el modelo de datos y es un contrato implementable; el modelo de datos es coherente con las decisiones aprobadas; los documentos ejecutivos describen exactamente la arquitectura final (sin referencias obsoletas a `APNs` directo, login propio, o estados retirados); y ningún `TBD` restante oculta una contradicción conocida — todos están explícitamente justificados en la §5 de este documento.

Ver también `29-v1-final-readiness.md` (primer gate, READY) y `30-documentation-consistency-review.md`/`31-documentation-pack-audit.md` (revisión de consistencia previa a la generación ejecutiva) para el historial completo de este proceso.
