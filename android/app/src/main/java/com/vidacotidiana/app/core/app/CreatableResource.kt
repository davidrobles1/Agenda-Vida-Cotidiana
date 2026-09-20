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
 *
 * ADEMÁS DE QUÉ SE PIDE, AQUÍ SE DECLARA CÓMO SE RESPONDE: el icono de la fila,
 * el ejemplo que enseña el grano esperado y los atajos de fecha razonables para
 * ESE campo. Son datos del dominio, no del dibujo — «cada 3, 6 o 12 meses» es
 * cierto de un mantenimiento y falso de un importe, y una garantía vence en años
 * mientras un seguimiento vence en días. Ponerlos en la hoja habría obligado a
 * la hoja a saber de qué recurso está hablando, que es justo lo que este archivo
 * existe para evitar.
 */

/**
 * El icono de la fila. Enum y no `ImageVector` para que el contrato no dependa
 * de Compose: aquí se dice QUÉ representa el campo, y el dibujo lo resuelve la
 * hoja.
 */
enum class FieldIcon { PEN, CLOCK, CALENDAR, PIN, DOC, COIN, BOX, PERSON, REPEAT, LINK, CLIP, FLAG, TAG, HASH, FOLDER }

/**
 * La familia de color del icono.
 *
 * CINCO, y las cinco salen de tokens que los nueve temas ya definen
 * (ADR-023: los temas reescriben variables, no se duplican componentes). No hay
 * ni un color nuevo, así que la hoja se ve correcta en Papel, en Neo y en Lumen
 * sin tocar nada.
 */
enum class FieldTone { TIME, PLACE, TEXT, DATA, FILE }

/**
 * Un atajo de fecha. Cada uno se rotula con lo que pone y, al abrirlo, la hoja
 * enseña la fecha exacta que resultaría: ninguno es un valor por defecto, no
 * hay nada puesto hasta que se toca.
 */
enum class DateShortcut { HOY, MANANA, SEMANA, MES, TRES_MESES, SEIS_MESES, ANIO, DOS_ANIOS }

/** Un campo del formulario. `required` refleja el `@NotNull`/`@NotBlank` real. */
sealed interface FormField {
    val key: String
    val label: String
    val required: Boolean
    val icon: FieldIcon
    val tone: FieldTone

    data class Text(
        override val key: String,
        override val label: String,
        override val required: Boolean = true,
        val multiline: Boolean = false,
        /**
         * El grano que se espera, como ejemplo.
         *
         * NO es el rótulo otra vez. Antes el marcador de posición recibía
         * `field.label`, así que cada campo decía su nombre dos veces —una
         * arriba y otra dentro— y el hueco donde cabía una pista útil estaba
         * ocupado por un eco.
         */
        val example: String = "",
        override val icon: FieldIcon = FieldIcon.PEN,
        override val tone: FieldTone = FieldTone.TEXT,
    ) : FormField

    data class Number(
        override val key: String,
        override val label: String,
        override val required: Boolean = false,
        val decimal: Boolean = false,
        val example: String = "",
        /**
         * Los valores que este campo tiene de verdad casi siempre.
         *
         * Vacío cuando el dominio no los tiene: un importe puede ser cualquier
         * cifra y ofrecer tres sería fingir que hay unas habituales. Un
         * intervalo de mantenimiento sí las tiene —3, 6 o 12 meses— y teclear
         * «6» a mano es trabajo que no hacía falta.
         */
        val suggestions: List<String> = emptyList(),
        override val icon: FieldIcon = FieldIcon.HASH,
        override val tone: FieldTone = FieldTone.DATA,
    ) : FormField

    /** Selector de fecha. El valor viaja como ISO local (`2026-05-12`). */
    data class Date(
        override val key: String,
        override val label: String,
        override val required: Boolean = true,
        /**
         * Los atajos que tienen sentido para ESTE campo. Una garantía vence en
         * años y un seguimiento en días: una sola tira para los siete campos de
         * fecha habría estado mal en casi todos.
         */
        val shortcuts: List<DateShortcut> = listOf(DateShortcut.HOY, DateShortcut.MANANA, DateShortcut.SEMANA),
        override val icon: FieldIcon = FieldIcon.CALENDAR,
        override val tone: FieldTone = FieldTone.TIME,
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
        val shortcuts: List<DateShortcut> = listOf(DateShortcut.HOY, DateShortcut.MANANA, DateShortcut.SEMANA),
        override val icon: FieldIcon = FieldIcon.CLOCK,
        override val tone: FieldTone = FieldTone.TIME,
    ) : FormField

