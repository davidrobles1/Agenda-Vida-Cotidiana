# 11 — Autenticación, autorización y seguridad

## V1
**DECISION (DEC-004/ADR-008):** identidad delegada a **Keycloak** (OIDC/OAuth 2.1 self-hosted), con soporte de passkeys/WebAuthn, MFA, gestión de sesiones, recuperación de cuenta y protección contra fuerza bruta. Aplica a Android, iOS y Web. La comparación completa de alternativas (Zitadel, Authentik, Ory) queda registrada en `22-decision-log.md` como histórico.

**DECISION (DEC-014):** la verificación de email se delega al flujo estándar de Keycloak ("Verify Email" required action). La aplicación no implementa un sistema propio de verificación de email.

**Nota (UX-005, no es un ADR nuevo):** el login de Keycloak usa un tema visual personalizado por cliente (`vida-cotidiana-mobile` para `android-app`/`ios-app`, `vida-cotidiana-web` para `web-spa`) — ver `Documentacion/02-ux-ui/login-theme.md`. Es una implementación visual (FreeMarker + CSS, mecanismo estándar de temas de Keycloak) sobre la decisión ya aprobada en este ADR-008: no cambia el flujo Authorization Code + PKCE, no añade ni quita ningún paso de autenticación, y no modifica el nivel de seguridad. Referenciado también en `22-decision-log.md` bajo ADR-008.

No implementar criptografía/autenticación propia.

**DECISION (ADR-008, consecuencia):** la aplicación no expone `POST /auth/login` propio; el backend actúa como resource server (Spring Security OAuth2 Resource Server) validando el token emitido por Keycloak. Esto corrige el `DOCUMENTATION_CONFLICT` señalado en `27-v1-readiness-review.md` §4.1.

## Autorización
- autenticación != autorización;
- todas las operaciones de recursos verifican ownership;
- deny-by-default;
- mínimo privilegio;
- no confiar en `userId` enviado por cliente.

### Autorización sobre recursos compartidos (DECISION, ADR-006)
- Roles por recurso: `OWNER` (control total) y `COLLABORATOR` (ver + completar/marcar pendiente, sobre el estado único global del recordatorio — DEC-001).
- Solo `OWNER` puede: editar, eliminar, invitar, cancelar invitaciones pendientes, listar invitaciones/colaboradores, revocar acceso de un colaborador activo.
- `COLLABORATOR` con `REMINDER_SHARE.status = REVOKED` pierde autorización de forma inmediata — no debe existir ventana de gracia ni caché de permisos obsoleta.
- **DECISION (DEC-002):** eliminar un recordatorio no requiere revocar primero a los colaboradores; se elimina en cascada y se notifica a los colaboradores `ACTIVE` (ver `09-data-model.md`, `AC-013`).
- El modelo de autorización se diseña para permitir en el futuro (V2/V3) roles adicionales, hogares/familias y permisos granulares, pero V1 implementa únicamente OWNER/COLLABORATOR — no anticipar esa complejidad en el código de V1 (RECOMMENDATION: aislar la decisión de roles detrás de una interfaz simple para no reescribir en V2/V3).

### Manejo de tokens por plataforma
- Android/iOS: tokens en almacenamiento seguro nativo (Android Keystore / iOS Keychain).
- Web (SPA, DEC-007): **consideración técnica pendiente, no bloqueante** — ver `08c-web-architecture.md` para las opciones evaluadas (almacenamiento en memoria + renovación silenciosa vs. patrón BFF). No es una decisión de negocio; debe resolverse antes de implementar el cliente Web.

## Sesiones
- expiración;
- revocación;
- protección contra replay según mecanismo elegido;
- credenciales almacenadas de forma segura en Android.

### DECISION (DEC-016/ADR-017) — Expiración por inactividad: 2 horas
Decidido por el Product Owner el 2026-08-28. Aplica a la SPA Web; Android/iOS quedan **TBD** hasta que exista su cliente de sesión (no se asume que el plazo sea el mismo en móvil).

| Parámetro | Valor | Dónde se aplica |
|---|---|---|
| Inactividad máxima | **2 h** (7200 s) | Web: `web/src/core/auth/authClient.ts` (`IDLE_TIMEOUT_MS`). Keycloak: `ssoSessionIdleTimeout` en `infra/keycloak/realm-vida-cotidiana*.json` |
| Vida absoluta de sesión | 10 h (36000 s) | Keycloak `ssoSessionMaxLifespan` — **sin cambios**, es un tope independiente del de inactividad |
| Vida del access token | 5 min (300 s) | Keycloak `accessTokenLifespan` — **sin cambios** |

**Por qué hacen falta las dos mitades y no basta con Keycloak:** la renovación silenciosa de la SPA (`prompt=none` cada ~4,5 min) reinicia el contador de inactividad de Keycloak en cada renovación. Con la app abierta ese contador nunca avanzaba, así que la sesión sobrevivía hasta el tope absoluto de 10 h. Por eso:

