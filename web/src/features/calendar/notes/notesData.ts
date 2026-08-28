/**
 * Modelo de una Nota — real, persistido en backend (`note` module,
 * mismo patrón que `warranty`/`reminder`). Los catálogos de Icono/Emoji/
 * Sticker viven en `core/ui/pickers/pickerCatalog.ts` — compartidos con
 * Agenda, ya no exclusivos de Notas.
 */
export interface Note {
  id: string
  ownerUserId: string
  title: string
  description: string
  /** Independiente del texto de `description` — ver `ICON_OPTIONS` en
      `core/ui/pickers/pickerCatalog.ts`. */
  iconId?: string
  /** Independiente de `description` — ver `STICKER_OPTIONS`. */
  stickerId?: string
  /** ADR-016 Fase 3a/FR-029 (candidato V4, Módulo Laboral) — vínculo opcional a Persona/Proyecto. Personal/Agenda lo ignoran sin problema. */
  personId?: string
  projectId?: string
  /** ADR-016 Fase 3d/FR-035 (candidato V4) — true si el usuario ya convirtió
      o descartó la sugerencia de tarea de esta nota; entonces deja de
      ofrecerse. Solo lo usa el Módulo Laboral; Personal/Agenda lo ignoran. */
  taskSuggestionResolved?: boolean
  version: number
  createdAt: string
  updatedAt: string
}
