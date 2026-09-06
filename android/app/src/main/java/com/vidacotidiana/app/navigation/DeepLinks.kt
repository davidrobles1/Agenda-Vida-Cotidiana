package com.vidacotidiana.app.navigation

/**
 * Los enlaces internos con los que una notificación —o un acceso directo del
 * icono— abre una sección concreta.
 *
 * Existen porque hasta ahora una notificación abría `MainActivity` sin decir a
 * dónde: el usuario aterrizaba donde lo hubiera dejado y tenía que buscar
 * aquello de lo que se le acababa de avisar.
 *
 * El esquema es propio y NO colisiona con el de AppAuth
 * (`com.vidacotidiana.app://callback`), que es el que atiende el redirect de
 * Keycloak. Son dos esquemas distintos a propósito: mezclarlos habría hecho que
 * un enlace de navegación entrara por el receptor del login.
 */
object DeepLinks {
    const val SCHEME = "vidacotidiana"

    /** `vidacotidiana://seccion/<ruta>` — abre un destino que ya existe. */
    const val SECTION_HOST = "seccion"

    /** `vidacotidiana://crear/tarea` — abre el formulario de alta que ya existe. */
    const val CREATE_HOST = "crear"

    fun section(route: String): String = "$SCHEME://$SECTION_HOST/$route"

    fun createTask(): String = "$SCHEME://$CREATE_HOST/tarea"

    /**
     * Marca interna para «abrir el alta de tarea». Viaja por el mismo canal que
     * una ruta porque el destino y la acción entran por el mismo intent; no es
     * una ruta de navegación y por eso no está en `Routes`.
     */
    const val ACTION_CREATE_TASK = "__crear_tarea"

    /**
     * El patrón que registra cada destino navegable. Navigation Compose lo
     * resuelve contra el `Intent` entrante; por eso las rutas son las mismas
     * constantes de `Routes` y no una segunda lista que mantener.
     */
    fun sectionPattern(route: String): String = section(route)
}

/**
 * A dónde lleva cada tipo de evento push.
 *
 * ATENCIÓN AL LÍMITE REAL DEL PAYLOAD: el backend manda solo
 * `type` (el nombre del enum) y un mensaje; NO manda el id del recurso. Y los
 * seis tipos están REUTILIZADOS entre tres dominios distintos —familia,
 * compartición de recordatorios y recursos compartidos—, así que
 * `INVITATION_RECEIVED` puede significar «te invitaron a una familia» o «te
 * comprometieron con un recurso».
 *
 * Con esa información no se puede acertar siempre. La regla elegida, y el
 * motivo:
 *
 *  - La familia `INVITATION_*` lleva a **Familia**, porque es la ÚNICA pantalla
 *    de Complete que muestra invitaciones pendientes y ofrece aceptar o
 *    rechazar. Si el aviso pide una acción al usuario, está ahí.
 *  - `REMINDER_SHARE_REVOKED` lleva a **Compartidos**: solo lo emite
 *    `ResourceSharingService` y no es ambiguo.
 *  - `REMINDER_DELETED` lleva a **Tareas**: solo lo emite `ReminderService`.
 *
 * Un tipo desconocido abre Inicio en vez de no hacer nada: que el backend
 * añada un evento nuevo no debe convertir su notificación en un toque muerto.
 *
 * Para llevar al REGISTRO concreto haría falta que el payload incluyera su id
 * —un cambio de backend de una línea—, que esta fase no toca.
 */
object PushDestinations {

    fun routeFor(type: String?): String = when (type) {
        "INVITATION_RECEIVED",
        "INVITATION_ACCEPTED",
        "INVITATION_REJECTED",
        "INVITATION_CANCELLED",
        -> Routes.FAMILY

        "REMINDER_SHARE_REVOKED" -> Routes.SHARED
        "REMINDER_DELETED" -> Routes.TASKS

        else -> Routes.HOME
    }

    /**
     * Los tipos anteriores viven todos en el contexto Personal, así que una
     * notificación no debe dejar al usuario en Laboral mirando una sección que
     * allí no existe.
     */
    fun contextFor(type: String?): AppContext = AppContext.PERSONAL
}
