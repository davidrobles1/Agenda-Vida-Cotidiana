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

## FR-014 Selección de propósito de uso en registro (ADR-015)
**Version: V3 (implementación adelantada en paralelo con V2 desde 2026-08-18 — ver ADR-015).**

**DECISION (ADR-015):** durante el registro, se pregunta al usuario el propósito de uso mediante dos casillas independientes: "Personal" y "Laboral". El usuario debe marcar **al menos una** para completar el registro (no se permite continuar con ambas vacías). El resultado determina qué modos aparecen habilitados en el selector de navegación (FR-015). El Calendario general queda disponible siempre, sin importar la respuesta.

## FR-015 Selector de modo y navegación condicional (ADR-015)
**Version: V3 (implementación adelantada en paralelo con V2 desde 2026-08-18 — ver ADR-015).**

**DECISION (ADR-015):** la navegación superior muestra "Calendario" más únicamente los modos habilitados por el usuario (Personal y/o Laboral, según FR-014/FR-016). Cada modo habilitado tiene su propio conjunto de opciones de navegación (Inicio, Calendario del modo, Tareas, Compartidos — lista exacta TBD). Al abrir la aplicación, la vista por defecto es Calendario (no Inicio).

## FR-016 Activar modo adicional desde Ajustes (ADR-015)
**Version: V3 (implementación adelantada en paralelo con V2 desde 2026-08-18 — ver ADR-015).**

**DECISION (ADR-015):** un usuario que no habilitó un modo (Personal o Laboral) durante el registro puede activarlo posteriormente desde Ajustes. Al activarlo, el modo aparece en el selector de navegación (FR-015).

**TBD:** si Ajustes también permite desactivar un modo ya habilitado (ver ADR-015).

## FR-017 Calendario general (ADR-015)
**Version: V3 (implementación adelantada en paralelo con V2 desde 2026-08-18 — ver ADR-015).**

**DECISION (ADR-015):** el Calendario general agrega los recordatorios de todos los modos habilitados por el usuario, mostrando cada uno con el color asociado a su modo de origen (no un tercer color neutral). Admite vistas diario, semanal y mensual, seleccionables por el usuario.

## FR-018 Calendario por modo (ADR-015)
**Version: V3 (implementación adelantada en paralelo con V2 desde 2026-08-18 — ver ADR-015).**

**DECISION (ADR-015):** cada modo habilitado (Personal, Laboral) incluye su propio Calendario, específico de ese modo, dentro de su navbar — distinto del Calendario general (FR-017), que agrega ambos.

## FR-019 Contexto de recordatorio inferido por navbar de origen (ADR-015)
**Version: V3 (implementación adelantada en paralelo con V2 desde 2026-08-18 — ver ADR-015).**

**DECISION (ADR-015):** cuando un usuario tiene ambos modos habilitados, el `context` (`PERSONAL`/`LABORAL`) de un recordatorio nuevo se infiere automáticamente del navbar desde el que se creó — no existe un selector de contexto explícito en el formulario de creación (FR-004).

## FR-020 Garantías y Mantenimiento reales (`BE-037`/`WEB-009`)
**Version: V2 — aprobado y entregado 2026-08-18, mismo ciclo de trabajo que `UX-013` y `ADR-015` (ver `05-v2-plan.md`, nota de alcance en paralelo).**

**DECISION (usuario, 2026-08-18):** Garantías y Mantenimiento, hasta ahora 100% simulados (`core/mock/mockData.ts`, `UX-006`), pasan a tener persistencia real, mismo patrón de autorización y CRUD que `REMINDER` (dueño-únicamente, 404 nunca 403 sobre recurso ajeno). Los campos son exactamente los que la UI mock ya mostraba — no se inventó ninguno nuevo (ver `09-data-model.md`). Reemplaza el toggle de "completado" local-only (`mockCompletedIds`, se perdía al recargar) por una acción real persistida.

## FR-021 Personas — contactos profesionales (ADR-016)
**Version: V3.**

**DECISION (ADR-016):** en el contexto Laboral, el usuario podrá crear, consultar, editar y eliminar Personas: nombre, rol (texto libre), organización (texto libre — ver ADR-016(c), no es una entidad propia en esta fase). Cada Persona pertenece a un único dueño (`owner_user_id`); sin colaboradores en V1 de este módulo.