    /** Opciones fijas: el valor guardado es el del enum, la etiqueta es la que se lee. */
    data class Choice(
        override val key: String,
        override val label: String,
        val options: List<Pair<String, String>>,
        override val required: Boolean = true,
        override val icon: FieldIcon = FieldIcon.TAG,
        override val tone: FieldTone = FieldTone.DATA,
    ) : FormField

    /** Elegido entre datos que el usuario ya tiene (personas, proyectos). */
    data class Reference(
        override val key: String,
        override val label: String,
        val source: ReferenceSource,
        override val required: Boolean = true,
        override val icon: FieldIcon = FieldIcon.BOX,
        override val tone: FieldTone = FieldTone.PLACE,
    ) : FormField

    /** Archivo del dispositivo. `mime` restringe lo que ofrece el selector. */
    data class File(
        override val key: String,
        override val label: String,
        val mime: String = "*/*",
        override val required: Boolean = true,
        override val icon: FieldIcon = FieldIcon.CLIP,
        override val tone: FieldTone = FieldTone.FILE,
    ) : FormField
}

enum class ReferenceSource { PEOPLE, PROJECTS, INVENTORY }

enum class CreatableResource(
    val title: String,
    val fields: List<FormField>,
    /**
     * El verbo del botón. «Crear» en casi todo; Documentos SUBE un archivo y
     * una nota se GUARDA. Decir «Crear» sobre una subida describe mal lo que
     * va a pasar.
     */
    val verb: String = "Crear",
) {
    // La Web rotula este campo «Fecha y hora (opcional)» y deja guardar sin
    // él: una tarea sin fecha existe y se ve en Tareas, solo que no en el
    // calendario. Esa regla se conserva tal cual.
    TASK(
        "Nueva tarea",
        listOf(
            FormField.Text(
                "title", "¿Qué hay que hacer?",
                example = "Cambiar el aceite del coche",
                icon = FieldIcon.PEN, tone = FieldTone.TEXT,
            ),
            FormField.DateTime(
                "dueAt", "Fecha y hora", required = false,
                shortcuts = listOf(DateShortcut.HOY, DateShortcut.MANANA, DateShortcut.SEMANA),
            ),
            // ADR-016/FR-024. TEXTO LIBRE: el backend guarda un `VARCHAR(500)`
            // sin ninguna validación de formato, así que aquí tampoco se valida
            // ni se transforma. No es un lugar del catálogo —esa es otra
            // funcionalidad— sino "dónde es", escrito a mano.
            FormField.Text(
                "location", "¿Dónde?", required = false,
                example = "Taller de la esquina",
                icon = FieldIcon.PIN, tone = FieldTone.PLACE,
            ),
            FormField.Text(
                "description", "Detalle", required = false, multiline = true,
                example = "Llevar la cartilla de revisiones",
                icon = FieldIcon.DOC, tone = FieldTone.TEXT,
            ),
        ),
    ),

    // ADR-020: el importe vive aquí porque Pagos responde "cuánto representa".
    PAYMENT(
        "Agregar pago",
        listOf(
            FormField.Text(
                "service", "¿Qué pagas?",
                example = "Internet de casa",
                icon = FieldIcon.PEN, tone = FieldTone.TEXT,
            ),
            FormField.Date(
                "nextPaymentDate", "Próximo pago",
                shortcuts = listOf(DateShortcut.SEMANA, DateShortcut.MES),
            ),
            FormField.Choice(
                "billingCycle", "Cada cuánto",
                listOf("MONTHLY" to "Mensual", "YEARLY" to "Anual", "WEEKLY" to "Semanal"),
                icon = FieldIcon.REPEAT,
            ),
            // SIN sugerencias: un importe puede ser cualquier cifra, y ofrecer
            // tres fingiría que hay unas habituales.
            FormField.Number(
                "amount", "Importe", required = false, decimal = true,
                example = "549.00", icon = FieldIcon.COIN,
            ),
        ),
    ),

    MAINTENANCE(
        "Nuevo mantenimiento",
        listOf(
            FormField.Text(
                "item", "¿Qué hay que revisar?",
                example = "Filtros del aire acondicionado",
                icon = FieldIcon.PEN, tone = FieldTone.TEXT,
            ),
            FormField.Date(
                "nextDueAt", "Próxima vez",
                shortcuts = listOf(DateShortcut.MES, DateShortcut.TRES_MESES, DateShortcut.SEIS_MESES),
            ),
            FormField.Number(
                "intervalMonths", "Cada cuántos meses", required = false,
                example = "6", suggestions = listOf("3", "6", "12"), icon = FieldIcon.REPEAT,
            ),
            // V31: OPCIONAL, a diferencia del de la garantía. También se
            // mantiene lo que no es un artículo inventariado —el techo, el
            // jardín—, y obligarlo aquí expulsaría casos reales.
            FormField.Reference(
                "inventoryItemId", "¿A qué artículo?", ReferenceSource.INVENTORY, required = false,
                icon = FieldIcon.BOX,
            ),
        ),
    ),

    WARRANTY(
        "Nueva garantía",
        listOf(
            FormField.Text(
                "item", "¿Qué producto?",
                example = "Lavadora Bosch",
                icon = FieldIcon.PEN, tone = FieldTone.TEXT,
            ),
            FormField.Date(
                "expiresAt", "Vence el",
                // Una garantía vence en AÑOS. Ofrecerle «Mañana» habría sido
                // copiar la tira de la tarea sin mirar el dominio.
                shortcuts = listOf(DateShortcut.ANIO, DateShortcut.DOS_ANIOS, DateShortcut.SEIS_MESES),
            ),
            // Obligatorio de verdad: una garantía siempre cubre un artículo del
            // inventario, y el backend rechaza el alta sin él
            // (`WarrantyService#create`, DECISION del 2026-09-06).
            FormField.Reference(
                "inventoryItemId", "¿Qué artículo cubre?", ReferenceSource.INVENTORY,
                icon = FieldIcon.BOX,
            ),
            // Obligatorio de verdad: el endpoint es multipart y exige el archivo.
            FormField.File("file", "Comprobante"),
        ),
    ),

    INVENTORY(
        "Nuevo artículo",
        listOf(
            FormField.Text(
                "name", "¿Qué es?",
                example = "Taladro", icon = FieldIcon.BOX, tone = FieldTone.TEXT,
            ),
            FormField.Choice("category", "Categoría", VidaVocabulary.inventoryCategories),
            FormField.Text(
                "location", "¿Dónde está?", required = false,
                example = "Garaje, estante de arriba",
                icon = FieldIcon.PIN, tone = FieldTone.PLACE,
            ),
        ),
    ),

    DOCUMENT(
        "Subir documento",
        listOf(
            FormField.Text(
                "name", "Nombre",
                example = "Recibo de la lavadora",
                icon = FieldIcon.DOC, tone = FieldTone.TEXT,
            ),
            FormField.Choice("category", "Categoría", VidaVocabulary.documentCategories),
            FormField.File("file", "Archivo"),
        ),
        verb = "Subir",
    ),

    PERSON(
        "Nueva persona",
        listOf(
            FormField.Text(
                "name", "Nombre",
                example = "Liz Márquez", icon = FieldIcon.PERSON, tone = FieldTone.PLACE,
            ),
            FormField.Text(
                "role", "Rol", required = false,
                example = "Contacto de compras", icon = FieldIcon.TAG, tone = FieldTone.DATA,
            ),
            FormField.Text(
                "organization", "Organización", required = false,
                example = "VantiSoft", icon = FieldIcon.FOLDER, tone = FieldTone.TIME,
            ),
        ),
    ),

    PROJECT(
        "Nuevo proyecto",
        listOf(
            FormField.Text(
                "name", "Nombre",
                example = "Migración del portal", icon = FieldIcon.FOLDER, tone = FieldTone.TEXT,
            ),
            // `Project.status` es texto libre en el modelo: se pregunta, no se
            // impone una lista de estados que el dominio no tiene.
            FormField.Text(
                "status", "Estado", required = false,
                example = "En curso", icon = FieldIcon.TAG, tone = FieldTone.DATA,
            ),
            FormField.Date(
                "deadline", "Entrega", required = false,
                shortcuts = listOf(DateShortcut.SEMANA, DateShortcut.MES, DateShortcut.TRES_MESES),
            ),
        ),
    ),

    COMMITMENT(
        "Nuevo seguimiento",
        listOf(
            FormField.Text(
                "description", "¿Qué se acordó?",
                example = "Mandar la cotización revisada",
                icon = FieldIcon.PEN, tone = FieldTone.TEXT,
            ),
            FormField.Reference(
                "personId", "¿Con quién?", ReferenceSource.PEOPLE,
                icon = FieldIcon.PERSON,
            ),
            FormField.Choice(
                "direction", "¿De quién depende?",
                listOf("MINE" to "Me toca a mí", "THEIRS" to "Lo espero"),
                icon = FieldIcon.REPEAT,
            ),
            FormField.Date(
                "dueAt", "Para cuándo",
                shortcuts = listOf(DateShortcut.HOY, DateShortcut.MANANA, DateShortcut.SEMANA),
            ),
        ),
    ),

    NOTE(
        "Nueva nota",
        listOf(
            FormField.Text(
                "title", "Título",
                example = "Preguntar por la garantía",
                icon = FieldIcon.PEN, tone = FieldTone.TEXT,
            ),
            FormField.Text(
                "description", "Detalle", required = false, multiline = true,
                example = "Creo que aún cubre el motor",
                icon = FieldIcon.DOC, tone = FieldTone.TEXT,
            ),
        ),
        verb = "Guardar",
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
     * botones pudieran aplicar sin inventárselo. Por lo mismo tampoco hay
     * sugerencias: no sabemos si la meta se mide en libros, kilómetros o euros.
     */
    OBJECTIVE(
        "Nuevo objetivo",
        listOf(
            FormField.Text(
                "title", "¿Qué quieres lograr?",
                example = "Leer 12 libros este año",
                icon = FieldIcon.FLAG, tone = FieldTone.TEXT,
            ),
            FormField.Number("targetValue", "Meta", example = "12"),
            FormField.Number("currentValue", "Vas por", example = "4"),
            FormField.Date(
                "deadline", "Fecha límite", required = false,
                shortcuts = listOf(DateShortcut.MES, DateShortcut.TRES_MESES, DateShortcut.ANIO),
            ),
        ),
    ),

    /**
     * FR-032. Los TRES obligatorios son reales: `CreateRoutineRequest` marca
     * `title` con `@NotBlank`, y `frequency` y `nextExecutionDate` con
     * `@NotNull`.
     *
     * La fecha la elige el usuario y no la deriva la aplicación: AC-019 deja
     * como TBD explícito de dónde saldría una primera fecha automática. Los
     * atajos no cambian eso: no hay nada puesto hasta que se toca uno, y el que
     * se toca dice qué pone.
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
            FormField.Text(
                "title", "¿Qué haces con regularidad?",
                example = "Regar las plantas",
                icon = FieldIcon.REPEAT, tone = FieldTone.TEXT,
            ),
            FormField.Choice(
                "frequency", "¿Cada cuánto?", VidaVocabulary.routineFrequencies,
                icon = FieldIcon.REPEAT,
            ),
            FormField.Date(
                "nextExecutionDate", "Próxima vez",
                shortcuts = listOf(DateShortcut.HOY, DateShortcut.MANANA, DateShortcut.SEMANA),
            ),
            FormField.Text(
                "description", "Detalle", required = false, multiline = true,
                example = "Las del balcón necesitan más",
                icon = FieldIcon.DOC, tone = FieldTone.TEXT,
            ),
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
            FormField.Text(
                "name", "¿Qué es?",
                example = "Plantilla de cotización",
                icon = FieldIcon.PEN, tone = FieldTone.TEXT,
            ),
            FormField.Choice("type", "Tipo", VidaVocabulary.resourceTypes),
            FormField.Text(
                "reference", "Enlace o referencia", required = false,
                example = "drive.com/plantilla", icon = FieldIcon.LINK, tone = FieldTone.DATA,
            ),
            FormField.Text(
                "description", "Detalle", required = false, multiline = true,
                example = "La versión de 2026", icon = FieldIcon.DOC, tone = FieldTone.TEXT,
            ),
            FormField.Reference(
                "personId", "¿De quién?", ReferenceSource.PEOPLE, required = false,
                icon = FieldIcon.PERSON,
            ),
            FormField.Reference(
                "projectId", "¿De qué proyecto?", ReferenceSource.PROJECTS, required = false,
                icon = FieldIcon.FOLDER, tone = FieldTone.TIME,
            ),
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
            FormField.Text(
                "name", "¿Cómo lo llamas?",
                example = "Oficina de VantiSoft",
                icon = FieldIcon.PIN, tone = FieldTone.TEXT,
            ),
            FormField.Text(
                "address", "Dirección", required = false,
                example = "Av. Reforma 222", icon = FieldIcon.PIN, tone = FieldTone.PLACE,
            ),
            FormField.Reference(
                "personId", "¿De quién es?", ReferenceSource.PEOPLE, required = false,
                icon = FieldIcon.PERSON,
            ),
        ),
    ),
    ;

    /**
     * El campo que abre la hoja: el primero, cuando es de texto.
     *
     * Hoy lo es en los catorce —comprobado uno a uno—, y es lo que permite que
     * «uno abierto, el resto en filas» valga para todas sin excepciones por
     * recurso. Se devuelve NULO en vez de forzar la conversión para que un alta
     * futura que no empiece por texto caiga con elegancia en «todo en filas»
     * en lugar de reventar al abrirse.
     */
    val headField: FormField.Text? get() = fields.firstOrNull() as? FormField.Text

    /** Todo lo demás, que vive en filas. */
    val rowFields: List<FormField> get() = if (headField != null) fields.drop(1) else fields
}
