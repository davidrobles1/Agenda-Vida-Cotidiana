# 34 — Propuesta: Módulo Laboral como espacio profesional universal (ADR-016)

**Estado: DECISION (núcleo, ver `22-decision-log.md` ADR-016) + RECOMMENDATION (clasificación V4/Post-V4, matriz por profesión, automatizaciones).** Este documento es el análisis de producto completo detrás de ADR-016. Los IDs de requerimiento (`FR-021` a `FR-028`, `NFR-011`, `SEC-004`), casos de uso (`UC-17` a `UC-21`) y flujos ya están integrados en `03-prd.md`, `04-use-cases.md`, `05-user-flows.md`, `09-data-model.md` y `12-traceability.md` — este documento no los repite en detalle normativo, los explica y los justifica.

**Prototipo navegable de referencia (no autoritativo, solo para validar navegación y relaciones entre entidades — no fija ningún componente de UI final):** https://claude.ai/code/artifact/fca1566a-a489-46eb-acf5-4c2174f5c8c0 — construido con los tokens reales de la app (paleta Laboral de UX-012, Inter/Fraunces, `notebook-bg`), permite recorrer las 7 secciones núcleo y los 5 flujos principales con datos ficticios, y cambiar entre 4 perfiles de ejemplo (Consultor tecnológico, Arquitecto, Abogado, Vendedor) para verificar que el mismo modelo se adapta sin plantillas por profesión.

---

## 1. Resumen ejecutivo

El módulo Laboral evoluciona de "agenda de trabajo genérica" (hoy: navegación por contexto sobre `REMINDER`, ver ADR-015) hacia una **memoria profesional universal**: un conjunto pequeño de entidades relacionadas (Persona, Proyecto, Tarea, Reunión, Compromiso) que sirve a cualquier profesión sin plantillas verticales. La Agenda sigue siendo el centro del producto; Laboral le añade contexto relacional. No se introduce IA, CRM completo, ni finanzas — quedan `FUTURE`/Post-V4 (§16).

## 2. Problema que resolvemos

La sección Laboral actual (ADR-015) es una vista filtrada sobre el mismo `REMINDER`: un recordatorio no sabe con quién es, para qué proyecto, ni qué se prometió. El usuario profesional necesita recordar **compromisos sociales** ("le prometí a Carlos el viernes"), no solo fechas. El costo cognitivo real está en rastrear relaciones, no en gestionar tareas aisladas — ahí es donde Notion/Jira imponen demasiada estructura y una agenda pura se queda corta.

## 3. Principios UX

- **Universal, no vertical**: mismas entidades para ingeniero, abogado o vendedor; la especialización es de vocabulario/filtro (UX-014), no de arquitectura.
- **Superficie simple, profundidad opcional**: 7 secciones núcleo visibles (§7); todo lo demás (notas, documentos, objetivos) es contextual, alcanzable desde una entidad, no otro ítem de sidebar.
- **Captura sin fricción primero, organización después**: Inbox (FR-028) existe para no forzar categorización en el momento.
- **Nunca doble calendario**: Laboral no tiene su propio reloj; comparte el tiempo con Personal — principio ya resuelto por ADR-015, este módulo no lo reabre.
- **Color + texto para estado, nunca solo color** — regla heredada del design system (`design-system.md` §5), aplica igual a los estados de `COMMITMENT`.

## 4. Modelo conceptual de entidades

```
Persona ──┬── Proyecto ──┬── Tarea (REMINDER + person_id/project_id)
          │              └── Reunión (REMINDER + location + REMINDER_SHARE)
          ├── Compromiso (dirección MINE/THEIRS — unifica Seguimiento y Esperando)
          └── Nota (NOTE existente, vínculo opcional)
```

**Decisión de diseño clave — Seguimiento vs. Esperando: no son entidades independientes, son la misma entidad ("Compromiso") vista por dirección.** Justificación completa en ADR-016 (Alternativas): un compromiso cambia de dirección con frecuencia (tras que la otra persona responde, el turno de acción se invierte) — modelarlos como entidades separadas obligaría a migrar registros entre tablas cada vez. Con un solo modelo (`COMMITMENT.direction`), "Seguimientos" = compromisos `MINE`, "Esperando" = compromisos `THEIRS`; ambas son **vistas filtradas**, no módulos distintos.

## 5. Arquitectura de información

