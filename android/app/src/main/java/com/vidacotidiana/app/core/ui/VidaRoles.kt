package com.vidacotidiana.app.core.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * QUÉ SIGNIFICA CADA COLOR. No añade colores: reparte los que ya hay.
 *
 * EL PROBLEMA. El acento significaba cuatro cosas a la vez. En la pantalla de
 * Pagos convivían, en el mismo verde, el botón «+» (acción), el chip «Todos»
 * (filtro activo), el check de cada pieza (acción rápida) y la píldora de
 * estado. Y en Calm no es una impresión: `primary` es #7D8F68 y `success` es
 * #5B8A5E — el mismo verde salvia a ojo. Cuando cuatro papeles comparten color,
 * el usuario deja de poder distinguir "esto es un botón" de "esto ya está bien".
 *
 * EL CASO MÁS GRAVE, visible en Tareas: una tarea PENDIENTE mostraba un check
 * verde en su esquina —el botón para completarla— y una tarea HECHA mostraba
 * también un check verde —su estado—. El mismo signo para "púlsame" y para "ya
 * está". Eso no es un matiz cromático, es una ambigüedad funcional.
 *
 * LA REGLA, en una línea: **el acento es para lo que se PULSA; el estado se
 * dice en tinta y en relleno suave, nunca con el acento.**
 *
 * De ahí sale el reparto de abajo. Ningún valor es nuevo: todos salen de
 * `VidaColors`, que a su vez transcribe `themes.css`. Lo que cambia es a qué se
 * aplica cada uno, que es exactamente lo que pedía P0-3 sin tocar la paleta.
 */
@Immutable
data class VidaRoles(
    /** ACCIÓN PRIMARIA. El «+», el botón que resuelve la pantalla. Único dueño del acento sólido. */
    val actionBg: Color,
    val actionFg: Color,

    /** ACCIÓN SECUNDARIA. Existe, se puede pulsar, no reclama. Contorno, sin relleno. */
    val actionQuietFg: Color,
    val actionQuietBorder: Color,

    /** SELECCIONADO. El filtro activo, la sección donde estás. Comparte acento con la acción a propósito: las dos responden a "esto responde a lo que hiciste". */
    val selectedBg: Color,
    val selectedFg: Color,

    /**
     * COMPLETAR — el botón sobre algo que AÚN NO está hecho.
     * Neutro deliberadamente: es una acción disponible, no un estado alcanzado.
     * Antes se pintaba con `successContainer`, y por eso una tarea pendiente
     * parecía terminada.
     */
    val completeFg: Color,
    val completeBorder: Color,

    /** HECHO — el estado de algo ya resuelto. Aquí sí entra el verde de éxito, y solo aquí. */
    val doneBg: Color,
    val doneFg: Color,

    /** ATENCIÓN — vence pronto, toca ya. Nunca rojo: rojo es lo que está mal. */
    val warnBg: Color,
    val warnFg: Color,

    /** ERROR — algo falló o está vencido. */
    val errorBg: Color,
    val errorFg: Color,

    /** INFORMACIÓN NEUTRA — una etiqueta que clasifica sin valorar. */
    val neutralBg: Color,
    val neutralFg: Color,

    /** DESHABILITADO — presente pero inoperante. Se dice con tinta apagada, no con transparencia. */
    val disabledFg: Color,
    val disabledBg: Color,
)

/**
 * Reparte los colores del tema entre los papeles.
 *
 * `completeFg` toma `textSecondary` y no `success` a propósito: es la decisión
 * central de esta regla. `doneBg`/`doneFg` conservan el verde porque ahí sí
 * describe un hecho.
 */
fun rolesFor(c: VidaColors): VidaRoles = VidaRoles(
    actionBg = c.primary,
    actionFg = c.onPrimary,

    actionQuietFg = c.text,
    actionQuietBorder = c.border,

    selectedBg = c.primary,
    selectedFg = c.onPrimary,

    completeFg = c.textSecondary,
    completeBorder = c.border,

    doneBg = c.successContainer,
    doneFg = c.successText,

    warnBg = c.warningContainer,
    warnFg = c.warningText,

    errorBg = c.errorContainer,
    errorFg = c.error,

    neutralBg = c.sunken,
    neutralFg = c.textSecondary,

    disabledFg = c.textTertiary,
    disabledBg = c.sunken,
)

val LocalVidaRoles = staticCompositionLocalOf { rolesFor(LightVidaColors) }
