# Guías por plataforma — qué se comparte, qué es nativo

**Estado: RECOMMENDATION.**

Principio (ver `ux-principles.md` §5): **paridad funcional, no paridad pixel-por-pixel.** Se comparte todo lo que el usuario percibe como "identidad de marca" (color, tipografía, espaciado, forma, tono del texto); se deja nativo todo lo que el usuario espera que se comporte como el resto de su sistema operativo/navegador.

## Se comparte literalmente (mismo token, mismo valor)

- Paleta de color completa (`design-system.md` §1) — incluye light/dark.
- Escala de espaciado de 8pt (§3).
- Radio de esquina y criterio de elevación (§4).
- Estructura de estado (texto + color, nunca solo color — §5).
- Copy/tono de texto (mismos strings en botones/mensajes de error donde aplica — no hay traducción/localización todavía, `TBD` fuera de alcance).

## Usa convención nativa (mismo propósito, forma distinta)

| Elemento | Android | Web | Por qué diverge |
|---|---|---|---|
| Acción principal de pantalla (crear recordatorio) | `FloatingActionButton` (Material 3) | Botón fijo en la parte superior del formulario, mismo `color-primary` | Un FAB es la convención de Android para "acción principal de la pantalla" (Material Design lo define así); en Web no existe esa convención — un botón de formulario normal es lo esperado. Mismo color/función, forma distinta. |
| Navegación persistente (Inicio/Tareas/Compartidos/Más módulos) | `NavigationBar` de Material 3, 5 slots (Inicio/Tareas/+/Compartidos/Más), oculta en pantallas empujadas (`VidaBottomNav.kt`) | Sidebar fija de 232px con los mismos destinos (más los 6 módulos de maqueta), colapsa a franja horizontal <900px (`AppShell.tsx`) | **Hecho en `UX-006`** (esta fila decía "candidato futuro" desde `UX-004`/`UX-005` — la migración ocurrió en esta tarea, a partir de las imágenes de referencia del dashboard). Un `NavigationBar` inferior de 5 slots es idiomático en Android para navegación de nivel superior; una sidebar fija es la convención de un dashboard de escritorio en Web — mismo propósito, forma nativa a cada plataforma. Los antiguos links de header "Invitations"/"Notifications" de `RemindersScreen`/`RemindersPage` siguen existiendo donde algún test real depende de su texto exacto (`notifications_button` en Android; el enlace "Notifications" de la campana del encabezado en Web), pero ya no son el mecanismo principal de navegación. |
| Diálogo de confirmación/panel inline (compartir, ej. `ShareDialog`) | Panel inline en la misma pantalla (ya así desde `AND-004`) | Panel inline en la misma pantalla (ya así desde `WEB-004`) | Ambos ya convergen en el mismo patrón (no un modal/`AlertDialog` nativo) — se mantiene, solo se restilizan los colores/espaciado. |
| Indicador de carga | `CircularProgressIndicator` (Material 3) | Texto "Loading…" (ya existente) — candidato a spinner CSS en una iteración futura | Cambiar el indicador de Web a un spinner visual es una mejora cosmética menor, no prioritaria frente a lo que sí pide esta tarea (card, spacing, estados, share dialog) — se deja como está, con mejor color/tipografía de acompañamiento únicamente. |
| Tipografía | Roboto (Material 3 default) | Inter (`@fontsource/inter`) | Cada plataforma usa la fuente que se siente nativa en su ecosistema — Roboto es la fuente de sistema esperada en Android, Inter es una elección de Web ampliamente usada por su legibilidad en pantalla y por no depender de la fuente del sistema operativo del usuario (que varía mucho más en desktop que en Android). |

## Qué NO cambia en esta pasada (fuera de alcance, declarado)

Válido para `UX-001`..`UX-005` (pasada estrictamente visual, sin tocar flujo/lógica):

- Estructura de navegación (rutas, jerarquía de pantallas) — es lógica de flujo, no visual.
- Modelo de datos, llamadas de red, manejo de estado de los `ViewModel`/hooks existentes.
- Cualquier funcionalidad no visual — esta pasada es estrictamente presentación sobre lo que ya funciona (`01-technical-backlog.md`, `AND-002`..`WEB-006`).

**`UX-006` es la excepción documentada a la primera regla de arriba:** sí añade rutas/pantallas nuevas (Home, sidebar/bottom nav, 6 módulos de maqueta) — pero sobre las pantallas reales existentes (Tareas/Compartidos/Notificaciones) sigue aplicando la misma regla: solo presentación, ninguna llamada de red/lógica de esas tres pantallas cambió. Detalle completo en `01-technical-backlog.md` (`UX-006`) y `design-system.md` §7.

## UX-012 Navegación condicional por modo (ADR-015) — implementado (Web)

**DONE, Web** (2026-08-18, ver `01-technical-backlog.md` UX-012). La navegación descrita arriba (`NavigationBar`/sidebar única de Inicio/Tareas/Compartidos/Más) queda **ampliada, no reemplazada**, por ADR-015:

- Un selector superior (Calendario / Personal / Laboral) es el nivel de navegación más alto, por encima de la navegación existente — Calendario no tiene el navbar de Inicio/Tareas/Compartidos (FR-015); Personal y Laboral sí, cada uno con su propia instancia de ese navbar (real: `AppShell.tsx`'s `modeSwitcher`/`modeNavItems`).
- El selector muestra solo Calendario + los modos habilitados (FR-014/FR-016) — 2 o 3 opciones según el usuario, confirmado real en `e2e/mode-navigation.spec.ts` (solo "Calendario"+"Personal" antes de activar Laboral desde Ajustes; "Laboral" aparece después, sin reload).
- Tema de color: Personal reutiliza la paleta cálida existente sin cambios; Laboral usa la paleta fría real (`--color-laboral-*`, navy `#1E3F5C` + verde/gris de foco, WCAG 2.1 AA-verificada) documentada en `design-system.md` §11 — ya no TBD.
- **TBD sin cambios, no decidido en esta pasada:** si Android replica el mismo selector superior o usa una convención nativa distinta (p. ej. un segmented control del sistema) para el mismo propósito — Android sigue pausado hasta que Web tenga beta (decisión del usuario), esta pasada fue Web-only.

## UX-014 Módulo Laboral (ADR-016) — sin divergencia de plataforma declarada todavía

El Módulo Laboral (Personas/Proyectos/Compromisos) reutiliza la sidebar/AppShell y la paleta Laboral ya existentes (UX-012) — no introduce ningún patrón de navegación nuevo a nivel de plataforma. Las 7 secciones núcleo (Hoy, Agenda, Tareas, Personas, Proyectos, Seguimientos, Inbox) se documentan como una lista plana, igual que el navbar actual de Personal/Laboral; no se decide todavía si en Android esa lista vive en un `NavigationBar` de 5 slots con "Más" (como ya ocurre hoy) o en otro patrón — `TBD`, sin bloquear el diseño (Android sigue pausado, ver `§UX-012`).
