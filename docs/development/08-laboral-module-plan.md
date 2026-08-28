# 08 — Plan de trabajo: Módulo Laboral (ADR-016)

Este documento traduce la decisión aprobada en `Documentacion/22-decision-log.md` ADR-016 y el análisis de `Documentacion/34-laboral-module-proposal.md` en tareas de ingeniería. Sigue el mismo formato que `01-technical-backlog.md` (IDs `BE-xxx`/`WEB-xxx`/`UX-xxx`, estado DONE/PARTIAL/BLOCKED/TODO).

**Actualización (2026-08-22):** las Fases 1 (Backend) y 2 (Web) se implementaron y verificaron en esta misma sesión — ver `BE-039`/`WEB-010` en `01-technical-backlog.md` (detalle completo). El TBD de `WEB-010` (¿retirar Vision Board/Compartidos del navbar de Laboral?) fue confirmado explícitamente por el usuario: **sí, solo el enlace del navbar — los módulos, rutas, componentes y datos de Vision Board/Compartidos no se tocan.**

**Actualización (mismo día):** Fase 3a (Notas vinculadas, `BE-040`/`WEB-011`), 3c (última interacción derivada, `WEB-012`) y 3b (Documentos vinculados, `BE-041`/`WEB-013`) también se cerraron, cada una como incremento aislado — 3d/3e/3f siguen sin empezar. **Nota operativa recurrente:** cada incremento de backend en este entorno compartido (sin `spring-boot-devtools`) requirió reiniciar el proceso Java para cargar el código nuevo; el `OIDC_ISSUER`/`DB_URL` correctos están documentados en `22-decision-log.md` ADR-016.

**Actualización (mismo día, desbloqueo de 3e):** el Product Owner aprobó la definición funcional mínima de Objetivos/Rutinas/Lugares/Recursos (ver `22-decision-log.md` ADR-016 adenda Fase 3e, `03-prd.md` FR-031 a FR-034, `04-use-cases.md` UC-24 a UC-27, `13-acceptance.md` AC-018 a AC-021, `09-data-model.md` §"V4 candidato — Fase 3e"). 3e deja de ser un bloque `BLOCKED` único y se divide en cuatro incrementos aislados (3e1-3e4, ver desglose abajo) — **ninguno implementado todavía**, esto es solo la definición de alcance para desarrollo posterior. 3d sigue `BLOCKED` por separado (no se resolvió en esta actualización).

Fuente de verdad de los requerimientos: `Documentacion/03-prd.md` (FR-021 a FR-028, NFR-011), `Documentacion/09-data-model.md` (§"V3 — Módulo Laboral"), `Documentacion/11-auth-security.md` (SEC-004), `Documentacion/openapi/openapi.yaml` (pendiente de extender).

## Resumen de fases

| Fase | Contenido | Depende de | Versión |
|---|---|---|---|
| 1 | Backend: modelo de datos + API core | Ninguna (ADR-016 ya aprobado) | V3 |
| 2 | Web: navegación y pantallas core | Fase 1 (API real) | V3 |
| 3 | Notas/Documentos vinculados, última interacción, automatizaciones simples | Fase 2 | V4 (candidato, no comprometido) |
| 4 | Pipeline/CRM avanzado, Casos, Obras, Clases, Insights, Asistente | — | Post-V4 (no planificado) |

## Fase 1 — Backend: modelo de datos y API core (V3) — **DONE (2026-08-22)**

