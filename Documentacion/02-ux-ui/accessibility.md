# Accesibilidad — Vida Cotidiana

**Estado: RECOMMENDATION** para los umbrales (estándar WCAG 2.1 AA, no una invención propia) y **DECISION** para aplicarlos como piso mínimo de V1 — no es negociable por plataforma, ambas cumplen lo mismo.

## 1. Contraste de color (WCAG 2.1 AA)

Umbral: **4.5:1** para texto normal, **3:1** para texto grande (≥18pt/24px o ≥14pt/19px bold) y para componentes gráficos/de UI (bordes de input, íconos con significado). Verificado calculando la fórmula real de contraste (luminancia relativa WCAG), no por inspección visual — ver script usado en la sesión que generó este documento.

| Par | Ratio | Uso | Resultado |
|---|---|---|---|
| `color-text` (`#111827`) / `color-surface` (`#FFFFFF`) | 17.74:1 | Texto principal, modo claro | ✅ AA (texto normal) |
| `color-text-secondary` (`#6B7280`) / `color-surface` | 4.83:1 | Texto secundario, modo claro | ✅ AA (texto normal) |
| `color-on-primary` (`#FFFFFF`) / `color-primary` (`#4F46E5`) | 6.29:1 | Texto sobre botón primario | ✅ AA (texto normal) |
| `color-primary` / `color-primary-container` (`#E0E7FF`) | 5.10:1 | Texto de énfasis sobre fondo primario tenue | ✅ AA (texto normal) |
| `color-success` (`#059669`) / blanco | 3.77:1 | — | ❌ Insuficiente para texto normal; **solo** válido como color de ícono/borde (≥3:1) |
| `color-success-text` (`#047857`) / blanco | 5.48:1 | Texto de estado "Completed" | ✅ AA (texto normal) |
| `color-warning` (`#D97706`) / blanco | 3.19:1 | — | ❌ Insuficiente para texto normal; **solo** válido como color de ícono/borde |
| `color-warning-text` (`#B45309`) / blanco | 5.02:1 | Texto de estado "Pending" | ✅ AA (texto normal) |
| `color-error` (`#DC2626`) / blanco | 4.83:1 | Texto de error | ✅ AA (texto normal) |
| `color-text` (`#F3F4F6`) / `color-surface` (`#16171D`) | 16.25:1 | Texto principal, modo oscuro | ✅ AA |
| `color-text-secondary` (`#9CA3AF`) / `color-surface` (dark) | 7.04:1 | Texto secundario, modo oscuro | ✅ AA |

**Regla derivada de este hallazgo real:** `color-success`/`color-warning` (tono 600) existen solo como color de ícono/fondo/borde — nunca como `color` de texto. Cualquier componente nuevo que quiera pintar texto de estado usa `color-success-text`/`color-warning-text` (tono 700), no el tono base. Esto ya está reflejado en `design-system.md` §1 y debe respetarse en cualquier componente futuro, no solo en los restilizados en esta pasada.

## 2. Tamaño de touch target

- **Android:** mínimo `48dp` × `48dp` por objetivo táctil (guía de Material Design/Android Accessibility). Aplica a botones, íconos clicables, filas de lista completas cuando son el objetivo de tap.
- **Web:** mínimo `44px` × `44px` (WCAG 2.5.5 AA, "Target Size") — ligeramente distinto del valor de Android porque son guías de plataformas distintas, no un error de transcripción.
- Ningún botón/ícono restilizado en esta pasada queda por debajo de su mínimo de plataforma, incluyendo los botones de acción secundaria dentro de una card (p. ej. "Complete"/"Share" en la fila de un recordatorio) — el bug real de `AND-004`/`AND-006` (botones comprimidos a ancho cero por overflow de `Row`) es exactamente el tipo de regresión que este requisito busca prevenir explícitamente hacia adelante.

## 3. Soporte de lector de pantalla

Aplica a todo componente tocado en esta pasada de diseño (`RemindersScreen`/`ShareDialog`/`InvitationsScreen`/`NotificationsScreen`/`LoginScreen` en Android; sus equivalentes en Web):

- **Android (Compose/TalkBack):** todo `Icon` decorativo sin texto adyacente debe llevar `contentDescription` (o `contentDescription = null` explícito si es puramente decorativo y ya hay texto redundante al lado — nunca omitido por descuido). Los estados de carga usan `CircularProgressIndicator` con una descripción semántica (`Modifier.semantics { contentDescription = "Loading" }` cuando no hay texto visible acompañante). Los mensajes de error usan `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` para que TalkBack los anuncie sin que el usuario tenga que navegar manualmente hasta ellos.
- **Web:** los mensajes de error ya usan `role="alert"` (patrón ya establecido en `RemindersPage.tsx`/`ShareDialog.tsx`/etc. desde `WEB-002`) — se mantiene y se extiende a cualquier mensaje de error nuevo introducido en esta pasada. Los estados vacíos/carga son texto real (no solo un ícono), legible por lectores de pantalla sin configuración adicional. Los botones de ícono-solo (si los hay tras esta pasada) llevan `aria-label` explícito.
- Ninguna información de estado se comunica **solo** por color (ver `design-system.md` §5) — un lector de pantalla no percibe el color, así que el texto/ícono acompañante no es un adorno, es el canal accesible real.

## 4. Foco de teclado (Web)

Todo elemento interactivo mantiene un `:focus-visible` visible (outline con `color-primary`, nunca `outline: none` sin reemplazo) — necesario para navegación por teclado, no solo mouse/touch. Esto ya era técnicamente cierto por el estilo por defecto del navegador antes de esta pasada; se mantiene explícito al introducir estilos propios de botón/input para no perderlo por accidente (un error común al reemplazar el estilo nativo de un `<button>`).

## 5. Qué NO se cubre en V1 (TBD/FUTURE, declarado explícitamente)

- Auditoría formal con herramienta automatizada (axe, Lighthouse accessibility score) — no hay una corrida real documentada todavía. **TBD**: se puede incorporar como parte de `19-cicd.md` en una iteración futura, fuera del alcance acotado de esta tarea.
- Pruebas manuales reales con TalkBack/VoiceOver activado de punta a punta — lo verificado en esta pasada es la implementación (`contentDescription`, `role="alert"`, contraste calculado), no una sesión de uso real con el lector de pantalla encendido. **FUTURE**, cuando haya presupuesto de QA dedicado a accesibilidad.