## FR-022 Proyectos (ADR-016)
**Version: V3.**

**DECISION (ADR-016):** el usuario podrá crear, consultar, editar y eliminar Proyectos: nombre, estado (texto libre en V1; `TBD` si se cierra a un enum fijo), fecha límite opcional, Persona cliente opcional. El vocabulario mostrado ("Proyecto"/"Obra"/"Caso"/"Oportunidad") es una capa de presentación (UX-014), no cambia el esquema.

## FR-023 Vínculo de Tarea con Persona y Proyecto (ADR-016)
**Version: V3.**

**DECISION (ADR-016):** un `REMINDER` con `context = LABORAL` podrá vincularse opcionalmente a una Persona y/o un Proyecto ya existentes (`person_id`, `project_id`, ambos nullable). No se crea una entidad Tarea independiente de `REMINDER` — ver `09-data-model.md`.

## FR-024 Reunión: ubicación y participantes sobre Tarea (ADR-016)
**Version: V3.**

**DECISION (ADR-016):** un `REMINDER` con `context = LABORAL` podrá registrar `location` (texto libre) y compartirse con participantes mediante el mecanismo ya existente de compartir (FR-007/ADR-006, `REMINDER_SHARE`). No se crea una entidad Evento/Reunión independiente.

## FR-025 Compromisos: Seguimientos y Esperando (ADR-016)
**Version: V3.**

**DECISION (ADR-016):** el usuario podrá crear, consultar, resolver y reprogramar Compromisos: Persona asociada, descripción, dirección (`MINE` = el usuario debe actuar; `THEIRS` = el usuario espera una acción de la otra persona), fecha, Proyecto opcional, origen opcional (el `REMINDER`/reunión del que surgió). "Seguimientos" y "Esperando" son la misma entidad filtrada por dirección, no dos entidades — ver ADR-016, Alternativas.

## FR-026 Vista "Hoy" del contexto Laboral (ADR-016)
**Version: V3.**

El usuario verá, en una sola pantalla: los `REMINDER` con `context = LABORAL` que vencen hoy, los Compromisos con fecha de hoy o vencidos, y un resumen (sin mezclar datos) de sus eventos `PERSONAL` del mismo día, para no perder de vista el total de su tiempo disponible. Mismo principio agregador que el Calendario general (FR-017), aplicado a un resumen diario en vez de al Calendario completo.

## FR-027 Vista "Seguimientos" con pestañas Mías / Esperando (ADR-016)
**Version: V3.**

El usuario podrá ver sus Compromisos abiertos agrupados en dos pestañas: "Mías" (`direction = MINE`) y "Esperando" (`direction = THEIRS`), cada uno mostrando Persona, descripción, fecha y antigüedad cuando está vencido.

## FR-028 Inbox de captura rápida (ADR-016)
**Version: V3.**

**DECISION (ADR-016):** el usuario podrá capturar una nota rápida sin clasificarla (reutiliza la entidad `NOTE` existente, sin vínculo a Persona/Proyecto/Reminder) y, después, convertirla en Tarea, en Compromiso, o descartarla. No introduce una entidad nueva.

**FUTURE (V4, candidatos — ver `34-laboral-module-proposal.md` §14):** Notas y Documentos vinculados a Persona/Proyecto; "última interacción" derivada automáticamente por Persona; automatizaciones simples deterministas (vencimiento → Hoy; nota de reunión → sugerencia de tarea); Objetivos, Rutinas, Lugares, Recursos; vista Kanban de Proyecto.

## FR-029 Notas vinculadas a Persona/Proyecto (ADR-016 Fase 3a)
**Version: V4 candidato (RECOMMENDATION, no comprometido como alcance de versión — ver `34-laboral-module-proposal.md` §14). Implementado el 2026-08-22 como incremento aislado sobre la Fase 1/2 ya cerradas, sin reabrirlas.**

**DECISION (ADR-016 Fase 3a):** una `NOTE` (entidad ya existente, reutilizada — ver FR "Notas" de `26-v1-backlog.md`) podrá vincularse opcionalmente a una Persona y/o un Proyecto propios (`personId`/`projectId`, ambos nullable), visibles desde el detalle de esa Persona/Proyecto. No se crea una entidad nueva. Mismo contrato de autorización que FR-023 (404 sobre un recurso ajeno).