| ID | Tarea | Detalle | Estado |
|---|---|---|---|
| `BE-039` | Migración `V11__adr016_laboral_module.sql` (renumerada desde la "V7" tentativa: V7-V10 ya tomadas) | Tablas `PERSON`, `PROJECT`, `COMMITMENT` + columnas nullable en `REMINDER` (`person_id`, `project_id`, `location`) — ver `09-data-model.md`. Sin backfill necesario. | **DONE** |
| `BE-039` | Módulo `person` (Clean Architecture) | CRUD dueño-únicamente, mismo patrón que `warranty`/`maintenance`: `domain`/`application`/`api` (sin capa `infrastructure` separada, igual que `warranty`). Endpoints `GET/POST /people`, `GET/PATCH/DELETE /people/{id}`. | **DONE** |
| `BE-039` | Módulo `project` | CRUD dueño-únicamente, mismo patrón. Endpoints `GET/POST /projects`, `GET/PATCH/DELETE /projects/{id}`. `clientPersonId`, si se envía, validado contra `PersonService` (404 `PERSON_NOT_FOUND` si no pertenece al mismo dueño). | **DONE** |
| `BE-039` | Módulo `commitment` | CRUD + `POST /commitments/{id}/resolve`. Filtro `GET /commitments?direction=MINE\|THEIRS` (FR-027) — no dos endpoints separados. Validación cruzada de `personId`/`projectId`/`originReminderId` contra el mismo dueño. | **DONE** |
| `BE-039` | Extender módulo `reminder` | `POST`/`PATCH /reminders/{id}` aceptan `personId`/`projectId`/`location` opcionales, validados igual que arriba. Constructores/`applyEdit` sobrecargados (mismo patrón que `BE-038` con iconId/stickerId) — cero cambio a los call sites existentes. | **DONE** |
| `BE-039` | Actualizar `openapi.yaml` | 3 recursos nuevos (`Person`/`Project`/`Commitment` + requests) y campos nuevos de `Reminder`/`CreateReminderRequest`/`UpdateReminderRequest`. Validado como YAML antes de compilar. | **DONE** |
| `BE-039` | Tests | `PersonControllerIntegrationTest` (6), `ProjectControllerIntegrationTest` (8, incluye el cruce de `clientPersonId` ajeno), `CommitmentControllerIntegrationTest` (9, incluye filtro por dirección y resolve), + 2 casos nuevos en `ReminderControllerIntegrationTest`. `./mvnw clean test`: **217/217 en verde**. | **DONE** |

Ver el detalle completo (incluyendo el ASSUMPTION sobre `Commitment.personId` NOT NULL y el razonamiento de dependencias entre módulos) en `01-technical-backlog.md` (`BE-039`).

## Fase 2 — Web: navegación y pantallas core (V3) — **DONE (2026-08-22)**

| ID | Tarea | Detalle | Estado |
|---|---|---|---|
| `WEB-010` | Navbar Laboral: 7 secciones núcleo | Hoy, Agenda, Tareas, Personas, Proyectos, Seguimientos, Inbox — reemplaza el navbar reducido anterior (`Inicio`, `Calendario laboral`, `Vision Board`, `Compartidos`) **solo en `laboralNavItems` de `AppShell.tsx`**. Confirmado explícitamente por el usuario: Vision Board/Compartidos no se borran (módulos/rutas/componentes/datos intactos, siguen usándose en Personal), solo pierden su enlace en *este* navbar — mismo patrón que Documentos/Inventario en Personal. | **DONE** |
| `WEB-010` | Pantalla "Hoy" | `features/laboral/HoyPage.tsx` — FR-026: `REMINDER` (context=LABORAL, vencen hoy) + `COMMITMENT` (requieren atención) + captura rápida hacia el Inbox. | **DONE** |
| `WEB-010` | "Agenda" | Reutiliza la ruta/componente `CalendarPage.tsx` existente (`/laboral/calendar`) tal cual — cero cambios de código, solo la etiqueta del navbar. | **DONE** |
| `WEB-010` | Pantalla "Tareas" | `features/laboral/TareasPage.tsx` (página nueva, no `RemindersPage.tsx` — deliberado, ver `01-technical-backlog.md` `WEB-010`) + `CreateTaskDialog.tsx` con selector de Persona/Proyecto (FR-023). | **DONE** |
| `WEB-010` | Pantalla "Personas" | `features/people/PeoplePage.tsx` + `CreatePersonDialog`/`PersonDetailDialog` (compromisos/proyectos/tareas vinculados + "Crear seguimiento" embebido, FR-021/UC-18). | **DONE** |
| `WEB-010` | Pantalla "Proyectos" | `features/projects/ProjectsPage.tsx` + `CreateProjectDialog`/`ProjectDetailDialog` ("Nueva tarea" embebida, FR-022/UC-19). Sin pestañas Reuniones/Documentos (no hay entidad Reunión/Documento propia — ver ADR-016). | **DONE** |
| `WEB-010` | Pantalla "Seguimientos" | `features/commitments/CommitmentsPage.tsx` — pestañas Mías/Esperando vía `GET /commitments?direction=` real (FR-027/UC-20). | **DONE** |
| `WEB-010` | Pantalla "Inbox" | `features/laboral/InboxPage.tsx` — captura + conversión a Tarea/Compromiso. **No reutiliza `/notes`** como se planteaba aquí originalmente: el backend `NOTE` no se extendió con vínculos opcionales en la Fase 1, así que no hay forma de filtrar "sin clasificar" server-side — implementado en `localStorage` (ver `inboxStorage.ts`), ASSUMPTION documentada. | **DONE** |