- **Con la app abierta manda el cliente**: mide interacción real del usuario (`mousedown`, `keydown`, `touchstart`, `scroll`), corta la renovación al vencer el plazo y termina la sesión SSO llamando al endpoint de logout de Keycloak — soltar el token del navegador no bastaría, porque el siguiente `prompt=none` volvería a entrar solo.
- **Con la app cerrada manda Keycloak**: si nadie renueva, `ssoSessionIdleTimeout` caduca la sesión en el mismo plazo.
- **Al volver a abrir la app**, el cliente compara la marca de última actividad (compartida entre pestañas del mismo navegador, `localStorage`, solo una marca de tiempo — la regla WEB-002 de no persistir el token sigue intacta) antes de intentar restaurar la sesión.

El usuario siempre ve el motivo del cierre en la pantalla de login; no se le devuelve al login en silencio.

**ASSUMPTION:** "actividad" se define como interacción real del usuario, no como tráfico de la aplicación. Que la pestaña esté abierta, visible o haciendo peticiones en segundo plano **no** cuenta como actividad.

## Seguridad API
- HTTPS;
- validación Bean Validation;
- rate limiting;
- CORS restringido si aplica;
- headers de seguridad;
- límites de payload;
- gestión uniforme de errores.

## OWASP
Baseline:
- OWASP ASVS;
- OWASP API Security Top 10;
- OWASP Mobile Application Security Verification Standard;
- OWASP Top 10.

## Privacidad
V1 debe aplicar minimización:
- no finanzas;
- no IA;
- no estados de cuenta;
- no datos que no sean necesarios.

## Auditoría
Registrar eventos de seguridad relevantes sin guardar secretos, incluyendo: creación/cancelación/aceptación/rechazo/expiración de invitación y revocación de acceso compartido.

## SEC-001 Prevención de enumeración de usuarios
Ningún endpoint relacionado con invitaciones (`POST /reminders/{id}/shares`, y cualquier búsqueda de destinatario) debe responder de forma distinguible según si el email introducido pertenece o no a una cuenta existente. Aplica rate limiting sobre la creación de invitaciones para mitigar abuso.

## SEC-002 Invitaciones: expiración, cancelación y revocación
- Las invitaciones pendientes expiran a los 7 días (ASSUMPTION, FR-007) y dejan de ser válidas automáticamente (no requiere acción del usuario).
- **DECISION (DEC-003):** `INVITATION.status` usa `CANCELLED` (acción del propietario sobre una invitación pendiente) — nunca `REVOKED`. La revocación de acceso por parte del propietario ocurre sobre `REMINDER_SHARE` y es efectiva de inmediato; el backend debe validar el estado `REMINDER_SHARE.status = ACTIVE` en cada operación del colaborador, no solo en el momento de la aceptación.
- Los tokens/identificadores de invitación deben ser no predecibles (UUID) y no deben poder reutilizarse una vez `ACCEPTED`, `REJECTED`, `EXPIRED` o `CANCELLED`.
- **RECOMMENDATION (técnica):** toda transición de `INVITATION.status` desde `PENDING` (aceptar, rechazar, cancelar) debe implementarse como una actualización condicional atómica (`WHERE status = 'PENDING'`) para eliminar condiciones de carrera entre acciones concurrentes sobre la misma invitación (ver `09-data-model.md`, `UC-14`).

## SEC-003 Retención y eliminación de cuenta (DEC-015)
- Eliminación de cuenta: soft delete con periodo de gracia de 30 días (`PENDING_DELETION` → purga). Los datos personales solo se purgan definitivamente al vencer el plazo.
- Invitaciones sin cuenta asociada (`invited_user_id` nulo) en estado `REJECTED`/`EXPIRED`/`CANCELLED`: purgar el email tras un plazo corto de retención (ASSUMPTION: 90 días) — minimización de datos de terceros que nunca usaron el servicio (NFR-002/NFR-010).
- Los tokens de push (`DEVICE_PUSH_TOKEN`) deben eliminarse al cerrar sesión o al purgar la cuenta asociada.

## SEC-004 Autorización de Persona/Proyecto/Compromiso (ADR-016)
- Mismo patrón dueño-únicamente que `REMINDER`/`WARRANTY`/`MAINTENANCE_RECORD` (`owner_user_id`): sin colaboradores sobre `PERSON`/`PROJECT`/`COMMITMENT` en V1 de este módulo. Todo endpoint sobre estos recursos debe responder `404` (nunca `403`) sobre un recurso ajeno, igual que el resto de la API.
- Compartir el contexto de una reunión (participantes) sigue viviendo en `REMINDER_SHARE` (ADR-006) — no se introduce un mecanismo de permisos nuevo para `PERSON`/`PROJECT`/`COMMITMENT`.
- `PERSON` representa a una persona real que no necesariamente es usuaria del sistema: aplican los mismos principios de minimización que a `INVITATION.invited_email` (NFR-002/NFR-011) — no almacenar más datos de terceros que los estrictamente necesarios.
