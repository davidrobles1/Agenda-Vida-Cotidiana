# Design System — Vida Cotidiana

**Estado: RECOMMENDATION.** Todo lo de este documento es una decisión de diseño, no de negocio — si al verlo en pantalla algo no encaja, se ajusta sin que eso implique reabrir ningún requerimiento funcional. Ningún token de aquí condiciona un `FR-*`/`UC-*`; solo la presentación de lo que ya está definido en `03-prd.md`/`04-use-cases.md`.

Este documento es la fuente única de los tokens visuales compartidos entre Android y Web. `platform-guidelines.md` explica qué se comparte literalmente (estos tokens) y qué usa convención nativa (forma de los componentes).

---

## 1. Color

**RECOMMENDATION.** Un solo color semilla (`primary`) con roles derivados, siguiendo el mismo modelo de "color roles" de Material 3 — así Android puede generar su `ColorScheme` directamente desde estos valores y Web los usa como variables CSS sin duplicar la decisión.

Justificación breve: azul índigo — profesional, calmado, no asociado a alarma/urgencia (evita rojo/naranja como color dominante en una app de organización cotidiana, donde el usuario la abre muchas veces al día). Verde para éxito/completado porque es la asociación más universal y no entra en conflicto con el índigo. Los grises son neutros puros (sin tinte de color) para no competir con el primario.

| Token | Light | Dark | Uso |
|---|---|---|---|
| `color-primary` | `#4F46E5` | `#A5B4FC` | Acciones principales, links, FAB (Android), foco |
| `color-primary-container` | `#E0E7FF` | `#312E81` | Fondo de elementos primarios de baja énfasis (chips, fondos de card seleccionada) |
| `color-on-primary` | `#FFFFFF` | `#1E1B4B` | Texto/ícono sobre `color-primary` |
| `color-success` | `#059669` | `#34D399` | Ícono/borde/fondo de estado "completado" — **no usar para texto** (3.77:1 en fondo blanco, insuficiente para texto normal AA) |
| `color-success-text` | `#047857` | `#34D399` | Texto del estado "completado" (5.48:1 en blanco) |
| `color-success-container` | `#D1FAE5` | `#064E3B` | Fondo de estado "completado" |
| `color-warning` | `#C2670A` | `#FBBF24` | Ícono/borde/fondo de estado "pendiente" — **no usar para texto** |
| `color-warning-text` | `#92400E` | `#FBBF24` | Texto del estado "pendiente" |
| `color-warning-container` | `#FEF3C7` | `#78350F` | Fondo de estado "pendiente" |
| `color-info` | `#2563EB` | `#60A5FA` | Ícono/borde/fondo de estado informativo ("Programada", categoría Documentos) |
| `color-info-text` | `#1D4ED8` | `#93C5FD` | Texto del estado informativo |
| `color-info-container` | `#DBEAFE` | `#1E3A8A` | Fondo de estado informativo |
| `color-error` | `#B91C1C` | `#FCA5A5` | Errores, acción destructiva (revocar, rechazar), texto sobre `color-error-container` |
| `color-error-container` | `#FEE2E2` | `#7F1D1D` | Fondo de mensajes de error |
| `color-surface` | `#FFFFFF` | `#101418` | Fondo de pantalla |
| `color-surface-variant` | `#F9FAFB` | `#171D22` | Fondo de card/superficie elevada |
| `color-border` | `#8B909A` | `#616B79` | Bordes, separadores — incluye el borde de `input` (Web), un componente interactivo real, no solo decorativo |
| `color-text` | `#111827` | `#F3F4F6` | Texto principal |
| `color-text-secondary` | `#6B7280` | `#9CA3AF` | Texto secundario (metadatos, timestamps) |

