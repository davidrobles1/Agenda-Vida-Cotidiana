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

## 9. UX-008 — Identidad "Agenda" generalizada a toda la Web (paleta, tipografía, Keycloak)

**DECISION (usuario, 2026-08-17).** El rediseño de `LoginPage.tsx` (beige cálido + azul elegante + acento terracota + tipografía serif/script) se generalizó a **toda la plataforma Web** — reemplaza la paleta índigo de `UX-001`/`UX-006` como identidad visual activa. **Alcance explícito de esta pasada: solo Web.** Android/iOS no se tocaron — se homologarán después, como tarea aparte.

Antes de tocar cualquier archivo se hizo un inventario real del sistema existente (no se asumió la estructura): **15 de 16 archivos `.module.css` ya consumían exclusivamente `var(--color-*)` de `index.css`** — cero hex propios. El único outlier era `LoginPage.module.css`, con su paleta hardcodeada completa. Eso redujo esta tarea a (1) recalcular los valores de los tokens ya existentes, (2) migrar ese único archivo para que también los consuma, y (3) dos ajustes puntuales de jerarquía visual (ver abajo) — no una reescritura.

**Psicología del color, validada antes de aplicarla (no asumida):** azul se mantiene como el único color con permiso de decir "toca aquí" (mismo rol que ya tenía el índigo — no hay ruptura de expectativa de uso). Beige queda solo como fondo de página, nunca de tarjeta (evita que sesiones largas de uso diario se sientan "pesadas" — la tarjeta más clara es la que hace respirar al ojo). **Terracota es deliberadamente un acento raro** (destacados/badges "featured"), no un segundo color de acción — dos acentos saturados compitiendo en la misma jerarquía habría sido exactamente el problema que esta pasada buscaba evitar.

### Paleta — recalculada, no reutilizada a ojo

Cada valor verificado con la fórmula WCAG 2.1 real (luminancia relativa) contra la superficie donde realmente renderiza, mismo método que `ACC-001`:

| Token (`index.css`, mismo nombre de siempre) | Valor | Uso | Contraste real |
|---|---|---|---|
| `--color-primary` | `#2C5F8C` | Acción primaria, enlaces, foco, nav activo | 6.73:1 (texto blanco encima) |
| `--color-primary-container` | `#E4EEF6` | Fondo de chip/ítem activo | `--color-primary` sobre este: 5.72:1 |
| `--color-success` / `-text` | `#4D6B46` | Completado — un solo valor cubre ícono y texto (ya pasa AA como texto) | 4.87:1 sobre `-container` |
| `--color-warning` / `-text` | `#8A5A12` | Pendiente/por vencer | 4.70:1 sobre `-container` |
| `--color-info` / `-text` | `#2F5F66` | Informativo — deliberadamente **no** reutiliza `--color-primary`: azul implica "es clicable", info no lo es | 5.80:1 sobre `-container` |
| `--color-error` | `#B91C1C` | Sin cambio — ya verificado en `ACC-001`, el rojo se mantiene reconocible sin importar la calidez de la paleta alrededor | 5.30:1 sobre `-container` |
| `--color-terracotta` | `#B47B48` | Solo decorativo/gráfico (ilustración, tipografía grande) — **no** válido como texto normal (3.35:1) | — |
| `--color-terracotta-text` | `#8B5E36` | Terracota cuando sí es texto/ícono real (badge "destacado") | 4.84:1 |
| `--color-surface` | `#F5EEDF` | Fondo de página (`body`, `.content` de `AppShell`) | — |
| `--color-surface-variant` | `#FFFCF6` | Tarjetas — más clara que `--color-surface` a propósito, para que la tarjeta "flote" sobre la página (antes eran casi el mismo tono) | — |
| `--color-border` | `#6E8192` | Borde funcional real | 3.48:1 sobre `--color-surface` (el peor caso — pasa incluso ahí) |
| `--color-text` | `#2C425B` | Texto principal | 8.92:1 sobre `--color-surface` |
| `--color-text-secondary` | `#556479` | Texto secundario | 5.22:1 sobre `--color-surface` (el peor caso) |

