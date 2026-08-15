# 10 — API REST / OpenAPI

La especificación formal deberá mantenerse en `openapi/openapi.yaml` (fuente canónica; este documento es su compañero narrativo y no debe contradecirlo — ver `30-documentation-consistency-review.md` hallazgo E, corregido en este ciclo).

## Convenciones
Base path:
`/api/v1`

Content-Type:
`application/json`

IDs:
UUID.

Timestamps:
ISO-8601 UTC.

Seguridad:
todos los endpoints (salvo el healthcheck operativo, fuera de este contrato) requieren un bearer token OIDC emitido por Keycloak, definido como `securitySchemes.keycloakBearer` en `openapi.yaml`.

Paginación:
los endpoints de listado (`GET /reminders`, `GET /reminders/{id}/shares`, `GET /me/invitations`) aceptan `page`/`size` y devuelven metadatos de paginación (`PageMeta`).

Concurrencia:
`REMINDER` incluye un campo `version` (bloqueo optimista); `PATCH /reminders/{id}` lo exige y `POST /reminders/{id}/complete` lo acepta opcionalmente. Un conflicto de versión responde `409`.

Errores:
toda respuesta de error usa el schema `Error` (`code`, `message`, `traceId`), definido en `openapi.yaml`.

## Endpoints V1

### Authentication (DOCUMENTATION_CONFLICT corregido, DEC-004/ADR-008)
La aplicación **no implementa login propio**. La autenticación ocurre contra **Keycloak** (Authorization Code + PKCE) desde cada cliente (Android/iOS/Web); el backend actúa únicamente como **resource server**, validando el token (JWT) emitido por Keycloak en cada request (Spring Security OAuth2 Resource Server). No existe `POST /api/v1/auth/login` ni `POST /api/v1/auth/logout` en el backend propio — ambos fueron retirados de esta documentación por no formar parte del contrato canónico (`openapi.yaml`) y por contradecir el patrón resource-server-only.

El "logout" es enteramente responsabilidad del cliente: (1) el cliente descarta sus tokens locales y, si aplica, invoca el `end_session_endpoint` de Keycloak directamente; (2) el cliente llama a `DELETE /api/v1/me/devices/{deviceId}` sobre este backend para dar de baja el token de push de ese dispositivo (ver `UC-06`, `UC-12`, `FR-012`). No existe ningún endpoint de sesión en este backend que limpiar, porque el backend nunca crea una sesión propia.

### Current user
`GET /api/v1/me`
`DELETE /api/v1/me` — solicita la eliminación de la propia cuenta (soft delete, DEC-015/ADR-012). Pasa `USER.deletion_status` a `PENDING_DELETION` con purga a los 30 días. No elimina datos de inmediato.

### Devices (push) — DEC-005/DEC-010
`POST /api/v1/me/devices` — registra o actualiza el token de push del dispositivo actual (`platform`, `token`). Requiere sesión autenticada.
`GET /api/v1/me/devices` — lista los dispositivos registrados del usuario autenticado.
`DELETE /api/v1/me/devices/{deviceId}` — elimina el registro de un dispositivo (p. ej. al cerrar sesión en ese dispositivo).

### Reminders
`GET /api/v1/reminders` — paginado (`page`/`size`); incluye propios y compartidos.
`POST /api/v1/reminders` — body: `title` (obligatorio), `description`, `dueAt`.
`GET /api/v1/reminders/{id}`
`PATCH /api/v1/reminders/{id}` — solo propietario; body incluye `version` obligatorio (bloqueo optimista); `409` si no coincide.
`POST /api/v1/reminders/{id}/complete` — propietario o colaborador activo; body opcional con `version`.
`DELETE /api/v1/reminders/{id}` — solo propietario.

### Sharing (DECISION, ADR-006)
`POST /api/v1/reminders/{id}/shares` — crea invitación (body: `email` o `username`). Solo propietario.
`GET /api/v1/reminders/{id}/shares` — lista colaboradores/invitaciones del recordatorio. Solo propietario.
`DELETE /api/v1/reminders/{id}/shares/{shareId}` — revoca acceso de un colaborador. Solo propietario. Efecto inmediato.

### Invitations
`GET /api/v1/me/invitations` — invitaciones pendientes recibidas por el usuario autenticado; paginado.
`POST /api/v1/invitations/{invitationId}/accept` — solo el destinatario; transición atómica `PENDING → ACCEPTED` (ver "Concurrencia" arriba y `UC-08`).
`POST /api/v1/invitations/{invitationId}/reject` — solo el destinatario; transición atómica `PENDING → REJECTED`.
`DELETE /api/v1/invitations/{invitationId}` — cancela invitación pendiente (`PENDING → CANCELLED`, nunca `REVOKED`, DEC-003). Solo quien la creó (inviter). Ver `UC-14`/`AC-017`.

## Regla crítica
Cada `{id}` debe autorizarse contra el usuario autenticado, distinguiendo permisos de propietario vs. colaborador (FR-009). Ningún endpoint de invitación debe revelar si un email pertenece a una cuenta existente (SEC-001).

## Error envelope
Definido formalmente como el schema `Error` en `openapi.yaml`:
```json
{
  "code": "REMINDER_NOT_FOUND",
  "message": "The requested reminder was not found.",
  "traceId": "..."
}
```

No devolver stack traces ni detalles internos.

## HTTP
400 validación
401 no autenticado
403 sin permiso
404 recurso inexistente/no accesible
409 conflicto — dos causas distintas, diferenciadas por el campo `code` del error: (a) invitación duplicada pendiente para el mismo destinatario (`INVITATION_ALREADY_PENDING`); (b) conflicto de bloqueo optimista al editar/completar un recordatorio con una `version` desactualizada (`REMINDER_VERSION_CONFLICT`)
410 invitación expirada o ya resuelta (aceptada/rechazada/cancelada) — nunca "revocada": `INVITATION` no tiene ese estado (DEC-003)
429 rate limit (aplica especialmente a creación de invitaciones, para mitigar abuso/enumeración)
500 error interno
503 dependencia no disponible

`DELETE /api/v1/me` responde `202 Accepted` (no `204`): la eliminación es diferida (soft delete con purga a 30 días, DEC-015), no inmediata.