```
AGENDA
├── Calendario General          (ADR-015, sin cambios)
├── Personal                    (ADR-015, sin cambios)
└── Laboral
    ├── Hoy              (núcleo, FR-026)
    ├── Agenda           (núcleo — comparte reloj con Personal, ADR-015)
    ├── Tareas           (núcleo, FR-023)
    ├── Personas         (núcleo, FR-021)
    ├── Proyectos        (núcleo, FR-022)
    ├── Seguimientos     (núcleo — pestañas Mías/Esperando, FR-025/FR-027)
    └── Inbox            (núcleo, FR-028)

Contextual (no en sidebar, se llega desde una entidad):
    Notas · Documentos (FUTURE V4) · Objetivos · Rutinas · Lugares · Recursos (FUTURE V4)

Futuro (Post-V4, activable por perfil/comportamiento si se aprueba):
    CRM avanzado (pipeline, oportunidades) · Casos · Obras · Clases
    Automatizaciones proactivas · Insights · Asistente
```

## 6. Propuesta de menú (Laboral)

7 ítems fijos en el navbar del modo Laboral — mismo patrón visual que el navbar actual (icono + label, activo = `primary-container`, ver `design-system.md`). Nada de submenús anidados en el nivel 1; el detalle vive dentro de cada sección. Ver `02-ux-ui/platform-guidelines.md` UX-014.

## 7. Secciones universales (núcleo, V3)

| Sección | Por qué es universal | Requerimiento |
|---|---|---|
| Hoy | Todas las profesiones necesitan un punto de entrada diario | FR-026 |
| Agenda | El tiempo es la restricción compartida por cualquier trabajo | ADR-015 (reutilizado) |
| Tareas | Acción pendiente es el átomo común a todo trabajo | FR-023 |
| Personas | Todo trabajo profesional ocurre *con* alguien | FR-021 |
| Proyectos | Unidad de agrupación universal (obra, caso, cuenta, curso = "proyecto") | FR-022 |
| Seguimientos/Esperando | Compromiso social — universal en cualquier relación profesional | FR-025/FR-027 |
| Inbox | Captura rápida sin decidir categoría, reduce fricción | FR-028 |

## 8. Secciones opcionales / contextuales

Notas (reutiliza `NOTE` existente), Documentos, Objetivos, Rutinas, Lugares, Recursos — todas **subordinadas a una entidad** (se abren desde una Persona o Proyecto), no ocupan sidebar. Documentos/Objetivos/Rutinas/Lugares/Recursos son candidatos V4 (§14), no comprometidos todavía. Automatizaciones/Insights/Asistente son `FUTURE` (requieren datos acumulados; sin valor claro en V3 con poco histórico). CRM/Pipeline/Casos/Obras/Clases son Post-V4 — especialización real, alto costo, bajo % de usuarios cada uno.

## 9. Matriz por profesión (necesidades universales vs. especializadas)

| Necesidad | Ingeniero | Arquitecto | Abogado | Vendedor | Admin | Secretaria | Profesor | Salud |
|---|---|---|---|---|---|---|---|---|
| Personas/contactos | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Proyectos/agrupación | ✅ | ✅ (obra) | ✅ (caso) | ✅ (cuenta) | ✅ | ✅ | ✅ (curso) | △ |
| Tareas | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Reuniones → notas → tareas | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | △ | △ |
| Seguimiento/Esperando | ✅ | ✅ | ✅ (vencimientos) | ✅✅ (crítico) | ✅ | ✅ | △ | △ |
| Pipeline/oportunidades | ✗ | △ | ✗ | ✅✅ | △ | ✗ | ✗ | ✗ |
| Documentos formales | ✅ | ✅✅ (planos) | ✅✅ (legal) | △ | ✅ | ✅ | △ | ✅ (historial) |
| Visitas/ubicación | △ | ✅✅ | ✗ | ✅ | ✗ | ✗ | ✗ | ✅ (consultorio) |

✅✅ = crítico y diferenciador · ✅ = usado regularmente · △ = ocasional · ✗ = irrelevante. **ASSUMPTION:** esta matriz es una estimación cualitativa del autor de esta propuesta, no una encuesta a usuarios reales de cada profesión — se documenta como tal, no como dato validado. Confirma que el núcleo (Personas/Proyectos/Tareas/Compromisos) es razonablemente universal; Pipeline y Ubicación son las primeras candidatas a especialización opt-in (Post-V4).

