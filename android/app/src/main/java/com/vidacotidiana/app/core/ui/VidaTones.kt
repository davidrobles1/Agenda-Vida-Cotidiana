package com.vidacotidiana.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * LOS OCHO TONOS DE MÓDULO DEL ARTEFACTO MAESTRO.
 *
 * Fuente de verdad:
 * https://claude.ai/code/artifact/b31707e4-2719-4a26-b7e8-b870ded5a0ec — objeto `TONE`.
 *
 * QUÉ RESUELVE. El artefacto da a cada registro el color de SU tipo: un
 * servicio no se ve igual que una tarjeta ni que un crédito, así que la lista se
 * lee de un vistazo antes de leer una sola palabra. La aplicación pintaba todas
 * las filas de una sección del mismo color, y con ello se perdía esa lectura —
 * que es justamente la diferencia de riqueza cromática que se nota al comparar
 * las dos Pagos.
 *
 * NO ES DECORACIÓN NI ES ALEATORIO: el tono sale del tipo real del registro
 * (`PaymentKind`, en el caso de Pagos), así que dos pagos del mismo tipo se ven
 * iguales siempre y cambiar el tipo cambia el color. Un color que no se derivara
 * del dato sería ruido.
 *
 * DISTINTO DE [VidaRoles]. Ahí se reparte qué significa cada color por su PAPEL
 * —acción, selección, estado—. Aquí se reparte por MÓDULO o TIPO. Son dos ejes y
 * no compiten: el rojo sigue reservado a lo que está mal, y por eso `rose` no
 * entra en ninguna asignación por tipo.
 */
@Immutable
data class VidaTone(val accent: Color, val container: Color)

object VidaTones {

    /** `TONE.indigo` — el acento de la casa. */
    @Composable fun indigo() = VidaTone(VidaTheme.colors.primary, VidaTheme.colors.primaryContainer)

    /** `TONE.green` — lo que está en marcha y va bien. */
    @Composable fun green() = VidaTone(VidaTheme.colors.success, VidaTheme.colors.successContainer)

    /** `TONE.amber` — lo que pide atención. Nunca rojo: ADR-018. */
    @Composable fun amber() = VidaTone(VidaTheme.colors.warning, VidaTheme.colors.warningContainer)

    /** `TONE.coral` — el tono cálido que faltaba en la paleta. */
    @Composable fun coral() = VidaTone(VidaTheme.colors.coral, VidaTheme.colors.coralContainer)

    /** `TONE.violet` — la identidad secundaria. */
    @Composable fun violet() = VidaTone(VidaTheme.colors.second, VidaTheme.colors.secondContainer)

    /** `TONE.violet2` — el violeta suave, el segundo que faltaba. */
    @Composable fun violet2() = VidaTone(VidaTheme.colors.violet2, VidaTheme.colors.violet2Container)

    /** `TONE.ink` — sin tono propio: lo que no necesita identificarse. */
    @Composable fun ink() = VidaTone(VidaTheme.colors.textSecondary, VidaTheme.colors.sunken)

    /**
     * EL TONO DE UN COMPROMISO DE PAGO, por su tipo real.
     *
     * `PaymentKind` tiene seis valores y el artefacto pinta cinco familias de
     * pago con cinco colores distintos. La correspondencia respeta las dos
     * cosas: el color que el artefacto da a cada familia, y la distinción
     * estructural que ADR-020(d) hace entre recurrente, tarjeta y crédito.
     *
     * Un tipo desconocido cae en índigo en vez de romperse: el backend puede
     * añadir un valor antes de que esta lista lo conozca.
     */
    @Composable
    fun payment(kind: String?): VidaTone = when (kind?.trim()?.uppercase()) {
        "SERVICE" -> amber()        // artefacto: SERVICIO — luz, agua, internet
        "SUBSCRIPTION" -> green()   // artefacto: DIGITAL — lo que se renueva solo
        "MEMBERSHIP" -> violet2()   // la cuota que se paga por pertenecer
        "CARD" -> violet()          // artefacto: TARJETA — corte y fecha límite
        "CREDIT" -> coral()         // artefacto: CRÉDITO — el que termina
        else -> indigo()            // CUSTOM y cualquier valor nuevo
    }
}