**Corrección real de accesibilidad, no solo estética:** al migrar `LoginPage.module.css` a estos tokens, dos de sus colores originales quedaron reemplazados automáticamente — `.welcome p` (`#64768f`, 4.34:1, fallaba AA de texto normal) y `.footerText` (`#7488a2`, 3.40:1, fallaba con más margen) ahora usan `--color-text-secondary` (verificado ≥4.5:1). No fue una pasada de accesibilidad separada — fue un efecto directo de centralizar en tokens ya auditados.

**Inversión de jerarquía, dos ajustes puntuales:** como `--color-surface` (página) pasó a ser más oscuro que `--color-surface-variant` (tarjeta) — al revés que antes, cuando ambos eran casi blancos — dos usos que tomaban `--color-surface` por ser "el tono neutro disponible" (no porque fueran literalmente "la página") se movieron a `--color-surface-variant` para no perder definición de borde: el `input` global (`index.css`) y `FilterChip`. Documentado inline en ambos archivos, no un cambio silencioso.

### Tipografía — Fraunces reemplaza 'Brush Script MT'

**Hallazgo real, no cosmético:** `'Brush Script MT'`/`'Segoe Script'` (el acento cursivo del diseño original de `LoginPage.tsx`) son fuentes de Windows — no existen en macOS/Linux/Android/iOS, donde el navegador cae al genérico `cursive` (resultado impredecible por sistema/navegador). Para un elemento de marca repetido en cada pantalla, eso no es un detalle menor.

Corregido auto-hospedando **Fraunces** vía `@fontsource/fraunces` (`main.tsx`, pesos 500/600 + itálicas) — mismo patrón y misma justificación de minimización de datos que ya se usó para Inter (`design-system.md` §2, no una dependencia nueva en espíritu). `--font-serif` (`index.css`) para encabezados (incluido el `h1` global de toda la app); la itálica real de Fraunces reemplaza el script — mismo carácter cálido-editorial, renderiza igual en cualquier sistema. Inter se mantiene para cuerpo/UI — separar "marca" (serif) de "interfaz" (sans) es la jerarquía que evita que un formulario largo se sienta fuera de lugar.

### Modo oscuro — pausado, no perdido

**DECISION (usuario):** solo modo claro por ahora. La app tenía un modo oscuro real y completo (paleta índigo, `UX-006`); se removió el bloque `@media (prefers-color-scheme: dark)` de `index.css` (`color-scheme: light`, no `light dark`) en vez de dejarlo con la paleta vieja mezclada con la nueva — una mezcla índigo-oscuro + beige-claro se habría visto como un bug, no como una decisión. Diseñar una variante oscura cálida coherente con esta identidad es del mismo tamaño de esfuerzo que el modo claro — queda como **TBD** en `05-v2-plan.md`, tarea aparte, no a medio hacer aquí.

### Sidebar/header — neutro, no cálido

**DECISION (usuario):** `AppShell`'s `.sidebar`/`.topBar` usan `--color-surface-variant` (casi blanco) explícitamente, mientras `.content` hereda el `--color-surface` (beige) de `.shell` sin cambios — el chrome de navegación no compite visualmente con el contenido; el beige queda reservado al lienzo de trabajo.

### Qué no se tocó en esta pasada

El tema de Keycloak (`infra/keycloak/themes/vida-cotidiana-web/`, homologado visualmente en una tarea previa el mismo día) usa sus propios valores hex — no se resincronizó con esta segunda ronda de correcciones de contraste porque el pedido de esta tarea fue específicamente "centralizar en un archivo y que lo reutilicen los componentes" de la app React, no el theme de Keycloak (server-rendered, fuera del build de Vite). Sus valores ya pasaban AA de forma independiente cuando se verificaron. Sincronizarlo exactamente con esta paleta final es un ajuste menor pendiente, no urgente.

## 10. UX-011 — React Aria Components + Motion (Fase 0 + Fase 1: Calendario)

