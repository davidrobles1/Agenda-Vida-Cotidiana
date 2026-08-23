# 09 — Modelo de datos

## V1

**DECISION (ADR-006):** se añaden `REMINDER_SHARE` (colaboración activa) e `INVITATION` (invitación pendiente/resuelta) para soportar compartir un recordatorio con múltiples colaboradores (1:N), sin implementar grupos/hogares/roles.

**DECISION (DEC-001, `28-v1-decision-pack.md`):** `REMINDER.status` es un único estado global por recordatorio (`PENDING`/`COMPLETED`), compartido entre propietario y colaboradores. No existe completado individual por persona en V1.

**DECISION (DEC-005):** se añade `DEVICE_PUSH_TOKEN` para soportar múltiples dispositivos por usuario (ver ADR-007).

**DECISION (DEC-015):** `USER` incorpora campos de soft delete con periodo de gracia de 30 días.

**DECISION (ADR-015, 2026-08-18):** `USER` incorpora dos indicadores independientes de modo habilitado (`personal_enabled`, `laboral_enabled`); al menos uno debe ser `true` desde el registro (FR-014), modificables después desde Ajustes (FR-016, solo activar — desactivar queda `TBD`, ver ADR-015). `REMINDER` incorpora un campo `context` (`PERSONAL`/`LABORAL`, obligatorio) para soportar el Calendario general agregado y coloreado por origen (FR-017/FR-018). **DECISION (ADR-015/FR-019, ya no TBD):** `context` se infiere del navbar de origen al crear el recordatorio — no hay selector explícito en el formulario; el endpoint `POST /reminders` acepta un `context` opcional y por default asigna `PERSONAL` cuando el caller no lo envía (todo caller anterior a este ADR). **Version: V3, implementación adelantada en paralelo con V2** (decisión del Product Owner, 2026-08-18 — ver `05-v2-plan.md` y la nota de "Refinamiento cerrado" en `22-decision-log.md` ADR-015; esta nota reemplaza una anterior que decía "no se implementa hasta cerrar V2", ya superada).

**DECISION (Product Owner, 2026-08-18, `BE-038`) — migración de backfill real, ya aplicada:** `V6__adr015_context_modes.sql` agrega ambas columnas y resuelve los datos ya existentes en la misma migración — este fue un TBD explícito planteado antes de implementar (no asumido): todo `REMINDER` preexistente recibe `context = 'PERSONAL'`, y en la misma migración se hace *grandfather* de `personal_enabled = true` para todo `USER` preexistente (antes de este ADR la app era de un solo contexto, implícitamente personal — la migración lo reconoce explícitamente en el dato en vez de dejar una inconsistencia entre los recordatorios de un usuario y su propio selector de navegación). Verificado contra la base de datos de desarrollo real (no solo en Testcontainers): 4/4 usuarios reales quedaron con `personal_enabled = true`, 158/158 recordatorios reales quedaron con `context = 'PERSONAL'`, cero nulos.

**DECISION (usuario, 2026-08-18, `BE-037`/`WEB-009`) — Garantías y Mantenimiento pasan de mock a reales.** `WARRANTY`/`MAINTENANCE_RECORD` se agregan siguiendo el mismo patrón de `REMINDER` (Clean Architecture, dueño-únicamente, sin colaboradores — no hay concepto de compartir para estos dos), incluyendo bloqueo optimista (`version`). Los campos no se inventaron: son exactamente los que la UI mock ya mostraba (`web/src/core/mock/mockData.ts`, `WarrantiesPage.tsx`/`MaintenancePage.tsx`) antes de esta tarea — `item`, la fecha (`expires_at`/`next_due_at`, mismos nombres que ya usaba la UI), y un estado. **RECOMMENDATION técnica:** `status` (VIGENTE/POR_VENCER/VENCIDA para Warranty; AL_DIA/PROXIMO/VENCIDO para Maintenance) se deriva en el momento de la respuesta comparando la fecha contra "ahora" — no es una columna almacenada — excepto `COMPLETADO`, que sí es un estado explícito y persistido (reemplaza el toggle local-only que existía en el mock, `mockCompletedIds`, ahora una acción real `POST .../{id}/complete`). **ASSUMPTION** (no una decisión de negocio explícita, documentada como tal): "próximo a vencer" (`POR_VENCER`/`PROXIMO`) se calcula como dentro de 30 días de la fecha — ningún umbral estaba definido antes, 30 días es un valor técnico razonable, no aprobado explícitamente por el Product Owner.