**Verificación real (no solo `tsc`/lint):** Playwright contra el backend/Keycloak/Postgres reales ya corriendo — login, activar Laboral, navbar de 7 items confirmado, Persona→Proyecto→Tarea→Compromiso creados de punta a punta, tabs Mías/Esperando, Inbox (captura+conversión), cero errores de consola. Navbar de Personal reverificado sin cambios (10 items). Detalle completo en `01-technical-backlog.md` (`WEB-010`).

**No se construyó en esta fase (candidato futuro, `RECOMMENDATION`):** `UX-015` vocabulario adaptable por perfil (`design-system.md` §12) — no bloqueaba nada de lo anterior, se deja como mejora incremental.

## Fase 3 — V4 (candidato, `RECOMMENDATION`, no comprometido en bloque)

**Actualización (2026-08-22):** este bloque se desagregó en sub-fases independientes para poder avanzar de forma incremental y aislada, sin comprometer las demás de una sola vez.

| Sub-fase | Contenido | Estado | Depende de |
|---|---|---|---|
| **3a — Notas vinculadas** | `NOTE.person_id`/`project_id` opcionales (`FR-029`/`UC-22`); sección "Notas" + alta embebida en `PersonDetailDialog`/`ProjectDetailDialog` | **DONE** (2026-08-22, `BE-040`/`WEB-011`) | Ninguna — incremento aislado sobre Fases 1/2 ya cerradas |
| 3b — Documentos vinculados | Extender el módulo `document` con `person_id`/`project_id`; sección "Documentos" (solo lectura) en `PersonDetailDialog`/`ProjectDetailDialog` | **DONE** (2026-08-22, `BE-041`/`WEB-013`) — se verificó inactividad real (~7h sin cambios) antes de tocarlo, no una confirmación formal de la otra sesión | Ninguna |
| 3c — Última interacción derivada | Calcular en `PersonDetailDialog` a partir de notas/tareas/compromisos ya vinculados | **DONE** (2026-08-22, `WEB-012`) | 3a (ya cumplida) |
| 3d — Automatizaciones simples | Sugerencia de tarea a partir de una nota, con disparador manual | **DONE** (2026-08-28, `BE-047`/`WEB-019`) — la regla la definió el Product Owner ese día: botón "Sugerir tarea" (manual, sin detección automática), convertir o descartar, y una vez resuelta no vuelve a ofrecerse. FR-035, UC-28, AC-022 | Ninguna |
| 3e1 — Objetivos | Entidad `OBJECTIVE` independiente, progreso manual, resumen en "Hoy" | **DONE** (2026-08-28, `BE-042`/`WEB-015`) | Ninguna — incremento aislado |
| 3e2 — Rutinas | Entidad `ROUTINE`, marcado manual de ejecución, **sin** generar `REMINDER`/`COMMITMENT` | **DONE** (2026-08-28, `BE-043`/`WEB-016`) — el TBD de la fórmula de avance lo resolvió el Product Owner el mismo día (opción B: desde la fecha programada) | Ninguna — deliberadamente separada de 3d |
| 3e3 — Lugares | Entidad `PLACE`, autocompletado de texto en `CreateTaskDialog` (sin FK en `REMINDER`) | **DONE** (2026-08-28, `BE-044`/`WEB-017`) | Ninguna |
| 3e4 — Recursos | Entidad `RESOURCE`, vínculo opcional a Persona/Proyecto (no sustituye `DOCUMENT`) | **DONE** (2026-08-28, `BE-045`/`WEB-018`) — el TBD de `reference` lo resolvió el Product Owner el mismo día (opción A: campo único de texto libre) | Ninguna |
| 3f — Kanban de Proyecto | Vista alterna de Tareas por estado (Pendientes/Completadas — los 2 únicos estados reales de `REMINDER`, sin inventar columnas intermedias) | **DONE** (2026-08-22, `WEB-014`) | Ninguna (solo UI, reutiliza `completeReminder`) |