**DECISION (usuario, 2026-08-17).** Tras dos rondas de evaluación explícita (ver `05-v2-plan.md`), se adoptó **React Aria Components** (primitivos headless/accesibles) + **Motion** (antes Framer Motion) como base de interacción/microinteracción de toda la Web — ambos MIT/Apache-2.0, sin capa de pago. Regla no negociable del usuario: **"no animaciones por animar"** — cada animación debe orientar, comunicar cambio de estado o dar continuidad visual, nunca decorar. **Alcance de esta pasada: solo Calendario (Fase 1), solo Web.** El resto de la app y otros módulos (ShareDialog, etc.) quedan para fases siguientes, módulo por módulo, según lo pedido explícitamente.

**Por qué Calendario primero:** validado contra un gap real, no una preferencia estética — el grid de días anterior (botones HTML sueltos, sin relación de grid entre sí) no tenía navegación real por teclado entre celdas (flechas, Home/End, PageUp/PageDown). El `<Calendar>` de React Aria implementa el patrón ARIA grid completo de fábrica; verificado con un test e2e real (`e2e/calendar.spec.ts` → *"arrow keys move focus between day cells in the accessible grid"*) que ArrowRight/ArrowDown mueven el foco a la celda correcta, algo que antes no existía.

**Cero cambio de apariencia impuesto por las librerías:** React Aria es headless (sin CSS propio) y Motion solo anima propiedades que el propio `CalendarView.module.css` ya definía — la paleta, tipografía y forma de `UX-008` quedan intactas. La única diferencia estructural real: `<CalendarGrid>` renderiza una `<table>` (patrón ARIA grid accesible: `<thead>/<tbody>/<tr>/<td>`) en vez del `display:grid` anterior — el elemento interactivo real es un `<div>` con `tabindex` dentro de cada `<td>` (no el `<td>` mismo), y todos los estados visuales (hoy/seleccionado/hover/fuera-de-mes/foco) se manejan vía atributos `[data-*]` que React Aria expone, no clases calculadas a mano.

**Corrección real, no buscada:** el encabezado de días de la semana usaba un array fijo `['L','M','M','J','V','S','D']` con una ambigüedad real (martes y miércoles ambos "M"). `weekdayStyle="narrow"` de React Aria usa los datos CLDR reales de `es-ES`, que sí distinguen miércoles como "X" — la ambigüedad desapareció como efecto de usar datos reales de internacionalización en vez de un array hardcodeado.

### Motion Design System — categorías nombradas (`web/src/core/motion/tokens.ts`)

Para que la animación no quede a criterio ad-hoc de cada desarrollador, per pedido explícito del usuario:

| Token | Transición | Uso real (no especulativo) |
|---|---|---|
| `quick` | 150ms ease-out | Tap/press, checkbox |
| `base` | spring 380/32 | Entrada/salida de contenido — panel de día en Calendario |
| `smooth` | spring 300/30 | Apertura/cierre de diálogos/popovers (fases siguientes) |
| `spatial` | spring 260/28 (la más larga a propósito) | Cambio de mes en Calendario — debe sentirse como desplazamiento real, no un fade genérico |
| `feedbackSuccess` | spring 500/25 | Confirmación de tarea completada (fases siguientes) |
| `feedbackError` | 400ms shake, no-spring | Error de validación (fases siguientes) |

Reglas fijas (aplicadas en cada punto de uso, no en los tipos): hover/foco se quedan en CSS puro, nunca Motion; una salida siempre es más rápida/rígida que su entrada correspondiente; `prefers-reduced-motion` se resuelve una sola vez a nivel global — `<MotionConfig reducedMotion="user">` envuelve `<App />` en `main.tsx` — ningún componente lo comprueba individualmente.

### Qué se implementó en Calendario (Fase 1)

- **Navegación real por teclado** entre celdas del grid (el gap que justificó empezar aquí).
- **Transición espacial de mes**, consciente de dirección (izquierda/derecha según se avance o retroceda), disparada por los botones, por teclado (cruce de mes en los bordes del grid) y por **gesto de swipe** en móvil (`drag="x"` sobre el grid, con umbral de 60px) — los tres caminos usan el mismo estado de foco controlado, no lógica duplicada.
- **Continuidad día → panel**: al seleccionar un día distinto, el contenido del panel de detalle (título + items o estado vacío) hace un fade+shift sutil (`base`) en vez de un salto brusco — el formulario de alta rápida se queda fuera de esa animación a propósito (no es "contenido del día", es una acción siempre presente).
- Botón "Hoy" y navegación prev/next simplificados: React Aria maneja el estado de mes/foco internamente (controlado vía `focusedValue`/`onFocusChange`), así que `CalendarPage.tsx` ya no necesita `displayedMonth`/`shiftMonth`/`goToday` propios — menos estado duplicado, no una regresión de funcionalidad.

