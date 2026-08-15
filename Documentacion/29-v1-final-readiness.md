# 29 — V1 Final Readiness

Auditoría de consistencia posterior al cierre de las 15 decisiones de `28-v1-decision-pack.md` (aprobadas 2026-08-09). No se ha escrito código. No se generó DOCX/PPTX — solo Markdown/OpenAPI.

## 1. Documentos modificados en este ciclo

- `22-decision-log.md` — ADR-006/007/008 con refinamientos (DEC-001/002/003/005/010/014); ADR-008 pasa de Proposed a Accepted (Keycloak); nuevos ADR-009 (AWS), ADR-010 (Swift/SwiftUI), ADR-011 (React/TS SPA), ADR-012 (retención de cuenta).
- `09-data-model.md` — enum `INVITATION` corregido; `ON DELETE CASCADE` documentado; entidad `DEVICE_PUSH_TOKEN` añadida; campos de soft delete en `USER`; constraints de unicidad (`email`, `username`, `REMINDER_SHARE`, índice parcial de invitación pendiente) añadidas; confirmación de estado único de completado.
- `openapi/openapi.yaml` — enum `Invitation.status` corregido; endpoints nuevos `DELETE /me`, `GET/POST /me/devices`, `DELETE /me/devices/{deviceId}`; schemas `RegisterDeviceRequest`/`DevicePushToken`. Validado estructuralmente (YAML parseable, referencias `$ref` resueltas, todas las operaciones con `responses`).
- `10-api-openapi.md` — sección "Authentication" corregida (sin `POST /auth/login` propio); nuevos endpoints de dispositivos y eliminación de cuenta documentados; código `202` añadido.
- `03-prd.md` — FR-001 (Keycloak, verificación delegada), FR-009 (estado único), FR-010 (cascada + cancelación vs. revocación), FR-011 (FCM), FR-012 (nuevo, registro de dispositivo), FR-013 (nuevo, eliminar cuenta), NFR-008 (versiones mínimas), NFR-010 (nuevo, retención).
- `04-use-cases.md` — UC-01 (verificación vía Keycloak), UC-05 (cascada + notificación), UC-10 (aclarado: solo `REMINDER_SHARE`), UC-11 (evento de eliminación añadido), UC-12 (nuevo, registrar dispositivo), UC-13 (nuevo, eliminar cuenta).
- `13-acceptance.md` — AC-005 aclarado; AC-012 ampliado; AC-013 a AC-016 nuevos.
- `12-traceability.md` — filas para FR-012, FR-013, NFR-010, UC-12, UC-13; nota de cambios de este ciclo.
- `06-c4.md` — Identity Provider → Keycloak; push → FCM; correo → SES; iOS → SwiftUI; Web → React/TS SPA; nota de despliegue AWS.
- `07-backend-architecture.md` — adapter FCM concreto; `DEVICE_PUSH_TOKEN`; adapter SES; jobs de purga (cuenta e invitaciones).
- `08-android-architecture.md` — `minSdk 30`; registro/baja de token FCM.
- `08b-ios-architecture.md` (nuevo) — arquitectura iOS (Swift + SwiftUI, iOS 17 mínimo).
- `08c-web-architecture.md` (nuevo) — arquitectura Web (React + TypeScript SPA, navegadores soportados, nota de manejo de tokens pendiente).
- `11-auth-security.md` — Keycloak concreto; verificación de email delegada; corrección de conflicto de login; aclaración cancelación vs. revocación; SEC-003 (retención); nota de manejo de tokens en SPA Web.
- `21-security.md` — dos amenazas nuevas (robo de token en SPA, retención excesiva de PII de terceros).
- `01-scope.md` — todos los supuestos antes `TBD` ahora `DECISION` con su valor; nueva mención de eliminación de cuenta en el alcance de V1.
- `README.md` — stack actualizado con las 15 decisiones; sección "Estado" reescrita.
- `26-v1-backlog.md` — US-016 (eliminar cuenta), US-017 (registrar dispositivo push).
- `25-open-questions.md` — 15 preguntas movidas a "Resueltas"; lista de pendientes reducida y reorganizada (ninguna de las 15 decisiones de este pack permanece abierta).
- `28-v1-decision-pack.md` — los 15 `DECISION: TBD` reemplazados por el valor aprobado, con fecha.
- `AI-CONTEXT.md` — reescrito para reflejar las 15 decisiones aprobadas (arquitectura, stack, ADRs, reglas de seguridad, prohibiciones, TBDs restantes).

## 2. Contradicciones corregidas

1. **`DOCUMENTATION_CONFLICT` de `POST /auth/login`** (señalado en `27-v1-readiness-review.md` §4.1): corregido. La aplicación no implementa login propio; Keycloak emite los tokens directamente a los clientes, el backend es resource server. Reflejado en `10-api-openapi.md`, `openapi.yaml` (no se añadió tal endpoint), `11-auth-security.md` y `22-decision-log.md` (ADR-008).
2. **Lifecycle de `INVITATION` incorrecto** (señalado en `27-v1-readiness-review.md` §2.3 y §4.2): corregido. Enum ahora `PENDING, ACCEPTED, REJECTED, EXPIRED, CANCELLED`; `REVOKED` retirado de `INVITATION` y confirmado exclusivo de `REMINDER_SHARE`. Reflejado en `09-data-model.md`, `openapi.yaml`, `03-prd.md`, `04-use-cases.md`, `11-auth-security.md`.
3. **Ambigüedad de "completado" entre `FR-009` y el modelo de datos** (señalada en `27-v1-readiness-review.md` §2.1 y §5): corregida. Se confirmó estado único global; `FR-009`/`AC-005` reescritos para no sugerir completado individual por colaborador.
4. **Comportamiento indefinido al eliminar un recordatorio compartido** (señalado en `27-v1-readiness-review.md` §2.2): corregido. Cascada + notificación push a colaboradores activos, documentado en `09-data-model.md`, `FR-010`, `UC-05`, `AC-013`.
5. **Falta de entidad de dispositivo/push token** (señalada en `27-v1-readiness-review.md` §2.5 y §5): corregida. `DEVICE_PUSH_TOKEN` añadida al modelo de datos y a `openapi.yaml`.

