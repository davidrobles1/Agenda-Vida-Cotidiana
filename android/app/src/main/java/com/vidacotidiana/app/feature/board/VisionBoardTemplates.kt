package com.vidacotidiana.app.feature.board

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.max
import kotlin.math.min

/**
 * Las nueve plantillas: las cinco de partida y las cuatro del portal, con
 * sus mismas coordenadas sobre el lienzo de referencia de 1600×1000.
 *
 * LAS CUATRO DEL PORTAL TRAEN SUS 44 FOTOGRAFÍAS, empaquetadas en
 * `assets/board-templates` — las mismas que la Web guarda en
 * `templates/assets`, byte a byte.
 *
 * La Web las referencia por `data.url` porque ahí un `url` es una ruta que
 * su propio bundle sirve. En el móvil esa vía no existe, así que un
 * elemento de plantilla guarda `data.assetFile` con el nombre del fichero y
 * el ViewModel lo resuelve al aplicarla: sube la foto al almacén de
 * imágenes del usuario y crea el elemento con el `imageId` que le devuelve.
 *
 * Es más trabajo que copiar una ruta, y es el único camino correcto: el
 * endpoint de imágenes es de dueño único, así que la plantilla no puede
 * repartir ids ajenos. Cada usuario acaba con SUS copias, que es lo que
 * hace que la plantilla siga funcionando el día que se borre el tablero
 * original.
 */

data class TemplateElement(
    val type: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float = 0f,
    val data: JsonObject,
)

data class VisionBoardTemplate(
    val id: String,
    val name: String,
    val description: String,
    val elements: List<TemplateElement>,
    /** Informativo: el tema con el que se diseñó. Aplicarla no lo cambia. */
    val theme: String? = null,
    /** Las cuatro que el usuario diseñó en el portal, con fotografías. */
    val fromPortal: Boolean = false,
) {
    /** Cuántas fotos hay que subir al aplicarla. Cero en las de partida. */
    val photoCount: Int get() = elements.count { it.data["assetFile"] != null }
}

const val TEMPLATE_REFERENCE_WIDTH = 1600f
const val TEMPLATE_REFERENCE_HEIGHT = 1000f
private const val MIN_SIZE = 24f

private fun text(value: String) = buildJsonObject { put("text", value) }

/**
 * Una fotografía de plantilla: el nombre del fichero dentro de
 * `assets/board-templates`, y su marco si lo lleva. `VisionBoardViewModel`
 * lo cambia por un `imageId` real al aplicar.
 */
private fun asset(file: String, frame: String? = null) = buildJsonObject {
    put("assetFile", file)
    if (frame != null) put("frameStyle", frame)
}
private fun shape(id: String) = buildJsonObject { put("shape", id) }
private fun sticker(id: String) = buildJsonObject { put("stickerId", id) }