### Verificación real

`npx tsc --noEmit`, `npm run build`, `npm run lint` limpios. Suite completa de Playwright (10/10, `--reporter=line`, 1 worker) pasando, incluyendo el nuevo test de navegación por teclado. Capturas reales en `Documentacion/02-ux-ui/screenshots/` (`web-calendar-ux011-*`).

### Fase 2 — ShareDialog: de panel inline a Popover real

**Análisis real antes de tocar código:** pese a llamarse "ShareDialog", el componente (`src/features/sharing/ShareDialog.tsx`) no era un diálogo real — era un `<div>` que se insertaba en el flujo de la lista al hacer clic en "Share" (sin overlay, sin backdrop). Inspeccionado el código antes de decidir cómo migrarlo (regla del usuario: "no migrar porque sí"), se identificaron **3 gaps reales, no cosméticos**: (1) sin focus trap — el foco no entraba al panel al abrirlo; (2) sin cierre con Escape; (3) sin semántica `role="dialog"` real ni retorno de foco al botón "Share" al cerrar. Estos tres son exactamente el tipo de "complejidad de interacción difícil de construir a mano correctamente" que el usuario definió como criterio válido para usar React Aria.

**Decisión de diseño explícita, no heredada del plan genérico de Fase 2:** el plan original decía "Dialog/Modal", pero el comportamiento real existente era de panel flotante/desplegable, no de modal bloqueante con backdrop — convertirlo en un modal centrado habría sido un cambio visual/de interacción mayor al pedido. Se usó en su lugar `DialogTrigger` + `Popover` (no `Modal`/`ModalOverlay`) — no bloqueante, anclado al botón "Share", con la misma apariencia de panel flotante que ya tenía, ahora con semántica y comportamiento de diálogo reales.

**Integración real Motion + React Aria, verificada, no asumida:** React Aria detecta el fin de una animación de salida vía `Element.getAnimations()` sobre el nodo DOM real del `Popover` — una API estándar del navegador que también detecta animaciones iniciadas por Motion (usa Web Animations API de forma nativa para propiedades `transform`/`opacity`, exactamente las que se animan aquí). Se usó `motion.create(Popover)` para que Motion anime el nodo del propio `Popover` (no un `motion.div` anidado, que `getAnimations()` no vería) — sin `AnimatePresence`, el `animate` de Motion simplemente apunta al estado `isOpen` controlado por la propia app, y React Aria decide cuándo desmontar de verdad una vez que esa animación real termina. **Verificado, no solo razonado:** un test real confirma que tras cerrar (Escape/botón/clic afuera) el panel desaparece por completo del DOM, sin quedar "atascado" a medio cerrar.

**Simplificación de estado como efecto secundario correcto, no un objetivo en sí:** `RemindersPage.tsx` tenía `sharingReminderId`/`onShareToggle` para coordinar qué tarjeta mostraba su panel — ya no hace falta, cada `ShareDialog` es autónomo (`DialogTrigger` controlado con su propio `isOpen`), así que ese estado se eliminó sin pérdida de funcionalidad, igual que `displayedMonth`/`shiftMonth`/`goToday` en Calendario (Fase 1).

**Mejora real de usabilidad, encontrada haciendo este trabajo, no buscada aparte:** el panel anterior no tenía botón de cierre — la única forma de cerrarlo era volver a pulsar "Share". Se agregó un botón "×" real (usa el `close()` que `Dialog` expone a sus hijos), consistente con lo que cualquier diálogo/popover necesita y ahora sí tiene.