### Corregidas como efecto colateral (no eran parte de las 15 decisiones, pero quedaron resueltas al tocar `09-data-model.md`)
- Falta de unicidad en `USER.email`/`USER.username` (señalada en `27-v1-readiness-review.md` §5) — añadida.
- Falta de constraint que respalde el 409 de invitación duplicada (`AC-007`) — añadido índice único parcial.
- Falta de constraint contra colaboraciones duplicadas — añadido unique en `REMINDER_SHARE`.

## 3. TBDs que siguen bloqueando V1

**Ninguno de los bloqueadores identificados en `27-v1-readiness-review.md` sigue abierto.** Los cinco bloqueadores de esa revisión (semántica de completado, eliminación con colaboradores, lifecycle de `INVITATION`, proveedor OIDC, almacenamiento de push tokens) quedaron resueltos por las 15 decisiones de este pack.

No se identifican nuevos bloqueadores introducidos por estas decisiones.

## 4. TBDs que NO bloquean V1 (quedan abiertos, ver `25-open-questions.md`)

**Producto/negocio:** nombre del producto, mercado inicial, primer grupo de validación, licencia/repositorio, límite máximo de colaboradores por recordatorio, formato/reglas del username, notificación al colaborador cuando el propietario edita el recordatorio, auto-vinculación de una invitación pendiente si el invitado se registra después, comportamiento exacto de reversión durante `PENDING_DELETION`, modo offline / cuenta real vs. local.

**Técnicos, no bloqueantes:**
- Patrón final de manejo de tokens OIDC en el cliente Web SPA (memoria + renovación silenciosa vs. alternativas) — documentado como consideración pendiente en `08c-web-architecture.md`, no bloquea el resto de V1.
- Entidad `AUDIT_EVENT`/`AUDIT_LOG` explícita en el modelo de datos (mencionada solo conceptualmente) — pendiente.
- Versiones exactas de Spring Boot/Gradle/AGP/Kotlin/Xcode (`17-dependencies.md`) — se fijan al hacer el bootstrap del proyecto.
- Dispositivos soportados más allá de la versión mínima de SO (gama, RAM) — no cubierto por DEC-011/012/013.

**Actualización (auditoría "V1 development gate", ver `32-v1-development-gate-audit.md`):** la formalización de `openapi.yaml` mencionada en la versión original de este párrafo (`securitySchemes`/OIDC, schema `Error` reutilizable, `requestBody` para `POST`/`PATCH /reminders`, referencias `$ref` de `Invitation`/`ReminderShare`/`Reminder` en respuestas, paginación en endpoints de listado) **ya fue completada** en ese ciclo posterior — ver `32-v1-development-gate-audit.md` para el detalle. Este párrafo se conserva como registro histórico del estado en 2026-08-09; no reabre nada.

## 5. Auditoría de consistencia cruzada — resultado

Se verificó cruzando `AI-CONTEXT.md`, `22-decision-log.md`, `01-scope.md`, `03-prd.md`, `04-use-cases.md`, `13-acceptance.md`, `12-traceability.md`, `09-data-model.md`, `openapi.yaml`/`10-api-openapi.md`, `11-auth-security.md`, `21-security.md`, `06-c4.md`, `07-backend-architecture.md`, `08-android-architecture.md`, `08b-ios-architecture.md`, `08c-web-architecture.md`, `26-v1-backlog.md`, `25-open-questions.md` y `28-v1-decision-pack.md`.

- **IDs:** no se eliminó ni reutilizó ningún ID existente. Nuevos: `FR-012`, `FR-013`, `NFR-010`, `UC-12`, `UC-13`, `AC-013` a `AC-016`, `ADR-009` a `ADR-012`, `SEC-003`. Se corrigió un hueco de numeración detectado durante esta misma auditoría (UC-13/UC-14 se renombraron a UC-12/UC-13 antes de publicarse en ningún otro documento, para no dejar un salto en la secuencia).
- **openapi.yaml:** validado estructuralmente (YAML parseable; 12 `paths`; todas las operaciones con `responses`; todas las referencias `$ref` resuelven contra `components.schemas`). Gaps preexistentes no resueltos (ver §4) quedan explícitamente listados, no ocultos.
- **Coherencia PRD ↔ casos de uso ↔ criterios de aceptación ↔ modelo de datos ↔ API:** verificada para las 15 decisiones; no se detectaron nuevas contradicciones introducidas por este ciclo.
- **AI-CONTEXT.md:** actualizado únicamente con las decisiones aprobadas explícitamente en este ciclo, tal como se pidió; no contiene ninguna decisión no aprobada por el Product Owner.

## V1_READINESS_STATUS: READY (para iniciar el desarrollo del núcleo de V1)

No quedan bloqueadores de los identificados en `27-v1-readiness-review.md`. Los TBDs restantes (§4) son de producto (nombre, mercado, límites de UX menores) o detalles de implementación no bloqueantes (patrón de tokens en Web SPA, formalización adicional de `openapi.yaml`, tabla de auditoría) que pueden resolverse en paralelo al desarrollo sin rehacer lo ya decidido.

Recomendación antes de escribir la primera migración Flyway: revisar una vez más `09-data-model.md` en conjunto con quien implemente el backend, dado el volumen de cambios de este ciclo (nueva entidad, nuevos constraints, cascadas).
