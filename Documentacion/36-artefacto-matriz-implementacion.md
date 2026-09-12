# 36 — Matriz de implementación del artefacto maestro

**Estado:** Fases 1 y 2 completadas (inspección + correspondencia)
**Fecha:** 2026-09-11
**Artefacto (fuente de verdad):** https://claude.ai/code/artifact/b31707e4-2719-4a26-b7e8-b870ded5a0ec
**Complementa:** `35-artefacto-maestro-matriz-capacidades.md`

---

## 1. Estado real inspeccionado

### Android
- **74 archivos Kotlin · 16 595 líneas · 28 composables de pantalla · 26 rutas.**
- Arquitectura: Compose + `VidaScreen`/`VidaScaffold`, sistema de tokens
  `VidaThemeSpec → VidaColors → LocalVidaColors` (ADR-023, 9 temas).
- **No hay mocks.** Las dos coincidencias de «MockData» en `VidaRepository.kt`
  y `ResourceApis.kt` son comentarios históricos: la app ya consume la API real
  vía `loadAll()` y 44 métodos de escritura.
- Fuentes ya empaquetadas: Inter 400/500/600/700, Sora 200/300/400/600,
  Caveat 500/600.

### Backend
- **23 controladores · 22 módulos · 32 migraciones Flyway (última: V32).**
- Patrón por módulo, constante y limpio:
  `api/XController.java` · `api/dto/*` · `application/XService.java` ·
  `domain/X.java` + `XRepository.java` + enums.
- La nueva numeración de migraciones arranca en **V33**.

---

## 2. BLOQUEANTES DE FIDELIDAD DETECTADOS

Se reportan en vez de resolverse por cuenta propia, porque las tres afectan a
decisiones aprobadas.

### B-1 · Falta `sora_700.ttf` — BLOQUEA fidelidad tipográfica

El artefacto usa **Sora 700** en todas las cifras grandes (`.fig`): el `5` de
«Para hoy» a 54 px, el `2` de «Atrasado» a 46 px, los `27`/`21` a 38 px, los
importes de Pagos, los porcentajes de los héroes.

Android solo tiene `sora_200/300/400/600`. Bajar a 600 sería exactamente la
sustitución por aproximado que §2 prohíbe.

**Acción requerida:** añadir `android/app/src/main/res/font/sora_700.ttf`
(Sora Bold, SIL Open Font License). Es un binario: no puede generarse desde
aquí. **Decisión del Product Owner:** aportar el fichero, o aprobar por escrito
el uso de Sora 600.

### B-2 · El artefacto tiene 2 temas; ADR-023 tiene 9

- **Artefacto:** pantalla Apariencia con **Claro** y **Noche** únicamente.
  Paleta blanca, Sora + Inter + Caveat, índigo `#4F46E5`, radios 24/26/30.
- **ADR-023 (aprobado):** nueve agendas — Aurora, Lumen, Neo, Calm, Studio,
  **Papel** (por defecto, crema `#F6F1E4` + Fraunces + azul `#2F6188`),
  Minimal, Productivity, Organic.

§17 exige un solo lenguaje visual; §25 del encargo anterior prohíbe reabrir
decisiones aprobadas. Las dos cosas no pueden ser ciertas a la vez.

**RECOMMENDATION (a aprobar):** la arquitectura de tokens ya resuelve esto sin
duplicar nada. Se añaden **Claro** y **Noche** como dos `VidaThemeSpec` que
transcriben los tokens del artefacto, y **Claro pasa a ser `DEFAULT`**. Los
otros ocho temas siguen compilando y funcionando — no son un segundo diseño,
son el mismo sistema con otros valores. La pantalla Apariencia muestra Claro y
Noche arriba, y los ocho restantes bajo «Más agendas».

**Decisión pendiente:** ¿se retiran los ocho temas de ADR-023, o conviven?
Mientras no haya respuesta, **conviven** (es lo reversible).

### B-3 · Módulo `mood` y clasificación de datos

El §12 del encargo lista `mood` como capacidad a implementar; el §13 pide
detener la parte que tenga una decisión pendiente explícita. `35-…-capacidades.md`
§7 pregunta 1 sigue abierta: **el ánimo es dato de salud** y aprobarlo cambia
retención, cifrado, exportación y borrado de toda la aplicación.

**Plan:** se implementa el backend **exactamente como está especificado** en
`35-…-capacidades.md` §4.1 — sin inventar campos, retención ni cifrado — y en la
misma entrega se actualiza `05-data/data-classification.md`. Esa actualización
documental queda marcada para tu firma. No se usan mocks como sustituto.

