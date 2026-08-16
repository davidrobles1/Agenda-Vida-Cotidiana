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
| Navegación entre "Reminders"/"Invitations"/"Notifications" | Botones de header (ya implementados, `AND-004`/`AND-005`) — candidato futuro a `NavigationBar` de Material 3 si la app crece | Links de texto en el header | Un `NavigationBar` inferior es idiomático en Android para 3-5 destinos; Web no tiene un equivalente igual de idiomático para tan pocos destinos — los links de header ya cumplen el propósito sin inventar un patrón. **No se migra a `NavigationBar` en esta pasada** (cambiaría la navegación, fuera del alcance "solo visual" de esta tarea) — queda anotado para una iteración de UX futura. |
| Diálogo de confirmación/panel inline (compartir, ej. `ShareDialog`) | Panel inline en la misma pantalla (ya así desde `AND-004`) | Panel inline en la misma pantalla (ya así desde `WEB-004`) | Ambos ya convergen en el mismo patrón (no un modal/`AlertDialog` nativo) — se mantiene, solo se restilizan los colores/espaciado. |
| Indicador de carga | `CircularProgressIndicator` (Material 3) | Texto "Loading…" (ya existente) — candidato a spinner CSS en una iteración futura | Cambiar el indicador de Web a un spinner visual es una mejora cosmética menor, no prioritaria frente a lo que sí pide esta tarea (card, spacing, estados, share dialog) — se deja como está, con mejor color/tipografía de acompañamiento únicamente. |
| Tipografía | Roboto (Material 3 default) | Inter (`@fontsource/inter`) | Cada plataforma usa la fuente que se siente nativa en su ecosistema — Roboto es la fuente de sistema esperada en Android, Inter es una elección de Web ampliamente usada por su legibilidad en pantalla y por no depender de la fuente del sistema operativo del usuario (que varía mucho más en desktop que en Android). |

## Qué NO cambia en esta pasada (fuera de alcance, declarado)

- Estructura de navegación (rutas, jerarquía de pantallas) — es lógica de flujo, no visual.
- Modelo de datos, llamadas de red, manejo de estado de los `ViewModel`/hooks existentes.
- Cualquier funcionalidad no visual — esta pasada es estrictamente presentación sobre lo que ya funciona (`01-technical-backlog.md`, `AND-002`..`WEB-006`).
