package com.vidacotidiana.app.core.vocabulary

/**
 * UX-014/UX-015, ADR-016(d) — vocabulario adaptable por perfil profesional.
 *
 * Transcripción literal de `web/src/core/user/vocabulary.ts`, que a su vez
 * copia la tabla de `design-system.md` §12. Ningún término inventado: los
 * cuatro perfiles y sus ocho palabras son exactamente los aprobados.
 *
 * CAPA DE PRESENTACIÓN PURA. `PROJECT`/`PERSON` son las mismas tablas y los
 * mismos endpoints en cualquier perfil; esto solo cambia cómo se llaman en la
 * interfaz — incluido el cuarto acceso de la barra inferior de Laboral, que
 * toma `projectPlural`.
 */
enum class ProfessionalProfile(
    val id: String,
    val label: String,
    val project: String,
    val projectPlural: String,
    val person: String,
    val personPlural: String,
) {
    CONSULTOR("CONSULTOR", "Consultor tecnológico", "Proyecto", "Proyectos", "Persona", "Personas"),
    ARQUITECTO("ARQUITECTO", "Arquitecto", "Obra", "Obras", "Contacto", "Contactos"),
    // La tabla de §12 mantiene "Persona" para Abogado — no se cambió.
    ABOGADO("ABOGADO", "Abogado", "Caso", "Casos", "Persona", "Personas"),
    VENDEDOR("VENDEDOR", "Vendedor", "Oportunidad", "Oportunidades", "Persona", "Personas");

    companion object {
        val DEFAULT = CONSULTOR
        fun from(id: String?): ProfessionalProfile = entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}