---

## 3. Matriz de correspondencia — Personal

| # | Pantalla (artefacto) | Ruta Android | Composable | Endpoint real | Entidad · tabla | Acciones |
|---|---|---|---|---|---|---|
| 1 | `portal` | **NUEVA** `portal` | *nuevo* `PortalScreen` | `GET /me` | `User` · `users` | Elegir contexto |
| 2 | `inicio` | `home` | `HomeScreen` ↻ | `loadAll()` | agregado | Navegar, marcar paso, ánimo |
| 3 | `dia` | **NUEVA** `dia` | *nuevo* `DiaScreen` | `GET /reminders` | `Reminder` · `reminder` | Abrir, poner hora |
| 4 | `bienestar` | **NUEVA** `bienestar` | *nuevo* `BienestarScreen` | **`/moods` (crear)** | **`MoodEntry`** | Marcar ánimo, etiquetas, nota |
| 5 | `calendario` | `calendar` | `CalendarScreen` ↻ | `loadAll()` + `/day-notes` | `DayNote` · `day_note` | Navegar, editar nota (doble clic) |
| 6 | `tareas` | `tasks` | `TasksScreen` ↻ | `GET /reminders` | `Reminder` | Filtrar, completar, abrir |
| 7 | `tarea` | **NUEVA** `tarea/{id}` | *nuevo* `TareaDetalleScreen` | `/reminders/{id}` + **`/steps`** | **`ReminderStep`** | Pasos, prioridad, editar, borrar |
| 8 | `pagos` | `payments` | `PaymentsScreen` ↻ | `GET /subscriptions` | `Subscription` | Filtrar, abrir |
| 9 | `pago` | **NUEVA** `pago/{id}` | *nuevo* `PagoDetalleScreen` | `/subscriptions/{id}/payments` | `SubscriptionPayment` | Registrar pago, editar |
| 10 | `garantias` | `warranties` | `WarrantiesScreen` ↻ | `GET /warranties` | `Warranty` | Abrir, completar |
| 11 | `mantenimientos` | `maintenance` | `MaintenanceScreen` ↻ | `GET /maintenance-records` | `MaintenanceRecord` | Abrir, completar |
| 12 | `mant` | **NUEVA** `mant/{id}` | *nuevo* `MantDetalleScreen` | `/maintenance-records/{id}` | idem + `occurrences` | Completar, borrar |
| 13 | `inventario` | `inventory` | `InventoryScreen` ↻ | `GET /inventory-items` | `InventoryItem` | Buscar, abrir |
| 14 | `documentos` | `documents` | `DocumentsScreen` ↻ | `GET /documents` | `Document` | Subir, descargar, compartir |
| 15 | `lugares` | `lugares` | `PlacesScreen` ↻ | `GET /places` | `Place` | CRUD |
| 16 | `notas` | **NUEVA** `notas` | *nuevo* `NotasDiaScreen` | `GET /day-notes` | `DayNote` | Editar a doble clic |
| 17 | `rutinas` | `rutinas` | `RoutinesScreen` ↻ | `/routines` + **`/progress`** | **`RoutineProgress`** | Ejecutar, sumar hábito |
| 18 | `compartidos` | `shared` | `SharedScreen` ↻ | `/shared-resources`, `/family` | `ResourceShare` | Mi parte, invitar |
| 19 | `board` | `board` | `VisionBoardScreen` ↻ | `/vision-boards` | `VisionBoard*` | Mover, editar, borrar |

## 4. Matriz de correspondencia — Laboral

| # | Pantalla | Ruta Android | Composable | Endpoint real | Entidad | Acciones |
|---|---|---|---|---|---|---|
| 20 | `hoy` | `hoy` | `HoyScreen` ↻ | `loadAll()` | agregado | Navegar |
| 21 | `agenda` | `agenda` | `AgendaScreen` ↻ | `GET /reminders`, `/commitments` | `Reminder`, `Commitment` | Navegar semana |
| 22 | `tareasLab` | `tasks-lab` | `LaboralTasksScreen` ↻ | `GET /reminders?context=LABORAL` | `Reminder` | CRUD |
| 23 | `personas` | `personas` | `PeopleScreen` ↻ | `GET /people` | `Person` | Buscar, abrir |
| 24 | `persona` | **NUEVA** `persona/{id}` | *nuevo* `PersonaDetalleScreen` | `/people/{id}` | `Person` | Llamar/mensaje (intent) |
| 25 | `proyectos` | `proyectos` | `ProjectsScreen` ↻ | `GET /projects` | `Project` | Abrir |
| 26 | `proyecto` | **NUEVA** `proyecto/{id}` | *nuevo* `ProyectoDetalleScreen` | `/projects/{id}/participants` | `ProjectParticipant` | Añadir participante |
| 27 | `seguimientos` | `seguimientos` | `CommitmentsScreen` ↻ | `GET /commitments` | `Commitment` | Resolver |
| 28 | `inbox` | `inbox` | `InboxScreen` ↻ | `/notes` + `resolve-task-suggestion` | `Note` | Clasificar |

