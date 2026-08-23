import type { ComponentType, SVGProps } from 'react'
import {
  AlarmClock,
  Apple,
  Award,
  Backpack,
  Baby,
  Beef,
  Bike,
  Brush,
  Cake,
  CalendarCheck,
  Camera,
  Car,
  Clapperboard,
  Clock,
  Clock3,
  CloudSun,
  Coffee,
  Coins,
  Compass,
  CreditCard,
  Crown,
  CupSoda,
  Diamond,
  Dices,
  Disc3,
  DollarSign,
  Droplet,
  Film,
  Flag,
  Flame,
  Flower,
  Flower2,
  Fuel,
  Gamepad2,
  Gauge,
  Gem,
  Gift,
  Globe,
  Guitar,
  Headphones,
  Heart,
  HeartHandshake,
  HeartPulse,
  Home,
  Hourglass,
  House,
  IceCream,
  Joystick,
  Leaf,
  Lightbulb,
  ListChecks,
  Luggage,
  Map,
  MapPin,
  Medal,
  Mic2,
  Moon,
  Mountain,
  Music,
  Music2,
  Palmtree,
  Palette,
  PartyPopper,
  PenTool,
  PiggyBank,
  Pizza,
  Plane,
  Popcorn,
  Puzzle,
  Rocket,
  Sailboat,
  Salad,
  Scissors,
  Ship,
  Smile,
  Soup,
  Sparkle,
  Sparkles,
  Star,
  Sun,
  Swords,
  Target,
  Tent,
  Ticket,
  Timer,
  TrendingUp,
  Trophy,
  Umbrella,
  UsersRound,
  UtensilsCrossed,
  Video,
  Wallet,
  Watch,
  Waves,
  Wine,
  Zap,
} from 'lucide-react'

/**
 * BLOQUE D (post-MVP) — "😊 Emojis" catalog, explicitly separate from
 * STICKER_OPTIONS (core/ui/pickers/pickerCatalog.ts, shared with Notes/
 * Agenda): the phase asks for it "dentro de Elementos" of the Vision
 * Board specifically, not as a change to that other, already-shared
 * catalog — kept local to this feature.
 *
 * "No usar emojis Unicode como contenido principal... crear un catálogo
 * interno propio" — every entry here is a real `lucide-react` component
 * (already a project dependency, ISC-licensed) inside a colored circular
 * badge, the exact same technique STICKER_OPTIONS' own expansion just
 * added — never a Unicode emoji character. ASSUMPTION: "estilo
 * consistente" is satisfied by that one shared badge treatment (color +
 * icon), not by every icon sharing a single hand-drawn illustration style
 * (no such asset set exists to draw from — see this file's sibling doc
 * comment in pickerCatalog.ts for the same constraint).
 *
 * An "emoji" is stored as a STICKER-type element (VisionBoardElementType
 * never gained a 4th "EMOJI" value — no backend change needed) with
 * `data.emojiId` instead of `data.stickerId`; VisionBoardElementView.tsx's
 * STICKER case checks `emojiId` first. Behaves as a real board element in
 * every way STICKER already does (drag/resize/rotate/layers/autosave/undo/
 * duplicate) because it *is* one.
 */
export interface VisionBoardEmojiOption {
  id: string
  label: string
  category: string
  Icon: ComponentType<SVGProps<SVGSVGElement>>
  color: string
}

const CATEGORY_COLORS: Record<string, string> = {
  Viajes: '#38bdf8',
  Fiesta: '#ec4899',
  'Autos deportivos': '#ef4444',
  Lujo: '#a78bfa',
  Relojes: '#f472b6',
  Vacaciones: '#fb923c',
  Café: '#a16207',
  Creatividad: '#f97316',
  Productividad: '#0ea5e9',
  Dinero: '#059669',
  Metas: '#f59e0b',
  Amor: '#e11d48',
  Familia: '#8b5cf6',
  Bienestar: '#22c55e',
  Comida: '#f43f5e',
  Aventura: '#65a30d',
  Naturaleza: '#16a34a',
  Gaming: '#6366f1',
  Música: '#db2777',
  Cine: '#334155',
}

function entry(id: string, label: string, category: string, Icon: ComponentType<SVGProps<SVGSVGElement>>): VisionBoardEmojiOption {
  return { id, label, category, Icon, color: CATEGORY_COLORS[category] }
}

