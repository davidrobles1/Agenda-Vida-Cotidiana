import type { ComponentType, SVGProps } from 'react'
import {
  AlertCircle,
  Bell,
  Briefcase,
  Cake,
  CalendarDays,
  CheckCircle2,
  Folder,
  GraduationCap,
  HeartPulse,
  Home,
  ListTodo,
  PartyPopper,
  PencilLine,
  Plane,
  Shield,
  ShoppingCart,
  Smile,
  Sticker,
  UtensilsCrossed,
  Users,
  Wrench,
  // BLOQUE D (post-MVP): expanded sticker catalog — new icon-badge entries
  // (see StickerOption's own doc comment below on why these are lucide
  // icons, not more downloaded Fluent Emoji SVG files).
  Palmtree,
  Gem,
  Watch,
  Umbrella,
  Coffee,
  Dumbbell,
  PawPrint,
  Trees,
  Wallet,
  Cpu,
  Music,
  Clapperboard,
  Trophy,
  Sofa,
} from 'lucide-react'

import celebrationSticker from './assets/stickers/celebration.svg'
import workSticker from './assets/stickers/work.svg'
import studySticker from './assets/stickers/study.svg'
import foodSticker from './assets/stickers/food.svg'
import travelSticker from './assets/stickers/travel.svg'
import wellnessSticker from './assets/stickers/wellness.svg'
import reminderSticker from './assets/stickers/reminder.svg'
import shoppingSticker from './assets/stickers/shopping.svg'
import birthdaySticker from './assets/stickers/birthday.svg'
import homeSticker from './assets/stickers/home.svg'
import eventSticker from './assets/stickers/event.svg'
import emotionSticker from './assets/stickers/emotion.svg'

/**
 * Catálogo compartido de Icono/Emoji-trigger/Sticker — usado por Notas
 * (`features/calendar/notes/`) y Agenda (`features/calendar/reminders*` /
 * `CalendarPage.tsx`). Antes vivía dentro de `notes/notesData.ts`; movido
 * aquí porque ahora dos dominios reales lo consumen (no dos copias).
 *
 * `lucide-react` (ISC) para iconos — nombres verificados contra los
 * archivos reales de la versión instalada, no contra documentación.
 * `core/ui/icons.tsx` NO se toca: sigue siendo la fuente para el resto de
 * la app (sidebar, topBar, etc.); este catálogo es exclusivo de Icono/
 * Emoji/Sticker en Agenda y Notas.
 */
export const ICON_OPTIONS: Array<{
  id: string
  label: string
  Icon: ComponentType<SVGProps<SVGSVGElement>>
}> = [
  { id: 'home', label: 'Hogar', Icon: Home },
  { id: 'tasks', label: 'Tareas', Icon: ListTodo },
  { id: 'calendar', label: 'Calendario', Icon: CalendarDays },
  { id: 'bell', label: 'Recordatorio', Icon: Bell },
  { id: 'alert', label: 'Importante', Icon: AlertCircle },
  { id: 'check', label: 'Hecho', Icon: CheckCircle2 },
  { id: 'folder', label: 'Documento', Icon: Folder },
  { id: 'shield', label: 'Garantía', Icon: Shield },
  { id: 'wrench', label: 'Mantenimiento', Icon: Wrench },
  { id: 'users', label: 'Familia', Icon: Users },
  { id: 'celebration', label: 'Celebración', Icon: PartyPopper },
  { id: 'work', label: 'Trabajo', Icon: Briefcase },
  { id: 'study', label: 'Estudio', Icon: GraduationCap },
  { id: 'food', label: 'Comida', Icon: UtensilsCrossed },
  { id: 'travel', label: 'Viaje', Icon: Plane },
  { id: 'wellness', label: 'Salud', Icon: HeartPulse },
  { id: 'shopping', label: 'Compras', Icon: ShoppingCart },
  { id: 'birthday', label: 'Cumpleaños', Icon: Cake },
  { id: 'emotion', label: 'Emociones', Icon: Smile },
]

export function findIconOption(iconId: string | undefined) {
  if (!iconId) return undefined
  return ICON_OPTIONS.find((option) => option.id === iconId)
}

/** Icono del botón que abre `EmojiPickerButton`. */
export const EMOJI_TRIGGER_ICON = Smile

/** Icono decorativo junto al campo "Sticker" (no es trigger de Popover — el
    catálogo es una grilla inline, ver `StickerPicker.tsx`). */
