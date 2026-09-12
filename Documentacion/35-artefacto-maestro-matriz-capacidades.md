# 35 — Artefacto maestro navegable: matriz de capacidades

**Estado:** RECOMMENDATION (pendiente de aprobación del Product Owner)
**Fecha:** 2026-09-11
**Artefacto:** https://claude.ai/code/artifact/b31707e4-2719-4a26-b7e8-b870ded5a0ec
**Referencia visual:** https://claude.ai/code/artifact/d5ffdf83-9e29-4570-a5c8-37f4e0a69fba

---

## 1. Propósito

El artefacto maestro navegable define la UX/UI de Agenda · Vida Cotidiana. Este
documento compara **lo que el artefacto exige** con **lo que el backend ya
tiene**, y especifica lo que falta crear.

La regla de precedencia aprobada por el Product Owner es explícita: cuando el
diseño y el backend no coinciden, **evoluciona el backend**. No se reduce el
alcance del diseño para evitar trabajo técnico.

---

## 2. Inventario del backend existente

Inspección directa de `backend/src/main/java/com/vidacotidiana/` el 2026-09-11.
**23 controladores**, 22 módulos de dominio:

| Módulo | Ruta base | Operaciones relevantes |
|---|---|---|
| `reminder` | `/api/v1/reminders` | CRUD + `complete` |
| `subscription` | `/api/v1/subscriptions` | CRUD + `payments` (registro, último, listado global) |
| `warranty` | `/api/v1/warranties` | CRUD + `complete` + `content` (blob propio) |
| `maintenance` | `/api/v1/maintenance-records` | CRUD + `complete` + `occurrences` |
| `inventory` | `/api/v1/inventory-items` | CRUD |
| `document` | `/api/v1/documents` | CRUD + `content`, `download`, `share`, `make-public/private` |
| `place` | `/api/v1/places` | CRUD |
| `routine` | `/api/v1/routines` | CRUD + `execute` |
| `daynote` | `/api/v1/day-notes` | CRUD + `position`, `data`, `bring-to-front` |
| `visionboard` | `/api/v1/vision-boards` | CRUD + `elements` (crear, editar, reordenar, borrar) |
| `visionboard` (img) | `/api/v1/vision-board-images` | subir, leer |
| `sharing` | `/api/v1/shared-resources` | `received`, `sent`, `part-done`, shares |
| `sharing` (recordatorio) | `/api/v1/reminders/{id}/shares` | CRUD |
| `family` | `/api/v1/family`, `/api/v1/users/search` | miembros, invitaciones, búsqueda |
| `sharing` (invitaciones) | `/api/v1/invitations` | aceptar, rechazar, cancelar |
| `person` | `/api/v1/people` | CRUD |
| `project` | `/api/v1/projects` | CRUD + `participants`, `participations` |
| `commitment` | `/api/v1/commitments` | CRUD + `resolve` |
| `objective` | `/api/v1/objectives` | CRUD |
| `resource` | `/api/v1/resources` | CRUD |
| `note` | `/api/v1/notes` | CRUD + `resolve-task-suggestion` |
| `user` | `/api/v1/me` | perfil, borrado, `modes` |
| `notification` | `/api/v1/me/devices` | registro de dispositivos |

**ASSUMPTION:** Android consume hoy 14 de esos 23 controladores. Sin consumir:
`objectives`, `places`, `routines`, `resources`.

---

## 3. Matriz — artefacto vs backend

| Capacidad exigida por el artefacto | ¿Existe? | Acción | Impacto |
|---|---|---|---|
| Tareas, pagos, garantías, mantenimientos, inventario, documentos, lugares, rutinas | Sí | Mantener | Ninguno |
| Compartidos, Familia, invitaciones, búsqueda ≥5 caracteres | Sí | Mantener | Ninguno |
| Laboral: personas, proyectos, seguimientos, objetivos, recursos, Inbox | Sí | Mantener | Android debe consumir 4 controladores que ignora |
| Vision Board (canvas, elementos, reordenar) | Sí | Mantener | Ninguno |
| Notas del día (posición, datos, traer al frente) | Sí | Mantener | Cubre la edición a doble clic sin cambios |
| **Ánimo del día (Bienestar)** | **No** | **Crear** | Módulo nuevo + 2 tablas + 4 endpoints |
| **Prioridad de una tarea** | **No** | **Crear** | 1 columna + migración + DTO |
| **Pasos dentro de una tarea** | **No** | **Crear** | 1 tabla + endpoints anidados |
| **Contador diario de un hábito** | **No** | **Crear** | 2 columnas + 1 tabla |
| **Adjuntos en cualquier recurso** | Parcial | **Adaptar** | 2 columnas en `document` + migración del blob de `warranty` |
| **Deep link desde notificación push** | Parcial | **Adaptar** | Id del recurso en el payload + `FirebaseMessagingService` en Android |

