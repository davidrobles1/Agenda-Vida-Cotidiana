# Prompt para Claude Code — Calendario UI + Backend Calendario + ADR-015 (en paralelo)

Contexto: proyecto "Vida Cotidiana", monolito modular Java 21/Spring Boot/PostgreSQL + Web React/TypeScript
(Vite). Android/iOS quedan **pausados** hasta que exista una beta de Web — no toques `android/` ni `ios/`
en esta tarea. Sigue `CLAUDE.md` (raíz del repo) y `Documentacion/AI-CONTEXT.md` como constitución: no
inventes requerimientos, etiqueta DECISION/ASSUMPTION/RECOMMENDATION/TBD/FUTURE donde corresponda, usa IDs
de backlog estables continuando la numeración real de `docs/development/01-technical-backlog.md` (no
reutilices IDs ya usados), y no me des resultados simulados — todo lo que reportes de vuelta debe ser
evidencia real (`./mvnw clean test` real, capturas/consola reales de Playwright o del navegador, etc.).

Estas tres líneas de trabajo avanzan **a la vez**, no una detrás de otra. Coordínalas solo en el punto
exacto donde una depende de otra (marcado abajo como "Depende de"); todo lo demás avanza en paralelo.

---

## Bloque A — Vistas Semana/Día del Calendario (Web, UX-010)

Hoy `web/src/features/calendar/CalendarPage.tsx` + `core/ui/components/CalendarView.tsx` solo tienen vista
de Mes. Agrega vista Semana y vista Día, seleccionables por el usuario (mismo patrón de selector de rango
que ya usamos en la maqueta que aprobé: pastillas "Día / Semana / Mes").

- Reutiliza `useCalendarData()` (`web/src/features/calendar/useCalendarData.ts`) tal cual — ya trae
  `reminders`, `invitations`, `warranties`/`maintenanceRecords` (mock), `mockCompletedIds`,
  `mockScheduledDueAt`. No dupliques esa lógica de datos: solo agrega las nuevas vistas de presentación
  sobre los mismos datos.
- Vista Semana: 7 columnas (días), franjas horarias razonables; vista Día: una sola columna con franjas
  horarias, mismo criterio visual (Fraunces/paleta "Agenda" ya centralizada en `index.css`, ver
  `Documentacion/02-ux-ui/design-system.md`).
- Mantén las mismas interacciones ya reales que tiene la vista Mes (crear, completar, reprogramar — aunque
  reprogramar sigue siendo mock hasta que exista un `PATCH`/`PUT` real de `dueAt`, ver nota en
  `useCalendarData.ts` línea 33-40 — no lo conviertas a real en este bloque, eso es Bloque B si acaso).
- Responsive: en pantallas angostas, Semana/Día deben seguir siendo usables (no asumas que el usuario
  siempre tiene escritorio ancho).
- Verificación real requerida: Playwright real (no simulado) navegando las 3 vistas, creando/completando
  un recordatorio en cada una.

**No hagas en este bloque:** no toques el color/agrupación por `context` (Personal/Laboral) — eso es
Bloque C, y depende de que `REMINDER.context` exista (Bloque B).

---

## Bloque B — Backend real de Garantías/Mantenimiento + campos de ADR-015

### B.1 — Garantías y Mantenimiento reales (hoy 100% mock en `core/mock/mockData.ts`)

Sigue el mismo patrón ya usado por `reminder` (Clean Architecture: `api/application/domain/infrastructure`,
autorización por recurso igual que `ReminderControllerIntegrationTest` — dueño ve/edita, ajeno recibe `404`
nunca `403`, mismo criterio que `SEC-001`). No inventes campos que no estén ya sugeridos por la UI mock
existente (`web/src/core/mock/mockData.ts` — revísalo antes de diseñar el schema, los campos que ya
muestra la UI son la especificación real, no un ejemplo).

