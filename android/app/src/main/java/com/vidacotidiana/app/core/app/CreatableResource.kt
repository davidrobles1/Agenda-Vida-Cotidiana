package com.vidacotidiana.app.core.app

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

enum class ReferenceSource { PEOPLE, PROJECTS }

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
        ),
    ),

    WARRANTY(
        "Nueva garantía",
        listOf(
            FormField.Text("item", "¿Qué producto?"),
            FormField.Date("expiresAt", "Vence el"),
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
                listOf("HOGAR" to "Hogar", "ELECTRONICOS" to "Electrónicos", "VEHICULOS" to "Vehículos"),
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
                listOf(
                    "IDENTIFICACION" to "Identificación",
                    "COMPROBANTES" to "Comprobantes",
                    "SEGUROS" to "Seguros",
                    "CONTRATOS" to "Contratos",
                    "OTROS" to "Otros",
                ),
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
}