export const EMOJI_CATALOG: VisionBoardEmojiOption[] = [
  // Viajes
  entry('trip-plane', 'Avión', 'Viajes', Plane),
  entry('trip-pin', 'Destino', 'Viajes', MapPin),
  entry('trip-compass', 'Brújula', 'Viajes', Compass),
  entry('trip-luggage', 'Equipaje', 'Viajes', Luggage),
  entry('trip-globe', 'Mundo', 'Viajes', Globe),
  entry('trip-ship', 'Crucero', 'Viajes', Ship),
  // Fiesta
  entry('party-popper', 'Celebración', 'Fiesta', PartyPopper),
  entry('party-cake', 'Pastel', 'Fiesta', Cake),
  entry('party-gift', 'Regalo', 'Fiesta', Gift),
  entry('party-sparkles', 'Chispas', 'Fiesta', Sparkles),
  entry('party-music', 'Fiesta musical', 'Fiesta', Music2),
  entry('party-disc', 'DJ', 'Fiesta', Disc3),
  // Autos deportivos
  entry('car-sport', 'Auto deportivo', 'Autos deportivos', Car),
  entry('car-gauge', 'Velocímetro', 'Autos deportivos', Gauge),
  entry('car-fuel', 'Gasolina', 'Autos deportivos', Fuel),
  entry('car-flag', 'Bandera de meta', 'Autos deportivos', Flag),
  entry('car-rocket', 'Aceleración', 'Autos deportivos', Rocket),
  entry('car-bike', 'Moto', 'Autos deportivos', Bike),
  // Lujo
  entry('luxury-gem', 'Gema', 'Lujo', Gem),
  entry('luxury-crown', 'Corona', 'Lujo', Crown),
  entry('luxury-diamond', 'Diamante', 'Lujo', Diamond),
  entry('luxury-star', 'Estrella', 'Lujo', Star),
  entry('luxury-award', 'Distinción', 'Lujo', Award),
  entry('luxury-sparkle', 'Brillo', 'Lujo', Sparkle),
  // Relojes
  entry('watch-classic', 'Reloj', 'Relojes', Watch),
  entry('watch-clock', 'Hora', 'Relojes', Clock),
  entry('watch-timer', 'Cronómetro', 'Relojes', Timer),
  entry('watch-alarm', 'Alarma', 'Relojes', AlarmClock),
  entry('watch-hourglass', 'Cuenta regresiva', 'Relojes', Hourglass),
  entry('watch-clock3', 'Reloj de pared', 'Relojes', Clock3),
  // Vacaciones
  entry('vacation-umbrella', 'Playa', 'Vacaciones', Umbrella),
  entry('vacation-palm', 'Palmera', 'Vacaciones', Palmtree),
  entry('vacation-sun', 'Sol', 'Vacaciones', Sun),
  entry('vacation-waves', 'Olas', 'Vacaciones', Waves),
  entry('vacation-sailboat', 'Velero', 'Vacaciones', Sailboat),
  entry('vacation-backpack', 'Mochila', 'Vacaciones', Backpack),
  // Café
  entry('coffee-cup', 'Café', 'Café', Coffee),
  entry('coffee-soda', 'Bebida', 'Café', CupSoda),
  entry('coffee-cookie', 'Galleta', 'Café', Sparkle),
  entry('coffee-croissant', 'Desayuno', 'Café', Soup),
  entry('coffee-wine', 'Copa', 'Café', Wine),
  entry('coffee-icecream', 'Postre', 'Café', IceCream),
  // Creatividad
  entry('creative-palette', 'Paleta', 'Creatividad', Palette),
  entry('creative-brush', 'Pincel', 'Creatividad', Brush),
  entry('creative-pen', 'Diseño', 'Creatividad', PenTool),
  entry('creative-bulb', 'Idea', 'Creatividad', Lightbulb),
  entry('creative-camera', 'Fotografía', 'Creatividad', Camera),
  entry('creative-scissors', 'Recorte', 'Creatividad', Scissors),
  // Productividad
  entry('productivity-check', 'Completado', 'Productividad', ListChecks),
  entry('productivity-target', 'Objetivo', 'Productividad', Target),
  entry('productivity-calendar', 'Planificación', 'Productividad', CalendarCheck),
  entry('productivity-zap', 'Energía', 'Productividad', Zap),
  entry('productivity-rocket', 'Impulso', 'Productividad', Rocket),
  entry('productivity-bulb', 'Enfoque', 'Productividad', Lightbulb),
  // Dinero
  entry('money-wallet', 'Billetera', 'Dinero', Wallet),
  entry('money-coins', 'Monedas', 'Dinero', Coins),
  entry('money-piggy', 'Ahorro', 'Dinero', PiggyBank),
  entry('money-card', 'Tarjeta', 'Dinero', CreditCard),
  entry('money-trend', 'Crecimiento', 'Dinero', TrendingUp),
  entry('money-dollar', 'Dólar', 'Dinero', DollarSign),
  // Metas
  entry('goals-target', 'Meta', 'Metas', Target),
  entry('goals-flag', 'Logro', 'Metas', Flag),
  entry('goals-trophy', 'Trofeo', 'Metas', Trophy),
  entry('goals-rocket', 'Despegue', 'Metas', Rocket),
  entry('goals-mountain', 'Cumbre', 'Metas', Mountain),
  entry('goals-medal', 'Medalla', 'Metas', Medal),
  // Amor
  entry('love-heart', 'Corazón', 'Amor', Heart),
  entry('love-handshake', 'Vínculo', 'Amor', HeartHandshake),
  entry('love-flower', 'Flor', 'Amor', Flower2),
  entry('love-gift', 'Detalle', 'Amor', Gift),
  entry('love-smile', 'Sonrisa', 'Amor', Smile),
  entry('love-star', 'Especial', 'Amor', Star),
  // Familia
  entry('family-users', 'Familia', 'Familia', UsersRound),
  entry('family-home', 'Hogar', 'Familia', Home),
  entry('family-baby', 'Bebé', 'Familia', Baby),
  entry('family-house', 'Casa', 'Familia', House),
  entry('family-heart', 'Cariño', 'Familia', Heart),
  entry('family-users2', 'Reunión', 'Familia', UsersRound),
  // Bienestar
  entry('wellness-pulse', 'Salud', 'Bienestar', HeartPulse),
  entry('wellness-leaf', 'Equilibrio', 'Bienestar', Leaf),
  entry('wellness-sun', 'Energía positiva', 'Bienestar', Sun),
  entry('wellness-moon', 'Descanso', 'Bienestar', Moon),
  entry('wellness-drop', 'Hidratación', 'Bienestar', Droplet),
  entry('wellness-flower', 'Bienestar', 'Bienestar', Flower),
  // Comida
  entry('food-utensils', 'Comida', 'Comida', UtensilsCrossed),
  entry('food-pizza', 'Pizza', 'Comida', Pizza),
  entry('food-icecream', 'Helado', 'Comida', IceCream),
  entry('food-apple', 'Fruta', 'Comida', Apple),
  entry('food-salad', 'Ensalada', 'Comida', Salad),
  entry('food-beef', 'Carne', 'Comida', Beef),
  // Aventura
  entry('adventure-mountain', 'Montaña', 'Aventura', Mountain),
  entry('adventure-compass', 'Exploración', 'Aventura', Compass),
  entry('adventure-tent', 'Campamento', 'Aventura', Tent),
  entry('adventure-backpack', 'Mochila', 'Aventura', Backpack),
  entry('adventure-map', 'Mapa', 'Aventura', Map),
  entry('adventure-flame', 'Fogata', 'Aventura', Flame),
  // Naturaleza
  entry('nature-trees', 'Árboles', 'Naturaleza', Flower),
  entry('nature-leaf', 'Hoja', 'Naturaleza', Leaf),
  entry('nature-flower', 'Flor', 'Naturaleza', Flower2),
  entry('nature-mountain', 'Paisaje', 'Naturaleza', Mountain),
  entry('nature-sun', 'Sol', 'Naturaleza', Sun),
  entry('nature-cloud', 'Cielo', 'Naturaleza', CloudSun),
  // Gaming
  entry('gaming-pad', 'Control', 'Gaming', Gamepad2),
  entry('gaming-joystick', 'Joystick', 'Gaming', Joystick),
  entry('gaming-dice', 'Dados', 'Gaming', Dices),
  entry('gaming-trophy', 'Victoria', 'Gaming', Trophy),
  entry('gaming-puzzle', 'Puzzle', 'Gaming', Puzzle),
  entry('gaming-swords', 'Batalla', 'Gaming', Swords),
  // Música
  entry('music-note', 'Música', 'Música', Music),
  entry('music-note2', 'Melodía', 'Música', Music2),
  entry('music-headphones', 'Audífonos', 'Música', Headphones),
  entry('music-mic', 'Micrófono', 'Música', Mic2),
  entry('music-guitar', 'Guitarra', 'Música', Guitar),
  entry('music-disc', 'Disco', 'Música', Disc3),
  // Cine
  entry('cinema-clapper', 'Claqueta', 'Cine', Clapperboard),
  entry('cinema-film', 'Película', 'Cine', Film),
  entry('cinema-popcorn', 'Palomitas', 'Cine', Popcorn),
  entry('cinema-ticket', 'Boleto', 'Cine', Ticket),
  entry('cinema-video', 'Video', 'Cine', Video),
  entry('cinema-camera', 'Cámara', 'Cine', Camera),
]

export function findEmojiOption(emojiId: string | undefined): VisionBoardEmojiOption | undefined {
  if (!emojiId) return undefined
  return EMOJI_CATALOG.find((option) => option.id === emojiId)
}

export const EMOJI_CATEGORIES: string[] = Array.from(new Set(EMOJI_CATALOG.map((option) => option.category)))