## FR-030 Documentos vinculados a Persona/Proyecto (ADR-016 Fase 3b)
**Version: V4 candidato (RECOMMENDATION, no comprometido — ver `34-laboral-module-proposal.md` §14). Backend implementado el 2026-08-22; Web solo lectura (ver TBD abajo).**

**DECISION (ADR-016 Fase 3b):** un `DOCUMENT` (entidad ya existente, módulo `document`, reutilizada) podrá vincularse opcionalmente a una Persona y/o un Proyecto propios (`personId`/`projectId`, ambos nullable) al subirlo o al editarlo. Mismo contrato de autorización que FR-023/FR-029.

**TBD (no bloqueante, alcance de UI):** el detalle de Persona/Proyecto solo **lista** los documentos ya vinculados (solo lectura) — no existe todavía en la Web un flujo para subir/vincular un documento nuevo desde ahí (requiere una pantalla de subida de archivo con validación MIME/tamaño, deliberadamente no construida en este incremento para no expandir su alcance sin pedirlo explícitamente). Vincular un documento existente hoy requiere `PATCH /documents/{id}` directamente.

**FUTURE (Post-V4, candidatos — fuera de alcance hasta validación explícita):** CRM avanzado/pipeline de ventas con etapas, gestión de casos legales, gestión de obra con planos, historiales clínicos, automatización proactiva basada en patrones/IA, asistente conversacional. No se implementan ni se documentan como endpoints reales (regla IA/Finanzas de `CLAUDE.md`).

## FR-035 Sugerencia de tarea desde una nota (ADR-016 Fase 3d)
**Version: V4 candidato (RECOMMENDATION, no comprometido — ver `34-laboral-module-proposal.md` §14). Implementado el 2026-08-28.**

**DECISION (Product Owner, 2026-08-28):** desde una nota vinculada a una Persona o Proyecto, el usuario podrá generar una sugerencia de tarea. La regla, que estuvo `BLOCKED` hasta esta fecha por falta de definición, es:
1. **El disparador es manual:** un botón "Sugerir tarea" en la nota. **No** hay detección automática por palabras clave, ni job, ni heurística — nada se dispara solo.
2. La sugerencia propone el texto de la nota como título (editable) y ofrece dos acciones: **convertir** en Tarea o **descartar**.
3. Una vez **convertida o descartada**, la sugerencia no vuelve a ofrecerse para esa nota (`NOTE.taskSuggestionResolved`).

Convertir crea un `REMINDER` normal (`context = LABORAL`, heredando la Persona/Proyecto de la nota) mediante el endpoint de siempre — no se introduce ninguna ruta de creación de Tareas paralela.

**FUERA DE ALCANCE explícito:** detección automática por contenido, sugerencias proactivas basadas en patrones o histórico, cualquier modelo o heurística de IA (prohibido por `CLAUDE.md`/ADR-003), y deshacer la resolución (ninguna regla aprobada contempla "volver a ofrecer la sugerencia").

## FR-031 Objetivos (ADR-016 Fase 3e1)
**Version: V4 candidato (RECOMMENDATION, no comprometido — ver `34-laboral-module-proposal.md` §14). Alcance definido el 2026-08-22, listo para desarrollo, todavía no implementado.**

**DECISION (ADR-016 adenda Fase 3e, 2026-08-22):** un Objetivo (`OBJECTIVE`, entidad nueva) representa una meta laboral que el usuario quiere conseguir: `title` (requerido), `targetValue`/`currentValue` (enteros opcionales, progreso numérico actualizado manualmente por el usuario, sin cálculo automático), `deadline` (opcional), `completed` (marcado manualmente, nunca derivado). Entidad independiente en este incremento, sin vínculo a `PROJECT`/`PERSON`. CRUD dueño-únicamente, mismo patrón de autorización que `PERSON`/`PROJECT` (404 sobre uno ajeno, nunca 403). UI: resumen en "Hoy" + página dedicada; no se agrega un ítem al navbar de Laboral (ya cerrado en Fase 2/`WEB-010`).

**FUERA DE ALCANCE explícito (3e1):** relación con Proyectos/Personas, sub-objetivos, cálculo automático de progreso, notificaciones de vencimiento, gráficas, cualquier automatización (eso pertenece a 3d, que sigue `BLOCKED` por separado).

