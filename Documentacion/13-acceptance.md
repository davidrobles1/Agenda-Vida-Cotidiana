# 13 — Criterios de aceptación

## AC-001 Registro
- Dado un usuario válido, cuando completa el registro, entonces la cuenta queda creada.
- Datos inválidos deben rechazarse.
- No se deben revelar detalles que permitan enumerar cuentas.

## AC-002 Login
- Credenciales válidas permiten acceso.
- Credenciales inválidas producen error genérico.
- Sesión válida permite consumir recursos autorizados.

## AC-003 Crear recordatorio
- Título obligatorio.
- Longitud máxima respetada.
- El recordatorio pertenece al usuario autenticado.
- Respuesta 201 contiene identificador.

## AC-004 Acceso a recordatorio
- Propietario puede consultar.
- Otro usuario recibe 404 o 403 según política definida, sin revelar existencia.

## AC-004b Editar recordatorio (bloqueo optimista)
- Solo el propietario puede editar (`PATCH /reminders/{id}`).
- El request debe incluir `version`; si no coincide con la versión actual almacenada, el backend responde `409` (`REMINDER_VERSION_CONFLICT`) sin aplicar el cambio, evitando sobrescribir una edición o un cambio de estado concurrente.
- Una edición exitosa incrementa `version`.

## AC-005 Completar
- Estado cambia a COMPLETED (estado único global del recordatorio, DEC-001; no hay estado por colaborador).
- Operación repetida es segura/idempotente.
- Cualquier usuario con acceso (propietario o colaborador `ACTIVE`) puede completar o revertir a pendiente; el cambio es visible para todos los que tienen acceso al recordatorio.
- Si el request incluye `version`, el backend la valida (bloqueo optimista) y responde `409` (`REMINDER_VERSION_CONFLICT`) si no coincide con la versión actual; si se omite, la operación se aplica sin verificación de concurrencia.

## AC-006 Error
- Backend no expone stack trace.
- Respuesta contiene traceId.

## AC-007 Compartir recordatorio
- Solo el propietario puede crear una invitación sobre su recordatorio.
- La invitación se crea en estado PENDING con expiración a 7 días.
- La respuesta no revela si el email pertenece a una cuenta existente.
- Un destinatario duplicado con invitación pendiente produce 409.

## AC-008 Aceptar invitación
- Solo el destinatario puede aceptar.
- Aceptar una invitación expirada o ya resuelta produce 410, sin otorgar acceso.
- Al aceptar, se crea `REMINDER_SHARE` en estado ACTIVE con permisos de colaborador.
- La transición `PENDING → ACCEPTED` se ejecuta como una actualización condicional atómica; si dos requests concurrentes intentan resolver la misma invitación (p. ej. aceptar y cancelar simultáneamente), solo una tiene éxito y la otra recibe 410.

## AC-009 Rechazar invitación
- Solo el destinatario puede rechazar.
- Rechazar no otorga ningún acceso y notifica al propietario.

## AC-010 Revocar acceso
- Solo el propietario puede revocar el acceso de un colaborador.
- La revocación es efectiva de inmediato: una solicitud posterior del colaborador sobre ese recordatorio responde 403/404 según política definida.

## AC-011 Permisos de colaborador
- Un colaborador puede ver y completar/deshacer completado.
- Un colaborador que intenta editar, eliminar o re-compartir recibe 403.

## AC-012 Notificación push
- Los eventos de invitación (recibida/aceptada/rechazada/cancelada), revocación, eliminación de recordatorio compartido y cambios en recordatorios compartidos generan una notificación push al usuario afectado, enviada vía FCM (DEC-010).
- El fallo del proveedor de push no debe romper la operación principal (creación/aceptación/eliminación/etc.), solo la notificación (best-effort).

## AC-013 Eliminar recordatorio compartido (DEC-002)
- Solo el propietario puede eliminar un recordatorio.
- Si existen colaboradores con `REMINDER_SHARE.status = ACTIVE`, cada uno recibe una notificación push antes/al momento de la eliminación.
- La eliminación no se bloquea por la existencia de colaboradores activos.
- Las filas de `INVITATION`/`REMINDER_SHARE` asociadas se eliminan en cascada.

## AC-014 Registro de dispositivo push (FR-012)
- Un usuario autenticado puede registrar un token de push para el dispositivo actual (`POST /me/devices`).
- Un usuario puede tener múltiples dispositivos registrados simultáneamente.
- Un usuario solo puede eliminar sus propios registros de dispositivo (403 en caso contrario).

## AC-015 Eliminar cuenta (DEC-015)
- Un usuario autenticado puede solicitar la eliminación de su cuenta (`DELETE /me`), respondiendo `202 Accepted`.
- La cuenta pasa a `PENDING_DELETION`; los datos personales no se eliminan de inmediato.
- Transcurridos 30 días desde la solicitud, un proceso periódico purga definitivamente los datos personales de la cuenta.

## AC-016 Purga de invitaciones sin cuenta asociada (DEC-015, A')
- Las invitaciones en estado `REJECTED`, `EXPIRED` o `CANCELLED` cuyo destinatario nunca creó una cuenta (`invited_user_id` nulo) deben purgar el email asociado pasado un plazo corto de retención (ASSUMPTION: 90 días).

## AC-017 Cancelar invitación pendiente (UC-14)
- Solo quien creó la invitación (inviter/propietario) puede cancelarla.
- Cancelar actúa exclusivamente sobre `INVITATION` en estado `PENDING`, transicionando a `CANCELLED` (`DELETE /invitations/{invitationId}`); nunca produce un estado `REVOKED` en `INVITATION` (DEC-003).
- Si la invitación ya no está `PENDING` (aceptada, rechazada o expirada), la solicitud responde 410 sin efecto.
- Se registra el evento de auditoría "invitación cancelada".