**Reposicionamiento automático, gratis por usar `Popover` real:** al flotar sobre el contenido (antes vivía en el flujo normal de la lista), React Aria calcula colisión con el viewport y cambia de "abajo" a "arriba" del botón cuando no hay espacio — confirmado visualmente en la captura real (`web-sharedialog-ux011-open-after.png`, el panel del primer recordatorio de la lista se abre hacia arriba porque no cabía hacia abajo).

**1 ajuste real de test, no un bug de producto:** al portar a un `Popover` real (que se renderiza en un portal a `<body>`, comportamiento correcto — evita que quede recortado por el `overflow` de algún contenedor ancestro), el diálogo dejó de estar dentro del `<li>` de la fila en el DOM. `sharing.spec.ts` buscaba el diálogo con `row.locator(...)`; corregido a `page.locator(...)` (solo puede haber un panel abierto a la vez, sin ambigüedad).

**Verificado real:** `npx tsc --noEmit`, `npm run build`, `npm run lint` limpios. Suite completa de Playwright **10/10 en verde**, más un test nuevo y permanente (`sharing.spec.ts`, "share popover: focus moves in on open, Escape closes it, focus returns to the trigger") que confirma los 3 gaps reales quedaron cerrados — no solo que el código compila. Captura real: `Documentacion/02-ux-ui/screenshots/web-sharedialog-ux011-open-after.png`.

### Fase 3 — Filtro de categoría en Inventario: de botones sueltos a `ToggleButtonGroup` real

**Análisis real, mismo método que las fases anteriores:** un subagente exploró todo `src/` buscando otros candidatos reales (no solo Inventario) antes de decidir — resultado honesto: el resto de la superficie interactiva de la app (Documentos/Familia/Mantenimiento/Suscripciones/Garantías) son listas mock sin interacción real, y todos los botones existentes ya son `<button>` reales con foco/teclado correctos. El único gap real y citable encontrado: `FilterChip` (`core/ui/components/FilterChip.tsx`) — N botones `aria-pressed` independientes dentro de un `<div>` sin rol de grupo ni navegación por flechas entre ellos.

**Resultado real, mejor que lo asumido inicialmente:** `ToggleButtonGroup` con `selectionMode="single"` no renderiza `role="group"`/`role="button"` (lo que se asumió al planear) sino `role="radiogroup"`/`role="radio"` — semánticamente más preciso para "exactamente una opción entre N mutuamente excluyentes", confirmado real revisando el DOM, no supuesto de la documentación. `disallowEmptySelection` porque "Todos" es en sí mismo siempre una opción seleccionable, nunca un estado de "nada elegido".

**Único consumidor, blast radius mínimo:** `FilterChip` solo se usaba en `InventoryPage.tsx` (confirmado por grep antes de tocarlo) — cero riesgo de romper otro módulo.

**Verificado real:** `tsc`/`build`/`lint` limpios; test nuevo y permanente (`e2e/inventory.spec.ts`) confirma que el filtrado real sigue funcionando (mismos datos mock de siempre) y que la navegación por flechas entre chips —el gap concreto— ahora existe. Captura real: `Documentacion/02-ux-ui/screenshots/web-inventory-ux011-filter-after.png` (idéntica visualmente a antes — cero cambio de apariencia).

### Fase 4 — Confirmación real antes de revocar acceso de un colaborador activo

**Alcance deliberadamente angosto, decisión explícita:** el barrido de Fase 3 no encontró ningún otro componente hecho a mano con semántica rota que migrar — el único gap real restante identificado fue de otro tipo: acciones destructivas sin ningún paso de confirmación (`ShareDialog.tsx`: Revoke/Cancel; `InvitationsPage.tsx`: Accept/Reject). Se decidió **no** agregar confirmación a las cuatro por igual — eso habría sido inventar fricción no pedida en toda la app. Se agregó **solo** para "Revoke" (`ShareDialog.tsx`) — la única con una consecuencia real e inmediata sobre el acceso de *otra persona*; "Cancel" (invitación aún pendiente) y Accept/Reject quedan de un solo clic a propósito, documentado inline en el código.

**`Modal` (bloqueante), no `Popover` — decisión de diseño correcta para este caso, a diferencia de Fase 2:** una confirmación debe interrumpir; el panel de compartir no debía. `role="alertdialog"` real.

