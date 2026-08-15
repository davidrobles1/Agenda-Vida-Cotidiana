# 12 — Matriz de trazabilidad

Cadena exigida: `Requirement → Use Case → Acceptance Criteria → API → Data Model → Architecture → Backlog → Test → Version`.

**Nota sobre la columna Test:** el proyecto está en fase de documentación pre-desarrollo (no existe código todavía, ver `AI-CONTEXT.md`). "Pendiente (pre-código)" es el estado esperado y honesto para toda la columna Test en este momento — no es un gap oculto ni una contradicción; se resolverá al implementar cada requisito según `20-testing-qa.md`. Ningún requisito queda con una celda vacía o sin valor.

| Req | Caso de uso | Criterio de aceptación | API | Modelo de datos | Arquitectura (módulo) | Backlog | Test | Versión |
|---|---|---|---|---|---|---|---|---|
| FR-001 | UC-01 | AC-001 | Auth vía Keycloak (sin endpoint propio) | USER | identity | US-001 | Pendiente (pre-código) | V1 |
| FR-002 | UC-02, UC-06 | AC-002 | Auth vía Keycloak (resource server) | USER | identity | US-002, US-009 | Pendiente (pre-código) | V1 |
| FR-003 | UC-03, UC-04 | AC-003, AC-005 | `GET /reminders` | REMINDER | reminder | US-003 | Pendiente (pre-código) | V1 |
| FR-004 | UC-03, UC-04, UC-05 | AC-003, AC-004, AC-004b, AC-005, AC-006 | `GET/POST /reminders`, `GET/PATCH/DELETE /reminders/{id}` | REMINDER | reminder | US-004, US-005, US-006, US-007 | `ReminderServiceTest`, `ReminderControllerIntegrationTest` (30 casos, `./mvnw clean test` real, ver `docs/development/02-validation-report.md` §8/§10) | V1 |
| FR-005 | UC-03, UC-11 | AC-012 (evento local, sin backend) | N/A — resuelto en el cliente | REMINDER.due_at | notification (cliente) | US-008 | Pendiente (pre-código) | V1 |
| FR-006 | UC-06 | AC-002 | `GET /me` | USER | user | US-009 | Pendiente (pre-código) | V1 |
| FR-007 | UC-07, UC-14 | AC-007, AC-017 | `POST /reminders/{id}/shares`, `DELETE /invitations/{invitationId}` | INVITATION | sharing | US-011 | `SharingServiceTest`/`SharingFlowIntegrationTest`: crear invitación (email con/sin cuenta, username existente/inexistente, duplicada→409, no-owner→404) y cancelar (owner→204, no-inviter→404, ya resuelta→410) — `./mvnw clean test` real, 72/72, ver `docs/development/02-validation-report.md` §11 | V1 |
| FR-008 | UC-08 | AC-008 | `POST /invitations/{invitationId}/accept` | INVITATION, REMINDER_SHARE | sharing | US-012 | `SharingServiceTest.acceptInvitation_*`/`SharingFlowIntegrationTest.acceptInvitation_*`: happy path crea `REMINDER_SHARE` ACTIVE; segunda resolución sobre la misma invitación → 410 (transición atómica condicional, AC-008) | V1 |
| FR-009 | UC-07, UC-08, UC-10 | AC-005, AC-011 | Autorización en `/reminders*` (OWNER/COLLABORATOR) | REMINDER, REMINDER_SHARE | reminder, sharing (autorización) | US-011, US-013 | `ReminderServiceTest` (+4 casos BE-022: colaborador activo lee/completa, no edita/elimina), `SharingFlowIntegrationTest.collaborator_canReadAndCompleteButNeverEditDeleteOrInvite`, `.acceptInvitation_happyPath_createsActiveShareAndGrantsAccess` (incluye `GET /reminders` con compartidos) | V1 |
| FR-010 | UC-09, UC-10, UC-14 | AC-009, AC-010, AC-017 | `POST /invitations/{id}/reject`, `DELETE /reminders/{id}/shares/{shareId}`, `DELETE /invitations/{id}`, cascada en `DELETE /reminders/{id}` | INVITATION, REMINDER_SHARE | sharing | US-012, US-013 | `SharingServiceTest`/`SharingFlowIntegrationTest`: rechazar (happy path + ya resuelta→410), revocar (efecto inmediato, colaborador revocado→404 sin ventana de gracia), cancelar (ver FR-007) | V1 |
| FR-011 | UC-11 | AC-012 | Evento interno entregado vía FCM (`PushNotificationSender`) | DEVICE_PUSH_TOKEN | notification | US-014 | `FcmPushNotificationSenderTest` (construcción de payload, envío real no verificable en este entorno), `SharingServiceTest`/`ReminderServiceTest`/`SharingFlowIntegrationTest` (eventos conectados) — `./mvnw clean test` real, 101/101, ver `docs/development/02-validation-report.md` §12 | V1 |
| FR-012 | UC-12 | AC-014 | `GET/POST /me/devices`, `DELETE /me/devices/{deviceId}` | DEVICE_PUSH_TOKEN | notification | US-017 | `DeviceRegistrationServiceTest` (7 casos), `DeviceControllerIntegrationTest` (5 casos, incluye el `403` real de AC-014 sobre dispositivo ajeno) | V1 |
| FR-013 | UC-13 | AC-015 | `DELETE /me` | USER | user | US-016 | `AccountDeletionServiceTest` (3 casos), `AccountDeletionIntegrationTest` (2 casos, purga real forzando `purge_at` al pasado), `UserSyncServiceTest` (fix real BE-034: cuenta purgada no se re-sincroniza desde el JWT) | V1 |
| NFR-001 | — (transversal) | AC-006 | Todos los endpoints (HTTPS, autorización por recurso) | Todas las entidades | shared / seguridad transversal | — (transversal) | Pendiente (pre-código) | V1 |
| NFR-002 | — (transversal) | AC-016 | N/A | USER, INVITATION (minimización) | user, sharing | — (transversal) | Pendiente (pre-código) | V1 |
| NFR-005 | — (transversal) | — (calidad de código, sin AC de negocio) | N/A | N/A | Todos los módulos (Clean Architecture) | — (transversal) | Pendiente (pre-código) | V1 |
| NFR-006 | — (transversal) | AC-006 | Error envelope (`Error` schema) en todos los endpoints | N/A | shared (logging/auditoría) | — (transversal) | Pendiente (pre-código) | V1 |
| NFR-008 | — (cliente) | — (sin AC de negocio; ver `03-prd.md`) | N/A | N/A | Android/iOS/Web architecture | US-015 | Pendiente (pre-código) | V1 |
| NFR-009 | — (transversal) | — (implícito en AC-001 a AC-017, mismo comportamiento en las 3 plataformas) | Todos los endpoints | Modelo de datos compartido | Android, iOS, Web, backend | US-015 | Pendiente (pre-código) | V1 |
| NFR-010 | UC-13 | AC-015, AC-016 | `DELETE /me` | USER, INVITATION | user (purga de cuenta), sharing (purga de invitaciones) | US-016 | Pendiente (pre-código) | V1 |

