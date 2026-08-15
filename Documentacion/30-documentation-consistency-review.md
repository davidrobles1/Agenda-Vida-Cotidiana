# 30 — Documentation Consistency Review

Revisión de consistencia cruzada de los 22 documentos autoritativos, previa a la generación del paquete DOCX/PPTX/PDF. No se corrigió nada en los documentos fuente durante esta fase — solo se identifica y reporta, como se pidió.

## DOCUMENTATION_STATUS: CONSISTENT — MINOR DRIFT, NO BLOCKERS

Ninguna decisión aprobada (DEC-001 a DEC-015) aparece contradicha en su forma resuelta; todo el drift encontrado es texto residual no actualizado tras alguna de las decisiones, o brechas de detalle/trazabilidad menores. Ninguno de los hallazgos revierte, reinterpreta o reabre una decisión ya aprobada.

## 1. Referencias caducas (stale references) — texto no actualizado tras una decisión ya aprobada

| # | Ubicación | Hallazgo | Decisión que lo vuelve caduco |
|---|---|---|---|
| A | `05-user-flows.md`, "Flujo de notificaciones push", último nodo | `"Adapter de push: FCM/APNs/Web Push - TBD por plataforma"` | DEC-010 ya fijó FCM como proveedor unificado; ya no es `TBD` |
| B | `07-backend-architecture.md`, párrafo `DECISION (ADR-006)` sobre el módulo `sharing` | Enumera los estados de invitación como `"PENDING/ACCEPTED/REJECTED/EXPIRED/REVOKED"` | DEC-003 retiró `REVOKED` de `INVITATION` (queda `CANCELLED`); `REVOKED` vive solo en `REMINDER_SHARE` |
| C | `22-decision-log.md`, ADR-007, párrafo "Decisión" original | `"Proveedor exacto por plataforma queda TBD"` | Superado por la subsección "Refinamientos cerrados" (DEC-010) inmediatamente debajo, pero sin la misma etiqueta explícita de "histórico" que sí tiene ADR-008 — un lector que solo vea el párrafo superior puede creer que sigue sin decidirse |
| D | `04-use-cases.md`, UC-02 "Iniciar sesión" | Paso 3: `"Backend crea sesión"`; no menciona Keycloak/Authorization Code + PKCE | Contradice el patrón resource-server-only fijado en ADR-008/DEC-004 (`11-auth-security.md`, `10-api-openapi.md`): el backend no crea sesión propia, valida el token emitido por Keycloak en cada request. UC-01 sí quedó actualizado con este detalle; UC-02 no |
| E | `10-api-openapi.md`, sección Authentication | Documenta `POST /api/v1/auth/logout` como endpoint V1 | `openapi/openapi.yaml` (fuente canónica) no define ese path en absoluto — ni antes ni después del pack de decisiones |
| F | `05-user-flows.md`, "Flujo de notificaciones push" | Lista de eventos disparadores no incluye "cancelada" ni "eliminación de recordatorio compartido" | `UC-11`/`FR-011`/`AC-012` sí incluyen ambos eventos tras el pack de decisiones — el diagrama quedó desactualizado respecto al texto |

## 2. Brecha de trazabilidad

**G.** La acción de **cancelar una invitación pendiente** (`FR-007`/`FR-010`, endpoint `DELETE /invitations/{invitationId}`) no tiene un caso de uso propio ni un criterio de aceptación propio, y no aparece como fila independiente en `12-traceability.md`. Crear (`UC-07`/`AC-007`), aceptar (`UC-08`/`AC-008`), rechazar (`UC-09`/`AC-009`) y revocar (`UC-10`/`AC-010`) sí tienen esa trazabilidad completa; cancelar, no. No es una contradicción de contenido, es una omisión de cobertura.

## 3. Inconsistencia de presentación (no es una contradicción de decisión)

**H.** En `03-prd.md`, el orden físico de los requisitos rompe la secuencia numérica: `FR-009`, `FR-010`, `FR-012`, `FR-013`, `FR-011` aparecen en ese orden en el archivo (en vez de `FR-009...FR-013`). Los IDs son estables y no se reutilizó ninguno — es solo el orden de lectura del documento el que quedó desordenado al insertar `FR-012`/`FR-013` antes que `FR-011` durante la edición anterior.

## 4. TBDs verificados — siguen siendo TBD (no se convirtieron en decisiones)

Se confirmó contra `25-open-questions.md` y `29-v1-final-readiness.md` que los siguientes siguen abiertos, sin cambios: nombre del producto, mercado inicial, primer grupo de validación, licencia/repositorio, cuenta real vs. modo local, modo offline, límite de colaboradores por recordatorio, formato de username, notificación al editar un recordatorio compartido, auto-vinculación de invitación pendiente al registrarse, reversión de `PENDING_DELETION`, despliegue AWS desde el inicio vs. staging, dispositivos soportados más allá de la versión de SO, patrón final de tokens en la SPA Web. Ninguno fue resuelto ni tocado en esta revisión.

## 5. Recomendaciones de corrección (no aplicadas — quedan para una pasada de edición posterior)

1. Actualizar `05-user-flows.md` para reflejar FCM (no "TBD por plataforma") y añadir "cancelada"/"eliminación" a la lista de eventos push.
2. Actualizar el párrafo `DECISION (ADR-006)` en `07-backend-architecture.md` para quitar `REVOKED` de la lista de estados de `INVITATION`.
3. Añadir a ADR-007 una etiqueta "Estado histórico previo" explícita (como ya tiene ADR-008) para que el párrafo original no se lea como vigente.
4. Actualizar `UC-02` para mencionar Keycloak/Authorization Code + PKCE y corregir "Backend crea sesión" por una descripción resource-server-only, igual que se hizo en `UC-01`.
5. Quitar `POST /api/v1/auth/logout` de `10-api-openapi.md` o añadirlo formalmente a `openapi.yaml` — decidir cuál de las dos fuentes es correcta.
6. Añadir un caso de uso y un criterio de aceptación explícitos para "cancelar invitación pendiente", y su fila en `12-traceability.md`.
7. Reordenar `FR-011`/`FR-012`/`FR-013` en `03-prd.md` a secuencia numérica ascendente (cambio cosmético, no de contenido).

## 6. Bloqueadores para la generación de documentación

**Ninguno.** Los hallazgos A–H son drift editorial y brechas de cobertura menores, no contradicciones de una decisión aprobada. Se procede a la Fase 2.

**Nota de tratamiento en los documentos generados:** para los puntos A, B, C y D, el DOCX/PPTX/PDF generados en las fases siguientes seguirán la versión **autoritativa** ya corregida (DEC-003, DEC-010, ADR-008, `11-auth-security.md`), no el texto residual señalado arriba — sin modificar los archivos Markdown fuente. Los puntos E, F, G y H se documentan tal cual están, señalando la brecha donde corresponda.