**RECOMMENDATION (técnica, no es decisión de negocio, añadida el 2026-08-15, BE-029):** `11-auth-security.md` §Auditoría exige registrar eventos de seguridad (creación/cancelación/aceptación/rechazo/expiración de invitación, revocación de acceso) "sin guardar secretos", pero no especificaba ningún schema de tabla — este documento tampoco incluía ninguna entidad de auditoría hasta ahora. `AUDIT_EVENT` es la adición mínima para cumplir literalmente ese requisito ya aprobado; no introduce ningún requisito nuevo. Deliberadamente **sin** columna de detalle libre/JSON: los cinco campos de abajo ya bastan para lo pedido, y una columna de texto arbitrario sería la puerta de entrada exacta para que alguien termine guardando un email o un token ahí más adelante, violando "sin guardar secretos".

```mermaid
erDiagram
    USER ||--o{ REMINDER : owns
    USER ||--o{ REMINDER_SHARE : "es colaborador en"
    USER ||--o{ INVITATION : "recibe (si tiene cuenta)"
    USER ||--o{ DEVICE_PUSH_TOKEN : "registra"
    USER ||--o{ AUDIT_EVENT : "actor de (nullable, ver Reglas)"
    USER ||--o{ WARRANTY : owns
    USER ||--o{ MAINTENANCE_RECORD : owns
    REMINDER ||--o{ REMINDER_SHARE : "compartido con"
    REMINDER ||--o{ INVITATION : "origina"

    USER {
      uuid id PK
      string email UK
      string username UK "nullable, único cuando se define (DEC-005 formato TBD)"
      string status
      string deletion_status "ACTIVE, PENDING_DELETION, DELETED"
      timestamp deletion_requested_at "nullable"
      timestamp purge_at "nullable, deletion_requested_at + 30 días"
      boolean personal_enabled "default false, ver FR-014/FR-016 (ADR-015)"
      boolean laboral_enabled "default false, ver FR-014/FR-016 (ADR-015)"
      timestamp created_at
      timestamp updated_at
    }
    REMINDER {
      uuid id PK
      uuid owner_user_id FK
      string title
      string description
      timestamp due_at
      string status "PENDING, COMPLETED (estado único global, DEC-001)"
      string context "PERSONAL, LABORAL (ADR-015/FR-019) — inferido del navbar de origen; default PERSONAL si el caller no lo envía"
      int version "bloqueo optimista, ver Reglas"
      timestamp created_at
      timestamp updated_at
    }
    INVITATION {
      uuid id PK
      uuid reminder_id FK "ON DELETE CASCADE"
      uuid inviter_user_id FK
      string invited_email
      uuid invited_user_id FK "nullable, se resuelve si el email coincide con una cuenta"
      string status "PENDING, ACCEPTED, REJECTED, EXPIRED, CANCELLED (DEC-003)"
      timestamp expires_at
      timestamp created_at
      timestamp resolved_at
    }
    REMINDER_SHARE {
      uuid id PK
      uuid reminder_id FK "ON DELETE CASCADE"
      uuid collaborator_user_id FK
      uuid invitation_id FK
      string status "ACTIVE, REVOKED"
      timestamp created_at
      timestamp revoked_at
    }
    DEVICE_PUSH_TOKEN {
      uuid id PK
      uuid user_id FK
      string platform "ANDROID, IOS, WEB"
      string token
      timestamp created_at
      timestamp last_seen_at
    }
    AUDIT_EVENT {
      uuid id PK
      string event_type "INVITATION_CREATED, INVITATION_CANCELLED, INVITATION_ACCEPTED, INVITATION_REJECTED, INVITATION_EXPIRED, SHARE_REVOKED"
      uuid actor_user_id FK "nullable — null cuando el actor es un job del sistema (p. ej. expiración)"
      string target_type "REMINDER, INVITATION, REMINDER_SHARE"
      uuid target_id
      timestamp occurred_at
    }
    WARRANTY {
      uuid id PK
      uuid owner_user_id FK
      string item
      timestamp expires_at
      string status "VIGENTE, POR_VENCER, VENCIDA, COMPLETADO — derivado en respuesta, ver Reglas"
      int version "bloqueo optimista, mismo patrón que REMINDER"
      timestamp created_at
      timestamp updated_at
    }
    MAINTENANCE_RECORD {
      uuid id PK
      uuid owner_user_id FK
      string item
      timestamp next_due_at
      string status "AL_DIA, PROXIMO, VENCIDO, COMPLETADO — derivado en respuesta, ver Reglas"
      int version "bloqueo optimista, mismo patrón que REMINDER"
      timestamp created_at
      timestamp updated_at
    }
```