export const STICKER_FIELD_ICON = Sticker

/** Icono del botón "Editar" en los Drawer de Notas/Agenda. No existía
    ningún icono de "editar/lápiz" en `core/ui/icons.tsx` (set completo
    revisado) — gap real, no una duplicación. */
export const EDIT_ICON = PencilLine

/**
 * Stickers — Microsoft Fluent Emoji (MIT, repo completo incluidos los
 * assets). Fuente: https://github.com/microsoft/fluentui-emoji, estilo
 * "Flat" (SVG), descargados individualmente (no se clonó el repo).
 *
 * `id` es estable y NO depende del nombre físico del archivo — el
 * `stickerId` de una Nota o un Reminder guarda solo el `id`, nunca la
 * ruta. `asset` puede cambiar sin romper ningún registro ya guardado.
 */
export interface StickerOption {
  id: string
  label: string
  /** Existing Fluent Emoji SVG entries (unchanged, real asset files). */
  asset?: string
  /** BLOQUE D (post-MVP) expansion — `Icon`+`color` is the alternative to
      `asset`: no more individual Fluent Emoji SVGs were downloaded (this
      catalog's own doc comment above already flags that as a one-by-one,
      hand-picked process), so the wider catalog reuses `lucide-react`
      (already a real dependency, ISC-licensed, the same library
      `ICON_OPTIONS` above already draws from) rendered inside a colored
      circular badge (StickerPicker.tsx / VisionBoardElementView.tsx) for a
      consistent "sticker" look, instead of a bare line icon. */
  Icon?: ComponentType<SVGProps<SVGSVGElement>>
  color?: string
}

export const STICKER_OPTIONS: StickerOption[] = [
  { id: 'celebration', label: 'Celebración', asset: celebrationSticker },
  { id: 'work', label: 'Trabajo', asset: workSticker },
  { id: 'study', label: 'Estudio', asset: studySticker },
  { id: 'food', label: 'Comida', asset: foodSticker },
  { id: 'travel', label: 'Viaje', asset: travelSticker },
  { id: 'wellness', label: 'Salud', asset: wellnessSticker },
  { id: 'reminder', label: 'Recordatorio', asset: reminderSticker },
  { id: 'shopping', label: 'Compras', asset: shoppingSticker },
  { id: 'birthday', label: 'Cumpleaños', asset: birthdaySticker },
  { id: 'home', label: 'Hogar', asset: homeSticker },
  { id: 'event', label: 'Eventos', asset: eventSticker },
  { id: 'emotion', label: 'Emociones', asset: emotionSticker },
  // BLOQUE D (post-MVP): expanded catalog, one per named category —
  // icon-badge entries (see StickerOption's own doc comment above).
  { id: 'trip', label: 'Viajes', Icon: Palmtree, color: '#38bdf8' },
  { id: 'luxury', label: 'Lujo', Icon: Gem, color: '#a78bfa' },
  { id: 'watch', label: 'Relojes', Icon: Watch, color: '#f472b6' },
  { id: 'beach', label: 'Vacaciones', Icon: Umbrella, color: '#fb923c' },
  { id: 'coffee', label: 'Café', Icon: Coffee, color: '#a16207' },
  { id: 'fitness', label: 'Fitness', Icon: Dumbbell, color: '#22c55e' },
  { id: 'pets', label: 'Mascotas', Icon: PawPrint, color: '#eab308' },
  { id: 'nature', label: 'Naturaleza', Icon: Trees, color: '#16a34a' },
  { id: 'money', label: 'Dinero', Icon: Wallet, color: '#059669' },
  { id: 'tech', label: 'Tecnología', Icon: Cpu, color: '#6366f1' },
  { id: 'music', label: 'Música', Icon: Music, color: '#ec4899' },
  { id: 'cinema', label: 'Cine', Icon: Clapperboard, color: '#334155' },
  { id: 'sports', label: 'Deportes', Icon: Trophy, color: '#f59e0b' },
  { id: 'household', label: 'Hogar (mueble)', Icon: Sofa, color: '#92400e' },
]

/** Resuelve un `stickerId` guardado contra el catálogo actual — `undefined`
    de forma segura si el id no existe (sticker eliminado del catálogo,
    dato corrupto, etc.), nunca lanza. */
export function findStickerOption(stickerId: string | undefined): StickerOption | undefined {
  if (!stickerId) return undefined
  return STICKER_OPTIONS.find((option) => option.id === stickerId)
}
