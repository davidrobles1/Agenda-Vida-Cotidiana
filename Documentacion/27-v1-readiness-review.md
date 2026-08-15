# 27 — V1 Readiness Review

Revisión de consistencia cruzada sobre las decisiones registradas hasta el 2026-08-09 (ADR-005 a ADR-008 y sus documentos derivados: `AI-CONTEXT.md`, `22-decision-log.md`, `01-scope.md`, `03-prd.md`, `04-use-cases.md`, `05-user-flows.md`, `06-c4.md`, `07-backend-architecture.md`, `09-data-model.md`, `10-api-openapi.md`, `openapi/openapi.yaml`, `11-auth-security.md`, `21-security.md`, `12-traceability.md`, `13-acceptance.md`, `25-open-questions.md`, `26-v1-backlog.md`).

No se ha escrito ni modificado código. No se ha tomado ninguna decisión de negocio nueva en este documento — donde se detecta una ambigüedad se marca `TBD`/`DOCUMENTATION_CONFLICT` y se eleva.

---

## 1. DECISIONES CONFIRMADAS

- **ADR-001** Monolito modular (Accepted).
- **ADR-002** Android nativo, Kotlin + Jetpack Compose (Accepted).
- **ADR-003 / ADR-004** IA y Finanzas fuera de V1–V4 (Accepted).
- **ADR-005** Plataformas V1 = Android + iOS + Web, mismo backend/API para las tres, sin código compartido asumido (Accepted).
- **ADR-006** Modelo de compartir: un `OWNER` + colaboradores `COLLABORATOR` 1:N vía invitación por email o username; invitación `PENDING` hasta aceptación explícita; expiración a 7 días (ASSUMPTION); revocación por el propietario con efecto inmediato; colaborador solo ve y completa/deshace su completado, no edita/elimina/re-comparte (Accepted).
- **ADR-007** Notificaciones: locales (cliente) + push (backend) detrás de una interfaz propia `PushNotificationSender`; proveedor concreto por plataforma queda TBD (Accepted).
- **Stack backend:** Java 21 LTS, Spring Boot 3.x, Spring Security, Spring Data JPA/Hibernate, PostgreSQL, Flyway, Testcontainers (Accepted, sin cambios).
- **Stack Android:** Kotlin, Jetpack Compose, Hilt, Coroutines/Flow, Retrofit (Accepted, sin cambios).
- **Identidad (parcial):** autenticación delegada a un proveedor OIDC/OAuth 2.1 self-hosted con passkeys/MFA; no se implementa criptografía/autenticación propia (Accepted). El proveedor exacto sigue Proposed (ver §2).

## 2. DECISIONES BLOQUEANTES PARA IMPLEMENTACIÓN

Estas cinco son las únicas que, si no se cierran, impiden generar correctamente el modelo de datos, las migraciones o el login/push de V1. Las tres primeras bloquean el núcleo (recordatorios + compartir); las dos últimas bloquean únicamente login real y push real, respectivamente — el resto del backend puede avanzar en paralelo.

