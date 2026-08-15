# 03 — PRD / Requerimientos

## FR-001 Registro
El sistema deberá permitir crear una cuenta mediante el mecanismo de identidad aprobado.

**DECISION:** la identidad se delega a **Keycloak** (proveedor OIDC/OAuth 2.1 self-hosted, DEC-004/ADR-008) — no se implementa almacenamiento/verificación de contraseñas propio. Aplica a Android, iOS y Web.

**DECISION (DEC-014):** la verificación de email se delega al flujo estándar de Keycloak ("Verify Email" required action). La aplicación **no implementa un sistema paralelo de verificación de email**.

**TBD:** email obligatorio en el registro (no decidido en este pack).

## FR-002 Autenticación
El usuario podrá iniciar y cerrar sesión.

## FR-003 Home
El usuario autenticado verá un resumen mínimo de sus pendientes.

## FR-004 Recordatorios
El usuario podrá:
- crear;
- consultar;
- editar;
- completar;
- eliminar un recordatorio.

Campos V1:
- id;
- título;
- descripción opcional;
- fecha/hora opcional;
- estado;
- timestamps.

## FR-005 Notificaciones locales
La aplicación podrá generar una notificación local cuando exista una fecha/hora configurada.

## FR-006 Cuenta
El usuario podrá cerrar sesión y consultar información básica de su cuenta.

## FR-007 Compartir recordatorio (DECISION, ADR-006)
El propietario de un recordatorio podrá invitar a una o varias personas a colaborar en él.

- Identificación del destinatario: por email (funciona aunque la persona invitada no tenga cuenta todavía) o por username/handle (solo usuarios existentes) — a elección del propietario.
- Cardinalidad: un propietario, múltiples colaboradores (1:N) por recordatorio.
- La invitación queda `PENDING` hasta que el destinatario la acepte explícitamente; no se otorga acceso antes de la aceptación.
- La invitación expira automáticamente si no se acepta en 7 días (**ASSUMPTION**, ajustable si aparece una razón técnica/UX).
- El propietario puede cancelar una invitación pendiente en cualquier momento.
- No se debe revelar si un email pertenece o no a una cuenta existente (mitigación de enumeración de usuarios — ver SEC-001 en `11-auth-security.md`).

## FR-008 Aceptar invitación
El destinatario podrá aceptar una invitación pendiente. Al aceptar, la invitación pasa a representar una relación de compartición activa y el colaborador obtiene los permisos de FR-009.

## FR-009 Permisos de colaborador
**DECISION (DEC-001):** el completado de un recordatorio es un **estado único global** (`PENDING`/`COMPLETED`), compartido entre propietario y colaboradores — no existe un estado de completado independiente por persona en V1.

Un colaborador sobre un recordatorio compartido podrá:
- ver el recordatorio y su información;
- marcarlo como completado o volver a marcarlo como pendiente (afecta el único estado compartido del recordatorio, visible para todos los que tienen acceso).

Un colaborador **no podrá**: editar título/descripción/fecha, cambiar recurrencia, eliminar el recordatorio, cambiar el propietario, volver a compartirlo ni modificar permisos de otros colaboradores.

El propietario conserva control total (edición, eliminación, gestión de colaboradores) en todo momento.

## FR-010 Revocar / rechazar / cancelar / expirar
- El destinatario podrá rechazar una invitación pendiente (`INVITATION.status = REJECTED`).
- El propietario podrá cancelar una invitación pendiente (`CANCELLED`) o revocar el acceso de un colaborador ya activo (`REMINDER_SHARE.status = REVOKED`) en cualquier momento; la revocación es efectiva de inmediato (el colaborador deja de poder consultar o completar el recordatorio). **DECISION (DEC-003):** estos son dos conceptos distintos — "cancelar" actúa sobre `INVITATION` (antes de aceptar), "revocar" actúa sobre `REMINDER_SHARE` (después de aceptar). `INVITATION` nunca tiene un estado `REVOKED`.
- Una invitación expirada (`EXPIRED`) no otorga ningún acceso; el propietario puede reenviar una nueva invitación.
- **DECISION (DEC-002):** al eliminar un recordatorio, se eliminan en cascada sus invitaciones y colaboraciones asociadas, y se notifica (push) a los colaboradores que tuvieran acceso `ACTIVE` en ese momento, antes de que el recurso deje de existir (ver `UC-05`, `AC-013`).
- Deben registrarse (auditoría) los eventos de invitación, aceptación, rechazo, cancelación, expiración, revocación y eliminación de un recordatorio compartido.

