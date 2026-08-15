# 04 — Casos de uso

## UC-01 Registrarse
Actor: visitante.
1. Abre la aplicación.
2. Selecciona registro.
3. Es redirigido a Keycloak (Authorization Code + PKCE) e introduce sus datos.
4. Keycloak valida y, si aplica, gestiona la verificación de email mediante su propio flujo (DEC-014) — la aplicación no implementa verificación propia.
5. Cuenta queda creada.
6. Usuario entra a Home.

## UC-02 Iniciar sesión
1. Usuario selecciona iniciar sesión.
2. Es redirigido a Keycloak (Authorization Code + PKCE) e introduce sus credenciales.
3. Keycloak valida y emite un token (JWT) directamente al cliente (Android/iOS/Web); el backend **no crea ninguna sesión propia** — actúa como resource server y valida ese mismo token en cada request posterior (DEC-004/ADR-008, ver `11-auth-security.md`).
4. App muestra Home.

## UC-03 Crear recordatorio
1. Usuario pulsa Nuevo.
2. Introduce título.
3. Opcionalmente fecha/hora.
4. Confirma.
5. Backend valida autorización.
6. Recordatorio se persiste.
7. Home se actualiza.

## UC-04 Completar recordatorio
1. Usuario selecciona pendiente.
2. Marca completado.
3. Sistema persiste estado.
4. UI refleja resultado.

## UC-05 Eliminar recordatorio
1. Usuario (propietario) selecciona eliminar.
2. App solicita confirmación.
3. Backend verifica propiedad (solo el propietario puede eliminar).
4. **DECISION (DEC-002):** si el recordatorio tiene colaboradores con acceso `ACTIVE`, el backend emite una notificación push a cada uno de ellos informando que el recordatorio fue eliminado, y luego elimina en cascada el recordatorio junto con sus `INVITATION`/`REMINDER_SHARE` asociadas. No se bloquea la eliminación ni se exige revocar primero.
5. Home se actualiza para el propietario y para los colaboradores afectados (el recordatorio desaparece de su lista).

## UC-06 Cerrar sesión
1. Usuario selecciona cerrar sesión.
2. App elimina credenciales/tokens locales.
3. App solicita `DELETE /me/devices/{deviceId}` para dar de baja el token de push del dispositivo actual (ver `UC-12`, `FR-012`). No existe una sesión propia del backend que invalidar (DEC-004/ADR-008); si aplica, la app puede además invocar el `end_session_endpoint` de Keycloak.
4. App vuelve a login.

## UC-07 Compartir recordatorio
Actor: propietario del recordatorio.
1. Propietario abre un recordatorio propio.
2. Selecciona "Compartir".
3. Introduce email o username del destinatario.
4. Backend valida autorización (solo el propietario puede invitar) y crea invitación en estado `PENDING` con expiración (7 días).
5. Backend NO revela si el email corresponde a una cuenta existente.
6. Se registra el evento de auditoría "invitación creada".
7. Se notifica (push) al destinatario si tiene cuenta; si no, la invitación queda asociada al email hasta que se registre.

## UC-08 Aceptar invitación
Actor: destinatario.
1. Destinatario ve la invitación pendiente (notificación push o listado "Mis invitaciones").
2. Selecciona "Aceptar".
3. Backend valida que la invitación no esté expirada/revocada.
4. Backend crea la relación de compartición activa con permisos de colaborador (ver FR-009).
5. Se notifica (push) al propietario.
6. Se registra el evento de auditoría "invitación aceptada".

## UC-09 Rechazar invitación
Actor: destinatario.
1. Destinatario selecciona "Rechazar" sobre una invitación pendiente.
2. Backend marca la invitación como `REJECTED`. No se otorga acceso.
3. Se notifica (push) al propietario.
4. Se registra el evento de auditoría "invitación rechazada".

## UC-10 Revocar acceso compartido
Actor: propietario.
**DECISION (DEC-003):** este caso de uso actúa exclusivamente sobre `REMINDER_SHARE` (una colaboración ya aceptada). Cancelar una invitación pendiente (antes de aceptar) es un caso distinto, cubierto por el endpoint `DELETE /invitations/{invitationId}` (FR-007), no por este UC.
1. Propietario abre la lista de colaboradores de un recordatorio.
2. Selecciona "Revocar" sobre un colaborador con `REMINDER_SHARE.status = ACTIVE`.
3. Backend valida autorización (solo el propietario revoca) y marca `REMINDER_SHARE.status = REVOKED` con efecto inmediato.
4. El colaborador deja de poder consultar/completar el recordatorio.
5. Se registra el evento de auditoría "acceso revocado".

## UC-11 Recibir notificación push
Actor: usuario (propietario o colaborador).
1. Ocurre un evento relevante (recordatorio programado, invitación recibida/aceptada/rechazada/cancelada, cambio en recordatorio compartido, revocación, **eliminación de un recordatorio compartido — DEC-002**).
2. Backend genera el evento y lo envía a través de **FCM** (DEC-010), detrás de la interfaz `PushNotificationSender` (FR-011).
3. Dispositivo(s) del usuario (registrados vía `DEVICE_PUSH_TOKEN`, FR-012) reciben la notificación push.
4. Usuario abre la notificación y accede directamente al recurso relacionado (si aún existe).

## UC-12 Registrar dispositivo para notificaciones push
Actor: usuario autenticado.
1. La app (Android/iOS/Web) obtiene un token de push del sistema/proveedor.
2. La app envía el token al backend (`POST /me/devices`) junto con la plataforma.
3. Backend crea o actualiza el registro en `DEVICE_PUSH_TOKEN` (FR-012).
4. Al cerrar sesión en ese dispositivo, la app solicita eliminar el registro (`DELETE /me/devices/{deviceId}`).

## UC-13 Eliminar cuenta
Actor: usuario autenticado.
1. Usuario solicita eliminar su cuenta (`DELETE /me`).
2. Backend marca `USER.deletion_status = PENDING_DELETION` y fija `purge_at` a 30 días (DEC-015/FR-013).
3. Durante el periodo de gracia, la cuenta permanece inaccesible para nuevas sesiones (comportamiento exacto de bloqueo/reversión: `TBD`, no bloqueante para V1).
4. Un job periódico purga definitivamente los datos personales de las cuentas cuyo `purge_at` ya venció.

## UC-14 Cancelar invitación pendiente
Actor: propietario (quien creó la invitación).
**DECISION (DEC-003):** este caso de uso actúa exclusivamente sobre `INVITATION` en estado `PENDING`, antes de que el destinatario responda. Es distinto de `UC-10` (que revoca una colaboración ya aceptada, sobre `REMINDER_SHARE`).
1. Propietario abre la lista de invitaciones pendientes de un recordatorio.
2. Selecciona "Cancelar" sobre una invitación `PENDING`.
3. Backend valida autorización (solo quien creó la invitación puede cancelarla) y ejecuta `DELETE /invitations/{invitationId}`, marcando `INVITATION.status = CANCELLED` mediante una actualización condicional (`WHERE status = 'PENDING'`) para evitar una condición de carrera con una aceptación/rechazo casi simultánea (ver `09-data-model.md`).
4. Si la invitación ya no está `PENDING` (fue aceptada, rechazada o expiró en el ínterin), el backend responde `410` sin efecto.
5. Se registra el evento de auditoría "invitación cancelada".
