package com.vidacotidiana.app.feature.board

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BeachAccess
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Chair
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.Cottage
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DesignServices
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Hiking
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalFlorist
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.Luggage
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.MusicVideo
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RamenDining
import androidx.compose.material.icons.outlined.Redeem
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Rocket
import androidx.compose.material.icons.outlined.Sailing
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Theaters
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.Umbrella
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Los 121 emojis y los 19 stickers del Vision Board.
 *
 * NO son caracteres Unicode: en la Web cada «emoji» es un icono de trazo con
 * el color de su categoría, y aquí es el icono equivalente de Material. Lo
 * que importa —y lo que se respeta uno a uno— es el `id`: un emoji colocado
 * en el móvil guarda `data.emojiId`, y el navegador lo busca en SU catálogo
 * por ese mismo id. Cambiar uno rompería el elemento al abrirlo en la Web.
 *
 * El icono es, por tanto, lo único que puede diferir entre plataformas, y se
 * eligió en cada caso el de Material que dice lo mismo que el de la Web.
 */

data class VisionBoardEmoji(
    val id: String,
    val label: String,
    val category: String,
    val icon: ImageVector,
    val color: Color,
)

private val CATEGORY_COLORS = mapOf(
    "Viajes" to Color(0xFF38BDF8),
    "Fiesta" to Color(0xFFEC4899),
    "Autos deportivos" to Color(0xFFEF4444),
    "Lujo" to Color(0xFFA78BFA),
    "Relojes" to Color(0xFFF472B6),
    "Vacaciones" to Color(0xFFFB923C),
    "Café" to Color(0xFFA16207),
    "Creatividad" to Color(0xFFF97316),
    "Productividad" to Color(0xFF0EA5E9),
    "Dinero" to Color(0xFF059669),
    "Metas" to Color(0xFFF59E0B),
    "Amor" to Color(0xFFE11D48),
    "Familia" to Color(0xFF8B5CF6),
    "Bienestar" to Color(0xFF22C55E),
    "Comida" to Color(0xFFF43F5E),
    "Aventura" to Color(0xFF65A30D),
    "Naturaleza" to Color(0xFF16A34A),
    "Gaming" to Color(0xFF6366F1),
    "Música" to Color(0xFFDB2777),
    "Cine" to Color(0xFF334155),
)

private fun e(id: String, label: String, category: String, icon: ImageVector) =
    VisionBoardEmoji(id, label, category, icon, CATEGORY_COLORS[category] ?: Color(0xFF6B7280))

