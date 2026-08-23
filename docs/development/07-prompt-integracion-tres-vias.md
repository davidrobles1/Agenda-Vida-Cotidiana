# Prompt para Claude Code — Integración de 3 worktrees (Bloque A + B.1 + B.2 + C)

Contexto: 4 líneas de trabajo verificadas por separado con evidencia real (UX-013 vistas Semana/Día,
BE-037/WEB-009 Garantías/Mantenimiento reales, BE-038 campos de ADR-015 hecho directamente en el árbol
principal, UX-012 navegación Personal/Laboral) viven hoy en worktrees/ramas separadas. Nada está
mezclado. Objetivo de esta tarea: un solo árbol coherente, sin perder ningún fix real ya verificado.

## Orden de integración (no cambiar el orden — cada paso reduce el riesgo de conflicto del siguiente)

1. **BE-038 primero** (campos de ADR-015: `USER.personal_enabled/laboral_enabled`, `REMINDER.context`,
   `POST /me/modes`, migración `V6__adr015_context_modes.sql`). Ya está en el árbol principal según el
   reporte — confirma que efectivamente es la base sobre la que mergeas todo lo demás, no un worktree
   aparte. Corre `./mvnw clean test` (esperado 120/120) antes de seguir.
2. **UX-013 segundo** (vistas Semana/Día). Toca `CalendarView.tsx`/`CalendarView.module.css`/
   `CalendarPage.module.css` — bajo riesgo de conflicto real con los siguientes porque B.1 y C tocan
   `useCalendarData.ts`/`CalendarPage.tsx` a un nivel distinto (datos vs. navegación), no las mismas
   líneas de vista. Aun así, corre la suite completa de `calendar-views.spec.ts` (16/16 esperado) después
   de mergear, no solo antes.
3. **BE-037/WEB-009 tercero** (Garantías/Mantenimiento reales). Este es el punto de riesgo real señalado
   en tu reporte:
   - Antes de aceptar su `calendar.spec.ts`, diff explícito contra la versión pre-agente (`git log -p` o
     `git diff` contra el commit anterior a que arrancara el agente) para localizar exactamente el test de
     navegación por teclado y el `exact:true` de `UX-011` Fase 1. Reinsértalos a mano en la versión nueva
     de B.1 — no aceptes su archivo completo tal cual, y no descartes el suyo tal cual tampoco (ambos
     tienen cambios reales que necesitas).
   - Mismo cuidado con `useCalendarData.ts`: B.1 reemplaza `warranties`/`maintenanceRecords` mock por
     llamadas reales — confirma que esa versión ya incluye (no revierte) cualquier cambio que UX-013 haya
     hecho al mismo archivo en el paso 2.
   - Corre con `PW_API_BASE_URL` seteada correctamente esta vez (ya identificaste que el fallback a :8080
     fue el problema, no un bug real) — `./mvnw clean test` (138/138 esperado) + e2e completo.
4. **UX-012 al final** (navegación Personal/Laboral). Se apoya en `CalendarPage.tsx` ya estabilizado por
   los 3 pasos anteriores como base de "Calendario general" (FR-017) — mergearlo último minimiza que tenga
   que reconciliarse dos veces. Después de mergear, antes de cualquier verificación e2e nueva: corre otra
   vez la suite completa heredada (no solo `mode-navigation.spec.ts`) para confirmar que envolver la
   navegación no rompió nada de lo ya verificado en los 3 pasos previos.

## Verificación final (después de los 4 pasos, no después de cada uno por separado)

- `./mvnw clean test` completo del árbol integrado — reporta el número real de casos, no un estimado.
- `tsc`/`lint`/`build` (web) del árbol integrado.
- Suite e2e completa (no solo los specs nuevos): `calendar.spec.ts`, `calendar-views.spec.ts`,
  `warranties-maintenance.spec.ts`, y `mode-navigation.spec.ts` si ya resolviste el redirect URI de
  Keycloak — si no, dilo explícitamente como bloqueado, no lo saltes en silencio.
- Confirma con `getComputedStyle` (mismo método que ya usaste para el bug de los botones) que el fix de
  padding de UX-013 sigue aplicado después del merge — es fácil que un merge automático reintroduzca el
  valor viejo si hay conflicto en ese archivo.

## Documentación pendiente (la que los agentes no alcanzaron a escribir)

Complétala como parte de esta misma tarea, no después: `01-technical-backlog.md` (entradas de B.1 y C que
faltan), `design-system.md` (si C definió los tokens `--color-laboral-*` finales, agrégalos ahí también,
no solo en código), `12-traceability.md` (filas de Garantías/Mantenimiento y FR-014..019 con columnas
API/Backlog reales, ya no `TBD`).

## Repórtame de vuelta

Evidencia real del árbol ya integrado (no de los worktrees por separado): resultado de test/build/e2e
completos, confirmación de que el test de teclado de `UX-011` sigue existiendo y pasa, y si el e2e de
Bloque C quedó verificado o sigue bloqueado por el redirect URI de Keycloak.
