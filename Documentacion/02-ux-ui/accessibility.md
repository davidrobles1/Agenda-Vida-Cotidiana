# Accesibilidad — Vida Cotidiana

**Estado: RECOMMENDATION** para los umbrales (estándar WCAG 2.1 AA, no una invención propia) y **DECISION** para aplicarlos como piso mínimo de V1 — no es negociable por plataforma, ambas cumplen lo mismo.

## 1. Contraste de color (WCAG 2.1 AA)

**Recalculado por completo el 2026-08-17 (`ACC-001`)** contra los tokens reales de `design-system.md` tal como quedaron después de `UX-006` — no una reverificación de los números que ya existían aquí. La pasada anterior (`UX-001`) solo había calculado la paleta clara base contra fondos neutros (blanco/superficie); nunca se había recalculado contra el tema oscuro completo, ni contra las combinaciones texto/ícono-sobre-su-propio-container que introdujo el sistema de pills/badges de `UX-006` (`StatusPill`, sidebar activo, chips). Metodología: fórmula de contraste WCAG 2.1 real (luminancia relativa), computada para cada par que el código realmente usa — no solo texto-sobre-fondo-de-pantalla.

**6 fallos reales de AA encontrados y corregidos** (4 en modo oscuro, 2 en modo claro — detalle completo y candidatos evaluados en `design-system.md` §1). Tabla completa recalculada, ambos temas:

| Par | Claro | Oscuro | Uso | Resultado |
|---|---|---|---|---|
| `color-text` / `color-surface` | 17.74:1 | 16.81:1 | Texto principal | ✅ AAA ambos |
| `color-text-secondary` / `color-surface` | 4.83:1 | 7.29:1 | Texto secundario | ✅ AA/AAA |
| `color-text-secondary` / `color-surface-variant` | 4.63:1 | 6.70:1 | Texto secundario sobre card | ✅ AA |
| `color-primary` / `color-surface` | 6.29:1 | 9.28:1 | Links, texto de énfasis | ✅ AA/AAA |
| `color-on-primary` / `color-primary` | 6.29:1 | 8.02:1 | Texto de botón primario | ✅ AA/AAA |
| `color-primary` / `color-primary-container` | 5.10:1 | 5.73:1 | Sidebar activo, chip seleccionado — **falló en oscuro antes de la corrección (3.83:1)** | ✅ AA (corregido) |
| `color-success-text` / `color-success-container` | 4.84:1 | 5.06:1 | Pill "Vigente"/"Al día" | ✅ AA |
| `color-warning-text` / `color-warning-container` | 6.37:1 | 5.43:1 | Pill "Pendiente"/"Por vencer" | ✅ AA |
| `color-info-text` / `color-info-container` | 5.49:1 | 5.74:1 | Pill "Programada" — **falló en oscuro antes de la corrección (4.07:1)** | ✅ AA (corregido) |
| `color-error` / `color-error-container` | 5.30:1 | 5.28:1 | Pill/badge de error, `role="alert"` — **falló en ambos temas antes de la corrección (3.95:1 / 3.62:1)** | ✅ AA (corregido) |
| `color-success` / `color-success-container` (gráfico) | 3.32:1 | 5.06:1 | Ícono de éxito | ✅ ≥3:1 |
| `color-warning` / `color-warning-container` (gráfico) | 3.60:1 | 5.43:1 | Ícono de advertencia — **falló en claro antes de la corrección (2.86:1)** | ✅ ≥3:1 (corregido) |
| `color-info` / `color-info-container` (gráfico) | 4.24:1 | 4.07:1 | Ícono info | ✅ ≥3:1 |
| `color-border` / `color-surface` (gráfico, SC 1.4.11) | 3.20:1 | 3.83:1 | Borde de `input` (Web, componente real) | ✅ ≥3:1 (corregido — antes 1.24:1 / 1.47:1) |
| `color-border` / `color-surface-variant` (gráfico) | 3.07:1 | 3.52:1 | Borde sobre card | ✅ ≥3:1 (corregido) |

**Regla derivada, ya reflejada en `design-system.md` §1:** ningún token se verifica solo contra "el fondo típico" (blanco/superficie) — se verifica contra **cada fondo real donde el código lo pinta**, incluyendo su propio `*-container` cuando existe. El método de verificación anterior (`UX-001`) fue precisamente el que dejó pasar los 6 fallos de arriba, porque solo probó contra blanco/superficie y nunca contra los containers introducidos después.

## 2. Tamaño de touch target

- **Android:** mínimo `48dp` × `48dp` por objetivo táctil (guía de Material Design/Android Accessibility). Aplica a botones, íconos clicables, filas de lista completas cuando son el objetivo de tap.
- **Web:** mínimo `44px` × `44px` (WCAG 2.5.5 AA, "Target Size") — ligeramente distinto del valor de Android porque son guías de plataformas distintas, no un error de transcripción.
- Ningún botón/ícono restilizado en esta pasada queda por debajo de su mínimo de plataforma, incluyendo los botones de acción secundaria dentro de una card (p. ej. "Complete"/"Share" en la fila de un recordatorio) — el bug real de `AND-004`/`AND-006` (botones comprimidos a ancho cero por overflow de `Row`) es exactamente el tipo de regresión que este requisito busca prevenir explícitamente hacia adelante.

## 3. Soporte de lector de pantalla

