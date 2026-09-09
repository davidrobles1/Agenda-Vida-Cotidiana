package com.vidacotidiana.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.ui.graphics.vector.ImageVector
import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.vocabulary.ProfessionalProfile

/**
 * Los tres contextos del producto (ADR-015/UX-012). Portal es el «GENERAL» de
 * la Web: el calendario transversal, sin barra lateral propia.
 */
enum class AppContext(val label: String) {
    PORTAL("Portal"),
    PERSONAL("Personal"),
    LABORAL("Laboral"),
}

/** Rutas reales. Ninguna inventada: son las que ya existen o las que el artefacto define. */
object Routes {
    /**
     * Presentación con el logo del Portal y puerta de entrada.
     *
     * Es el ÚNICO destino previo a la sesión: la antigua ruta `login` —una
     * pantalla propia con botones— se eliminó, porque el formulario real es el
     * de Keycloak y esta pantalla lo abre directamente.
     */
    const val INTRO = "intro"

    // Personal
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val BOARD = "board"
    const val SHARED = "shared"
    const val DOCUMENTS = "documents"
    const val INVENTORY = "inventory"
    const val WARRANTIES = "warranties"
    const val MAINTENANCE = "maintenance"
    const val PAYMENTS = "payments"
    const val FAMILY = "family"
    const val TASKS = "tasks"

    // Laboral
    const val HOY = "hoy"
    const val AGENDA = "agenda"
    const val TASKS_LAB = "tareas_lab"
    const val PEOPLE = "personas"
    const val PROJECTS = "proyectos"
    const val COMMITMENTS = "seguimientos"
    const val OBJECTIVES = "objetivos"
    const val ROUTINES = "rutinas"
    /** La ruta es «recursos» —lo que ve el usuario—; el tipo interno es
        `WorkResource` para no dar un tercer sentido a "resource" en el código. */
    const val WORK_RESOURCES = "recursos"
    const val PLACES = "lugares"
    const val INBOX = "inbox"

    // Cuenta
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val APPEARANCE = "appearance"
}

/**
 * La RAÍZ de cada módulo: el destino al que «volver al principio» significa
 * volver.
 *
 * Vive con las rutas y no dentro de una pantalla porque es una propiedad de la
 * navegación, no de quien la use: la barra inferior, el cambio de contexto y
 * el salto a inicio necesitan la misma respuesta, y tres copias de esta
 * decisión acabarían discrepando.
 */
fun rootRouteFor(context: AppContext): String = when (context) {
    AppContext.PERSONAL -> Routes.HOME
    AppContext.LABORAL -> Routes.HOY
    AppContext.PORTAL -> Routes.CALENDAR
}

/** Un destino navegable, con la etiqueta que le corresponde en el contexto activo. */
data class Destination(val route: String, val label: String, val icon: ImageVector, val count: Int? = null)

/**
 * La navegación completa de cada contexto — las diecisiete secciones del
 * artefacto. La barra inferior toma cuatro de aquí; el resto pasa al menú.
 */
