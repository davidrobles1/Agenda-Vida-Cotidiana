# 37 — Clasificación de datos

**Estado:** DECISION para `mood`; inventario del resto derivado del esquema real
**Fecha:** 2026-09-11
**Origen:** implementación de `35-artefacto-maestro-matriz-capacidades.md` §4.1

> **Nota de corrección.** El documento 35 cita este contenido como
> `05-data/data-classification.md`. Esa ruta es la de la estructura *propuesta*
> en `CLAUDE.md`, no la del repositorio, que usa numeración plana. El documento
> vive aquí.

---

## 1. Niveles

| Nivel | Qué significa | Consecuencia |
|---|---|---|
| **Operativo** | Datos del funcionamiento del producto | Reglas generales del sistema |
| **Personal** | Identifican a una persona | Minimización, borrado con la cuenta |
| **Sensible — salud** | Estado físico o emocional | Reglas reforzadas (§3) |

---

## 2. Inventario por tabla

| Tabla | Nivel | Nota |
|---|---|---|
| `users` | Personal | Correo y nombre. Borrado suave 30 días (DEC-015) |
| `reminders`, `reminder_steps` | Personal | Texto libre del usuario |
| `subscriptions`, `payment_records` | Personal | Importes — ADR-020, solo aquí |
| `warranties`, `maintenance_records`, `inventory_items` | Personal | Bienes del hogar |
| `documents` | Personal | Contenido arbitrario subido por el usuario |
| `places` | Personal | Direcciones en texto libre |
| `routines`, `routine_progress` | Personal | Hábitos: qué y cuánto, sin interpretación |
| `notes`, `day_note_elements` | Personal | Texto libre |
| `people`, `projects`, `commitments`, `objectives`, `resources` | Personal | Ámbito laboral |
| `vision_boards`, `vision_board_elements`, `vision_board_images` | Personal | Imágenes y texto |
| `resource_shares`, `family_links`, `family_invitations` | Personal | Relaciones entre usuarios |
| `device_push_tokens` | Operativo | Identificador de dispositivo |
| `audit_events` | Operativo | Trazabilidad técnica |
| **`mood_entry`, `mood_entry_tag`** | **Sensible — salud** | **Ver §3** |

---

## 3. `mood_entry` — reglas reforzadas

El ánimo del día no es una preferencia: es información sobre el estado
emocional de una persona. Las cuatro reglas siguientes vienen de
`35-…-capacidades.md` §4.1 y están implementadas en V36 y en el módulo `mood`.
**No se añade ninguna regla que no esté en esa especificación.**

### 3.1 No se comparte — nunca

`mood_entry` no participa en `resource_shares` ni en `family_links`. No existe
endpoint de compartir y no debe crearse. Ni siquiera quien comparte tareas
contigo puede leerlo.

*Dónde está implementado:* `MoodController` no expone ruta de compartir;
`MoodEntryRepository` exige `ownerUserId` en **todas** sus consultas, de modo
que la pertenencia va en la firma y no en una comprobación que pueda olvidarse.

### 3.2 El borrado es duro

`DELETE /api/v1/moods` elimina filas. No marca `deleted_at`, no archiva. Las
etiquetas caen por `ON DELETE CASCADE`.

*Dónde:* `MoodEntryRepository.deleteAllByOwnerUserId` con `@Modifying`; V36
define el `CASCADE`.

### 3.3 No entra en exportaciones

Ninguna salida de familia o de cuenta incluye el ánimo.

### 3.4 Granularidad de día, no de instante

`entry_date` es `DATE`. Guardar la hora permitiría responder «¿a qué hora
estabas mal?», que es precisamente la pregunta que el producto no hace. La
restricción `UNIQUE (owner_user_id, entry_date)` implica además que volver a
marcar **sustituye**: no se conserva un registro de altibajos dentro del día.

---

## 4. Lo que esta clasificación NO cambia

- No introduce cifrado a nivel de columna. El cifrado en reposo sigue siendo el
  del volumen, como para el resto del esquema.
- No define un período de retención propio: el ánimo vive mientras viva la
  cuenta, y cae con ella por `ON DELETE CASCADE` sobre `users`.
- No habilita analítica, agregados entre usuarios ni exportación a terceros.

Cualquiera de esas tres cosas sería una decisión nueva y exigiría su propio ADR.

---

## 5. Pendiente de firma del Product Owner

Este documento recoge lo implementado. Queda pendiente tu confirmación de que
la clasificación de `mood_entry` como **sensible — salud**, con las cuatro
reglas de §3, es la aprobada.