**Bug real encontrado y corregido durante la verificación visual, no solo funcional:** el patrón `motion.create(Modal)` (igual al usado con éxito en `Popover`, Fase 2) dejaba el diálogo de confirmación **funcionalmente interactivo pero visualmente invisible** — atascado en sus valores `initial` (`opacity:0`, `scale:0.95`) para siempre. Un test que solo revisara presencia en el DOM (`toBeVisible()` de Playwright no chequea `opacity`) lo habría dado por bueno — se detectó únicamente **inspeccionando el estilo computado real** (`getComputedStyle`) tras el "cierre exitoso" de un test funcional que sí pasaba. Causa real: a diferencia de `Popover`, `Modal` de React Aria trackea animaciones de entrada/salida por separado sobre dos refs distintos (backdrop y contenido) y su propio ciclo de re-render interno pisaba el estilo en línea que Motion acababa de aplicar. **Corrección:** animar el `Dialog` interno (`motion.create(Dialog)`) en vez de `Modal` — `Dialog` no tiene esa maquinaria de animación propia compitiendo, es un objetivo seguro para Motion; el backdrop (`Modal`) se dejó sin animar (aparece/desaparece al instante, aceptable — lo que necesitaba leerse como intencional era el panel, no el fondo). **Segundo bug real, mismo origen (verificación visual, no solo funcional):** el backdrop se renderizaba *detrás* del `Popover` de `ShareDialog` (`z-index:100000`, real, puesto por React Aria) porque el `z-index` propio era solo `100` — subido a `100001`, con el valor real de React Aria documentado inline como referencia, no un número arbitrario.

**Verificación real de la corrección, no solo re-ejecutar el test que ya pasaba:** se comprobó `getComputedStyle(...).opacity` real (>0.9 tras asentarse) y que el panel es realmente el elemento más al frente en su propia posición (`document.elementFromPoint`) — no solo que el rol ARIA está presente. Flujo completo real contra el backend real: crear recordatorio → invitar → (aceptar simulado a nivel de BD con las mismas dos escrituras que hace el endpoint real de aceptar, documentado en el test — la contraseña real de Keycloak de `userb` no se conoce en este entorno y no se intentó adivinar para no disparar la protección real de fuerza bruta) → Cancelar dentro del diálogo deja el share intacto → confirmar Revoke sí llama al backend real y la fila desaparece, confirmado además consultando Postgres directamente (`status = 'REVOKED'`).

**Sin test automatizado permanente para este flujo específico, documentado como limitación real, no ocultado:** verificar el camino completo requiere un colaborador con estado `ACTIVE` real, que solo se logra aceptando una invitación — no hay forma de automatizarlo sin la contraseña real de `userb` o un mecanismo de datos de prueba con fixtures (no existe todavía). Verificado real y a fondo en esta sesión (arriba); un test end-to-end permanente para este camino específico queda como TBD en `05-v2-plan.md`.

**Verificado real:** `tsc`/`build`/`lint` limpios; suite completa de Playwright reverificada en verde tras ambas correcciones. Captura real (post-corrección, diálogo genuinamente visible por encima del panel): `Documentacion/02-ux-ui/screenshots/web-sharedialog-ux011-revoke-confirm-after.png`.

## 11. UX-012 — Navegación Personal/Laboral (ADR-015): paleta Laboral

**DECISION (Product Owner, ADR-015).** El modo Laboral necesita distinguirse visualmente del modo Personal (que reutiliza la paleta cálida/terracota existente de `UX-008` sin cambios) — el Product Owner referenció un navy `#1E3F5C` como base, más un acento verde "foco" profesional. Los tokens semánticos (`success`/`warning`/`info`/`error`) se quedan compartidos entre ambos modos a propósito: una tarea completada es verde sin importar en qué modo se creó — solo los roles "de marca" (`primary`/`surface`/`text`/`border`) tienen versión Laboral propia.

### Paleta — `--color-laboral-*` (`web/src/index.css`)