---

## 4. Especificación de lo que falta

### 4.1 Módulo `mood` — Ánimo del día

**Por qué.** El artefacto sitúa «¿Cómo te sientes hoy?» en Inicio y le da
sección propia. No existe nada equivalente en el backend.

**Entidades**

```
mood_entry
  id              UUID PK
  owner_user_id   UUID NOT NULL
  entry_date      DATE NOT NULL
  value           SMALLINT NOT NULL      -- 0 genial … 4 bajo
  note            TEXT NULL
  version         INT NOT NULL
  created_at      TIMESTAMPTZ NOT NULL
  updated_at      TIMESTAMPTZ NOT NULL
  CONSTRAINT uq_mood_entry_user_date UNIQUE (owner_user_id, entry_date)
  CONSTRAINT ck_mood_value CHECK (value BETWEEN 0 AND 4)

mood_entry_tag
  mood_entry_id   UUID FK → mood_entry(id) ON DELETE CASCADE
  tag             VARCHAR(40) NOT NULL
  PRIMARY KEY (mood_entry_id, tag)
```

Índice: `idx_mood_entry_owner_date (owner_user_id, entry_date DESC)`.

**Endpoints**

| Método | Ruta | Nota |
|---|---|---|
| `POST` | `/api/v1/moods` | Upsert por fecha. Idempotente sobre `(owner, date)` |
| `GET` | `/api/v1/moods` | Rango `from`/`to`, paginado |
| `GET` | `/api/v1/moods/{date}` | Un día |
| `DELETE` | `/api/v1/moods` | Borrado total del historial (Ajustes → Privacidad) |

**DTO:** `MoodEntryDto(date, value, note, tags[])`.

**Privacidad — esto no es opcional.** El ánimo es dato de salud. No se comparte
nunca (no entra en `shared-resources`), no aparece en exportaciones de familia,
y `DELETE /api/v1/moods` debe ser borrado duro, no soft delete. Requiere
actualizar `05-data/data-classification.md` y `privacy.md` antes de implementar.

**TBD:** ¿Entra en V2, V3 o V4? El roadmap actual no lo contempla.

---

### 4.2 `Reminder.priority`

**Por qué.** La pantalla Crear pide «¿Cuánto aprieta?» con tres valores y el
Detalle la muestra como propiedad.

```sql
ALTER TABLE reminder
  ADD COLUMN priority VARCHAR(8) NOT NULL DEFAULT 'NORMAL';
-- valores: LOW | NORMAL | URGENT
CREATE INDEX idx_reminder_owner_priority ON reminder (owner_user_id, priority);
```

`ReminderDto` gana `priority`; `GET /api/v1/reminders` gana el filtro
`?priority=`. Sin reescribir nada: la columna tiene DEFAULT.

---

### 4.3 `reminder_step` — Pasos de una tarea

**Por qué.** El anillo de porcentaje del artefacto se deriva de los pasos. Hoy
no hay dónde guardarlos.

```
reminder_step
  id            UUID PK
  reminder_id   UUID FK → reminder(id) ON DELETE CASCADE
  title         VARCHAR(200) NOT NULL
  done          BOOLEAN NOT NULL DEFAULT false
  position      INT NOT NULL
  created_at    TIMESTAMPTZ NOT NULL
  updated_at    TIMESTAMPTZ NOT NULL
```

Índice: `idx_reminder_step_reminder (reminder_id, position)`.

**Endpoints:** `POST|GET /api/v1/reminders/{id}/steps`,
`PATCH|DELETE /api/v1/reminders/{id}/steps/{stepId}`,
`POST /api/v1/reminders/{id}/steps/reorder`.

**DECISION recomendada:** el porcentaje **se calcula, no se guarda**. Guardarlo
crearía dos fuentes de verdad para el mismo número.

---

### 4.4 Contador diario de hábito

**Por qué.** Los anillos «4 de 8» de Rutinas. Hoy `Routine` solo tiene
`frequency`, `nextExecutionDate` y `active`.

