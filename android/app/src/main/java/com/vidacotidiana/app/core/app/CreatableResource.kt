package com.vidacotidiana.app.core.app

import com.vidacotidiana.app.core.ui.VidaVocabulary

/**
 * Qué se puede dar de alta, y con QUÉ campos.
 *
 * Los campos no son una elección de diseño: son exactamente los que cada
 * endpoint exige. `CreateWarrantyRequest` pide archivo, `CreateCommitmentRequest`
 * pide una persona, `CreateSubscriptionRequest` pide periodicidad. Declararlos
 * aquí, en un solo sitio, es lo que permite que haya UN formulario en vez de
 * diez, y que ese formulario no pueda pedir algo que el backend no acepta ni
 * omitir algo que sí exige.
 *
 * Si el contrato del backend cambia, se cambia aquí y las diez altas siguen
 * siendo coherentes.
 */

/** Un campo del formulario. `required` refleja el `@NotNull`/`@NotBlank` real. */
sealed interface FormField {
    val key: String
    val label: String
    val required: Boolean

    data class Text(
        override val key: String,
        override val label: String,
        override val required: Boolean = true,
        val multiline: Boolean = false,
    ) : FormField

    data class Number(
        override val key: String,
        override val label: String,
        override val required: Boolean = false,
        val decimal: Boolean = false,
    ) : FormField

    /** Selector de fecha. El valor viaja como ISO local (`2026-05-12`). */
    data class Date(
        override val key: String,
        override val label: String,
        override val required: Boolean = true,
    ) : FormField

    /**
     * Fecha Y hora, en `2026-05-12T14:30`.
     *
     * Solo lo usa la tarea, y no por criterio propio: es el ÚNICO recurso al
     * que la Web le da hora (`DatePicker withTime` en `ReminderFormFields`).
     * Mantenimiento, pagos, garantías, seguimientos y proyectos usan allí el
     * mismo control sin hora, así que aquí tampoco la piden — añadirles una
     * inventaría una precisión que su dominio no tiene.
     *
     * Fecha y hora van JUNTAS: el control de la Web tiene granularidad de
     * minuto y no puede producir una fecha sin hora, así que aquí tampoco se
     * admite media selección. O se elige el momento completo, o no se elige.
     */
    data class DateTime(
        override val key: String,
        override val label: String,
        override val required: Boolean = true,
    ) : FormField

    /** Opciones fijas: el valor guardado es el del enum, la etiqueta es la que se lee. */
    data class Choice(
        override val key: String,
        override val label: String,
        val options: List<Pair<String, String>>,
        override val required: Boolean = true,
    ) : FormField

    /** Elegido entre datos que el usuario ya tiene (personas, proyectos). */
    data class Reference(
        override val key: String,
        override val label: String,
        val source: ReferenceSource,
        override val required: Boolean = true,
    ) : FormField

    /** Archivo del dispositivo. `mime` restringe lo que ofrece el selector. */
    data class File(
        override val key: String,
        override val label: String,
        val mime: String = "*/*",
        override val required: Boolean = true,
    ) : FormField
}

enum class ReferenceSource { PEOPLE, PROJECTS, INVENTORY }