fun navFor(context: AppContext, profile: ProfessionalProfile): List<Destination> = when (context) {
    AppContext.PERSONAL -> listOf(
        Destination(Routes.HOME, "Inicio", Icons.Outlined.Home),
        Destination(Routes.CALENDAR, "Calendario personal", Icons.Outlined.CalendarMonth),
        Destination(Routes.BOARD, "Vision Board", Icons.Outlined.Dashboard),
        Destination(Routes.SHARED, "Compartidos", Icons.Outlined.Share, count = 2),
        Destination(Routes.DOCUMENTS, "Documentos", Icons.Outlined.Description, count = 3),
        Destination(Routes.INVENTORY, "Inventario", Icons.Outlined.Inventory2, count = 3),
        Destination(Routes.WARRANTIES, "Garantías", Icons.Outlined.VerifiedUser, count = 3),
        Destination(Routes.MAINTENANCE, "Mantenimiento", Icons.Outlined.Build, count = 2),
        Destination(Routes.PAYMENTS, "Pagos", Icons.Outlined.Autorenew, count = 4),
        Destination(Routes.FAMILY, "Familia", Icons.Outlined.Groups, count = 1),
        Destination(Routes.TASKS, "Tareas", Icons.AutoMirrored.Outlined.Assignment),
    )
    AppContext.LABORAL -> listOf(
        Destination(Routes.HOY, "Hoy", Icons.Outlined.Home),
        Destination(Routes.AGENDA, "Agenda", Icons.Outlined.CalendarMonth),
        Destination(Routes.TASKS_LAB, "Tareas", Icons.AutoMirrored.Outlined.Assignment),
        Destination(Routes.PEOPLE, profile.personPlural, Icons.Outlined.Groups),
        Destination(Routes.PROJECTS, profile.projectPlural, Icons.Outlined.Description),
        Destination(Routes.COMMITMENTS, "Seguimientos", Icons.Outlined.Autorenew),
        // Objetivos vive en el CAJÓN y no en la barra inferior: la barra es
        // para lo que se consulta a diario, y un objetivo no lo es.
        Destination(Routes.OBJECTIVES, "Objetivos", Icons.Outlined.Flag),
        // Junto a Objetivos porque las dos son seguimiento del propio trabajo;
        // Seguimientos, en cambio, son compromisos con terceros. En el cajón,
        // no en la barra: la barra es para lo diario.
        Destination(Routes.ROUTINES, "Rutinas", Icons.Outlined.Repeat),
        Destination(Routes.WORK_RESOURCES, "Recursos", Icons.Outlined.FolderOpen),
        Destination(Routes.PLACES, "Lugares", Icons.Outlined.Place),
        Destination(Routes.INBOX, "Inbox", Icons.Outlined.Inbox),
    )
    AppContext.PORTAL -> listOf(
        Destination(Routes.CALENDAR, "Calendario", Icons.Outlined.CalendarMonth),
    )
}

/**
 * Los destinos prioritarios de la barra inferior, aprobados en la iteración
 * anterior del artefacto.
 *
 * Personal: Inicio · Calendario · «+» · Pagos · Vision Board
 * Laboral:  Hoy · Agenda · «+» · [perfil] · Inbox
 * Portal:   tiene una sola sección real — no se inventan destinos de relleno.
 */
fun bottomDestinations(context: AppContext, profile: ProfessionalProfile): List<Destination> = when (context) {
    AppContext.PERSONAL -> listOf(
        Destination(Routes.HOME, "Inicio", Icons.Outlined.Home),
        Destination(Routes.CALENDAR, "Calendario", Icons.Outlined.CalendarMonth),
        Destination(Routes.PAYMENTS, "Pagos", Icons.Outlined.Autorenew),
        Destination(Routes.BOARD, "Vision Board", Icons.Outlined.Dashboard),
    )
    AppContext.LABORAL -> listOf(
        Destination(Routes.HOY, "Hoy", Icons.Outlined.Home),
        Destination(Routes.AGENDA, "Agenda", Icons.Outlined.CalendarMonth),
        // El cuarto acceso se nombra con el vocabulario del perfil activo:
        // Proyectos · Obras · Casos · Oportunidades.
        Destination(Routes.PROJECTS, profile.projectPlural, Icons.Outlined.Description),
        Destination(Routes.INBOX, "Inbox", Icons.Outlined.Inbox),
    )
    AppContext.PORTAL -> listOf(
        Destination(Routes.CALENDAR, "Calendario", Icons.Outlined.CalendarMonth),
    )
}

/** Lo que el menú muestra: el resto del contexto, sin repetir la barra. */
fun drawerDestinations(context: AppContext, profile: ProfessionalProfile): List<Destination> {
    val inBar = bottomDestinations(context, profile).map { it.route }.toSet()
    return navFor(context, profile).filterNot { it.route in inBar }
}

/** Destinos de cuenta, comunes a los tres contextos. */
val accountDestinations = listOf(
    Destination(Routes.NOTIFICATIONS, "Notificaciones", Icons.Outlined.Notifications, count = 3),
    Destination(Routes.SETTINGS, "Ajustes", Icons.Outlined.Settings),
    Destination(Routes.APPEARANCE, "Tema", Icons.Outlined.Palette),
)

/**
 * Una accion del «+». Lleva el RECURSO, no solo su etiqueta: antes el menu
 * devolvia una cadena y nadie sabia que dar de alta con ella, asi que el boton
 * abria una hoja que no guardaba nada. Con el recurso dentro, elegir aqui y
 * crear de verdad son el mismo gesto.
 *
 * La etiqueta sigue siendo del perfil (ADR-016): «Nuevo proyecto» u «Obra»,
 * «Caso», «Oportunidad» segun quien mire.
 */