## 5. Cuenta, estados y transversales

| # | Pantalla | Ruta | Endpoint | Nota |
|---|---|---|---|---|
| 29 | `notificaciones` | `notifications` | `/me/devices` | Deep link exige id en payload (ver 35 §4.6) |
| 30 | `ajustes` | `settings` | `/me`, `/me/modes` | ↻ visual |
| 31 | `perfil` | **NUEVA** `perfil` | `GET /me`, `DELETE /me` | Borrado suave 30 días (DEC-015) |
| 32 | `apariencia` | `appearance` | local (`prefs`) | Ver B-2 |
| 33 | `capacidades` | **NUEVA** `capacidades` | — | Pantalla de estado del propio proyecto |
| 34 | `estadoCarga` | transversal | — | `LoadingRows` ya existe |
| 35 | `estadoError` | transversal | — | Componente nuevo `VidaError` |
| 36 | `estadoVacio` | transversal | — | `EmptyState` ya existe, se reviste |
| 37 | `crear` | **NUEVA** `crear/{tipo}` | POST del recurso | Sustituye a `CreateResourceSheet` |

**↻** = pantalla existente: se conserva su funcionalidad y se adapta su
presentación (§16). **NUEVA** = ruta que hay que dar de alta en `Routes`,
`NavGraph` y `Destinations`.

**Rutas nuevas: 12.** Pantallas existentes a revestir: 25.

---

## 6. Componentes compartidos — artefacto → Compose

| Artefacto | Compose | Estado |
|---|---|---|
| `C.row` | `ResourceRow` | Existe · se ajusta a los tokens del artefacto |
| `C.ring` | **`VidaRing`** | **Nuevo** — anillo con `stroke-dasharray` animado 560 ms |
| `C.nav` | `VidaBottomNav` | Existe · ajustar píldora, FAB `-18 dp` y desenfoque |
| `C.empty` | `EmptyState` | Existe · nueva composición centrada con icono |
| `C.loading` | `LoadingRows` | Existe · ajustar a silueta con anillo |
| `C.error` | **`VidaError`** | **Nuevo** |
| `.tile` | **`VidaTile`** | **Nuevo** — retícula `1.32f/1f × 120/100 dp` |
| `.hero` | `HeroCard` | Existe · degradado y radio 30 dp |
| `.chip` / `.chip-s` | `VidaPill` | Existe · añadir variante pulsable |
| `.seg` | **`VidaSegmented`** | **Nuevo** |
| `.prop` | **`VidaProp`** | **Nuevo** |
| `.tick` | **`VidaTick`** | **Nuevo** |
| cara del ánimo | **`MoodFace`** | **Nuevo** — muelle 0,15 + parpadeo |

---

## 7. Backend a implementar (detalle en `35-…-capacidades.md`)

| Migración | Contenido |
|---|---|
| `V33__reminder_priority.sql` | Columna `priority` con DEFAULT `NORMAL` + índice |
| `V34__reminder_steps.sql` | Tabla `reminder_step` + índice `(reminder_id, position)` |
| `V35__routine_progress.sql` | `target_count`, `unit` en `routine` + tabla `routine_progress` |
| `V36__mood.sql` | `mood_entry` + `mood_entry_tag` (sujeto a B-3) |
| `V37__document_generic_resource.sql` | `resource_type`, `resource_id` en `document` + índice |
| `V38__warranty_blob_to_document.sql` | Migración de datos; retirada de columnas en una segunda fase |

---

## 8. Orden de trabajo acordado

| Fase | Contenido | Estado |
|---|---|---|
| 1 | Inspección | **Hecha** |
| 2 | Matriz de correspondencia | **Hecha** (este documento) |
| 3 | Sistema visual en Compose | En curso |
| 4 | Navegación: 12 rutas nuevas + flujos | Pendiente |
| 5 | Backend faltante | Pendiente |
| 6 | Integración con API y BD reales | Pendiente |
| 7 | Responsive teléfono/tablet | Pendiente |
| 8 | Validación contra el artefacto | Pendiente |

Sin pruebas automatizadas en esta entrega: la validación es manual y la hace el
Product Owner.