## V3 — Módulo Laboral (ADR-016)

**DECISION (ADR-016, 2026-08-22):** se agregan tres entidades nuevas, exclusivas del contexto Laboral, y se extiende `REMINDER` con tres columnas nullable. No se crea una entidad Evento/Reunión independiente — una reunión es un `REMINDER` con `context = LABORAL`, `location` definido y participantes vía `REMINDER_SHARE` (ADR-006, ya existente, sin cambios). No se crea una entidad `ORGANIZATION`: `PERSON.organization` es texto libre en esta fase (RECOMMENDATION de minimización, NFR-011).

```mermaid
erDiagram
    USER ||--o{ PERSON : owns
    USER ||--o{ PROJECT : owns
    USER ||--o{ COMMITMENT : owns
    PERSON ||--o{ PROJECT : "es cliente de (opcional)"
    PERSON ||--o{ COMMITMENT : "asociada a"
    PERSON ||--o{ REMINDER : "vinculada a (opcional, FR-023)"
    PROJECT ||--o{ COMMITMENT : "asociado a (opcional)"
    PROJECT ||--o{ REMINDER : "vinculado a (opcional, FR-023)"
    REMINDER ||--o{ COMMITMENT : "origina (opcional)"

    PERSON {
      uuid id PK
      uuid owner_user_id FK
      string name
      string role "texto libre, nullable"
      string organization "texto libre, nullable — ver ADR-016(c)"
      timestamp created_at
      timestamp updated_at
      int version "bloqueo optimista, mismo patrón que REMINDER"
    }
    PROJECT {
      uuid id PK
      uuid owner_user_id FK
      string name
      uuid client_person_id FK "nullable, referencia PERSON"
      string status "texto libre en V1, TBD si se cierra a enum"
      timestamp deadline "nullable"
      timestamp created_at
      timestamp updated_at
      int version "bloqueo optimista, mismo patrón que REMINDER"
    }
    COMMITMENT {
      uuid id PK
      uuid owner_user_id FK
      uuid person_id FK "TBD si puede ser nullable, ver ADR-016"
      uuid project_id FK "nullable"
      string description
      string direction "MINE, THEIRS (ADR-016) — reemplaza dos entidades separadas"
      timestamp due_at
      string status "OPEN, DONE"
      uuid origin_reminder_id FK "nullable — REMINDER/reunión del que surgió"
      timestamp created_at
      timestamp updated_at
      int version "bloqueo optimista, mismo patrón que REMINDER"
    }
```

Columnas nuevas sobre `REMINDER` existente (mismo patrón que `context` de ADR-015 — nullable, sin romper ningún `REMINDER` preexistente):
- `person_id` (FK `PERSON`, nullable) — FR-023.
- `project_id` (FK `PROJECT`, nullable) — FR-023.
- `location` (texto, nullable) — FR-024, solo tiene sentido con `context = LABORAL`, no se restringe a nivel de base de datos (regla de negocio, no constraint).

**DOCUMENTATION_CONFLICT (detectado, no corregido en esta tarea):** `NOTE` se referencia en FR-028 (Inbox) y ya existe como recurso real en `openapi/openapi.yaml` (`/notes`), pero no aparece en el ERD de este documento — el ERD quedó desactualizado respecto al API contract en una tarea anterior a ADR-016. No se resuelve aquí para no exceder el alcance de esta decisión; queda como TBD abierto en `25-open-questions.md`.

**TBD (ADR-016):** si `COMMITMENT.person_id` admite `NULL` (compromiso sin contraparte) — ver ADR-016, TBD explícitos.

**Migración:** `V11__adr016_laboral_module.sql` — **implementada y aplicada (2026-08-22, `BE-039`)**. V7 a V10 ya estaban tomadas por trabajo posterior (notas, vision boards) para cuando esto se ejecutó; corrige la mención tentativa de "V7" de versiones anteriores de este documento. Ver `docs/development/01-technical-backlog.md` (`BE-039`).

## V4 candidato — Fase 3e (ADR-016): Objetivos / Rutinas / Lugares / Recursos

**DECISION (ADR-016, adenda Fase 3e, 2026-08-22):** se aprueba la definición funcional mínima de cuatro entidades nuevas de contexto del Módulo Laboral, divididas en cuatro incrementos independientes (3e1-3e4, ver `docs/development/08-laboral-module-plan.md`). **Ninguna está implementada todavía** — esta sección es la definición de esquema para desarrollo posterior, no una migración aplicada.