## FR-012 Registro de dispositivo para notificaciones push
**DECISION (DEC-005/DEC-010):** el cliente (Android/iOS/Web) registra el token de push del dispositivo actual contra el backend (`POST /me/devices`), que lo almacena en `DEVICE_PUSH_TOKEN` asociado al usuario autenticado. Un usuario puede tener múltiples dispositivos registrados simultáneamente. El envío efectivo de push usa **Firebase Cloud Messaging (FCM)** como proveedor unificado detrás de `PushNotificationSender` (ADR-007).

## FR-013 Eliminar cuenta (consecuencia directa de DEC-015)
**DECISION (DEC-015):** el usuario puede solicitar la eliminación de su cuenta (`DELETE /me`). La cuenta pasa a `PENDING_DELETION` con un periodo de gracia de 30 días antes de la purga definitiva. Esta capacidad no estaba explícita en `FR-006`/`26-v1-backlog.md`; se añade aquí como consecuencia necesaria de la política de retención aprobada (DEC-015) — no es una funcionalidad nueva inventada, sino un requisito implícito en la decisión ya tomada por el Product Owner. Ver `US-016` en `26-v1-backlog.md`.

## FR-011 Notificaciones push (DECISION, ADR-007)
Además de las notificaciones locales (FR-005), el backend podrá emitir notificaciones push para eventos que requieran sincronización entre dispositivos o entre usuarios:
- recordatorio programado (recordatorio propio, multi-dispositivo);
- invitación recibida para compartir un recordatorio;
- invitación aceptada o rechazada (visible para el propietario);
- cambios relevantes en un recordatorio compartido (p. ej. completado por un colaborador, revocación de acceso).

**DECISION (DEC-010):** el backend abstrae el proveedor de push detrás de una interfaz propia (`PushNotificationSender`, ADR-007), con **Firebase Cloud Messaging (FCM)** como proveedor unificado para Android, iOS (vía puente FCM↔APNs) y Web (vía Web Push).

**FUTURE (V2/V3):** preferencias de notificación por usuario, múltiples dispositivos por usuario, recordatorios recurrentes, reglas de notificación, digest/resúmenes. No se implementan en V1.

## NFR-001 Seguridad
Toda API deberá usar HTTPS. La autorización se validará por recurso.

## NFR-002 Privacidad
Solo se almacenarán datos necesarios para V1.

## NFR-003 Disponibilidad
Objetivo inicial: TBD.

## NFR-004 Rendimiento
Objetivo inicial API p95: TBD.

## NFR-005 Mantenibilidad
Código modular, testeable y con separación de responsabilidades.

## NFR-006 Observabilidad
Errores, métricas y logs técnicos sin secretos ni contenido sensible.

## NFR-007 Accesibilidad
Soporte inicial para tamaños de fuente del sistema y componentes accesibles.

## NFR-008 Compatibilidad
**DECISION (DEC-011):** versión mínima Android: **API 30 (Android 11)**.
**DECISION (DEC-012):** versión mínima iOS: **iOS 17**.
**DECISION (DEC-013):** navegadores soportados: últimas 2 versiones mayores de Chrome, Edge y Firefox (desktop); Safari desktop en su versión actual y la anterior; Safari en iOS en su versión actual y la anterior; Chrome en Android en su versión actual.
Dispositivos soportados: `TBD` (no cubierto por este pack de decisiones).

## NFR-009 Consistencia multiplataforma
Android, iOS y Web deben ofrecer un modelo funcional y de datos coherente (mismos recordatorios, mismo estado de compartición, misma autorización) aunque cada plataforma use convenciones nativas de UI. No se asume código compartido entre plataformas (CLAUDE.md).

## NFR-010 Retención y eliminación de datos
**DECISION (DEC-015):** las cuentas eliminadas por el usuario pasan a `PENDING_DELETION` con purga definitiva a los 30 días (soft delete). Los emails de personas invitadas sin cuenta se purgan tras expiración, rechazo o cancelación de la invitación, siguiendo una política de retención corta (ASSUMPTION: 90 días — ver `09-data-model.md`).