enum class CreatableResource(
    val title: String,
    val fields: List<FormField>,
) {
    // La Web rotula este campo «Fecha y hora (opcional)» y deja guardar sin
    // él: una tarea sin fecha existe y se ve en Tareas, solo que no en el
    // calendario. Esa regla se conserva tal cual.
    TASK(
        "Nueva tarea",
        listOf(
            FormField.Text("title", "¿Qué hay que hacer?"),
            FormField.DateTime("dueAt", "Fecha y hora", required = false),
            // ADR-016/FR-024. TEXTO LIBRE: el backend guarda un `VARCHAR(500)`
            // sin ninguna validación de formato, así que aquí tampoco se valida
            // ni se transforma. No es un lugar del catálogo —esa es otra
            // funcionalidad— sino "dónde es", escrito a mano.
            FormField.Text("location", "¿Dónde?", required = false),
            FormField.Text("description", "Detalle", required = false, multiline = true),
        ),
    ),

    // ADR-020: el importe vive aquí porque Pagos responde "cuánto representa".
    PAYMENT(
        "Agregar pago",
        listOf(
            FormField.Text("service", "¿Qué pagas?"),
            FormField.Date("nextPaymentDate", "Próximo pago"),
            FormField.Choice(
                "billingCycle", "Cada cuánto",
                listOf("MONTHLY" to "Mensual", "YEARLY" to "Anual", "WEEKLY" to "Semanal"),
            ),
            FormField.Number("amount", "Importe", required = false, decimal = true),
        ),
    ),

    MAINTENANCE(
        "Nuevo mantenimiento",
        listOf(
            FormField.Text("item", "¿Qué hay que revisar?"),
            FormField.Date("nextDueAt", "Próxima vez"),
            FormField.Number("intervalMonths", "Cada cuántos meses", required = false),
            // V31: OPCIONAL, a diferencia del de la garantía. También se
            // mantiene lo que no es un artículo inventariado —el techo, el
            // jardín—, y obligarlo aquí expulsaría casos reales.
            FormField.Reference(
                "inventoryItemId", "¿A qué artículo?", ReferenceSource.INVENTORY, required = false,
            ),
        ),
    ),

    WARRANTY(
        "Nueva garantía",
        listOf(
            FormField.Text("item", "¿Qué producto?"),
            FormField.Date("expiresAt", "Vence el"),
            // Obligatorio de verdad: una garantía siempre cubre un artículo del
            // inventario, y el backend rechaza el alta sin él
            // (`WarrantyService#create`, DECISION del 2026-09-06).
            FormField.Reference("inventoryItemId", "¿Qué artículo cubre?", ReferenceSource.INVENTORY),
            // Obligatorio de verdad: el endpoint es multipart y exige el archivo.
            FormField.File("file", "Comprobante"),
        ),
    ),

    INVENTORY(
        "Nuevo artículo",
        listOf(
            FormField.Text("name", "¿Qué es?"),
            FormField.Choice(
                "category", "Categoría",
                VidaVocabulary.inventoryCategories,
            ),
            FormField.Text("location", "¿Dónde está?", required = false),
        ),
    ),

    DOCUMENT(
        "Subir documento",
        listOf(
            FormField.Text("name", "Nombre"),
            FormField.Choice(
                "category", "Categoría",
                VidaVocabulary.documentCategories,
            ),
            FormField.File("file", "Archivo"),
        ),
    ),

    PERSON(
        "Nueva persona",
        listOf(
            FormField.Text("name", "Nombre"),
            FormField.Text("role", "Rol", required = false),
            FormField.Text("organization", "Organización", required = false),
        ),
    ),

    PROJECT(
        "Nuevo proyecto",
        listOf(
            FormField.Text("name", "Nombre"),
            // `Project.status` es texto libre en el modelo: se pregunta, no se
            // impone una lista de estados que el dominio no tiene.
            FormField.Text("status", "Estado", required = false),
            FormField.Date("deadline", "Entrega", required = false),
        ),
    ),

    COMMITMENT(
        "Nuevo seguimiento",
        listOf(
            FormField.Text("description", "¿Qué se acordó?"),
            FormField.Reference("personId", "¿Con quién?", ReferenceSource.PEOPLE),
            FormField.Choice(
                "direction", "¿De quién depende?",
                listOf("MINE" to "Me toca a mí", "THEIRS" to "Lo espero"),
            ),
            FormField.Date("dueAt", "Para cuándo"),
        ),
    ),

    NOTE(
        "Nueva nota al Inbox",
        listOf(
            FormField.Text("title", "Título"),
            FormField.Text("description", "Detalle", required = false, multiline = true),
        ),
    ),

    /**
     * FR-031. Solo el título es obligatorio: `CreateObjectiveRequest` es el
     * único campo con `@NotBlank`.
     *
     * NO lleva `completed` ni `version`: el primero es estado —se cambia con la
     * acción de la tarjeta, no escribiéndolo en un formulario— y el segundo es
     * fontanería del bloqueo optimista. Tampoco lleva persona ni proyecto:
     * FR-031 deja esa relación explícitamente fuera de alcance.
     *
     * `currentValue` SE PIDE aquí, y es la razón por la que la tarjeta no lleva
     * botones +/−: el contrato declara `@Min(0)` sin techo, sin unidad y sin
     * relación con `targetValue`, así que no hay ningún "paso" definido que unos
     * botones pudieran aplicar sin inventárselo.
     */
    OBJECTIVE(
        "Nuevo objetivo",
        listOf(
            FormField.Text("title", "¿Qué quieres lograr?"),
            FormField.Number("targetValue", "Meta", required = false),
            FormField.Number("currentValue", "Vas por", required = false),
            FormField.Date("deadline", "Fecha límite", required = false),
        ),
    ),

    /**
     * FR-032. Los TRES obligatorios son reales: `CreateRoutineRequest` marca
     * `title` con `@NotBlank`, y `frequency` y `nextExecutionDate` con
     * `@NotNull`.
     *
     * La fecha la elige el usuario y no la deriva la aplicación: AC-019 deja
     * como TBD explícito de dónde saldría una primera fecha automática.
     *
     * Las tres frecuencias son las únicas aprobadas — su enum declara que "no se
     * añaden recurrencias avanzadas ('cada 2 semanas', días específicos del
     * mes)". Las etiquetas son las que ya usa la Web, no traducciones nuevas.
     *
     * NO lleva `active`: pausar es una acción sobre una rutina que ya existe,
     * no una decisión del alta. Ni `completed`, que no existe en el dominio.
     */
    ROUTINE(
        "Nueva rutina",
        listOf(
            FormField.Text("title", "¿Qué haces con regularidad?"),
            FormField.Choice(
                "frequency", "¿Cada cuánto?",
                VidaVocabulary.routineFrequencies,
            ),
            FormField.Date("nextExecutionDate", "Próxima vez"),
            FormField.Text("description", "Detalle", required = false, multiline = true),
        ),
    ),

    /**
     * FR-034. Se llama `WORK_RESOURCE` y no `RESOURCE` a propósito: en este
     * cliente "resource" ya significa "cualquier cosa creable" —este mismo
     * enum, `ResourceEntry`, `ResourceListScreen`— y además existe
     * `SharedResource`. Un tercer significado del mismo nombre haría el código
     * ilegible. Para el usuario la sección se sigue llamando «Recursos».
     *
     * Solo `name` y `type` son obligatorios: son los únicos con `@NotBlank` y
     * `@NotNull` en `CreateResourceRequest`.
     *
     * `reference` es UN campo libre y no se valida como URL: varios tipos
     * aprobados (MANUAL, PLANTILLA, HERRAMIENTA) no tienen URL, y exigirla les
     * cerraría la puerta. Que sea o no un enlace se decide al MOSTRARLO, no al
     * escribirlo.
     *
     * `description` se pide aquí aunque la Web no lo haga: el backend la acepta
     * y guardarla sin poder escribirla no sirve de nada.
     *
     * Los dos vínculos son opcionales y NO excluyentes — el backend no impone
     * ninguna regla entre ellos, y un recurso sin ninguno es válido.
     */
    WORK_RESOURCE(
        "Nuevo recurso",
        listOf(
            FormField.Text("name", "¿Qué es?"),
            FormField.Choice(
                "type", "Tipo",
                VidaVocabulary.resourceTypes,
            ),
            FormField.Text("reference", "Enlace o referencia", required = false),
            FormField.Text("description", "Detalle", required = false, multiline = true),
            FormField.Reference("personId", "¿De quién?", ReferenceSource.PEOPLE, required = false),
            FormField.Reference("projectId", "¿De qué proyecto?", ReferenceSource.PROJECTS, required = false),
        ),
    ),

    /**
     * FR-033. Un lugar guardado: TRES campos y ninguno más.
     *
     * Solo `name` es obligatorio (`@NotBlank`). `address` es texto libre —el
     * backend NO tiene coordenadas de ningún tipo—, y `personId` permite decir
     * "la oficina de ACME".
     *
     * NO lleva proyecto: el backend no tiene esa relación y no se inventa.
     * Tampoco descripción, categoría, favorito ni estado — ninguno existe.
     */
    PLACE(
        "Nuevo lugar",
        listOf(
            FormField.Text("name", "¿Cómo lo llamas?"),
            FormField.Text("address", "Dirección", required = false),
            FormField.Reference("personId", "¿De quién es?", ReferenceSource.PEOPLE, required = false),
        ),
    ),
}