```mermaid
erDiagram
    USER ||--o{ OBJECTIVE : owns
    USER ||--o{ ROUTINE : owns
    USER ||--o{ PLACE : owns
    USER ||--o{ RESOURCE : owns
    PERSON ||--o{ PLACE : "ubicación de (opcional)"
    PERSON ||--o{ RESOURCE : "vinculado a (opcional)"
    PROJECT ||--o{ RESOURCE : "vinculado a (opcional)"

    OBJECTIVE {
      uuid id PK
      uuid owner_user_id FK
      string title
      int target_value "nullable — meta numérica simple"
      int current_value "default 0, actualizado manualmente por el usuario"
      timestamp deadline "nullable"
      boolean completed "default false, marcado manualmente — nunca derivado"
      timestamp created_at
      timestamp updated_at
      int version "bloqueo optimista, mismo patrón que REMINDER"
    }
    ROUTINE {
      uuid id PK
      uuid owner_user_id FK
      string title
      string description "nullable"
      string frequency "DAILY, WEEKLY, MONTHLY"
      timestamp next_execution_date
      boolean active "default true"
      timestamp created_at
      timestamp updated_at
      int version "bloqueo optimista"
    }
    PLACE {
      uuid id PK
      uuid owner_user_id FK
      string name
      string address "nullable"
      uuid person_id FK "nullable, referencia PERSON"
      timestamp created_at
      timestamp updated_at
      int version "bloqueo optimista"
    }
    RESOURCE {
      uuid id PK
      uuid owner_user_id FK
      string name
      string type "DOCUMENTO, ENLACE, PLANTILLA, MANUAL, HERRAMIENTA, OTRO"
      string reference "nullable, texto libre — URL o referencia (ASSUMPTION técnica, ver TBD abajo)"
      string description "nullable"
      uuid person_id FK "nullable"
      uuid project_id FK "nullable"
      timestamp created_at
      timestamp updated_at
      int version "bloqueo optimista"
    }
```

Notas por entidad:
- **OBJECTIVE:** sin relación con `PROJECT`/`PERSON` en este incremento (**FUERA DE ALCANCE** explícito, FR-031) — el progreso es 100% manual (`current_value` actualizado a mano por el usuario). Vincular a `PROJECT` para autocalcular el progreso queda como candidato futuro, no comprometido.
- **ROUTINE:** **no** genera `REMINDER`/`COMMITMENT` (decisión explícita del Product Owner, FR-032) — mantiene esta sub-fase separada de "Automatizaciones simples" (3d, sigue `BLOCKED`). "Marcar como realizada" solo avanza `next_execution_date`; no se guarda un historial de ejecuciones pasadas (no pedido).
- **PLACE:** `person_id` opcional, mismo patrón que `PROJECT.client_person_id`. No introduce `REMINDER.place_id` — la integración con `CreateTaskDialog` es solo autocompletado de texto en el cliente (FR-033).
- **RESOURCE:** no reemplaza `DOCUMENT` — sin archivo real almacenado, solo metadatos y una referencia de texto/URL (FR-034).

**TBD explícitos (no bloqueantes — pendientes de definición del Product Owner o de detalle técnico antes de codificar, ver `08-laboral-module-plan.md` Fase 3e para el desglose completo por incremento):**
- `ROUTINE`: fórmula exacta para calcular `next_execution_date` a partir de `frequency` (p. ej. `MONTHLY` sobre un día 31 en un mes sin día 31) y su valor inicial al crear la Rutina (¿el usuario lo fija, o se calcula "hoy + frequency"?).
- `ROUTINE`: si `active = false` debe ocultar la Rutina de algún listado/resumen — a diferencia de Objetivo, no hay integración con "Hoy" decidida para Rutinas.
- `RESOURCE.reference`: si debe ser un único campo de texto libre (elegido aquí, ASSUMPTION técnica) o dos campos separados (`url` estructurada + `description` de referencia) — el enunciado aprobado dice "url o referencia", ambiguo entre ambas formas.
- Punto de entrada exacto en la UI para Objetivos/Rutinas/Lugares/Recursos más allá de lo ya decidido para Objetivos (resumen en "Hoy" + página dedicada) y Lugares (selector inline en `CreateTaskDialog`) — ninguna se agrega al navbar de 7 secciones núcleo de Laboral (ya cerrado, `WEB-010`).