1. **Semántica de "completado" en un recordatorio compartido.** FR-009 dice que un colaborador puede "marcarlo como completado y deshacer **su propio** completado", lo que sugiere un estado de completado por persona. Pero `09-data-model.md` y `openapi.yaml` modelan `REMINDER.status` como un único campo global (`PENDING`/`COMPLETED`), igual que `AC-005`. Ambas cosas no pueden ser ciertas a la vez. Es una decisión de producto que determina si `REMINDER` necesita un estado único (más simple) o una tabla de completado por colaborador (más compleja) — no se puede corregir después sin migrar datos.
2. **Qué ocurre al eliminar un recordatorio con colaboradores activos.** `UC-05` sigue diciendo "se elimina o marca eliminado según decisión TBD" sin mencionar a los colaboradores; `09-data-model.md` no define el comportamiento `ON DELETE` de las FK `reminder_id` en `INVITATION`/`REMINDER_SHARE`; `openapi.yaml` no documenta ningún caso especial en `DELETE /reminders/{id}`. Es una decisión de producto (cascada / bloquear eliminación / notificar y eliminar) que además define un constraint de base de datos.
3. **Lifecycle de `INVITATION` incompleto/incorrecto.** El enum actual es `PENDING, ACCEPTED, REJECTED, EXPIRED, REVOKED`. Pero: (a) `FR-007` y el endpoint `DELETE /invitations/{invitationId}` permiten **cancelar** una invitación pendiente, y no existe un estado `CANCELLED` para reflejarlo; (b) `REVOKED` en el nivel de `INVITATION` es conceptualmente incorrecto — la revocación (`FR-010`) actúa sobre `REMINDER_SHARE` (una colaboración ya aceptada), no sobre la invitación en sí. Esto no es una decisión de negocio sino un defecto de diseño que debe corregirse antes de generar el schema/migración inicial.
4. **Selección (o adopción provisional) del proveedor OIDC (ADR-008).** No bloquea el diseño de autorización (es OIDC estándar), pero sí bloquea implementar el login real: hay que elegir con qué proveedor se levanta el entorno de identidad (Keycloak/Zitadel/Authentik/Ory), registrar los clientes de Android/iOS/Web y emitir tokens reales.
5. **Falta de un lugar donde guardar tokens de dispositivo para push.** `ADR-007`/`FR-011` exigen notificaciones push, pero ni `09-data-model.md` ni `openapi.yaml` tienen una entidad de tipo `DEVICE`/`PUSH_TOKEN` (usuario, plataforma, token, timestamps). Sin esto no se puede implementar el envío de push, aunque el resto de V1 no depende de ello.

## 3. DECISIONES NO BLOQUEANTES

Se pueden resolver en paralelo o incluso durante el desarrollo sin reescribir lo ya decidido:

- Límite máximo de colaboradores por recordatorio (25-open-questions Q11).
- Formato/reglas del username/handle (Q12).
- Si el colaborador revocado recibe o no una notificación explícita de la revocación.
- Si el colaborador debe ser notificado cuando el propietario edita el recordatorio (fecha/título).
- Qué pasa si el email invitado, sin cuenta, se registra después: ¿se enlaza automáticamente la invitación pendiente a la nueva cuenta? (no estaba en `25-open-questions.md`, se añade aquí como nuevo TBD).
- Paginación y filtros en `GET /reminders`, `GET /reminders/{id}/shares`, `GET /me/invitations`.
- Nombre del producto, mercado inicial, verificación de email, primer grupo de validación, retención/eliminación de cuenta, licencia/repo (todas ya listadas en `25-open-questions.md`).
- Modo offline / cuenta real vs. modo local antes de autenticar.
- Stack tecnológico definitivo de iOS y Web: bloquea **solo** el inicio de esos dos clientes, no el backend ni Android.
- Proveedor cloud, proveedor de correo, proveedor(es) de push exacto, versión mínima Android/iOS/navegadores.

## 4. DOCUMENTATION_CONFLICTS

1. **`10-api-openapi.md` vs. ADR-008.** El documento narrativo sigue listando `POST /api/v1/auth/login` y `POST /api/v1/auth/logout` como endpoints propios del backend ("Authentication"). Esto quedó desactualizado tras ADR-008: si la identidad se delega a un proveedor OIDC externo, el patrón estándar es que el cliente obtenga tokens directamente del IdP (Authorization Code + PKCE) y el backend actúe solo como resource server validando el token; un endpoint propio de "login" ya no encaja tal como está descrito. No se resolvió al registrar la decisión de identidad. **No se decide aquí cuál es correcto — se marca el conflicto para que el Product Owner/arquitecto lo resuelva.**
2. **`INVITATION.status` vs. `FR-007`/`FR-010`/endpoints.** Ya detallado en §2.3: el modelo de datos declara un valor `REVOKED` en `INVITATION` que no corresponde a ningún flujo descrito en el PRD (la revocación es sobre `REMINDER_SHARE`), y falta el valor `CANCELLED` que sí es necesario para el flujo descrito en `FR-007`.
3. **(Resuelto, referencia histórica)** El conflicto de plataformas (Android-only vs. CLAUDE.md) quedó cerrado por ADR-005; se menciona aquí solo para trazabilidad, no requiere acción.

## 5. INCONSISTENCIAS ENTRE PRD, DATA MODEL Y OPENAPI

