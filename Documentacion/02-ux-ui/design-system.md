# Design System — Vida Cotidiana

**Estado: RECOMMENDATION.** Todo lo de este documento es una decisión de diseño, no de negocio — si al verlo en pantalla algo no encaja, se ajusta sin que eso implique reabrir ningún requerimiento funcional. Ningún token de aquí condiciona un `FR-*`/`UC-*`; solo la presentación de lo que ya está definido en `03-prd.md`/`04-use-cases.md`.

Este documento es la fuente única de los tokens visuales compartidos entre Android y Web. `platform-guidelines.md` explica qué se comparte literalmente (estos tokens) y qué usa convención nativa (forma de los componentes).

---

## 1. Color

**RECOMMENDATION.** Un solo color semilla (`primary`) con roles derivados, siguiendo el mismo modelo de "color roles" de Material 3 — así Android puede generar su `ColorScheme` directamente desde estos valores y Web los usa como variables CSS sin duplicar la decisión.

Justificación breve: azul índigo — profesional, calmado, no asociado a alarma/urgencia (evita rojo/naranja como color dominante en una app de organización cotidiana, donde el usuario la abre muchas veces al día). Verde para éxito/completado porque es la asociación más universal y no entra en conflicto con el índigo. Los grises son neutros puros (sin tinte de color) para no competir con el primario.

| Token | Light | Dark | Uso |
|---|---|---|---|
| `color-primary` | `#4F46E5` | `#818CF8` | Acciones principales, links, FAB (Android), foco |
| `color-primary-container` | `#E0E7FF` | `#312E81` | Fondo de elementos primarios de baja énfasis (chips, fondos de card seleccionada) |
| `color-on-primary` | `#FFFFFF` | `#1E1B4B` | Texto/ícono sobre `color-primary` |
| `color-success` | `#059669` | `#34D399` | Ícono/borde/fondo de estado "completado" — **no usar para texto** (3.77:1 en fondo blanco, insuficiente para texto normal AA) |
| `color-success-text` | `#047857` | `#34D399` | Texto del estado "completado" (5.48:1 en blanco) |
| `color-success-container` | `#D1FAE5` | `#064E3B` | Fondo de estado "completado" |
| `color-warning` | `#D97706` | `#FBBF24` | Ícono/borde/fondo de estado "pendiente" — **no usar para texto** (3.19:1 en fondo blanco, insuficiente) |
| `color-warning-text` | `#B45309` | `#FBBF24` | Texto del estado "pendiente" (5.02:1 en blanco) |
| `color-warning-container` | `#FEF3C7` | `#78350F` | Fondo de estado "pendiente" |
| `color-error` | `#DC2626` | `#F87171` | Errores, acción destructiva (revocar, rechazar) — 4.83:1 en blanco, sí válido para texto |
| `color-error-container` | `#FEE2E2` | `#7F1D1D` | Fondo de mensajes de error |
| `color-surface` | `#FFFFFF` | `#16171D` | Fondo de pantalla |
| `color-surface-variant` | `#F9FAFB` | `#1F2028` | Fondo de card/superficie elevada |
| `color-border` | `#E5E7EB` | `#2E303A` | Bordes, separadores |
| `color-text` | `#111827` | `#F3F4F6` | Texto principal |
| `color-text-secondary` | `#6B7280` | `#9CA3AF` | Texto secundario (metadatos, timestamps) |

**Contraste — verificado calculando la fórmula de contraste WCAG 2.1 real (luminancia relativa, no asumido visualmente), ver `accessibility.md` §1 para la tabla completa de pares y sus ratios exactos.** Hallazgo real durante esta verificación: `color-success`/`color-warning` en su tono original (600) **no** cumplían 4.5:1 para texto normal sobre blanco (3.77:1 y 3.19:1) — quedan como color de ícono/fondo/borde únicamente (ahí sí cumplen el umbral de 3:1 para elementos gráficos/texto grande), y se añadieron `color-success-text`/`color-warning-text` (tono 700, más oscuro) específicamente para texto, que sí cumplen AA.

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

## 6. Dónde viven los tokens en código

- Android: `app/src/main/java/com/vidacotidiana/app/core/ui/{Color,Type,Spacing,Theme}.kt`.
- Web: `web/src/index.css` (custom properties `--color-*`/`--space-*`) + `@fontsource/inter`.

Ningún token se duplica a mano en un componente — todo componente nuevo o restilizado referencia estos archivos, nunca un valor hex/dp suelto (ver `15-coding-standards.md`).