- Migraciones Flyway nuevas para `WARRANTY` y `MAINTENANCE_RECORD` (dueño, item, fecha, estado — alinea
  los nombres de campo exactos con lo que ya renderiza `WarrantiesPage.tsx`/`MaintenancePage.tsx`).
- Endpoints REST equivalentes a los de `reminder` (`GET/POST`, `GET/PATCH/DELETE {id}`, completar) bajo
  `/api/v1`. Documenta en `Documentacion/openapi/openapi.yaml` (no rompas la validación de contrato real,
  `TEST-API-001`).
- Actualiza `Documentacion/09-data-model.md` (nuevas entidades en el ERD Mermaid) y `03-prd.md` (nuevo FR,
  siguiente ID disponible después de FR-019) — no autoasignes versión "V1"; estas entidades no estaban en
  el pack de decisiones original, quedan como parte de este ciclo de trabajo actual, etiqueta la versión
  que corresponda según cuándo se aprobó (hoy, 2026-08-18) y dilo explícitamente en el PRD.
- En Web: sustituye en `useCalendarData.ts` los arrays mock (`warranties`, `maintenanceRecords`,
  `mockCompletedIds` para esos dos tipos) por llamadas reales, siguiendo el mismo patrón que `reminders`
  (`listReminders`/`completeReminder`). El `mockScheduledDueAt` de recordatorios puede seguir mock si el
  `PATCH` de `dueAt` de `reminder` tampoco existe todavía — no lo inventes aquí tampoco, es un gap ya
  documentado (`useCalendarData.ts` línea 33-40), no un requisito nuevo de esta tarea.
- Verificación real requerida: tests de integración reales (Testcontainers, mismo patrón que
  `ReminderControllerIntegrationTest`), `./mvnw clean test` en verde con el número real de casos.

### B.2 — Campos de datos de ADR-015 (`Documentacion/22-decision-log.md` ADR-015, `09-data-model.md`)

- `USER`: agrega `personal_enabled` (boolean, default `false`) y `laboral_enabled` (boolean, default
  `false`) vía migración Flyway.
- `REMINDER`: agrega `context` (`PERSONAL`/`LABORAL`, **NOT NULL** — ver FR-019, se infiere del navbar de
  origen, no hay selector en el formulario). Como los recordatorios existentes no tienen contexto, define
  una migración de backfill explícita y documentada (ver TBD abajo — no la resuelvas asumiendo, pregúntame
  si no es obvia).
- Endpoint para activar un modo (`FR-016`) — sugerido `PATCH /me` o `POST /me/modes` (elige el que
  respete mejor el estilo REST ya usado en `openapi.yaml`, documenta la elección).

**TBD real, no lo resuelvas por tu cuenta — repórtalo de vuelta:** ¿qué `context` reciben los recordatorios
`REMINDER` que ya existen en la base de datos al aplicar la migración, si el usuario dueño no tenía ningún
modo habilitado antes de esta migración? No asumas `PERSONAL` por defecto sin decirlo explícitamente como
`ASSUMPTION` marcada — es una migración de datos reales, no un detalle cosmético.

**Depende de:** nada de otro bloque. B.1 y B.2 pueden avanzar en paralelo entre sí.

---

## Bloque C — ADR-015: navegación Personal/Laboral/Calendario (Web)

Referencia completa: `Documentacion/22-decision-log.md` ADR-015, `03-prd.md` FR-014 a FR-019,
`04-use-cases.md` UC-01 (paso 6-7)/UC-15/UC-16, `02-ux-ui/platform-guidelines.md` UX-012. Maqueta visual
de referencia ya aprobada por mí en esta conversación (selector superior en pastillas, paleta Personal =
actual beige/terracota/azul, paleta Laboral = azul marino `#1e3f5c` + verde/gris de foco, Calendario mezcla
colores por origen en vez de un tercer tema).