val VISION_BOARD_TEMPLATES = listOf(
    VisionBoardTemplate(
        "portal-lujo-aspiraciones", "Lujo & Aspiraciones",
        "Una visión sofisticada enfocada en estilo de vida, bienestar, logros y aspiraciones personales.",
        listOf(
            TemplateElement("IMAGE", 460f, 48.57f, 525.56f, 848.73f, 0f, asset("lujo-aspiraciones-01.jpg", "rounded")),
            TemplateElement("IMAGE", 1402.03f, 501.63f, 197.97f, 262.72f, 0f, asset("lujo-aspiraciones-02.jpg")),
            TemplateElement("IMAGE", 0f, -32.06f, 258.69f, 356.37f, 0f, asset("lujo-aspiraciones-03.jpg", "circle")),
            TemplateElement("IMAGE", 460f, 454.17f, 175f, 260f, 0f, asset("lujo-aspiraciones-04.jpg")),
            TemplateElement("IMAGE", 1257.46f, 10.65f, 342.54f, 524.05f, 0f, asset("lujo-aspiraciones-05.jpg")),
            TemplateElement("IMAGE", 1225.27f, 534.7f, 279.24f, 465.3f, 0f, asset("lujo-aspiraciones-06.jpg", "rounded")),
            TemplateElement("STICKER", 207.69f, 8.57f, 80f, 80f, 0f, sticker("emotion")),
            TemplateElement("STICKER", 386.69f, 147.94f, 80f, 80f, 0f, sticker("coffee")),
            TemplateElement("IMAGE", 0f, 246.13f, 518.51f, 753.87f, 0f, asset("lujo-aspiraciones-07.jpg")),
            TemplateElement("IMAGE", 258.69f, 292.49f, 180f, 260f, 0f, asset("lujo-aspiraciones-08.jpg")),
            TemplateElement("IMAGE", 992.25f, 32.06f, 266.51f, 455.88f, 0f, asset("lujo-aspiraciones-09.jpg")),
            TemplateElement("IMAGE", 992.25f, 623.07f, 259.66f, 375.66f, -4.13f, asset("lujo-aspiraciones-10.jpg", "polaroid")),
            TemplateElement("IMAGE", 264.13f, -19.84f, 195.87f, 312.33f, -1.2f, asset("lujo-aspiraciones-11.jpg", "film")),
            TemplateElement("IMAGE", 754.27f, 584.17f, 293.01f, 427.72f, -0.11f, asset("lujo-aspiraciones-04.jpg")),
            TemplateElement("IMAGE", 1008.18f, 415.93f, 250.59f, 348.41f, 1.41f, asset("lujo-aspiraciones-13.jpg", "rounded")),
            TemplateElement("IMAGE", 756.09f, 0f, 226.1f, 350.05f, 0f, asset("lujo-aspiraciones-14.jpg")),
            TemplateElement("IMAGE", 386.69f, 415.93f, 213.74f, 357.94f, 0f, asset("lujo-aspiraciones-15.jpg")),
            TemplateElement("TEXT", 817.87f, 292.49f, 206.48f, 63.64f, 0f, text("Vision Board")),
            TemplateElement("IMAGE", 515.73f, 714.17f, 238.54f, 292.55f, 0f, asset("lujo-aspiraciones-16.jpg")),
            TemplateElement("IMAGE", 13f, 204.55f, 232.69f, 310.88f, -9.25f, asset("lujo-aspiraciones-02.jpg")),
            TemplateElement("IMAGE", 118.7f, 566.34f, 229.99f, 396.01f, 0f, asset("lujo-aspiraciones-18.jpg")),
            TemplateElement("IMAGE", 600.43f, 356.13f, 233.68f, 326f, 0f, asset("lujo-aspiraciones-19.jpg")),
        ),
        theme = "LIGHT",
        fromPortal = true,
    ),
    VisionBoardTemplate(
        "portal-calma-bienestar", "Calma & Bienestar",
        "Una visión de confort, tranquilidad, equilibrio y bienestar personal.",
        listOf(
            TemplateElement("IMAGE", 369.58f, 277.38f, 989.42f, 445.24f, 89.31f, asset("calma-bienestar-01.jpg")),
            TemplateElement("IMAGE", 0f, 0f, 260f, 117f, 0f, asset("calma-bienestar-01.jpg")),
            TemplateElement("IMAGE", 0f, 0f, 739.16f, 1000f, 0f, asset("calma-bienestar-03.jpg")),
            TemplateElement("IMAGE", 950.12f, 0f, 649.88f, 1000f, 0f, asset("calma-bienestar-04.jpg")),
            TemplateElement("TEXT", 737.84f, 351.98f, 212.28f, 148.02f, 0f, text("2027")),
        ),
        theme = "PAPER",
        fromPortal = true,
    ),
    VisionBoardTemplate(
        "portal-metas-conquistas", "Metas & Conquistas",
        "Una visión enfocada en trabajo, viajes, vehículos, crecimiento y objetivos personales.",
        listOf(
            TemplateElement("IMAGE", 1027.2f, 0f, 572.8f, 1000f, 0f, asset("metas-conquistas-01.jpg")),
            TemplateElement("IMAGE", 0f, 0f, 581.37f, 1000f, 0f, asset("metas-conquistas-02.jpg")),
            TemplateElement("IMAGE", 581.37f, 0f, 445.83f, 1000f, 0f, asset("metas-conquistas-03.jpg")),
        ),
        theme = "ENERGY",
        fromPortal = true,
    ),
    VisionBoardTemplate(
        "portal-energia-progreso", "Energía & Progreso",
        "Una visión enfocada en viajes, negocios, ejercicio, disciplina y crecimiento personal.",
        listOf(
            TemplateElement("IMAGE", 0f, -9.08f, 1600f, 724.23f, 0f, asset("energia-progreso-01.jpg")),
            TemplateElement("IMAGE", 327.51f, -9.08f, 289.38f, 538.16f, 0f, asset("energia-progreso-02.jpg")),
            TemplateElement("IMAGE", 301f, 529.08f, 313f, 470.92f, 0f, asset("energia-progreso-03.jpg")),
            TemplateElement("IMAGE", 55.2f, 326.18f, 272.31f, 450.29f, 0f, asset("energia-progreso-04.jpg")),
            TemplateElement("IMAGE", 0f, 0f, 250.51f, 376.12f, 0f, asset("energia-progreso-05.jpg")),
            TemplateElement("IMAGE", 1064.62f, 405.69f, 318.13f, 388f, 0f, asset("energia-progreso-06.jpg")),
            TemplateElement("IMAGE", 616.89f, 26.43f, 296.16f, 502.65f, 0f, asset("energia-progreso-07.jpg")),
            TemplateElement("IMAGE", 1334.61f, 605.71f, 265.39f, 394.29f, 0f, asset("energia-progreso-08.jpg")),
            TemplateElement("IMAGE", 1382.75f, 291.32f, 151f, 260f, 0f, asset("energia-progreso-09.jpg")),
            TemplateElement("IMAGE", 1316.5f, 0f, 283.5f, 313.87f, 0f, asset("energia-progreso-10.jpg")),
            TemplateElement("IMAGE", 173.51f, 740f, 154f, 260f, 0f, asset("energia-progreso-11.jpg")),
            TemplateElement("IMAGE", 552.22f, 376.12f, 239.03f, 388.43f, 0f, asset("energia-progreso-12.jpg")),
            TemplateElement("IMAGE", 791.25f, 567.04f, 224.4f, 395f, 0f, asset("energia-progreso-13.jpg")),
            TemplateElement("IMAGE", 606.6f, 740f, 234f, 260f, 0f, asset("energia-progreso-10.jpg")),
            TemplateElement("IMAGE", 1003.61f, 0f, 281.23f, 353.03f, 0f, asset("energia-progreso-15.jpg")),
            TemplateElement("IMAGE", 893.65f, 227.32f, 244f, 356.74f, 0f, asset("energia-progreso-16.jpg")),
            TemplateElement("IMAGE", 0f, 638.35f, 191.35f, 364.65f, 0f, asset("energia-progreso-17.jpg")),
            TemplateElement("IMAGE", 985.16f, 671.68f, 238.9f, 328.32f, 0f, asset("energia-progreso-18.jpg")),
        ),
        theme = "LIGHT",
        fromPortal = true,
    ),

    VisionBoardTemplate(
        "suenos", "Sueños",
        "Un punto de partida para visualizar tus grandes aspiraciones.",
        listOf(
            TemplateElement("SHAPE", 60f, 40f, 760f, 180f, -1f, shape("rectangle")),
            TemplateElement("TEXT", 100f, 72f, 680f, 100f, -1.5f, text("Mis sueños")),
            TemplateElement("STICKER", 900f, 50f, 100f, 100f, 8f, sticker("celebration")),
            TemplateElement("NOTE", 100f, 280f, 330f, 190f, -3f, text("Viajar por el mundo y conocer nuevas culturas.")),
            TemplateElement("NOTE", 490f, 280f, 330f, 190f, 2.5f, text("Aprender algo nuevo cada año.")),
            TemplateElement("STICKER", 880f, 280f, 90f, 90f, -7f, sticker("wellness")),
            TemplateElement("SHAPE", 1000f, 430f, 140f, 140f, 0f, shape("circle")),
            TemplateElement("NOTE", 100f, 530f, 330f, 190f, 3f, text("Construir la vida que imagino.")),
        ),
    ),
    VisionBoardTemplate(
        "metas", "Metas",
        "Tres metas principales del año, listas para revisar y ajustar.",
        listOf(
            TemplateElement("SHAPE", 60f, 40f, 900f, 160f, -0.8f, shape("rectangle")),
            TemplateElement("TEXT", 100f, 68f, 820f, 100f, -1.5f, text("Metas del año")),
            TemplateElement("NOTE", 100f, 260f, 250f, 160f, -3f, text("Meta 1: salud y bienestar.")),
            TemplateElement("NOTE", 410f, 260f, 250f, 160f, 2f, text("Meta 2: finanzas personales.")),
            TemplateElement("NOTE", 720f, 260f, 250f, 160f, -2.5f, text("Meta 3: desarrollo profesional.")),
            TemplateElement("STICKER", 1030f, 260f, 90f, 90f, 6f, sticker("work")),
            TemplateElement("SHAPE", 100f, 470f, 800f, 6f, -1f, shape("line")),
            TemplateElement("TEXT", 100f, 520f, 500f, 70f, 1f, text("Revisar cada trimestre.")),
        ),
    ),
    VisionBoardTemplate(
        "viajes", "Viajes",
        "Tres próximos destinos y espacio para el presupuesto y fechas.",
        listOf(
            TemplateElement("SHAPE", 60f, 40f, 700f, 160f, -1f, shape("rectangle")),
            TemplateElement("TEXT", 100f, 68f, 600f, 100f, -1.5f, text("Próximos viajes")),
            TemplateElement("STICKER", 820f, 50f, 110f, 110f, -8f, sticker("travel")),
            TemplateElement("NOTE", 100f, 270f, 300f, 180f, -3f, text("Destino 1: Japón, en primavera.")),
            TemplateElement("NOTE", 460f, 270f, 300f, 180f, 2.5f, text("Destino 2: Perú, Machu Picchu.")),
            TemplateElement("NOTE", 820f, 270f, 300f, 180f, -2f, text("Destino 3: Islandia, auroras boreales.")),
            TemplateElement("SHAPE", 100f, 510f, 120f, 120f, 0f, shape("circle")),
            TemplateElement("TEXT", 280f, 530f, 500f, 80f, 1.5f, text("Presupuesto estimado y fechas.")),
        ),
    ),
    VisionBoardTemplate(
        "personal", "Personal",
        "Hábitos y recordatorios simples para el bienestar diario.",
        listOf(
            TemplateElement("SHAPE", 60f, 40f, 650f, 160f, 1f, shape("rectangle")),
            TemplateElement("TEXT", 100f, 68f, 560f, 100f, 1.5f, text("Bienestar personal")),
            TemplateElement("STICKER", 770f, 50f, 100f, 100f, 7f, sticker("emotion")),
            TemplateElement("NOTE", 100f, 270f, 290f, 170f, -3f, text("Meditar 10 minutos al día.")),
            TemplateElement("NOTE", 450f, 270f, 290f, 170f, 2.5f, text("Dormir 8 horas.")),
            TemplateElement("STICKER", 800f, 270f, 90f, 90f, -6f, sticker("wellness")),
            TemplateElement("SHAPE", 100f, 490f, 680f, 6f, 1f, shape("line")),
            TemplateElement("NOTE", 100f, 540f, 290f, 170f, -2.5f, text("Agradecer algo cada noche.")),
        ),
    ),
    VisionBoardTemplate(
        "productividad", "Productividad",
        "Rutinas de planificación y enfoque para la semana.",
        listOf(
            TemplateElement("SHAPE", 60f, 40f, 900f, 160f, -0.8f, shape("rectangle")),
            TemplateElement("TEXT", 100f, 68f, 820f, 100f, -1.5f, text("Productividad y enfoque")),
            TemplateElement("STICKER", 1030f, 50f, 90f, 90f, 8f, sticker("bell")),
            TemplateElement("NOTE", 100f, 270f, 270f, 170f, -3f, text("Planificar la semana los domingos.")),
            TemplateElement("NOTE", 420f, 270f, 270f, 170f, 2f, text("Bloques de trabajo profundo.")),
            TemplateElement("NOTE", 740f, 270f, 270f, 170f, -2.5f, text("Revisar pendientes cada tarde.")),
            TemplateElement("STICKER", 1080f, 270f, 90f, 90f, -7f, sticker("study")),
            TemplateElement("SHAPE", 100f, 490f, 1000f, 6f, -1f, shape("line")),
            TemplateElement("TEXT", 100f, 540f, 400f, 70f, 1.5f, text("Una tarea a la vez.")),
        ),
    ),
)

/**
 * Encaja y centra la composición en el tablero destino con UNA escala para
 * los dos ejes — nunca un estirado por eje, que deformaría una nota o una
 * forma. Es la misma función que la Web usa tanto para su vista previa como
 * para aplicar de verdad; solo cambian los números.
 */
fun fitTemplate(template: VisionBoardTemplate, targetWidth: Float, targetHeight: Float): List<TemplateElement> {
    val scale = min(targetWidth / TEMPLATE_REFERENCE_WIDTH, targetHeight / TEMPLATE_REFERENCE_HEIGHT)
    val offsetX = (targetWidth - TEMPLATE_REFERENCE_WIDTH * scale) / 2f
    val offsetY = (targetHeight - TEMPLATE_REFERENCE_HEIGHT * scale) / 2f
    return template.elements.map {
        it.copy(
            x = offsetX + it.x * scale,
            y = offsetY + it.y * scale,
            width = max(MIN_SIZE, it.width * scale),
            height = max(MIN_SIZE, it.height * scale),
        )
    }
}
