# 21 — Seguridad

## Threat model inicial
Activos:
- identidad;
- sesión;
- datos personales;
- recordatorios;
- credenciales;
- base de datos.

Amenazas:
- credential stuffing;
- account takeover;
- BOLA/IDOR;
- injection;
- abuso de API;
- secretos expuestos;
- supply-chain;
- pérdida de dispositivo;
- abuso interno.
- **enumeración de usuarios vía invitaciones** (probar emails para descubrir cuentas existentes) — mitigación: SEC-001, respuesta uniforme y rate limiting;
- **escalación de privilegios sobre recurso compartido** (colaborador intentando editar/eliminar/re-compartir) — mitigación: autorización por rol OWNER/COLLABORATOR verificada en cada endpoint, no solo en la UI;
- **abuso de invitaciones** (spam de invitaciones a terceros) — mitigación: rate limiting por usuario/IP sobre creación de invitaciones;
- **acceso residual tras revocación** (colaborador retiene acceso por caché/token no invalidado) — mitigación: validación de estado `ACTIVE` en cada request, sin caché de autorización de larga duración;
- **abuso del canal de notificación push** (spoofing o inyección de payload) — mitigación: validar origen/adapter de push, no confiar en contenido del payload para decisiones de autorización.
- **robo de token OIDC en el cliente Web (SPA)** — al ser React SPA (DEC-007) sin patrón BFF, un XSS exitoso podría exponer el token si se almacena de forma inadecuada — mitigación: almacenamiento en memoria (no `localStorage`), CSP estricta, renovación silenciosa de tokens de vida corta (ver `08c-web-architecture.md` y `11-auth-security.md`).
- **retención excesiva de datos personales de terceros** (emails de invitados que nunca crearon cuenta) — mitigación: purga tras expiración/rechazo/cancelación de la invitación (SEC-003, DEC-015).
- **condición de carrera en la resolución de una invitación** (dos requests casi simultáneos, p. ej. aceptar y cancelar, o dos aceptaciones) que podrían crear colaboraciones duplicadas o dejar el estado inconsistente — mitigación: transición `PENDING → ACCEPTED/REJECTED/CANCELLED` implementada como actualización condicional atómica en base de datos (`UPDATE ... WHERE status = 'PENDING'`); solo una solicitud tiene éxito, las demás reciben `410` (ver `09-data-model.md`, `07-backend-architecture.md`).
- **actualizaciones perdidas sobre un recordatorio** (edición del propietario en conflicto con un cambio de estado de un colaborador, o dos ediciones concurrentes) — mitigación: bloqueo optimista mediante `REMINDER.version`, validado en `PATCH /reminders/{id}` y opcionalmente en `POST /reminders/{id}/complete` (`409` en caso de conflicto).

## Controles V1
- HTTPS;
- proveedor de identidad maduro;
- mínimo privilegio;
- autorización por objeto;
- validación server-side;
- rate limiting;
- gestión de secretos;
- cifrado en reposo administrado;
- backups;
- logs sin secretos;
- auditoría de acciones sensibles;
- SAST/SCA/secret scanning;
- dependencias actualizadas.

## Clasificación
CRÍTICA: compromiso de cuenta/datos o bypass de autorización.
ALTA: explotación con impacto relevante.
MEDIA: requiere condiciones adicionales.
BAJA: impacto limitado.

## No implementado aún
- IA;
- finanzas;
- microservicios;
- zero trust entre decenas de servicios;
- service mesh;
- Kubernetes.

## Referencias
OWASP ASVS, OWASP API Security Top 10, OWASP MASVS, NIST SSDF, NIST CSF, ISO 27001/27002/27701 como marcos de referencia.

Cumplimiento formal: TBD y no se declara con este documento.