- `FR-009` ("deshacer su propio completado") vs. `REMINDER.status` global en `09-data-model.md` y en el schema `Reminder` de `openapi.yaml` — ver bloqueante §2.1.
- `AC-007` exige 409 ante invitación duplicada pendiente, pero `09-data-model.md` no declara la constraint que lo garantizaría a nivel de base de datos (unique parcial sobre `reminder_id + invited_email` con `status = PENDING`). Sin ella, la regla depende solo de lógica de aplicación, frágil ante condiciones de carrera.
- No hay unicidad declarada para `USER.email` ni `USER.username` en las "Reglas" de `09-data-model.md`, pese a que ambos se usan como identificadores de login e invitación.
- No hay unicidad declarada para `REMINDER_SHARE(reminder_id, collaborator_user_id)`, lo que permitiría duplicar la misma colaboración.
- No existe entidad para tokens de dispositivo/push (ver bloqueante §2.5), pese a que `ADR-007`/`FR-011` la requieren.
- `openapi.yaml` no define `requestBody` para `POST /reminders` ni `PATCH /reminders/{id}`, aunque `FR-004` especifica campos concretos (título, descripción, fecha/hora).
- El "Error envelope" (`code`/`message`/`traceId`) descrito en `10-api-openapi.md` no está formalizado como schema reutilizable en `openapi.yaml` ni referenciado en ninguna respuesta 4xx/5xx — se validó que el YAML es sintácticamente correcto (ver §Nota de validación), pero esta pieza no está modelada.
- Los schemas `Invitation` y `ReminderShare` existen en `components.schemas` pero ningún `path` los referencia en sus respuestas (`GET /reminders/{id}/shares` y `GET /me/invitations` no tienen `content`/`schema`).
- `CreateInvitationRequest` no impone mediante el schema la regla "exactamente uno de email o username" (solo está en la descripción textual, no es validable automáticamente por un generador de cliente/servidor).
- Ningún endpoint documenta 400 (validación) ni 401 (no autenticado) de forma explícita, pese a que `10-api-openapi.md` los define como transversales.

**Nota de validación de `openapi.yaml`:** se verificó estructuralmente (parseo YAML, presencia de `openapi`/`info`/`paths`, que cada operación tenga `responses` con `description`, y que todas las referencias `$ref` resuelvan contra `components.schemas`). Resultado: **sin errores estructurales**. No se pudo ejecutar un validador de esquema OpenAPI completo (offline, sin acceso a PyPI en este entorno), por lo que la validación de nivel superior (structural) es la evidencia disponible; se recomienda correr `openapi-spec-validator` o equivalente en CI (ya previsto conceptualmente en `19-cicd.md`).

## 6. RIESGOS DE SEGURIDAD

- **Ausencia de `securitySchemes`/OAuth2-OIDC en `openapi.yaml`.** No hay ningún esquema de seguridad declarado ni un `security:` global. La especificación formal no refleja el modelo de autenticación ya decidido (ADR-008), lo que puede llevar a generar clientes/servidores/mocks sin protección si alguien codifica directamente desde el YAML.
- **Mass assignment potencial.** `Reminder` se usa como único schema (no hay `CreateReminderRequest`/`UpdateReminderRequest` separados). Si se reutiliza tal cual para las peticiones de creación/edición, un cliente podría intentar enviar `id` o `status` directamente.
- **PII de terceros sin cuenta.** `INVITATION.invited_email` almacena el correo de alguien que puede no tener cuenta ni haber dado consentimiento. No hay política de retención/purga para invitaciones `EXPIRED`/`REJECTED` que contienen ese dato — riesgo de minimización de datos (ligado al TBD de retención en `25-open-questions.md`).
- **Colaboradores por recordatorio sin límite técnico.** Aunque existe rate limiting sobre la creación de invitaciones (SEC-001), no hay un tope al número total de colaboradores/invitaciones por recordatorio, lo que deja una superficie de abuso no acotada.
- **Regla de negocio sin respaldo en constraint de BD.** La unicidad de invitación pendiente (AC-007) depende solo de lógica de aplicación; en concurrencia (dos requests simultáneos) podría crearse una invitación duplicada antes de que la validación de aplicación lo detecte.
- **Silencio tras revocación.** No es una vulnerabilidad, pero conviene decidir conscientemente si el colaborador revocado debe enterarse (actualmente el diseño no lo notifica); el silencio reduce superficie de información pero puede generar confusión de producto.