```sql
ALTER TABLE routine
  ADD COLUMN target_count INT NULL,        -- NULL = rutina de sí/no, como hoy
  ADD COLUMN unit VARCHAR(20) NULL;        -- 'vasos', 'min', 'km', 'cap'
```

```
routine_progress
  id             UUID PK
  routine_id     UUID FK → routine(id) ON DELETE CASCADE
  progress_date  DATE NOT NULL
  count          INT NOT NULL DEFAULT 0
  CONSTRAINT uq_routine_progress UNIQUE (routine_id, progress_date)
```

**Endpoints:** `POST /api/v1/routines/{id}/progress` (incremento),
`GET /api/v1/routines/{id}/progress?from=&to=`.

**Compatibilidad:** con `target_count` NULL la rutina se comporta exactamente
como hoy. Nada existente se rompe.

---

### 4.5 Adjuntos genéricos — adaptar `document`

**Por qué.** El artefacto muestra adjuntos en tareas y mantenimientos. Hoy
`document` solo enlaza `person_id` y `project_id`, y `warranty` guarda su
**propio** blob (`document_content_type`, `document_size_bytes`) — una segunda
mecánica de archivos que viola el principio de no duplicación.

```sql
ALTER TABLE document
  ADD COLUMN resource_type VARCHAR(30) NULL,   -- REMINDER | MAINTENANCE | WARRANTY | INVENTORY_ITEM | SUBSCRIPTION
  ADD COLUMN resource_id   UUID NULL;
CREATE INDEX idx_document_resource ON document (resource_type, resource_id);
```

Migración de datos: cada `warranty` con blob pasa a ser un `document` con
`resource_type='WARRANTY'`. Después, las dos columnas de `warranty` quedan
obsoletas y se retiran en una segunda migración (nunca en la misma).

**RECOMMENDATION:** mantener `person_id`/`project_id` como están. Son enlaces de
dominio, no adjuntos, y migrarlos no aporta nada.

---

### 4.6 Deep link desde push

El envío ya existe (`PushNotificationSender` + `FcmPushNotificationSender`,
invocado desde sharing, family y reminder) y Android registra el token en
`/api/v1/me/devices`. Faltan dos cosas:

1. **Backend:** el payload lleva solo `type` + `message`. Añadir `resourceId` y
   `resourceType` para poder abrir el registro concreto.
2. **Android:** no existe `FirebaseMessagingService` — nadie recoge los
   mensajes.

**TBD:** confirmar si FCM está configurado en el despliegue (existe también un
`NoOpPushNotificationSender`).

---

## 5. Reconciliaciones hechas al construir el artefacto

Dos puntos donde el artefacto de referencia y una decisión aprobada no
coincidían. Se resolvieron a favor de la decisión aprobada, y se declaran aquí
en vez de aplicarse en silencio:

1. **Barra inferior de Personal.** El artefacto de referencia mostraba
   `Inicio · Día · + · Bienestar · Menú`. La navegación aprobada en el código
   (`bottomDestinations`) es `Inicio · Calendario · + · Pagos · Vision Board`.
   Se conserva la aprobada; el **aspecto** de la barra es el del artefacto.
   Bienestar y Tu día se alcanzan desde Inicio. «Menú» (comidas) no es un módulo
   del producto y se descartó.

2. **Módulo «Comidas».** Aparecía en una propuesta anterior. No está en la lista
   de secciones aprobadas ni en el roadmap. **Fuera del artefacto maestro.**

---

## 6. Lo que el artefacto NO hace

- No inventa datos para tapar un hueco. Las cuatro capacidades que faltan están
  declaradas en la propia aplicación (Ajustes → Capacidades).
- No abre decisiones cerradas: navegación, alertas ADR-018, Vision Board, Notas
  del Día, Familia, Pagos/ADR-020, separación Personal/Laboral y temas ADR-023
  se respetan tal cual.
- No introduce finanzas. Los importes siguen siendo una propiedad de Pagos.

---

## 7. Pendiente de decisión del Product Owner

| # | Pregunta | Bloquea |
|---|---|---|
| 1 | ¿Se aprueba el módulo `mood`? Es dato de salud y cambia la clasificación de datos | Bienestar entero |
| 2 | ¿En qué versión entran prioridad y pasos de tarea? | Crear y Detalle |
| 3 | ¿Los hábitos con contador son producto, o basta con rutinas de sí/no? | Anillos de Rutinas |
| 4 | ¿Se migra el blob de `warranty` a `document` ahora o en V3? | Adjuntos genéricos |
