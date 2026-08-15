# 20 — Testing y QA

## Pirámide
### Unit
Dominio y casos de uso.

### Integration
API + PostgreSQL real mediante Testcontainers.

### API
Contract/schema validation mediante OpenAPI.

### Android
- unit tests;
- ViewModel tests;
- Compose UI tests para flujos críticos.

### E2E
Solo flujos críticos de V1.

## Casos mínimos
- registro/login;
- acceso no autorizado;
- crear recordatorio;
- consultar propio recordatorio;
- intentar consultar recordatorio ajeno;
- editar;
- completar;
- eliminar;
- errores 4xx/5xx;
- pérdida de red;
- **editar/completar con `version` desactualizada → 409 (`REMINDER_VERSION_CONFLICT`)** (bloqueo optimista, ver `09-data-model.md`);
- **dos requests concurrentes resolviendo la misma invitación (p. ej. aceptar + cancelar) → solo una tiene éxito, la otra recibe 410** (transición atómica, ver `UC-14`, `AC-008`, `AC-017`);
- **listados paginados (`GET /reminders`, `GET /me/invitations`, `GET /reminders/{id}/shares`) respetan `page`/`size` y devuelven `PageMeta` consistente**;
- **rate limit en `POST /reminders/{id}/shares` → 429 tras exceder el umbral** (SEC-001);
- **cancelar invitación pendiente (`DELETE /invitations/{invitationId}`) por el propietario → 204, estado `CANCELLED`; por un tercero → 403** (UC-14/AC-017);
- **revocar colaborador activo (`DELETE /reminders/{id}/shares/{shareId}`) → acceso posterior del colaborador responde 403/404 de inmediato** (AC-010).

## Seguridad
Los tests de autorización son obligatorios para cada endpoint que acepte un identificador de recurso, incluyendo la distinción `OWNER` vs. `COLLABORATOR`. Los tests de contrato (`API`) deben validar el schema `Error` en toda respuesta de error y el schema `PageMeta` en toda respuesta paginada, contra `openapi.yaml`.