## FR-032 Rutinas (ADR-016 Fase 3e2)
**Version: V4 candidato (RECOMMENDATION, no comprometido — ver `34-laboral-module-proposal.md` §14). Alcance definido el 2026-08-22, listo para desarrollo, todavía no implementado.**

**DECISION (ADR-016 adenda Fase 3e, 2026-08-22):** una Rutina (`ROUTINE`, entidad nueva) representa una actividad laboral recurrente que el usuario marca manualmente como realizada. **Decisión explícita del Product Owner: una Rutina NO genera automáticamente `REMINDER` ni `COMMITMENT`** — esto mantiene 3e2 deliberadamente separada de FR "Automatizaciones simples" (3d, sigue `BLOCKED`). Campos: `title` (requerido), `description` (opcional), `frequency` (`DAILY`/`WEEKLY`/`MONTHLY`), `nextExecutionDate`, `active`. Una acción marca la ejecución actual como realizada y avanza `nextExecutionDate` a la siguiente ocurrencia según `frequency`. `completed` **no** se usa como estado permanente de la Rutina (a diferencia de Objetivo) — la misma Rutina se completa repetidamente.

**FUERA DE ALCANCE explícito (3e2):** generación automática de `REMINDER`/`COMMITMENT`, jobs/background workers, notificaciones, recurrencias avanzadas (p. ej. "cada 2 semanas", días específicos del mes), excepciones por ocurrencia.

## FR-033 Lugares (ADR-016 Fase 3e3)
**Version: V4 candidato (RECOMMENDATION, no comprometido — ver `34-laboral-module-proposal.md` §14). Alcance definido el 2026-08-22, listo para desarrollo, todavía no implementado.**

**DECISION (ADR-016 adenda Fase 3e, 2026-08-22):** un Lugar (`PLACE`, entidad nueva) representa una ubicación laboral reutilizable: `name` (requerido), `address` (opcional), `personId` (FK opcional a `PERSON`, mismo patrón que `PROJECT.clientPersonId`). Se integra con Tareas únicamente como catálogo de autocompletado en `CreateTaskDialog`: elegir un Lugar copia su nombre/dirección como texto al campo `REMINDER.location` ya existente (FR-024). **No se agrega `REMINDER.place_id`** — se prefiere esta integración sin modificar de nuevo el esquema de `REMINDER`.

**FUERA DE ALCANCE explícito (3e3):** `REMINDER.place_id` (FK real), geolocalización, coordenadas, mapas, navegación, reportes por ubicación.

## FR-034 Recursos (ADR-016 Fase 3e4)
**Version: V4 candidato (RECOMMENDATION, no comprometido — ver `34-laboral-module-proposal.md` §14). Alcance definido el 2026-08-22, listo para desarrollo, todavía no implementado.**

**DECISION (ADR-016 adenda Fase 3e, 2026-08-22):** un Recurso (`RESOURCE`, entidad nueva) representa un material o referencia reutilizable que ayuda al usuario a realizar su trabajo: `name` (requerido), `type` (enum `DOCUMENTO`/`ENLACE`/`PLANTILLA`/`MANUAL`/`HERRAMIENTA`/`OTRO`, requerido), una referencia de texto libre (URL o descripción, ver TBD técnico en `09-data-model.md`), `description` (opcional), `personId`/`projectId` (FKs opcionales, mismo patrón que `NOTE`/`DOCUMENT` en FR-029/FR-030). **No sustituye a `DOCUMENT`** (FR-030): sin almacenamiento de archivos nuevo, sin versionado, sin permisos compartidos.

**FUERA DE ALCANCE explícito (3e4):** almacenamiento de archivos nuevo, versionado documental, permisos compartidos, buscador avanzado, IA, cualquier forma de gestión documental que duplique a `DOCUMENT`.

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

## NFR-011 Minimización de datos del módulo Laboral (ADR-016)
**DECISION (ADR-016):** `PERSON` no incluye campos más allá de nombre/rol/organización en V1 de este módulo (sin teléfono, dirección postal ni otros datos personales de terceros no estrictamente necesarios); `PROJECT` y `COMMITMENT` no almacenan información financiera (montos, facturación, datos bancarios) — consistente con ADR-004 y con el límite de alcance de `34-laboral-module-proposal.md`.