**Contraste — recalculado por completo el 2026-08-17 contra el código real ya restilizado por `UX-006` (fórmula WCAG 2.1 real, luminancia relativa — no una reverificación de los números antiguos), ver `accessibility.md` §1 para la tabla completa de pares y sus ratios exactos.** La pasada original (`UX-001`) solo había verificado la paleta clara base; nunca se había recalculado contraste contra los tokens de tema oscuro reales de `UX-006`, ni contra combinaciones texto/ícono-sobre-container que no existían todavía en `UX-001` (pills de estado, sidebar activo, badges de módulos mock). Recalculando sistemáticamente las ~20 combinaciones texto/ícono-sobre-fondo que el código realmente usa, se encontraron **6 fallos reales de AA** (4 en oscuro, 2 en claro) — corregidos aquí, no solo documentados:

| Par que fallaba | Antes | Ahora | Motivo |
|---|---|---|---|
| `color-info-text` sobre `color-info-container` (oscuro) | 4.07:1 | 5.74:1 | El valor anterior (`#60A5FA`) nunca se había verificado contra su propio container (`#1E3A8A`) — solo contra `color-surface`, que no es donde vive el texto de un `StatusPill`. |
| `color-error` sobre `color-error-container` (oscuro) | 3.62:1 | 5.28:1 | Mismo error de método: solo se había verificado `error` como texto suelto sobre `surface`, nunca como texto de pill/badge sobre su propio container. |
| `color-primary` sobre `color-primary-container` (oscuro) | 3.83:1 | 5.73:1 | Par nuevo introducido por `UX-006` (ítem activo del sidebar, chip seleccionado) — nunca existía en `UX-001`/`UX-004`, nunca se verificó. |
| `color-border` sobre `color-surface` (oscuro y claro) | 1.47:1 / 1.24:1 | 3.83:1 / 3.20:1 | Nunca se había verificado como componente gráfico real (SC 1.4.11) — es el borde real de los `input` de Web, no solo un divisor decorativo. |
| `color-warning` sobre `color-warning-container` (claro) | 2.86:1 | 3.60:1 | Rol gráfico (ícono), nunca antes verificado contra su propio container en modo claro. |
| `color-error` sobre `color-error-container` (claro) | 3.95:1 | 5.30:1 | Mismo gap de método que arriba, esta vez en modo claro. |

Efecto colateral positivo confirmado, no asumido: el `color-primary` más claro en modo oscuro también mejora el par botón (texto sobre fondo primario), de 5.36:1 a 8.02:1 — ningún par se degradó al corregir estos 6.

**Corrección UX-006 (2026-08-16) — `color-info` añadido, superficies oscuras oscurecidas, verificado con muestreo de píxel real, no a ojo.** Al construir la pantalla de referencia del dashboard (`docs/Ejemplo APK vida Cotidiana.png`/`docs/Ejemplo Web vida Cotidiana .png`) contra este documento aparecieron dos gaps reales:
1. Faltaba un cuarto rol semántico (`info`, azul) — necesario para el badge de categoría "Documentos" y el pill de estado "Programada" de la referencia, que no encajan en `success`/`warning`/`error`. Añadido siguiendo el mismo patrón base/text/container ya establecido, con el mismo umbral AA verificado por fórmula (no asumido): 6.70:1 en blanco, 7.28:1 sobre el nuevo fondo oscuro.
2. Los valores de `color-surface`/`color-surface-variant`/`color-border` en modo oscuro (`#16171D`/`#1F2028`/`#2E303A`) eran una estimación de la primera pasada de este documento, nunca verificada contra una referencia real. Muestreando la referencia Android a nivel de píxel (`PIL`/`getpixel`, promediando parches pequeños para evitar ruido de anti-aliasing) el fondo real es más cercano a casi-negro (`~#0F1516`) que a gris oscuro — se ajustaron los tres tokens a los valores de arriba. Los tokens de texto se re-verificaron contra el nuevo fondo, no se asumió que seguirían pasando: `F3F4F6` sobre `#101418` = 16.81:1, `9CA3AF` = 7.29:1 — ambos siguen pasando AA con margen amplio.

**Nota sobre el modo claro/oscuro:** ambos temas (light/dark) ya eran reales desde la primera versión de este documento — Android reacciona a `isSystemInDarkTheme()` y Web a `prefers-color-scheme`, ambos automáticos según la preferencia del sistema operativo del usuario. **No existe (ni se agregó en `UX-006`) un selector manual de tema dentro de la app** — es un `TBD/FUTURE`, no una omisión silenciosa: ningún requerimiento funcional lo pide hoy, y la referencia visual mezcla capturas claras y oscuras sin especificar un mecanismo de cambio explícito.