| Token | Valor | Uso | Contraste real (WCAG 2.1, mismo método de luminancia relativa que §9/ACC-001) |
|---|---|---|---|
| `--color-laboral-primary` | `#1E3F5C` | Acción primaria, nav activo, foco | `--color-laboral-on-primary` encima: **10.93:1** |
| `--color-laboral-primary-container` | `#DCE6EC` | Fondo de chip/ítem activo de baja énfasis | `--color-laboral-primary` encima: **8.63:1** |
| `--color-laboral-on-primary` | `#FFFFFF` | Texto/ícono sobre `-primary` | (ver arriba) |
| `--color-laboral-accent` | `#2F6B4F` | Acento "foco" — anillo de color por origen de modo en el Calendario general (`FR-017`), no reemplaza ningún estado semántico | Sobre `-accent-container`: **5.07:1**; sobre `-surface`: **5.43:1** |
| `--color-laboral-accent-container` | `#DCEAE2` | Fondo de baja énfasis para el acento | (ver arriba) |
| `--color-laboral-surface` | `#EAEFF2` | Fondo de página/sidebar en modo Laboral | `--color-laboral-text` encima: **11.53:1** |
| `--color-laboral-surface-variant` | `#F7FAFB` | Fondo de card/superficie elevada | `--color-laboral-text` encima: **12.73:1** |
| `--color-laboral-border` | `#5B6B78` | Bordes/divisores | Sobre `-surface`: **4.75:1** (pasa como texto — usado también en iconografía fina) |
| `--color-laboral-text` | `#20313F` | Texto principal | (ver arriba) |
| `--color-laboral-text-secondary` | `#4C5C68` | Texto secundario/metadatos | Sobre `-surface`: **5.97:1** |

Los 8 pares con implicación de texto/AT pasan **4.5:1** (texto normal); todos superan cómodamente 3:1 (texto grande/UI no textual). Ningún par se dejó sin verificar antes de fijar el valor final — a diferencia del hallazgo real de `UX-002` (10 pares nunca verificados antes de esa auditoría), este juego de tokens nació ya verificado.

### Mecanismo de aplicación — remapeo de custom properties, no CSS por componente

`.laboral-theme` (`index.css`, aplicada a la raíz `.shell` de `AppShell` solo cuando `activeMode === 'LABORAL'`, ver `AppShell.tsx`) redefine los **mismos nombres genéricos** (`--color-primary`, `--color-primary-container`, `--color-on-primary`, `--color-surface`, `--color-surface-variant`, `--color-border`, `--color-text`, `--color-text-secondary`) para apuntar al juego `--color-laboral-*`. Como cada `.module.css` de la app ya lee exclusivamente `var(--color-*)` (inventario confirmado en `§9`), todo componente descendiente (cards, botones, inputs, nav, checkboxes) se re-tematiza automáticamente sin ningún cambio a nivel de componente — el mismo patrón, aplicado a un caso de "modo" en vez de "tema claro/oscuro".

**Bug real encontrado y corregido durante la integración de este bloque a un solo árbol** (no en el desarrollo aislado): `.modePill:hover` (2 clases, especificidad 0,2,0) superaba a `.modePillActive` (1 clase, 0,1,0) — pasar el mouse sobre el pill de modo ya activo devolvía su color de texto a `--color-text` sobre el fondo `--color-primary`, **1.53:1** real (confirmado con axe-core), muy por debajo de 4.5:1. Corregido con una regla `.modePillActive:hover` explícita que mantiene `--color-on-primary`. Encontrado por el mismo mecanismo que los hallazgos de `ACC-001`: escaneo axe-core real contra la pantalla ya renderizada, no una revisión visual.

## 12. UX-014 — Módulo Laboral: vocabulario adaptable por perfil, mismo sistema visual (ADR-016)

**RECOMMENDATION, no una decisión de negocio nueva.** El Módulo Laboral (`22-decision-log.md` ADR-016) no introduce ningún token, color ni componente nuevo — reutiliza íntegramente la paleta `--color-laboral-*` y el fondo `notebook-bg` ya definidos en `§11` (UX-012). Lo único nuevo es una capa de **vocabulario de presentación**, resuelta 100% en el cliente:

| Concepto interno (esquema, igual para todos) | Consultor tecnológico | Arquitecto | Abogado | Vendedor |
|---|---|---|---|---|
| `PROJECT` | "Proyecto" | "Obra" | "Caso" | "Oportunidad" |
| `PERSON` | "Persona" | "Contacto" | "Persona" | "Prospecto" |

Este mapeo vive en el cliente (una tabla de strings por perfil, sin lógica condicional de negocio) y no afecta el esquema de datos ni la API — `PROJECT`/`PERSON` son las mismas tablas para cualquier perfil (ver ADR-016(d)). Un perfil puede además mostrar un elemento contextual opcional sin agregar un módulo nuevo al menú: por ejemplo, el perfil "Vendedor" muestra una etiqueta de etapa sobre un `PROJECT` (`status`, ya existente, sin columna nueva) a modo de "pipeline ligero"; el perfil "Arquitecto" resalta el campo `REMINDER.location` de una reunión. Ninguno de los dos es un componente nuevo — son variaciones de presentación sobre campos que ya existen.

**Implementación real (2026-08-28, `WEB-020`):** la capa de vocabulario está implementada tal como se describe arriba. El perfil se elige en **Ajustes → Perfil** (visible solo si el modo Laboral está activo — sin ese modo el vocabulario no afecta a ninguna pantalla). `core/user/vocabulary.ts` contiene la tabla literal de §12, sin términos inventados; `useVocabulary()` la expone vía `useSyncExternalStore`, así que cambiar de perfil se refleja de inmediato sin recargar. Se aplica al navbar Laboral (solo las etiquetas de Personas/Proyectos: rutas, iconos y orden son idénticos), a los títulos y estados vacíos de esas dos páginas, y a las etiquetas de sus diálogos de alta.

**Detalle no anticipado en esta sección, resuelto explícitamente:** el vocabulario cambia el **género gramatical** entre perfiles ("las Obras" vs. "los Casos", "las Personas" vs. "los Contactos"), y los textos de la interfaz llevan artículos ("Todas las obras", "Todavía no has creado ningún caso"). Se añadió un campo `projectGender`/`personGender` a la tabla en vez de deducirlo de la terminación: la regla "-o/-a" acierta en estos ocho términos por casualidad, pero es falsa en español y se rompería al añadir un perfil nuevo.

**Persistencia:** `localStorage`, por dispositivo — ADR-016(d) define esto como capa de cliente, así que no se añadió columna en `USER` ni endpoint. Limitación declarada: no sincroniza entre dispositivos (mismo criterio y precedente que el Inbox de FR-028). El default es "Consultor tecnológico" porque usa los términos neutros que la app ya mostraba: un usuario que nunca toca la preferencia no percibe ningún cambio.

**Fuera de alcance (no implementado):** los elementos contextuales opcionales que este mismo párrafo menciona como posibilidad ("el perfil Vendedor muestra una etiqueta de etapa sobre un `PROJECT`", "el perfil Arquitecto resalta `REMINDER.location`") — son "puede además", no parte del vocabulario, y construirlos habría ampliado el alcance sin pedirlo.

**Prototipo de referencia (no autoritativo, solo para validar navegación):** un artefacto navegable construido con estos mismos tokens (paleta Laboral, Inter/Fraunces, `notebook-bg`) demostró la arquitectura de información de 7 secciones núcleo (Hoy, Agenda, Tareas, Personas, Proyectos, Seguimientos, Inbox) y los 5 flujos principales — ver `34-laboral-module-proposal.md` para el enlace y el detalle. El prototipo no es código de producto ni fija ningún componente; solo valida navegación y relaciones entre entidades.

**Verificado real:** `e2e/mode-navigation.spec.ts` confirma con `getComputedStyle` real (no solo la presencia de la clase `laboral-theme`) que `--color-laboral-primary` resuelve a `#1e3f5c` y que el `background-color` computado del logo-mark (que usa `--color-primary`) es literalmente `rgb(30, 63, 92)` en una pantalla Laboral — la cascada de custom properties funciona de extremo a extremo, no solo en el token declarado. `e2e/accessibility.spec.ts` (axe-core, WCAG 2.1 A/AA) en verde sobre las pantallas de modo Laboral tras la corrección de `.modePillActive:hover`.