## 7. RIESGOS DE ARQUITECTURA

- **Manejo de tokens en el cliente Web sin decidir.** Con tres plataformas, la forma en que el cliente Web maneja los tokens OIDC (SPA pública guardando el token en el navegador vs. patrón Backend-for-Frontend con cookie `httpOnly`) tiene implicaciones de seguridad importantes y no se ha discutido en ningún documento. Recomendado resolverlo antes de diseñar el cliente Web (ver §10).
- **Módulo `notification` incompleto frente a lo que exige ADR-007** por la falta de entidad de dispositivo/push (§2.5).
- **Módulo `sharing` sin tabla de auditoría propia.** `11-auth-security.md` exige auditar eventos de invitación/revocación, pero `09-data-model.md` no define ninguna entidad `AUDIT_EVENT`/`AUDIT_LOG`; hoy es solo una mención conceptual en `07-backend-architecture.md`.
- **Arquitectura de iOS/Web como placeholder.** Mientras no exista una recomendación de stack (ver §10), el C4 y los contenedores de dos de las tres plataformas de V1 siguen siendo marcadores de posición, lo que limita cuán "implementable" es hoy la multiplataforma.

## 8. TBDs DE PRODUCTO

Nombre definitivo del producto; mercado/país inicial; verificación de email en V1; primer grupo de usuarios de validación; política de retención/eliminación de cuenta (incluyendo el caso específico de emails de invitados sin cuenta); licencia y organización del repositorio; si se requiere cuenta real desde el inicio o se acepta modo local/offline previo; si se requiere modo offline en V1; comportamiento exacto al eliminar (§2.2) o editar significativamente un recordatorio compartido; límite máximo de colaboradores por recordatorio; formato/reglas del username; si una invitación pendiente se vincula automáticamente cuando el invitado por email se registra después; si el colaborador revocado recibe notificación explícita.

## 9. TBDs TÉCNICOS

Proveedor cloud (AWS es solo propuesta de CLAUDE.md, no ratificada — ver §Cloud); proveedor de correo transaccional; proveedor OIDC definitivo (§2.4); proveedor(es) de push exacto por plataforma; stack tecnológico definitivo de iOS y Web (§10 da recomendación, no decisión); versión mínima Android/iOS/navegadores soportados; versiones exactas de Spring Boot/Gradle/AGP/Kotlin (`17-dependencies.md`); entidad de dispositivo/push token (§2.5); índices y constraints de unicidad pendientes en el modelo de datos (§5); formalización en `openapi.yaml` de `securitySchemes`, schema `Error`, `requestBody` de reminders, `oneOf` en `CreateInvitationRequest`, referencias a `Invitation`/`ReminderShare`, y respuestas 400/401; entidad `AUDIT_EVENT`/`AUDIT_LOG`; patrón de manejo de tokens en el cliente Web.

## 10. RECOMENDACIONES

Todas las siguientes son **RECOMMENDATION** — ninguna se convierte en DECISION en este documento; requieren aprobación explícita del Product Owner/arquitecto.