**Hallazgo real corregido en `UX-006`, Android — bug de modo oscuro pre-existente desde `UX-004`:** cuatro pantallas (`RemindersScreen.kt`, `ShareDialog.kt`, `InvitationsScreen.kt`, `NotificationsScreen.kt`) referenciaban directamente constantes `VidaColor.*Light` (p. ej. `VidaColor.SurfaceVariantLight`) en vez de leer a través del `MaterialTheme`/`ColorScheme` activo — el `Theme.kt` de `UX-004` sí construía un `ColorScheme` oscuro correcto, pero estas cuatro pantallas nunca lo consultaban, así que se veían con fondo/texto claros incluso con el sistema en modo oscuro. Corregido introduciendo `VidaColors`/`LocalVidaColors`/`VidaTheme.colors` (un `CompositionLocal` con los roles semánticos que no tienen equivalente directo en `ColorScheme` de Material 3 — success/warning/info) y reemplazando cada referencia `VidaColor.*Light` por su equivalente `VidaTheme.colors.*`, reactivo al tema. Web nunca tuvo este bug: las variables CSS (`var(--color-*)`) son intrínsecamente reactivas al tema por diseño del navegador — confirmado con `grep` de que ningún `.tsx` de Web tenía un hex hardcodeado.

## 2. Tipografía

**RECOMMENDATION.**