val EMOJI_CATALOG: List<VisionBoardEmoji> = listOf(
    // Viajes
    e("trip-plane", "Avión", "Viajes", Icons.Outlined.Flight),
    e("trip-pin", "Destino", "Viajes", Icons.Outlined.Place),
    e("trip-compass", "Brújula", "Viajes", Icons.Outlined.Explore),
    e("trip-luggage", "Equipaje", "Viajes", Icons.Outlined.Luggage),
    e("trip-globe", "Mundo", "Viajes", Icons.Outlined.Public),
    e("trip-ship", "Crucero", "Viajes", Icons.Outlined.DirectionsBoat),
    // Fiesta
    e("party-popper", "Celebración", "Fiesta", Icons.Outlined.Celebration),
    e("party-cake", "Pastel", "Fiesta", Icons.Outlined.Cake),
    e("party-gift", "Regalo", "Fiesta", Icons.Outlined.CardGiftcard),
    e("party-sparkles", "Chispas", "Fiesta", Icons.Outlined.AutoAwesome),
    e("party-music", "Fiesta musical", "Fiesta", Icons.Outlined.MusicNote),
    e("party-disc", "DJ", "Fiesta", Icons.Outlined.Album),
    // Autos deportivos
    e("car-sport", "Auto deportivo", "Autos deportivos", Icons.Outlined.DirectionsCar),
    e("car-gauge", "Velocímetro", "Autos deportivos", Icons.Outlined.Speed),
    e("car-fuel", "Gasolina", "Autos deportivos", Icons.Outlined.LocalDrink),
    e("car-flag", "Bandera de meta", "Autos deportivos", Icons.Outlined.Flag),
    e("car-rocket", "Aceleración", "Autos deportivos", Icons.Outlined.Rocket),
    e("car-bike", "Moto", "Autos deportivos", Icons.Outlined.TwoWheeler),
    // Lujo
    e("luxury-gem", "Gema", "Lujo", Icons.Outlined.Diamond),
    e("luxury-crown", "Corona", "Lujo", Icons.Outlined.WorkspacePremium),
    e("luxury-diamond", "Diamante", "Lujo", Icons.Outlined.Diamond),
    e("luxury-star", "Estrella", "Lujo", Icons.Outlined.Star),
    e("luxury-award", "Distinción", "Lujo", Icons.Outlined.EmojiEvents),
    e("luxury-sparkle", "Brillo", "Lujo", Icons.Outlined.AutoAwesome),
    // Relojes
    e("watch-classic", "Reloj", "Relojes", Icons.Outlined.Watch),
    e("watch-clock", "Hora", "Relojes", Icons.Outlined.Schedule),
    e("watch-timer", "Cronómetro", "Relojes", Icons.Outlined.Timer),
    e("watch-alarm", "Alarma", "Relojes", Icons.Outlined.Alarm),
    e("watch-hourglass", "Cuenta regresiva", "Relojes", Icons.Outlined.HourglassEmpty),
    e("watch-clock3", "Reloj de pared", "Relojes", Icons.Outlined.AccessTime),
    // Vacaciones
    e("vacation-umbrella", "Playa", "Vacaciones", Icons.Outlined.Umbrella),
    e("vacation-palm", "Palmera", "Vacaciones", Icons.Outlined.BeachAccess),
    e("vacation-sun", "Sol", "Vacaciones", Icons.Outlined.WbSunny),
    e("vacation-waves", "Olas", "Vacaciones", Icons.Outlined.Waves),
    e("vacation-sailboat", "Velero", "Vacaciones", Icons.Outlined.Sailing),
    e("vacation-backpack", "Mochila", "Vacaciones", Icons.Outlined.Hiking),
    // Café
    e("coffee-cup", "Café", "Café", Icons.Outlined.LocalCafe),
    e("coffee-soda", "Bebida", "Café", Icons.Outlined.LocalDrink),
    e("coffee-cookie", "Galleta", "Café", Icons.Outlined.Cookie),
    e("coffee-croissant", "Desayuno", "Café", Icons.Outlined.RamenDining),
    e("coffee-wine", "Copa", "Café", Icons.Outlined.LocalDrink),
    e("coffee-icecream", "Postre", "Café", Icons.Outlined.Icecream),
    // Creatividad
    e("creative-palette", "Paleta", "Creatividad", Icons.Outlined.Palette),
    e("creative-brush", "Pincel", "Creatividad", Icons.Outlined.Brush),
    e("creative-pen", "Diseño", "Creatividad", Icons.Outlined.DesignServices),
    e("creative-bulb", "Idea", "Creatividad", Icons.Outlined.Lightbulb),
    e("creative-camera", "Fotografía", "Creatividad", Icons.Outlined.PhotoCamera),
    e("creative-scissors", "Recorte", "Creatividad", Icons.Outlined.ContentCut),
    // Productividad
    e("productivity-check", "Completado", "Productividad", Icons.Outlined.CheckCircle),
    e("productivity-target", "Objetivo", "Productividad", Icons.Outlined.TrackChanges),
    e("productivity-calendar", "Planificación", "Productividad", Icons.Outlined.EventAvailable),
    e("productivity-zap", "Energía", "Productividad", Icons.Outlined.Bolt),
    e("productivity-rocket", "Impulso", "Productividad", Icons.Outlined.Rocket),
    e("productivity-bulb", "Enfoque", "Productividad", Icons.Outlined.Lightbulb),
    // Dinero
    e("money-wallet", "Billetera", "Dinero", Icons.Outlined.AccountBalanceWallet),
    e("money-coins", "Monedas", "Dinero", Icons.Outlined.Savings),
    e("money-piggy", "Ahorro", "Dinero", Icons.Outlined.Savings),
    e("money-card", "Tarjeta", "Dinero", Icons.Outlined.CreditCard),
    e("money-trend", "Crecimiento", "Dinero", Icons.Outlined.TrendingUp),
    e("money-dollar", "Dólar", "Dinero", Icons.Outlined.AttachMoney),
    // Metas
    e("goals-target", "Meta", "Metas", Icons.Outlined.TrackChanges),
    e("goals-flag", "Logro", "Metas", Icons.Outlined.Flag),
    e("goals-trophy", "Trofeo", "Metas", Icons.Outlined.EmojiEvents),
    e("goals-rocket", "Despegue", "Metas", Icons.Outlined.Rocket),
    e("goals-mountain", "Cumbre", "Metas", Icons.Outlined.Terrain),
    e("goals-medal", "Medalla", "Metas", Icons.Outlined.MilitaryTech),
    // Amor
    e("love-heart", "Corazón", "Amor", Icons.Outlined.Favorite),
    e("love-handshake", "Vínculo", "Amor", Icons.Outlined.VolunteerActivism),
    e("love-flower", "Flor", "Amor", Icons.Outlined.LocalFlorist),
    e("love-gift", "Detalle", "Amor", Icons.Outlined.Redeem),
    e("love-smile", "Sonrisa", "Amor", Icons.Outlined.SentimentSatisfied),
    e("love-star", "Especial", "Amor", Icons.Outlined.Star),
    // Familia
    e("family-users", "Familia", "Familia", Icons.Outlined.Groups),
    e("family-home", "Hogar", "Familia", Icons.Outlined.Home),
    e("family-baby", "Bebé", "Familia", Icons.Outlined.ChildCare),
    e("family-house", "Casa", "Familia", Icons.Outlined.Cottage),
    e("family-heart", "Cariño", "Familia", Icons.Outlined.Favorite),
    e("family-users2", "Reunión", "Familia", Icons.Outlined.Groups),
    // Bienestar
    e("wellness-pulse", "Salud", "Bienestar", Icons.Outlined.MonitorHeart),
    e("wellness-leaf", "Equilibrio", "Bienestar", Icons.Outlined.Spa),
    e("wellness-sun", "Energía positiva", "Bienestar", Icons.Outlined.WbSunny),
    e("wellness-moon", "Descanso", "Bienestar", Icons.Outlined.Nightlight),
    e("wellness-drop", "Hidratación", "Bienestar", Icons.Outlined.WaterDrop),
    e("wellness-flower", "Bienestar", "Bienestar", Icons.Outlined.LocalFlorist),
    // Comida
    e("food-utensils", "Comida", "Comida", Icons.Outlined.Restaurant),
    e("food-pizza", "Pizza", "Comida", Icons.Outlined.LocalPizza),
    e("food-icecream", "Helado", "Comida", Icons.Outlined.Icecream),
    e("food-apple", "Fruta", "Comida", Icons.Outlined.LunchDining),
    e("food-salad", "Ensalada", "Comida", Icons.Outlined.RamenDining),
    e("food-beef", "Carne", "Comida", Icons.Outlined.LunchDining),
    // Aventura
    e("adventure-mountain", "Montaña", "Aventura", Icons.Outlined.Terrain),
    e("adventure-compass", "Exploración", "Aventura", Icons.Outlined.Explore),
    e("adventure-tent", "Campamento", "Aventura", Icons.Outlined.Park),
    e("adventure-backpack", "Mochila", "Aventura", Icons.Outlined.Hiking),
    e("adventure-map", "Mapa", "Aventura", Icons.Outlined.Map),
    e("adventure-flame", "Fogata", "Aventura", Icons.Outlined.LocalFireDepartment),
    // Naturaleza
    e("nature-trees", "Árboles", "Naturaleza", Icons.Outlined.Park),
    e("nature-leaf", "Hoja", "Naturaleza", Icons.Outlined.Spa),
    e("nature-flower", "Flor", "Naturaleza", Icons.Outlined.LocalFlorist),
    e("nature-mountain", "Paisaje", "Naturaleza", Icons.Outlined.Terrain),
    e("nature-sun", "Sol", "Naturaleza", Icons.Outlined.WbSunny),
    e("nature-cloud", "Cielo", "Naturaleza", Icons.Outlined.Cloud),
    // Gaming
    e("gaming-pad", "Control", "Gaming", Icons.Outlined.SportsEsports),
    e("gaming-joystick", "Joystick", "Gaming", Icons.Outlined.SportsEsports),
    e("gaming-dice", "Dados", "Gaming", Icons.Outlined.Casino),
    e("gaming-trophy", "Victoria", "Gaming", Icons.Outlined.EmojiEvents),
    e("gaming-puzzle", "Puzzle", "Gaming", Icons.Outlined.Extension),
    e("gaming-swords", "Batalla", "Gaming", Icons.Outlined.MilitaryTech),
    // Música
    e("music-note", "Música", "Música", Icons.Outlined.MusicNote),
    e("music-note2", "Melodía", "Música", Icons.Outlined.MusicVideo),
    e("music-headphones", "Audífonos", "Música", Icons.Outlined.Headphones),
    e("music-mic", "Micrófono", "Música", Icons.Outlined.Mic),
    e("music-guitar", "Guitarra", "Música", Icons.Outlined.MusicNote),
    e("music-disc", "Disco", "Música", Icons.Outlined.Album),
    // Cine
    e("cinema-clapper", "Claqueta", "Cine", Icons.Outlined.Movie),
    e("cinema-film", "Película", "Cine", Icons.Outlined.Theaters),
    e("cinema-popcorn", "Palomitas", "Cine", Icons.Outlined.LocalDrink),
    e("cinema-ticket", "Boleto", "Cine", Icons.Outlined.CardGiftcard),
    e("cinema-video", "Video", "Cine", Icons.Outlined.Movie),
    e("cinema-camera", "Cámara", "Cine", Icons.Outlined.CameraAlt),
)

