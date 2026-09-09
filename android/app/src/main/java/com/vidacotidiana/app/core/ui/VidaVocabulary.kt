package com.vidacotidiana.app.core.ui

/**
 * VALOR INTERNO → PALABRA HUMANA. Una sola vez, para toda la aplicación.
 *
 * El problema no era que faltaran las traducciones: es que estaban en un solo
 * lado. `CreatableResource` ya ofrecía «Electrónicos» y «Seguros» al dar de
 * alta, pero las listas, los encabezados de grupo y los chips de filtro
 * mostraban `ELECTRONICOS` y `SEGUROS` en crudo. El mismo dato tenía dos caras
 * según la pantalla, y la de mayúsculas es la de la base de datos.
 *
 * Aquí viven las etiquetas y de aquí las lee todo el mundo — el formulario
 * incluido —, así que no pueden volver a separarse.
 *
 * LA SEPARACIÓN SE MANTIENE INTACTA: esto no toca el enum, ni el modelo, ni lo
 * que viaja al backend. `InventoryItem.category` sigue valiendo `ELECTRONICOS`;
 * lo único que cambia es lo que lee una persona.
 *
 * `human()` NO inventa significados. Para un valor conocido devuelve su
 * etiqueta; para cualquier otro —una categoría que el backend añada mañana, o
 * un estado de proyecto que el usuario escribió a mano— lo devuelve tal cual si
 * ya viene en lenguaje normal, y solo arregla la caja cuando llega en el
 * formato de una constante (TODO_EN_MAYÚSCULAS_CON_GUIONES). Un texto libre
 * como «70%» o «En revisión» pasa sin tocarse.
 */
object VidaVocabulary {

    /** `inventory_items.category` — el CHECK de V13. */
    val inventoryCategories: List<Pair<String, String>> = listOf(
        "HOGAR" to "Hogar",
        "ELECTRONICOS" to "Electrónicos",
        "VEHICULOS" to "Vehículos",
    )

    /** `documents.category` — el CHECK de V12. */
    val documentCategories: List<Pair<String, String>> = listOf(
        "IDENTIFICACION" to "Identificación",
        "COMPROBANTES" to "Comprobantes",
        "SEGUROS" to "Seguros",
        "CONTRATOS" to "Contratos",
        "OTROS" to "Otros",
    )

    /** `resources.type` — el CHECK de V29. */
    val resourceTypes: List<Pair<String, String>> = listOf(
        "DOCUMENTO" to "Documento",
        "ENLACE" to "Enlace",
        "PLANTILLA" to "Plantilla",
        "MANUAL" to "Manual",
        "HERRAMIENTA" to "Herramienta",
        "OTRO" to "Otro",
    )

    /** `routines.frequency`. */
    val routineFrequencies: List<Pair<String, String>> = listOf(
        "DAILY" to "Diaria",
        "WEEKLY" to "Semanal",
        "MONTHLY" to "Mensual",
    )

    private val known: Map<String, String> =
        (inventoryCategories + documentCategories + resourceTypes + routineFrequencies).toMap()

    /**
     * Cómo se escribe un valor cuando lo lee una persona.
     *
     * Tres casos, en orden:
     *  1. valor conocido → su etiqueta.
     *  2. con pinta de constante (solo mayúsculas, dígitos y guiones bajos) →
     *     se pasa a caja de frase y los guiones bajos a espacios, para que un
     *     valor nuevo del backend no aparezca gritando mientras nadie lo añade
     *     a las listas de arriba.
     *  3. cualquier otra cosa → intacta. Es texto que escribió el usuario.
     */
    fun human(raw: String?): String {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return ""
        known[value]?.let { return it }
        if (!value.any { it.isLowerCase() } && value.any { it.isLetter() }) {
            return value.lowercase()
                .replace('_', ' ')
                .replaceFirstChar { it.uppercase() }
        }
        return value
    }
}