Detalle completo de 3a en `01-technical-backlog.md` (`BE-040`/`WEB-011`) y `22-decision-log.md` (ADR-016, incluye el hallazgo operativo del `OIDC_ISSUER` del entorno compartido).

### Fase 3e — desglose de incrementos (alcance aprobado, 2026-08-22, ninguno implementado)

Fuente de verdad de esta definición: `22-decision-log.md` ADR-016 adenda Fase 3e, `03-prd.md` FR-031 a FR-034, `04-use-cases.md` UC-24 a UC-27, `13-acceptance.md` AC-018 a AC-021, `09-data-model.md` §"V4 candidato — Fase 3e". Todas las entidades son dueño-únicamente (mismo patrón de autorización que `PERSON`/`PROJECT`/`COMMITMENT`, 404 nunca 403), Clean Architecture (`domain`/`application`/`api`, sin capa `infrastructure` separada, igual que `warranty`/`person`), y no requieren ninguna migración sobre datos existentes (tablas nuevas, sin backfill).

| Incremento | Alcance mínimo | Backend necesario | Frontend necesario | Relaciones | Criterios de aceptación | Ambigüedades / TBD |
|---|---|---|---|---|---|---|
| **3e1 — Objetivos** | CRUD completo, entidad standalone | Módulo nuevo `objective` (migración `OBJECTIVE`, domain/application/api, mismo patrón que `person`) | `ObjectivesPage.tsx` + `CreateObjectiveDialog` + tarjeta resumen en `HoyPage.tsx` (candidato `BE-042`/`WEB-015`, IDs reservados) | Ninguna (independiente en este incremento) | AC-018 | Punto de entrada exacto de la página dedicada (¿enlace desde "Hoy" únicamente, o también desde Ajustes?) — no bloqueante, no impide empezar |
| **3e2 — Rutinas** | CRUD + acción "marcar ejecución como realizada" (avanza `nextExecutionDate`) | Módulo nuevo `routine` (migración `ROUTINE`, mismo patrón) | `RoutinesPage.tsx` + `CreateRoutineDialog` (candidato `BE-043`/`WEB-016`) | Ninguna | AC-019 | Fórmula exacta de avance de `nextExecutionDate` por `frequency` (casos borde de fin de mes); valor inicial al crear; si `active=false` oculta la Rutina de algún listado — ninguna bloquea empezar el CRUD base, sí afectan la acción de completar |
| **3e3 — Lugares** | CRUD + selector "Lugar guardado" en `CreateTaskDialog` | Módulo nuevo `place` (migración `PLACE`, mismo patrón, más simple que `person`) | Selector inline en `CreateTaskDialog.tsx` (+ "Nuevo lugar" inline o página simple de gestión) (candidato `BE-044`/`WEB-017`) | `person_id` opcional (FK a `PERSON`) | AC-020 | Ninguna bloqueante — el alcance evita deliberadamente tocar `REMINDER` |
| **3e4 — Recursos** | CRUD + secciones "Recursos" en detalle de Persona/Proyecto (mismo patrón de solo lectura que "Documentos", `WEB-013`) | Módulo nuevo `resource` (migración `RESOURCE`, mismo patrón; `ResourceService` reutiliza `PersonService.getOwnedOrThrow`/`ProjectService.getOwnedOrThrow` igual que `NoteService`/`DocumentService`) | Sección "Recursos" en `PersonDetailDialog`/`ProjectDetailDialog` (candidato `BE-045`/`WEB-018`) | `person_id`/`project_id` opcionales (mismo patrón que `NOTE`/`DOCUMENT`) | AC-021 | `reference` como campo único de texto vs. dos campos separados (`url`/`description`) — ver `09-data-model.md` |