- **RECOMMENDATION (completado):** modelar el completado como un estado único por recordatorio para V1 (coherente con el schema actual y con AC-005), dejando "completado individual por colaborador" para una versión futura si el negocio lo confirma explícitamente. Alternativa si se prefiere lo contrario: crear una tabla de completado por usuario desde ya, pero eso amplía el alcance de V1.
- **RECOMMENDATION (eliminación con colaboradores):** bloquear la eliminación de un recordatorio mientras existan colaboradores con acceso `ACTIVE` (el propietario debe revocarlos primero), en vez de cascada silenciosa. Es la opción más simple y más segura para V1; no sobrearquitectura.
- **RECOMMENDATION (lifecycle invitación):** añadir el estado `CANCELLED` a `INVITATION.status` y retirar `REVOKED` de ese nivel (la revocación vive únicamente en `REMINDER_SHARE.status`).
- **RECOMMENDATION (proveedor OIDC de arranque):** usar Keycloak para desbloquear el desarrollo local del login por ser el más maduro y documentado, sin descartar migrar a Zitadel/Authentik tras evaluar carga operativa real en un entorno concreto. Esto es una recomendación técnica de arranque, no el cierre de ADR-008.
- **RECOMMENDATION (push token):** añadir una entidad `DEVICE_PUSH_TOKEN` (`user_id`, `platform`, `token`, `created_at`, `last_seen_at`) antes de implementar el módulo `notification`.
- **RECOMMENDATION (stack iOS, sin convertir en DECISION):** SwiftUI + Swift nativo — mejor soporte de Apple, seguridad, integración nativa y longevidad, alineado con el criterio de CLAUDE.md de no elegir cross-platform solo para ahorrar código.
- **RECOMMENDATION (stack Web, sin convertir en DECISION):** React + TypeScript como SPA autenticada; dado que V1 requiere sesión iniciada (poco SEO público relevante), una SPA pura puede bastar. Next.js queda como opción si más adelante aparece la necesidad de páginas públicas indexables.
- **RECOMMENDATION (tokens en Web):** definir un patrón Backend-for-Frontend con cookie `httpOnly` para el cliente Web antes de iniciar su implementación, en vez de almacenar tokens OIDC directamente en el navegador.
- **RECOMMENDATION (openapi.yaml):** formalizar `securitySchemes` (tipo `openIdConnect`, `openIdConnectUrl` en `TBD`), un schema `Error` reutilizable referenciado en las respuestas 4xx/5xx, `requestBody` para `POST`/`PATCH /reminders`, la restricción `oneOf` en `CreateInvitationRequest`, y referenciar `Invitation`/`ReminderShare` en las respuestas de los endpoints correspondientes. Cambios de documentación, no de código.
- **RECOMMENDATION (constraints de BD):** añadir unicidad sobre `USER.email`, `USER.username`, `REMINDER_SHARE(reminder_id, collaborator_user_id)` y un índice único parcial sobre `INVITATION(reminder_id, invited_email) WHERE status = 'PENDING'`, antes de la primera migración Flyway.
- **RECOMMENDATION (límite técnico):** definir un máximo técnico razonable de colaboradores por recordatorio (p. ej. 20) como salvaguarda operativa, incluso si el negocio no exige un límite funcional explícito.

---

## Identidad (ADR-008) — qué bloquea y qué no

**No bloquea la arquitectura:** el contrato OIDC estándar, el diseño de autorización por roles `OWNER`/`COLLABORATOR`, y la integración de Spring Security como resource server (todo esto es igual sin importar cuál de los cuatro proveedores se elija).

**Sí bloquea la implementación del login:** la selección concreta del proveedor (o al menos la adopción provisional de uno, ver RECOMMENDATION), porque hace falta levantar un servidor de identidad real, registrar los clientes de Android/iOS/Web y emitir tokens antes de poder probar cualquier flujo de autenticación end-to-end.

## Cloud

AWS sigue siendo únicamente una propuesta de `CLAUDE.md`, no ratificada por ninguna decisión de este proyecto. Se mantiene `TBD` en `01-scope.md`, `AI-CONTEXT.md` y `README.md`. No se cambia aquí.

---

## V1_READINESS_STATUS: NOT_READY

Bloqueadores reales (nada más que estos cinco):

1. Semántica de completado en recordatorios compartidos: ¿estado único por recordatorio o completado individual por colaborador? (define el modelo de datos central).
2. Comportamiento al eliminar un recordatorio con colaboradores activos.
3. Corrección del lifecycle de `INVITATION` (agregar `CANCELLED`, retirar `REVOKED` de ese nivel).
4. Selección o adopción provisional del proveedor OIDC (bloquea solo el login, no el resto del backend).
5. Definición de dónde se almacenan los tokens de dispositivo para push (bloquea solo las notificaciones push, no el resto de V1).

Resolviendo 1–3 se desbloquea la mayor parte del núcleo de V1 (cuenta, recordatorios, compartir). Los puntos 4 y 5 pueden resolverse en paralelo y solo bloquean login real y push real respectivamente.
