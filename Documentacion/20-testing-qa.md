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

## Pruebas de extremo a extremo: flujos, no datos (2026-09-01)

Regla de obligado cumplimiento para las specs de `web/e2e/`, adoptada tras
encontrar los dos defectos que produce incumplirla:

**1. Una prueba no depende de datos preexistentes.** Comprobar que "la fila
de hbo muestra el 27 de septiembre" no verifica nada: esa fila puede estar
bien por casualidad, y la prueba se rompe —o peor, pasa en falso— en cuanto
el usuario toca sus propios registros. Cada prueba **crea lo que necesita,
lo ejerce de punta a punta y afirma sobre el comportamiento**. Si la suite
pasa sobre una cuenta vacía, pasa siempre.

**2. Una prueba retira lo que trae.** `warranties-maintenance.spec.ts`
sembraba por API y no borraba: había dejado ocho mantenimientos y seis
garantías "WM test …" mezclados con los registros reales del usuario. La
limpieza va en `finally`, porque la ejecución que falla a medias es
justamente la que más residuo deja.

**3. Los datos inconsistentes se purgan, no se reparan.** Un registro
corrupto es síntoma; arreglarlo a mano oculta la causa y deja la tubería
rota. Se borra el dato y se corrige el flujo que lo produjo.

**4. Nada de asunciones sobre pantallas ajenas al flujo.** Ocho specs
afirmaban "Vista mensual" tras iniciar sesión y quedaron rojas cuando el
destino post-login pasó a depender del modo (ADR-015); el rótulo del botón
de Keycloak rompió otras nueve al migrar el tema. Se espera por lo
invariante —haber salido de `/realms/`, el `id` del control— no por un texto
que el producto tiene derecho a cambiar.

Suites de referencia: `web/e2e/payments-flows.spec.ts` (8 flujos, ADR-020) y
`web/e2e/maintenance-flows.spec.ts` (11 flujos, ADR-021).