- **Web:** [Inter](https://rsms.me/inter/) vía `@fontsource/inter` (paquete npm con los archivos de fuente incluidos — auto-hospedado, no una llamada a Google Fonts CDN en cada carga, coherente con minimización de datos: no se envía la IP del usuario a un tercero solo por renderizar texto). Fallback: `system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif`.
- **Android:** Material 3 type scale con la fuente por defecto del sistema (Roboto) — Material 3 ya define una escala tipográfica completa y accesible (`displayLarge` … `labelSmall`); no se introduce una fuente custom para no añadir peso a la app ni pelear con el renderizado nativo de Android. Si más adelante se decide una fuente de marca, es un cambio aislado a `Type.kt`, no a esta escala.

| Rol | Tamaño / interlineado | Peso | Uso |
|---|---|---|---|
| `display` | 32/40 (Web) · `displaySmall` (Android) | 700 | Título de pantalla de login |
| `headline` | 24/32 · `headlineSmall` | 600 | Encabezado de sección ("Vida Cotidiana", "Notifications") |
| `title` | 18/26 · `titleMedium` | 600 | Título de la tarjeta de recordatorio |
| `body` | 16/24 · `bodyLarge` | 400 | Texto de contenido, listas |
| `body-small` | 14/20 · `bodyMedium` | 400 | Texto secundario, descripciones |
| `label` | 13/18 · `labelMedium` | 500 | Metadatos (timestamps, estado), texto de botón |

## 3. Espaciado — grid de 8pt

**RECOMMENDATION.** Escala de múltiplos de 8 (con un paso de 4 para ajustes finos), la convención más extendida en Android/Material y trasladable 1:1 a Web en `px` (a diferencia del color, aquí no hace falta ni justificar alternativas — es el estándar de facto).

| Token | Valor | Uso típico |
|---|---|---|
| `space-1` | 4dp/px | Separación entre ícono y texto adyacente |
| `space-2` | 8dp/px | Padding interno de chip/botón pequeño |
| `space-3` | 12dp/px | Padding interno de card |
| `space-4` | 16dp/px | Padding de pantalla, gap entre cards |
| `space-6` | 24dp/px | Separación entre secciones |
| `space-8` | 32dp/px | Separación mayor (header vs. contenido) |

## 4. Forma y elevación

**RECOMMENDATION.**

- Radio de esquina: `12dp/px` para cards, `8dp/px` para botones/inputs, `9999px` (pill) para chips de estado.
- Elevación: cards usan una sombra sutil (`0 1px 3px rgba(0,0,0,0.08)` en Web; `2.dp` `tonalElevation` en Android Material 3) — evita el efecto "papel flotando" de sombras fuertes, consistente con Material 3's preferencia por tonal elevation sobre shadow elevation.

## 5. Estados de componente (color + texto, nunca solo color)

**RECOMMENDATION**, detallado en `accessibility.md` §"No depender solo del color": todo estado (pendiente/completado/error) se comunica con texto o ícono además de color, para no depender del color como único canal.

| Estado | Color | Texto/ícono acompañante |
|---|---|---|
| Pendiente | `color-warning-container` (fondo) + `color-warning-text` (texto) | Texto "Pending" |
| Completado | `color-success-container` (fondo) + `color-success-text` (texto) | Texto "Completed" + ✓ |
| Error/rechazado | `color-error` | Texto explícito del error, `role="alert"` (Web) / `liveRegion` (Android) |
| Informativo/programado | `color-info-container` (fondo) + `color-info-text` (texto) | Texto explícito del estado ("Programada", nombre de categoría) |

## 6. Dónde viven los tokens en código

- Android: `app/src/main/java/com/vidacotidiana/app/core/ui/{Color,Type,Spacing,Theme}.kt`.
- Web: `web/src/index.css` (custom properties `--color-*`/`--space-*`) + `@fontsource/inter`.

Ningún token se duplica a mano en un componente — todo componente nuevo o restilizado referencia estos archivos, nunca un valor hex/dp suelto (ver `15-coding-standards.md`).

## 7. UX-006 — Librería de componentes de dashboard

**RECOMMENDATION.** `UX-001`..`UX-005` cubrieron una app de una sola columna (lista de recordatorios + diálogos). `UX-006` introdujo un segundo patrón de layout — dashboard con navegación persistente — a partir de dos imágenes de referencia (`docs/Ejemplo APK vida Cotidiana.png`, `docs/Ejemplo Web vida Cotidiana .png`), con estos componentes nuevos, reutilizables entre las pantallas reales y las de datos simulados:

| Componente | Qué es | Android | Web |
|---|---|---|---|
| Metric card | Badge de ícono + número grande + etiqueta + subtítulo (opcionalmente con tono de alerta) | `core/ui/components/MetricCard.kt` | `core/ui/components/MetricCard.tsx` |
| List section card | Card con encabezado + "Ver todas" opcional + contenido | `ListSectionCard.kt` | `ListSectionCard.tsx` |
| List item row | Ícono circular con tono + título + subtítulo + pill de estado opcional | `ListItemRow.kt` | `ListItemRow.tsx` |
| Status pill | Texto + color, nunca solo color (§5) | `StatusPill.kt` | clases `.badge`/`.badge-*` (`index.css`) |
| Filter chip | Chip seleccionable, usado en Inventario | `VidaFilterChip.kt` | `FilterChip.tsx` |
| Donut chart | Solo con datos reales — nunca alimentado con Finanzas | `DonutChart.kt` (`Canvas`/`drawArc`) | `DonutChart.tsx` (SVG `stroke-dasharray`) |
| Navegación persistente | 5 slots (Inicio/Tareas/+/Compartidos/Más), patrones nativos (`Scaffold`+`NavigationBar`) | `VidaBottomNav.kt` | Sidebar de 232px, colapsa a franja horizontal <900px (`core/ui/layout/AppShell.tsx`) |
| Encabezado de pantalla | Saludo + avatar (Android); saludo + búsqueda decorativa + campana + botón "Nuevo" (Web) | `AppTopBar.kt` | integrado en `AppShell.tsx` |
| Calendar view (`UX-007`) | Grid de mes con navegación prev/siguiente + hasta 3 marcadores de color por día — puramente presentacional, el llamador decide qué significa cada marcador | `core/ui/components/CalendarView.kt` | `core/ui/components/CalendarView.tsx` |
| Fondo de agenda (`UX-007`) | Textura de líneas de libreta, opacidad baja, solo Home/Calendario | `core/ui/components/NotebookBackground.kt` (`Modifier.notebookBackground`) | clase `.notebook-bg` (`index.css`) |

Ningún componente nuevo introduce un color/espaciado que no exista ya en §1/§3 — `BadgeTone`/`Tone` (`primary`/`success`/`warning`/`info`/`error`) es solo un nombre para los pares container/texto que ya existían.

**Decisión de alcance (Sección 1 de la tarea, no reabierta aquí):** de los ítems del sidebar de la referencia, **Finanzas** e **"IA Asistente" quedan completamente fuera** — ninguna pantalla, ruta, ítem de menú ni mención "Próximamente" existe para ninguno de los dos, por restricción explícita de `CLAUDE.md`. **Configuración** tampoco se implementó — no hay pantalla de ajustes definida ni en alcance para esta tarea, y añadirla solo por completar visualmente el sidebar habría sido inventar una funcionalidad no pedida. **Compartidos** no aparece en el sidebar de la referencia (está implícito en el mockup del teléfono) — se agregó igualmente como ítem de navegación real (justo después de "Tareas" en Web, en la barra inferior en Android) porque es un módulo real que necesita un punto de entrada; documentado como `ASSUMPTION`, no como fidelidad literal a la imagen.

**Búsqueda (Web):** el ícono de lupa del encabezado es puramente decorativo (`aria-hidden`, no interactivo) — no existe búsqueda del lado del servidor para ningún recurso de V1 hoy. Implementar un campo de texto funcional habría implicado prometer una función que no existe; se documenta aquí como `FUTURE` en vez de construirse a medias.

## 8. UX-007 — Home como "Agenda", fondo de libreta, MetricCard compacto, Calendario

**RECOMMENDATION.** Pasada de diseño (2026-08-17) sobre la interfaz ya construida por `UX-001`..`UX-006` — sin tocar lógica/flujos que ya funcionaban, salvo lo estrictamente necesario para la pantalla de Calendario nueva. Antes/después real verificado con captura de pantalla (Playwright Web) y con el dispositivo físico Android — ver `01-technical-backlog.md` (`UX-007`) para el detalle completo de la verificación.

**Home: de "dashboard genérico" a "Agenda".** Reordenado (Android `HomeScreen.kt`, Web `HomePage.tsx`) para que la jerarquía sea "qué necesito ver/hacer hoy" primero, métricas después — antes el orden era Métricas → Próximas tareas → Recordatorios importantes (vencidos) → Progreso; ahora es Hoy (vencidos) → Próximos días (próximos) → Métricas (`MetricCard`, ya compactas) → Progreso. Es un reorder/rename puro sobre datos que `HomeViewModel`/`useHomeData` ya calculaban — ningún campo nuevo, ninguna llamada nueva. Subtítulo cambiado a "Tu agenda de hoy" (antes "Resumen de tu día"/"Aquí tienes un resumen de tu día"). Se agregó un estado vacío ("Estás al día...") cuando no hay nada vencido ni próximo, para que una agenda sin pendientes se sienta como un resultado real, no como una sección que desapareció.

**Fondo — textura de libreta, no una fotografía.** Mismo límite de costo/licencia que ya aplicaba al tema de login (`08c-web-architecture.md`): nada de fotografías ni imágenes generadas por IA. Líneas horizontales finas, opacidad muy baja (`color-mix(... 20%, transparent)` sobre `color-border` en Web; `Color.copy(alpha = 0.2f)` en Android), dibujadas detrás del contenido — nunca compiten con él. Aplicado únicamente a Home y Calendario (las dos pantallas "de agenda"), no a toda la app, para minimizar superficie de regresión visual sobre pantallas ya verificadas.
- Web: utilidad `.notebook-bg` en `index.css`, aplicada como clase adicional en el wrapper de cada página.
- Android: `Modifier.notebookBackground(lineColor, spacing)` en `core/ui/components/NotebookBackground.kt`, un `drawBehind` reutilizable.

**MetricCard — rediseño compacto, no solo texto más chico.** Antes: 3 filas apiladas (etiqueta+ícono, valor, subtítulo), padding `space-4`/`16dp`, ícono 36px/dp. Ahora: 1 fila (ícono 32px/dp + valor/etiqueta agrupados verticalmente junto al ícono + subtítulo como tag al final), padding `space-3`/`12dp`. Es un componente compartido (`core/ui/components/MetricCard.tsx`/`.kt`) — el cambio se propagó automáticamente a todo lo que ya lo usaba (Home en ambas plataformas; ningún otro módulo lo usaba a la fecha de esta pasada).

**Calendario — pantalla nueva, componente `CalendarView` reutilizable.** Vista de mes con marcadores por día + una lista "Pendientes" con checkbox. Ubicación en la navegación (decisión de diseño explícita, no una limitación técnica): en Web es un ítem de primer nivel del sidebar (`/calendar`, sin restricción de ancho); en Android vive dentro de "Más" en vez de un 6º ícono en la barra inferior — la barra ya tiene historial real de overflow con solo 4 íconos + FAB central (`AND-004`/`AND-006`, botones comprimidos a ancho cero), así que se prefirió no repetir ese riesgo por un destino de uso secundario/ocasional.

| Fuente | Real/Mock | Se marca en el grid | Completar desde Calendario |
|---|---|---|---|
| Tareas (`GET /reminders`, con `dueAt`) | Real | Sí, tono `primary` | Llama al mismo `POST /reminders/{id}/complete` real que `RemindersScreen`/`RemindersPage` — reutilizado, no reimplementado |
| Invitaciones pendientes (`GET /me/invitations`) | Real | No (solo lista, no tienen una fecha de "vencimiento de tarea" relevante para el grid) | N/A — accept/reject sigue solo en `InvitationsScreen`/`InvitationsPage`, no duplicado aquí |
| Garantías (`MockData`/`mockData.ts`) | **Mock** | Sí, tono `warning`, pill "Simulado" | **Estado local** (`remember`/`useState`) — se pierde al recargar/recrear la pantalla, nunca se envía a ningún backend |
| Mantenimiento (`MockData`/`mockData.ts`) | **Mock** | Sí, tono `info`, pill "Simulado" | Igual que Garantías — local-only |

Los ítems mock nunca se ven indistinguibles de uno real: pill "Simulado" adicional + texto explícito "dato simulado" en el subtítulo + ícono/tono distinto (Garantías = escudo/warning, Mantenimiento = llave/info, Tareas reales = documento/primary), verificado visualmente en ambas plataformas (capturas reales, ver `01-technical-backlog.md`).

Android's `MockWarranty`/`MockMaintenanceRecord` no tenían una fecha estructurada (solo `expiresLabel`/`nextDueLabel`, strings ya formateados para mostrar) — se agregaron `expiresOn`/`nextDueOn` (`java.time.LocalDate`) con los mismos valores que el label ya describía, no fechas nuevas/inventadas. Web's `mockData.ts` ya tenía `expiresAt`/`nextDueAt` estructurados, se usaron tal cual.

Cero backend nuevo: sin migraciones, sin endpoints, sin tablas para Calendario/Garantías/Mantenimiento. Ver `05-v2-plan.md` para lo que se identificó como trabajo de backend real a futuro si estos módulos dejan de ser mock.

**Landing post-login — la misma en ambas plataformas, corregido tras verificar en dispositivo físico (2026-08-17):** el borrador original de esta nota decía que Android aterrizaba en `Routes.HOME` tras login (mientras Web se quedaba en Reminders por sus e2e reales) — esa decisión resultó estar mal: los 3 tests instrumentados reales de Android (`LoginAndRemindersFlowTest`/`SharingFlowTest`/`NotificationsFlowTest`) dependen exactamente del mismo patrón que los de Web (`reminder_title_input`/`add_reminder_button` alcanzables justo tras el login), y aterrizar en Home los rompía — encontrado de verdad corriendo los tests en el teléfono físico, no anticipado por revisión de código. Corregido: Android también navega a `Routes.REMINDERS` tras el login (`NavGraph.kt`). "Inicio" queda a una pestaña de distancia en la barra inferior — no es un peor punto de aterrizaje que Tareas, ambas son pestañas de primer nivel.
