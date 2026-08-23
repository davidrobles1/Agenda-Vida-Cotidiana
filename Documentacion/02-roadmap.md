# 02 — Roadmap por versiones

| Versión | Objetivo | Resultado |
|---|---|---|
| V1 | MVP | Aplicación funcional con cuenta + Home + tareas/recordatorios |
| V2 | Estabilidad | Producto confiable, seguro y medible |
| V3 | Evolución | Nuevas funciones + rendimiento + escalabilidad |
| V4 | Final | Alcance completo aprobado |
| Post-V4 | IA/Finanzas | Fases separadas, sujetas a validación |

## V1 — Definition of Success
- usuario puede registrarse/iniciar sesión en Android, iOS y Web;
- puede crear y completar un recordatorio;
- puede compartir un recordatorio con una o varias personas mediante invitación, y estas pueden aceptar/rechazar/ver/completar según su permiso (ver ADR-006);
- recibe notificaciones locales y push para recordatorios propios y compartidos (ver ADR-007);
- datos quedan aislados por usuario y por relación de compartición explícita (owner vs. colaborador);
- aplicación maneja errores de red;
- backend tiene pruebas automatizadas;
- CI bloquea cambios que fallen compilación/pruebas;
- no existen secretos en repositorio;
- OpenAPI está versionado.

## V3 — Decisiones ya aprobadas, implementación adelantada en paralelo con V2
**DECISION (ADR-015, 2026-08-18):** navegación dual Personal/Laboral con selector superior condicional (Calendario + modos habilitados), calendario general agregado (día/semana/mes) y calendario propio por modo. Ver `22-decision-log.md` ADR-015, `03-prd.md` FR-014 a FR-019.

**Actualización (2026-08-18, mismo día):** el Product Owner decidió adelantar la implementación para avanzar **en paralelo** con el trabajo de Calendario de V2 (vistas Semana/Día, backend de Garantías/Mantenimiento), no después de cerrar V2. La etiqueta de versión (V3) se conserva sin cambios; solo cambió el orden de ejecución. Sigue sin priorizarse sobre CI real ni DEC-009, que no tienen relación de dependencia con este trabajo.

**DECISION (ADR-016, 2026-08-22):** el modo Laboral (ADR-015) evoluciona de una simple etiqueta de contexto a un espacio profesional con Personas, Proyectos y Compromisos (Seguimientos y Esperando unificados en una sola entidad), útil para cualquier profesión sin plantillas por profesión y sin convertirse en CRM/ERP/Jira/Notion. Núcleo en **V3** (FR-021 a FR-028): Personas, Proyectos, vínculo de Tarea con Persona/Proyecto, reuniones (ubicación + participantes), Compromisos, vistas Hoy/Seguimientos, Inbox. Ver `22-decision-log.md` ADR-016 y `34-laboral-module-proposal.md` para el análisis completo, y `docs/development/08-laboral-module-plan.md` para el plan de trabajo.

## V4 — Candidatos identificados (no comprometidos, `RECOMMENDATION`)
Notas y Documentos vinculados a Persona/Proyecto, "última interacción" derivada automáticamente, automatizaciones simples deterministas (vencimiento → Hoy; nota de reunión → sugerencia de tarea), Objetivos/Rutinas/Lugares/Recursos, vista Kanban de Proyecto — ver `34-laboral-module-proposal.md` §14. Ninguno de estos está aprobado todavía como alcance de V4; se documentan aquí como candidatos para no perderlos, no como compromiso.

## Post-V4 — Explícitamente fuera de alcance hasta validación
CRM avanzado/pipeline de ventas con etapas, gestión de casos legales, gestión de obra con planos, historiales clínicos, automatización proactiva basada en patrones, asistente conversacional/IA — ver `34-laboral-module-proposal.md` §16. Coherente con la prohibición de IA/Finanzas de `CLAUDE.md` y `AI-CONTEXT.md`.

## Regla de evolución
Una versión no debe requerir rehacer el dominio anterior. Los módulos se agregan detrás de contratos estables.