## 10. CRM mínimo viable

**Núcleo (V3):** Persona, Organización (texto libre, ADR-016(c)), última interacción (candidata V4, derivada — no manual), próxima acción (deriva de Compromisos), notas por persona (reutiliza `NOTE`).
**Post-V4:** oportunidades, pipeline con etapas, forecast, scoring — esto sí es CRM-de-ventas y queda fuera hasta validar demanda real del segmento ventas (decisión explícita del Product Owner: "no queremos un Salesforce").

## 11. Sistema de Seguimientos

Ver §4 y ADR-016. UX: pestaña "Mías" dentro de "Seguimientos" (FR-027) — Persona + acción + fecha + Proyecto opcional. Se crean desde: manual (UC-18), detalle de Persona, o al cerrar una reunión (UC-21).

## 12. Sistema Esperando

Pestaña "Esperando" (FR-027, `direction = THEIRS`) — resuelve la ansiedad de depender de terceros mostrando *antigüedad* del compromiso ("hace 9 días") y permitiendo reprogramar o resolver con un clic (UC-20). Muestra el contexto de origen (reunión/nota) cuando existe, para no perder el motivo del compromiso.

## 13. Automatizaciones

**Simples (candidatas V4, deterministas, sin IA):** vencimiento de compromiso → aparece en "Hoy"; nota de reunión → sugerencia (no automática) de convertir una línea en tarea/compromiso (regla de texto simple, no modelo de lenguaje).
**Proactivas (Post-V4, requieren histórico):** p. ej. "llevas 7 días sin contactar a este cliente con propuesta pendiente" — detección de patrón, no regla simple; requiere evaluar si es determinista o necesita un modelo, y por tanto cae bajo la prohibición de IA de `CLAUDE.md`/ADR-003 hasta que se decida lo contrario explícitamente.

## 14. Clasificación V3 / V4 / Post-V4

| Alcance | Contenido | Requerimiento | Valor | Complejidad |
|---|---|---|---|---|
| **V3 (núcleo, aprobado — ADR-016)** | Persona, Proyecto, vínculo Tarea↔Persona/Proyecto, Reunión (ubicación+participantes), Compromiso, Hoy, Seguimientos, Inbox | FR-021 a FR-028 | Alto | Media |
| **V4 (candidato, `RECOMMENDATION`)** | Notas/Documentos vinculados, última interacción derivada, automatizaciones simples deterministas, Objetivos/Rutinas/Lugares/Recursos, Kanban de Proyecto | Sin FR asignado todavía | Alto | Media-alta |
| **Post-V4 (`FUTURE`, no comprometido)** | Pipeline/CRM avanzado, Casos, Obras, Clases, Insights, Asistente, automatización proactiva/IA | N/A | Alto (nicho) | Alta |

## 15. Riesgos de producto

- Convertirse en Notion sin querer si `PROJECT`/`PERSON` ganan demasiados campos configurables — mitigado por NFR-011 (minimización) y por no crear `ORGANIZATION` todavía (ADR-016(c)).
- Confusión de calendario si Laboral llegara a tener su propio reloj — ya prevenido por ADR-015, este módulo no lo toca.
- Compromiso mal representado como "tarea" duplicaría trabajo de captura — mitigado por el modelo unificado `COMMITMENT.direction` (§4).
- El nombre del producto sigue `TBD` (`25-open-questions.md`) y ADR-015 menciona un rebrand a "Agenda Meraki" no ejecutado — este documento no depende de ese nombre y no lo asume.

## 16. Qué NO construir

ERP, pipeline de ventas completo, gestión de obra con planos, expedientes legales, historiales clínicos, IA/automatización predictiva, finanzas/facturación — todo Post-V4 o fuera de alcance por `CLAUDE.md`/ADR-003/ADR-004.

## 17. Recomendación final

Adoptar el modelo de tres entidades nuevas (`PERSON`, `PROJECT`, `COMMITMENT`) más extensiones de `REMINDER` (§4, ADR-016) como base del módulo Laboral, con `NOTE` como contenido subordinado ya reutilizado. No reemplaza `09-data-model.md`/ADR-015 — los extiende explícitamente (ver ADR-016, sección "Consecuencias"). Plan de implementación propuesto en `docs/development/08-laboral-module-plan.md`.