## Cambios de este ciclo (auditoría V1 development gate)
- Se reconstruyó la matriz para cerrar la cadena completa `Requirement → UC → AC → API → Data Model → Architecture → Backlog → Test`; la versión anterior carecía de las columnas AC, Data Model, Architecture y Backlog, y las columnas Test/Estado eran `TBD` sin explicación.
- Se añadieron las filas de `UC-14`/`AC-017` (cancelar invitación pendiente), que no tenían trazabilidad propia (gap detectado en `30-documentation-consistency-review.md` hallazgo G).
- Se referencian por primera vez los IDs de backlog (`US-001` a `US-017`, `26-v1-backlog.md`) desde la matriz de trazabilidad.

## Cambios del ciclo anterior (28-v1-decision-pack.md, 2026-08-09)
- FR-009/FR-010/UC-05/UC-10/AC-005/AC-013 actualizados por DEC-001/DEC-002/DEC-003.
- FR-001/UC-01 actualizados por DEC-004/DEC-014.
- FR-011/FR-012/UC-11/UC-12/AC-012/AC-014 actualizados/creados por DEC-005/DEC-010.
- FR-013/UC-13/AC-015/AC-016/NFR-010 creados por DEC-015.
- NFR-008 actualizado por DEC-011/DEC-012/DEC-013.

Regla: ningún requisito aprobado entra a desarrollo sin identificador estable, caso de uso, criterio de aceptación y mapeo a API/modelo de datos/arquitectura/backlog.