data class CreateAction(val resource: CreatableResource, val label: String, val icon: ImageVector)

/**
 * Qué crea el «+» cuando el usuario ya está DENTRO de una sección.
 *
 * Estando en Pagos, «+» debe crear un pago: mostrar antes un selector con seis
 * opciones obliga a repetir una información que la propia pantalla ya da.
 *
 * Devuelve null en las pantallas RAÍZ de cada módulo —Inicio, Calendario, Hoy,
 * Agenda— y también en las que no crean nada propio (Compartidos, Familia,
 * Vision Board, ajustes). Ahí «+» conserva el selector actual, porque desde una
 * raíz el usuario no ha dicho todavía qué quiere crear.
 */
fun createResourceForRoute(route: String?): CreatableResource? = when (route) {
    Routes.PAYMENTS -> CreatableResource.PAYMENT
    Routes.MAINTENANCE -> CreatableResource.MAINTENANCE
    Routes.WARRANTIES -> CreatableResource.WARRANTY
    Routes.INVENTORY -> CreatableResource.INVENTORY
    Routes.DOCUMENTS -> CreatableResource.DOCUMENT
    Routes.TASKS, Routes.TASKS_LAB -> CreatableResource.TASK
    Routes.PEOPLE -> CreatableResource.PERSON
    Routes.PROJECTS -> CreatableResource.PROJECT
    Routes.COMMITMENTS -> CreatableResource.COMMITMENT
    Routes.OBJECTIVES -> CreatableResource.OBJECTIVE
    Routes.ROUTINES -> CreatableResource.ROUTINE
    Routes.WORK_RESOURCES -> CreatableResource.WORK_RESOURCE
    Routes.PLACES -> CreatableResource.PLACE
    Routes.INBOX -> CreatableResource.NOTE
    else -> null
}

/**
 * Las acciones de creación de cada contexto. Son las que YA existen: el «+»
 * no inventa capacidades, solo reúne las que hay.
 */
fun createActions(context: AppContext, profile: ProfessionalProfile): List<CreateAction> = when (context) {
    AppContext.LABORAL -> listOf(
        CreateAction(CreatableResource.TASK, "Nueva tarea", Icons.AutoMirrored.Outlined.Assignment),
        CreateAction(CreatableResource.PROJECT, "Nuevo ${profile.project.lowercase()}", Icons.Outlined.Description),
        CreateAction(CreatableResource.PERSON, "Nueva ${profile.person.lowercase()}", Icons.Outlined.Groups),
        CreateAction(CreatableResource.COMMITMENT, "Nuevo seguimiento", Icons.Outlined.Autorenew),
        CreateAction(CreatableResource.OBJECTIVE, "Nuevo objetivo", Icons.Outlined.Flag),
        CreateAction(CreatableResource.ROUTINE, "Nueva rutina", Icons.Outlined.Repeat),
        CreateAction(CreatableResource.WORK_RESOURCE, "Nuevo recurso", Icons.Outlined.FolderOpen),
        CreateAction(CreatableResource.PLACE, "Nuevo lugar", Icons.Outlined.Place),
        CreateAction(CreatableResource.NOTE, "Nueva nota al Inbox", Icons.Outlined.Inbox),
    )
    AppContext.PORTAL -> listOf(
        CreateAction(CreatableResource.TASK, "Nueva tarea", Icons.AutoMirrored.Outlined.Assignment),
    )
    AppContext.PERSONAL -> listOf(
        CreateAction(CreatableResource.TASK, "Nueva tarea", Icons.AutoMirrored.Outlined.Assignment),
        CreateAction(CreatableResource.PAYMENT, "Agregar pago", Icons.Outlined.Autorenew),
        CreateAction(CreatableResource.MAINTENANCE, "Nuevo mantenimiento", Icons.Outlined.Build),
        CreateAction(CreatableResource.WARRANTY, "Nueva garantía", Icons.Outlined.VerifiedUser),
        CreateAction(CreatableResource.INVENTORY, "Nuevo artículo", Icons.Outlined.Inventory2),
        CreateAction(CreatableResource.DOCUMENT, "Subir documento", Icons.Outlined.Description),
    )
}