**Verificado con herramientas reales el 2026-08-17 (`ACC-001`), no solo por revisión de código:**

**Web — axe-core (`@axe-core/playwright`, reglas `wcag2a`/`wcag2aa`), corrida real contra la app en ejecución** (`web/e2e/accessibility.spec.ts`): login, y las pantallas autenticadas Tareas/Inicio/Compartidos/Notificaciones/Documentos (mock module representativo) — **0 violaciones reales en las 6 pantallas**, verificado con `npx playwright test accessibility.spec.ts` en verde, no una promesa de que debería pasar.

**Web — Lighthouse (categoría accesibilidad), corrida real vía `playwright-lighthouse`** contra el login autenticado en la misma sesión de navegador (necesario porque el token vive solo en memoria — DEC-007/`08c-web-architecture.md` — y una recarga de Lighthouse independiente perdería la sesión): **hallazgo real, corregido, no solo documentado** — `landmark-one-main`: el documento no tenía un landmark `<main>` (score 96/100). `LoginPage.tsx` no estaba envuelto por `AppShell` (que sí lo tiene desde `UX-006`), así que era la única pantalla real sin uno. Corregido envolviendo su contenedor en `<main>`. Reverificado tras el fix: **100/100, 0 hallazgos**.

**Android — pasada manual real con TalkBack activado en el dispositivo físico** (`adb shell settings put secure enabled_accessibility_services .../TalkBackService`, confirmado con la pantalla real "TalkBack está activado", no simulado): login, Tareas (con recordatorios reales, pendientes y completados), Más, Garantías (mock module con los 3 estados de `StatusPill`). Confirmado con el volcado real del árbol de accesibilidad (`uiautomator dump`) sobre Garantías: los pills **"Por vencer"/"Vigente"/"Vencida" existen como nodos de texto reales** — TalkBack los anuncia como texto, no dependen del color. El foco inicial de TalkBack aterriza de forma lógica en la acción principal de cada pantalla (botón "Log in", campo de usuario del login de Keycloak, título de pantalla). **Nota de método:** el volcado crudo de `uiautomator` no refleja correctamente el merge de semántica de Compose para nodos clicables con texto (limitación conocida de la herramienta, no un bug de la app) — para esos casos se usó en su lugar el volcado nativo de semántica de Compose (`printToLog`, usado en sesiones anteriores de este mismo proyecto) y una auditoría exhaustiva del código fuente: **cada uno de los 11 `Icon(...)` del proyecto Android** tiene `contentDescription` real (FAB "+" → `"Nuevo"`, ítems de la barra inferior → su label) o `contentDescription = null` explícito y correcto (ícono decorativo con texto adyacente redundante — `MetricCard`, `ListItemRow`, filas de `MoreScreen`, etc.) — cero íconos huérfanos sin etiqueta encontrados.

- **Android (Compose/TalkBack):** todo `Icon` decorativo sin texto adyacente lleva `contentDescription` (o `contentDescription = null` explícito si es puramente decorativo y ya hay texto redundante al lado — nunca omitido por descuido, confirmado por auditoría exhaustiva arriba). Los estados de carga usan `CircularProgressIndicator` con una descripción semántica. Los mensajes de error usan `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` para que TalkBack los anuncie sin que el usuario tenga que navegar manualmente hasta ellos.
- **Web:** los mensajes de error usan `role="alert"` — confirmado sin violaciones reales por axe-core. Los estados vacíos/carga son texto real (no solo un ícono), legible por lectores de pantalla sin configuración adicional. Los botones de ícono-solo llevan `aria-label` explícito (p. ej. la campana de notificaciones en `AppShell.tsx`).
- Ninguna información de estado se comunica **solo** por color (ver `design-system.md` §5) — confirmado con datos reales del dispositivo en §3 arriba, no solo por inspección de código.

## 4. Foco de teclado (Web)

Todo elemento interactivo mantiene un `:focus-visible` visible (outline con `color-primary`, nunca `outline: none` sin reemplazo) — necesario para navegación por teclado, no solo mouse/touch. Esto ya era técnicamente cierto por el estilo por defecto del navegador antes de esta pasada; se mantiene explícito al introducir estilos propios de botón/input para no perderlo por accidente (un error común al reemplazar el estilo nativo de un `<button>`).

## 5. Qué NO se cubre en V1 (TBD/FUTURE, declarado explícitamente)

- ~~Auditoría formal con herramienta automatizada (axe, Lighthouse)~~ — **cerrado 2026-08-17** (`ACC-001`), ver §3. Integrado como spec real de Playwright (`web/e2e/accessibility.spec.ts`), no todavía como gate de CI (no hay CI real corriendo — depende del mismo gap ya declarado en `05-v2-plan.md` punto 8, fuera del alcance de esta tarea).
- ~~Pruebas manuales reales con TalkBack activado~~ — **cerrado en Android 2026-08-17** (`ACC-001`), ver §3. **VoiceOver (iOS): sigue sin cubrir** — desarrollo de iOS pausado por decisión explícita del usuario, mismo gap ya declarado en `05-v2-plan.md` ("Continuar el pulido visual a iOS").
- **TBD — Gate de CI real para axe-core:** el spec existe y pasa en verde localmente, pero no corre automáticamente en ningún pipeline (no hay CI real — mismo gap del punto 8 de `05-v2-plan.md`).