- **Onboarding (FR-014):** tras el registro exitoso en Keycloak, pantalla nueva con dos checkboxes
  independientes "Personal"/"Laboral". Debe exigir al menos una marcada para continuar (validación real en
  el formulario, con mensaje de error visible, no solo deshabilitar el botón silenciosamente).
- **Selector superior condicional (FR-015):** "Calendario" siempre presente + solo los modos habilitados.
  Vista por defecto al abrir la app: Calendario (no Home/Inicio) — este es un cambio real de ruta inicial,
  no cosmético; verifícalo con un test real de navegación post-login.
  - Personal y Laboral: cada uno con su propio navbar (Inicio, Calendario del modo, Tareas, Compartidos —
    usa exactamente esos 4 por ahora; la lista completa de items queda `TBD` en `platform-guidelines.md`,
    no agregues módulos que no estén ya decididos).
  - Calendario (general): sin el navbar de Inicio/Tareas/Compartidos — es su propio nivel de navegación
    (ver UX-012). Reutiliza el `CalendarPage.tsx` ya existente (con Semana/Día del Bloque A) como base de
    esta vista general; no crees una página nueva duplicada.
- **Ajustes / activar modo adicional (FR-016):** `web/src/features/settings/` está vacío hoy (solo
  `.gitkeep`) — esta es la primera pantalla real de Ajustes del proyecto. Construye lo mínimo necesario
  para este requisito (activar el modo no habilitado); no agregues configuración no pedida.
- **Contexto inferido (FR-019):** el formulario de nuevo recordatorio, cuando se crea desde el navbar de
  Personal, envía `context: "PERSONAL"`; desde Laboral, `"LABORAL"`. Sin selector visible.
- **Colores por modo:** Personal reutiliza los tokens `--color-*` ya existentes en `index.css`. Laboral es
  un set de tokens nuevo — sigue el mismo patrón de nombrado (`--color-laboral-primary`, etc., o el que ya
  uses de convención) y documenta los valores hex finales en `design-system.md`, actualizando UX-012 de
  "pendiente de implementar" a implementado con los valores reales.
- **Calendario general colorea por origen (FR-017):** cada recordatorio se muestra con el acento de su
  `context` (terracota si `PERSONAL`, verde/gris si `LABORAL`), no un tercer color neutral — igual que en
  la maqueta aprobada.

**Depende de Bloque B.2** (necesita `USER.personal_enabled/laboral_enabled` y `REMINDER.context` reales
antes de que el selector condicional y el coloreado por origen puedan funcionar contra datos reales). Si
llegas a este bloque antes de que B.2 esté listo, construye la UI igual pero contra datos mock claramente
marcados, y no bloquees el resto — avísame en el reporte qué quedó sobre mock esperando a B.2.

**Verificación real requerida:** registro real de un usuario nuevo marcando solo Personal, verifica que
Laboral no aparece en el selector; actívalo desde Ajustes, verifica que aparece; crea un recordatorio desde
cada navbar y confirma en el Calendario general que aparece con el color correcto.

---

## Al terminar cada bloque

Actualiza en paralelo (no al final de todo) la documentación que corresponda a lo que ya implementaste:
`docs/development/01-technical-backlog.md` (IDs nuevos, continuando la numeración real — no reutilices
`WEB-008`/`BE-036`/`UX-011` ni ninguno ya usado), `Documentacion/12-traceability.md` (filas FR-014..019 y
la nueva de Garantías/Mantenimiento, columnas API/Backlog dejan de ser `TBD`), y
`docs/development/05-v2-plan.md`. No toques `docs/generated/*.docx`/`*.pptx` (son snapshots de V1, no
corresponden a este ciclo).

Repórtame de vuelta: qué se completó con evidencia real (no builds/tests simulados), qué quedó bloqueado
por el TBD de la migración de `REMINDER.context` en datos existentes, y cualquier `DOCUMENTATION_CONFLICT`
que encuentres entre lo que pide este prompt y lo que ya existe en el código.