## Reglas
- UUID/identificador no predecible.
- FK a owner/user.
- índices sobre `user_id`/`owner_user_id`, `INVITATION(invited_email)`, `INVITATION(status, expires_at)` (para el job de expiración), `REMINDER_SHARE(collaborator_user_id)` y `DEVICE_PUSH_TOKEN(user_id)`.
- constraints de unicidad: `USER.email` único; `USER.username` único cuando no es nulo; `REMINDER_SHARE(reminder_id, collaborator_user_id)` único (evita colaboraciones duplicadas); índice único parcial `INVITATION(reminder_id, invited_email) WHERE status = 'PENDING'` (respalda el 409 de `AC-007`); `DEVICE_PUSH_TOKEN.token` único (un mismo token físico de push no debe existir en más de una fila; `POST /me/devices` actúa como upsert por `token` — si el token ya existe asociado a otro `user_id`, se reasigna al usuario autenticado actual, comportamiento normal cuando un dispositivo cambia de cuenta).
- **RECOMMENDATION (técnica, no es decisión de negocio):** `REMINDER.version` (entero, bloqueo optimista) se incrementa en cada `UPDATE` sobre el recordatorio (edición por el propietario o cambio de `status` por el propietario/un colaborador `ACTIVE`). `PATCH /reminders/{id}` y `POST /reminders/{id}/complete` deben validar la `version` enviada por el cliente contra la almacenada y responder `409` en caso de conflicto, evitando actualizaciones perdidas cuando dos actores modifican el mismo recordatorio casi simultáneamente.
- **RECOMMENDATION (técnica, no es decisión de negocio):** la transición `INVITATION.status = PENDING → ACCEPTED/REJECTED` debe ejecutarse como una actualización condicional atómica (`UPDATE ... WHERE status = 'PENDING'`) para evitar una condición de carrera si dos requests (p. ej. aceptar y rechazar, o dos aceptaciones) llegan casi simultáneamente sobre la misma invitación; solo una debe tener éxito, la otra debe recibir `410`.
- timestamps en UTC.
- migraciones versionadas con Flyway.
- **DECISION (DEC-002):** `INVITATION.reminder_id` y `REMINDER_SHARE.reminder_id` usan `ON DELETE CASCADE` — al eliminar un `REMINDER`, sus invitaciones y colaboraciones se eliminan con él. Antes de ejecutar el `DELETE` físico, el backend debe emitir una notificación push a los colaboradores con `REMINDER_SHARE.status = ACTIVE` en ese momento (ver `FR-010`, `UC-05`, `AC-013`). `REMINDER` en sí no usa soft delete (sin requerimiento que lo exija); `USER` sí, ver más abajo.
- `INVITATION.invited_email` nunca debe usarse para responder si existe o no una cuenta con ese email (mitigación de enumeración, SEC-001).
- `INVITATION.expires_at` = `created_at` + 7 días (ASSUMPTION, FR-007).
- un `REMINDER_SHARE.status = REVOKED` debe bloquear inmediatamente el acceso del colaborador (sin ventana de gracia).
- **DECISION (DEC-003):** `INVITATION.status` no incluye `REVOKED`. La revocación de acceso vive únicamente en `REMINDER_SHARE.status`.
- **DECISION (DEC-015):** al solicitar la eliminación de una cuenta, `USER.deletion_status` pasa a `PENDING_DELETION` y `purge_at` se fija a 30 días después; un job periódico purga (`deletion_status = DELETED`, anonimiza/borra datos personales) las cuentas cuyo `purge_at` ya venció. Mientras esté en `PENDING_DELETION`, el usuario puede cancelar la solicitud (revertir a `ACTIVE`) — comportamiento exacto de reversión: `TBD` de UX, no bloqueante.
- **DECISION (DEC-015, A'):** las filas de `INVITATION` en estado `REJECTED`, `EXPIRED` o `CANCELLED` deben purgar `invited_email` (o la fila completa) pasado un plazo corto de retención (ASSUMPTION: 90 días) cuando `invited_user_id` es nulo (invitado sin cuenta) — minimización de datos de terceros sin cuenta (NFR-002).
- **RECOMMENDATION (técnica, BE-029):** `AUDIT_EVENT` se escribe en la misma transacción que la operación de negocio que audita (a diferencia de push, que es best-effort) — un evento de auditoría perdido pese a que la operación tuvo éxito sería peor que no tener el log. Índices sobre `(target_type, target_id)` y sobre `occurred_at`. Sin endpoint de lectura en V1 (no está en `openapi.yaml`); es solo almacenamiento, la consulta queda fuera de alcance hasta que se decida explícitamente.