**Estado real (2026-08-28): las cuatro sub-fases están implementadas y verificadas.** Se ejecutaron en el orden recomendado abajo (3e1 → 3e3 → 3e2 → 3e4), cada una como incremento aislado con sus propios tests. Las dos ambigüedades señaladas en la tabla se elevaron al Product Owner en cuanto bloquearon el avance y se resolvieron el mismo día, sin adivinarlas: **3e2 opción B** (avance desde la fecha programada) y **3e4 opción A** (`reference` como campo único de texto libre). Detalle completo en `01-technical-backlog.md` (`BE-042` a `BE-046`, `WEB-015` a `WEB-018`) y en `22-decision-log.md` (ADR-016).

**Recomendación de orden de implementación (redactada antes de implementar, cumplida tal cual):** **3e1 (Objetivos) primero.** Es la única con un ejemplo concreto ya documentado desde `34-laboral-module-proposal.md`, sigue el patrón CRUD más simple ya usado en `person`/`project`/`warranty` sin ninguna relación nueva que validar, y no depende de resolver ninguna ambigüedad de negocio pendiente. **3e3 (Lugares)** es la segunda opción razonable — bajo riesgo, complementa un campo ya existente (`REMINDER.location`, FR-024) sin tocar su esquema. **3e2 (Rutinas)** y **3e4 (Recursos)** tienen ambigüedades técnicas menores (ver tabla) que no bloquean empezar, pero conviene resolverlas antes de terminar cada uno respectivamente.

## Fase 4 — Post-V4 (no planificado)

Pipeline/CRM avanzado, Casos, Obras, Clases, Insights, Asistente conversacional, automatización proactiva/IA. Fuera de alcance hasta validación explícita — coherente con la prohibición de IA/Finanzas de `CLAUDE.md`/ADR-003/ADR-004.

**Revisión documental (2026-08-28), a petición del Product Owner de "determinar alcance, dependencias y orden correcto" de esta fase:** se revisaron `02-roadmap.md` §Post-V4, `34-laboral-module-proposal.md` §10/§13/§14/§16 y `22-decision-log.md`. **Conclusión: esta fase no tiene alcance implementable hoy, y no por falta de detalle, sino por decisiones ya tomadas.** Su contenido documentado se reparte en tres grupos, los tres cerrados:

| Contenido documentado de Fase 4 | Por qué no es implementable |
|---|---|
| Pipeline/CRM de ventas con etapas, oportunidades, forecast, scoring | **Descartado explícitamente por el Product Owner** (ADR-016, Alternativa (c): "no queremos un ERP/Salesforce/Jira/Notion"). No es un TBD: es un "no". |
| Casos legales, Obras con planos, Clases, historiales clínicos | Especialización vertical, `34-laboral-module-proposal.md` §14 los clasifica como "alto costo, bajo % de usuarios cada uno". Sin definición funcional de ninguno — arrancarlos exigiría inventar requerimientos. |
| Insights, Asistente conversacional, automatización proactiva por patrones | **Prohibidos por `CLAUDE.md`/ADR-003** (regla de IA). `34-laboral-module-proposal.md` §13 lo dice explícitamente: "requiere evaluar si es determinista o necesita un modelo, y por tanto cae bajo la prohibición de IA hasta que se decida lo contrario". |