val EMOJI_CATEGORIES: List<String> = EMOJI_CATALOG.map { it.category }.distinct()

fun emojiOf(id: String?): VisionBoardEmoji? = EMOJI_CATALOG.firstOrNull { it.id == id }

/* =========================================================
   STICKERS  ·  data.stickerId
   ========================================================= */

data class VisionBoardSticker(val id: String, val label: String, val icon: ImageVector, val color: Color)

/**
 * Los veintiséis de `core/ui/pickers/pickerCatalog.ts`, con sus colores.
 *
 * La Web los dibuja como una INSIGNIA: el icono dentro de un círculo de
 * color, no un icono suelto — es lo que los hace verse como pegatinas y no
 * como iconos de interfaz. Aquí se hace igual (ver `StickerContent`).
 *
 * Los doce primeros son, en la Web, ilustraciones sueltas (Fluent Emoji);
 * aquí se representan con el icono de Material que dice lo mismo, dentro de
 * la misma insignia. El `id` —que es lo que viaja en `data.stickerId`— es
 * idéntico, así que un sticker puesto en el móvil sigue siendo el suyo al
 * abrir el tablero en el navegador.
 */
val STICKER_CATALOG = listOf(
    VisionBoardSticker("celebration", "Celebración", Icons.Outlined.Celebration, Color(0xFFEC4899)),
    VisionBoardSticker("work", "Trabajo", Icons.Outlined.Work, Color(0xFF0EA5E9)),
    VisionBoardSticker("study", "Estudio", Icons.Outlined.School, Color(0xFF6366F1)),
    VisionBoardSticker("food", "Comida", Icons.Outlined.Restaurant, Color(0xFFF43F5E)),
    VisionBoardSticker("travel", "Viaje", Icons.Outlined.Flight, Color(0xFF38BDF8)),
    VisionBoardSticker("wellness", "Salud", Icons.Outlined.MonitorHeart, Color(0xFF22C55E)),
    VisionBoardSticker("reminder", "Recordatorio", Icons.Outlined.Notifications, Color(0xFFF59E0B)),
    VisionBoardSticker("shopping", "Compras", Icons.Outlined.ShoppingCart, Color(0xFFA855F7)),
    VisionBoardSticker("birthday", "Cumpleaños", Icons.Outlined.Cake, Color(0xFFE11D48)),
    VisionBoardSticker("home", "Hogar", Icons.Outlined.Home, Color(0xFF8B5CF6)),
    VisionBoardSticker("event", "Eventos", Icons.Outlined.EventAvailable, Color(0xFF0891B2)),
    VisionBoardSticker("emotion", "Emociones", Icons.Outlined.SentimentSatisfied, Color(0xFFF97316)),
    VisionBoardSticker("trip", "Viajes", Icons.Outlined.BeachAccess, Color(0xFF38BDF8)),
    VisionBoardSticker("luxury", "Lujo", Icons.Outlined.Diamond, Color(0xFFA78BFA)),
    VisionBoardSticker("watch", "Relojes", Icons.Outlined.Watch, Color(0xFFF472B6)),
    VisionBoardSticker("beach", "Vacaciones", Icons.Outlined.Umbrella, Color(0xFFFB923C)),
    VisionBoardSticker("coffee", "Café", Icons.Outlined.LocalCafe, Color(0xFFA16207)),
    VisionBoardSticker("fitness", "Fitness", Icons.Outlined.FitnessCenter, Color(0xFF22C55E)),
    VisionBoardSticker("pets", "Mascotas", Icons.Outlined.Pets, Color(0xFFEAB308)),
    VisionBoardSticker("nature", "Naturaleza", Icons.Outlined.Park, Color(0xFF16A34A)),
    VisionBoardSticker("money", "Dinero", Icons.Outlined.AccountBalanceWallet, Color(0xFF059669)),
    VisionBoardSticker("tech", "Tecnología", Icons.Outlined.Memory, Color(0xFF6366F1)),
    VisionBoardSticker("music", "Música", Icons.Outlined.MusicNote, Color(0xFFEC4899)),
    VisionBoardSticker("cinema", "Cine", Icons.Outlined.Movie, Color(0xFF334155)),
    VisionBoardSticker("sports", "Deportes", Icons.Outlined.EmojiEvents, Color(0xFFF59E0B)),
    VisionBoardSticker("household", "Hogar (mueble)", Icons.Outlined.Chair, Color(0xFF92400E)),
)

fun stickerOf(id: String?): VisionBoardSticker? = STICKER_CATALOG.firstOrNull { it.id == id }
