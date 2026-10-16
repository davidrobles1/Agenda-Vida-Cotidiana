package com.vidacotidiana.app.feature.resources

import com.vidacotidiana.app.core.app.CreatableResource
import com.vidacotidiana.app.core.ui.SectionVisual
import com.vidacotidiana.app.core.ui.VidaSections
import com.vidacotidiana.app.core.ui.components.HeroMotif

/**
 * Cómo se presenta un tipo de registro en su ficha (ADR-028): la palabra en
 * singular del rótulo, la identidad de su sección (icono y color) y el motivo
 * de la lámina de fondo.
 */
data class ResourceIdentity(val label: String, val section: SectionVisual, val motif: HeroMotif)

val CreatableResource.identity: ResourceIdentity
    get() = when (this) {
        CreatableResource.TASK -> ResourceIdentity("Tarea", VidaSections.Tareas, HeroMotif.CHECKLIST)
        CreatableResource.PAYMENT -> ResourceIdentity("Pago", VidaSections.Pagos, HeroMotif.PAYMENT)
        CreatableResource.MAINTENANCE -> ResourceIdentity("Mantenimiento", VidaSections.Mantenimiento, HeroMotif.WRENCH)
        CreatableResource.WARRANTY -> ResourceIdentity("Garantía", VidaSections.Garantias, HeroMotif.WARRANTY)
        CreatableResource.INVENTORY -> ResourceIdentity("Artículo", VidaSections.Inventario, HeroMotif.BOX)
        CreatableResource.DOCUMENT -> ResourceIdentity("Documento", VidaSections.Documentos, HeroMotif.FOLDER)
        CreatableResource.PERSON -> ResourceIdentity("Persona", VidaSections.Personas, HeroMotif.PERSON)
        CreatableResource.PROJECT -> ResourceIdentity("Proyecto", VidaSections.Proyectos, HeroMotif.PROJECT)
        CreatableResource.COMMITMENT -> ResourceIdentity("Seguimiento", VidaSections.Seguimientos, HeroMotif.COMMITMENT)
        CreatableResource.NOTE -> ResourceIdentity("Nota", VidaSections.Calendario, HeroMotif.NOTE)
        CreatableResource.OBJECTIVE -> ResourceIdentity("Objetivo", VidaSections.Objetivos, HeroMotif.TARGET)
        CreatableResource.ROUTINE -> ResourceIdentity("Rutina", VidaSections.Calendario, HeroMotif.ROUTINE)
        CreatableResource.WORK_RESOURCE -> ResourceIdentity("Recurso", VidaSections.Recursos, HeroMotif.LINK)
        CreatableResource.PLACE -> ResourceIdentity("Lugar", VidaSections.Lugares, HeroMotif.PLACE)
    }