`02-roadmap.md` §Post-V4 lo resume: *"Explícitamente fuera de alcance hasta validación"*. Avanzar cualquiera de los tres grupos exigiría inventar funcionalidades o levantar la prohibición de IA — ambas cosas fuera de lo que un incremento de ingeniería puede decidir.

**Además, todos los candidatos V4 que sí estaban definidos ya están hechos:** Notas vinculadas (3a), Documentos vinculados (3b), última interacción (3c), automatizaciones simples (3d), Objetivos/Rutinas/Lugares/Recursos (3e), Kanban (3f). Es decir, la Fase 3 cierra el alcance V4 completo de `02-roadmap.md` §V4.

**Único trabajo real que quedaba documentado y no prohibido** (no es Fase 4, pero era lo que existía): `UX-015` — vocabulario adaptable por perfil profesional (`02-ux-ui/design-system.md` §12, ADR-016(d)). **DONE (2026-08-28, `WEB-020`):** el Product Owner resolvió la decisión pendiente — el perfil se elige en **Ajustes → Perfil**, como preferencia del usuario. Implementado como capa de presentación pura (sin cambio de esquema, API ni comportamiento), persistida en `localStorage` por coherencia con ADR-016(d). Ver `01-technical-backlog.md` (`WEB-020`) y `design-system.md` §12 para el detalle, incluida la decisión sobre género gramatical y lo que quedó deliberadamente fuera.

**Con esto no queda trabajo pendiente definido en el Módulo Laboral.** Cualquier avance adicional requiere una decisión de producto nueva: aprobar alguno de los verticales Post-V4, o levantar la prohibición de IA de `CLAUDE.md`/ADR-003.

## Qué no se hace en este plan

- **Android/iOS:** sin cambios — desarrollo pausado hasta beta de Web (decisión ya registrada para ADR-015 en `05-v2-plan.md`; este plan hereda la misma pausa, no la reabre).
- **Ningún endpoint de IA ni integración financiera** (regla del proyecto, `CLAUDE.md`).
- **Ninguna migración de datos existentes** — todas las columnas/tablas nuevas son aditivas y nullable; ningún `REMINDER`/`USER` preexistente cambia de valor (a diferencia del backfill real que sí requirió ADR-015/`BE-038`).

## Riesgos de ejecución

- ~~`WEB-010` tenía una decisión de UX pendiente de confirmar (retirar o no `Vision Board`/`Compartidos` del nivel superior de Laboral).~~ Resuelto: el usuario confirmó explícitamente retirar solo el enlace del navbar, sin tocar módulos/rutas/datos — implementado así.
- ~~`commitment` depende de `person`/`project` por las FKs — no paralelizar sin coordinar el orden de migraciones.~~ Resuelto en `BE-039`: implementado en el orden correcto (person → project → reminder → commitment), sin ciclos.
- ~~Ninguna tarea de este plan tiene todavía criterios de aceptación en `13-acceptance.md` — deben añadirse (`AC-018` en adelante) antes de considerar cualquier tarea "lista para desarrollo", siguiendo la regla de `12-traceability.md`.~~ Resuelto parcialmente: `AC-018` a `AC-021` ya cubren 3e1-3e4 (2026-08-22). Las Fases 1-3b/3f (`FR-021` a `FR-030`) siguen sin AC propio en `13-acceptance.md` — gap preexistente, fuera del alcance de esta actualización (no se tocó por no haber sido pedido).
- 3e1-3e4 quedan `TODO` (alcance definido, no implementado). Al implementar cada uno: seguir el mismo patrón de incremento aislado que 3a/3b/3c/3f (una sub-fase por sesión, no re-ejecutar la suite completa, verificar solo lo afectado, actualizar `01-technical-backlog.md`/`22-decision-log.md` al cerrar cada una).
