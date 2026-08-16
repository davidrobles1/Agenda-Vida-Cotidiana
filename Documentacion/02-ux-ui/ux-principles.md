# Principios UX — Vida Cotidiana

**Estado: RECOMMENDATION.** Principios de diseño, no requerimientos funcionales — informan cómo se construyen las pantallas de `04-use-cases.md`, no qué existe en ellas.

## 1. Claridad sobre densidad

V1 no compite por retención con notificaciones constantes ni pantallas cargadas de opciones — es una herramienta de organización que el usuario abre, resuelve una tarea concreta (crear/completar/compartir un recordatorio) y cierra. Cada pantalla prioriza una acción primaria clara sobre mostrar todas las opciones posibles a la vez.

## 2. El estado del dato siempre es visible

Un recordatorio nunca debe dejar ambigüedad sobre su estado (pendiente/completado), quién lo posee, y con quién está compartido. Esto ya está resuelto a nivel de datos (`09-data-model.md`); la responsabilidad de UX es que la jerarquía visual lo comunique sin que el usuario tenga que inferirlo (ver `design-system.md` §5, estados con texto + color, nunca solo color).

## 3. Los tres estados de carga siempre existen

Toda pantalla que depende de una llamada de red define explícitamente sus tres estados — **loading**, **error**, **vacío** — antes de mostrar contenido real. Ninguno de los tres es "ya se me ocurrirá después"; son parte del mismo trabajo que la pantalla "feliz". Ver `accessibility.md` para el requisito de que el estado de error sea anunciado a lectores de pantalla, no solo visible.

## 4. Acciones destructivas o de alcance amplio piden confirmación textual, no solo un botón de color distinto

Revocar un colaborador, rechazar una invitación, cancelar una invitación enviada: el texto del botón dice explícitamente qué hace ("Revoke", "Reject", "Cancel"), nunca un ícono ambiguo solo. Un color de alerta (`color-error`) refuerza la señal pero no la reemplaza.

## 5. Paridad funcional, no paridad pixel-por-pixel

Android y Web comparten los mismos tokens (`design-system.md`) y el mismo propósito por pantalla, pero cada plataforma puede resolver un componente con su convención nativa cuando eso mejora la experiencia real (ver `platform-guidelines.md`) — por ejemplo, un FAB flotante en Android vs. un botón fijo en Web para "crear recordatorio". La paridad que importa es "el usuario puede hacer lo mismo", no "el pixel está en el mismo lugar".

## 6. Nada nuevo sin necesidad real

Consistente con la regla de CLAUDE.md de no sobrearquitecturar: esta pasada de diseño no introduce componentes, animaciones, ni pantallas que `04-use-cases.md` no pida. Es una capa visual sobre flujos que ya existen y ya fueron verificados funcionalmente (`01-technical-backlog.md`, `AND-002`..`WEB-006`) — no una oportunidad para expandir el alcance de V1.
