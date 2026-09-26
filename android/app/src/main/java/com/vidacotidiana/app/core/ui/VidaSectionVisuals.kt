package com.vidacotidiana.app.core.ui

import androidx.annotation.DrawableRes
import com.vidacotidiana.app.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * LA IDENTIDAD VISUAL DE CADA SECCIÓN, en un solo sitio.
 *
 * Las láminas de ilustraciones definen un lenguaje muy concreto: cada
 * sección tiene SU color y SU icono, y esa pareja se repite en todas partes
 * —el acceso rápido del inicio, la baldosa del widget, el estado vacío, la
 * fila de una lista, la barra de abajo—. Lo que hace que el lenguaje se
 * sienta uno solo no son las ilustraciones sueltas: es que Pagos sea SIEMPRE
 * el mismo verde con la misma tarjeta, se dibuje donde se dibuje.
 *
 * Por eso esto es una tabla y no una decisión que toma cada pantalla. Antes
 * cada una elegía su icono y su color a mano, y por eso Inventario salía
 * ámbar en un sitio y coral en otro.
 *
 * LOS TONOS SON LITERALES, no del tema, y es deliberado — el mismo criterio
 * que la Web aplica a los colores de una forma del Vision Board: el color de
 * un módulo es su identidad y debe sobrevivir a que el usuario cambie de
 * tema, igual que el rojo de una marca no cambia porque cambie el fondo. Lo
 * que sí se deriva del tono es el CONTENEDOR (el mismo color muy diluido),
 * así que sobre fondo claro u oscuro sigue siendo un halo del color y nunca
 * un rectángulo que no pega con nada.
 */
data class SectionVisual(
    val label: String,
    val icon: ImageVector,
    /** El color de identidad. Pinta el icono y los acentos. */
    val tint: Color,
    /**
     * LA ILUSTRACIÓN DE LA LÁMINA, recortada de `ilustraciones2.jpeg`
     * («Ilustraciones de acceso rápido (cada módulo)») y empaquetada en
     * `res/drawable-nodpi`.
     *
     * No es un icono de trazo: es el dibujo con volumen y sombra que la
     * propuesta usa, con su propio fondo teñido. Donde hay ilustración se
     * pinta ella; `icon` se queda como respaldo para los sitios donde una
     * imagen de 90 px no funciona —una fila de lista, una pestaña de 20 dp—
     * porque forzarla ahí se vería emborronada.
     */
    @DrawableRes val art: Int? = null,
) {
    /** El halo detrás del icono. Se deriva del tono, no se declara aparte. */
    val container: Color get() = tint.copy(alpha = 0.14f)

    /** Un halo aún más suave, para fondos de tarjeta grandes. */
    val wash: Color get() = tint.copy(alpha = 0.07f)
}

/**
 * Los once colores de las láminas, con sus nombres.
 *
 * No se inventa ninguno: son los que aparecen en «ICONOS DE SECCIONES
 * PRINCIPALES» y en «Iconos principales (secciones del home)».
 */
object VidaSections {
    val Tareas = SectionVisual("Tareas", Icons.AutoMirrored.Outlined.Assignment, Color(0xFF4F46E5), R.drawable.ill_tareas)
    val Pagos = SectionVisual("Pagos", Icons.Outlined.Autorenew, Color(0xFF10B981), R.drawable.ill_pagos)
    val Documentos = SectionVisual("Documentos", Icons.Outlined.Description, Color(0xFFF59E0B), R.drawable.ill_documentos)
    val Inventario = SectionVisual("Inventario", Icons.Outlined.Inventory2, Color(0xFFEF4444), R.drawable.ill_inventario)
    val Compartidos = SectionVisual("Compartidos", Icons.Outlined.Share, Color(0xFF8B5CF6), R.drawable.ill_compartidos)
    val Calendario = SectionVisual("Calendario", Icons.Outlined.CalendarMonth, Color(0xFF3B82F6), R.drawable.ill_agenda)
    val Mantenimiento = SectionVisual("Mantenimiento", Icons.Outlined.Build, Color(0xFFF97316), R.drawable.ill_mantenimiento)
    // La lámina dibuja UN escudo y lo rotula «Mantenimiento». Garantías
    // comparte ese dibujo a propósito: son las dos secciones de «esto está
    // cubierto», y las distingue su color —rosa frente a naranja—, que es lo
    // que la propia lámina usa para separarlas.
    val Garantias = SectionVisual("Garantías", Icons.Outlined.VerifiedUser, Color(0xFFE11D48), R.drawable.ill_mantenimiento)
    val VisionBoard = SectionVisual("Vision Board", Icons.Outlined.Star, Color(0xFF14B8A6), R.drawable.ill_visionboard)
    val Familia = SectionVisual("Familia", Icons.Outlined.Groups, Color(0xFFEC4899), R.drawable.ill_familia)
    val Ajustes = SectionVisual("Ajustes", Icons.Outlined.Settings, Color(0xFF6B7280))

    /*
     * LAS DE LABORAL Y LAS SECUNDARIAS.
     *
     * Las láminas no las dibujan —enumeran las once de Personal— pero lo que
     * definen no es una lista cerrada: es una GRAMÁTICA (icono dentro de
     * cuadrado teñido, un color por sección). Dejar estas seis sin identidad
     * habría producido exactamente lo que no se quiere: unas pantallas con el
     * lenguaje nuevo y otras con el anterior.
     *
     * Los tonos salen de la misma paleta de las láminas, sin estrenar
     * ninguno: se reparten los que ya usan las secciones hermanas.
     */
    val Personas = SectionVisual("Personas", Icons.Outlined.Groups, Color(0xFFEC4899))
    val Proyectos = SectionVisual("Proyectos", Icons.Outlined.Description, Color(0xFF3B82F6))
    val Seguimientos = SectionVisual("Seguimientos", Icons.Outlined.Autorenew, Color(0xFFF59E0B))
    val Objetivos = SectionVisual("Objetivos", Icons.Outlined.Flag, Color(0xFF8B5CF6))
    val Recursos = SectionVisual("Recursos", Icons.Outlined.FolderOpen, Color(0xFF4F46E5))
    val Lugares = SectionVisual("Lugares", Icons.Outlined.Place, Color(0xFF10B981))
    val Inbox = SectionVisual("Inbox", Icons.Outlined.Inbox, Color(0xFF6B7280))

    /** Las cinco del acceso rápido del inicio, en el orden de la lámina. */
    val quickAccess = listOf(Tareas, Pagos, Documentos, Inventario, Compartidos)

    val all = listOf(
        Tareas, Pagos, Documentos, Inventario, Compartidos,
        Calendario, Mantenimiento, Garantias, VisionBoard, Familia, Ajustes,
        Personas, Proyectos, Seguimientos, Objetivos, Recursos, Lugares, Inbox,
    )

    /**
     * La identidad de una ruta.
     *
     * Existe para que la barra de abajo, el cajón y cualquier lista puedan
     * teñirse sin repetir la tabla. Una ruta que no esté aquí devuelve
     * `null` en vez de un color inventado: mejor el color del tema que uno
     * que no significa nada.
     */
    fun forRoute(route: String?): SectionVisual? = when (route) {
        "tasks", "tasks_lab" -> Tareas
        "payments" -> Pagos
        "documents" -> Documentos
        "inventory" -> Inventario
        "shared" -> Compartidos
        "calendar", "agenda" -> Calendario
        "maintenance" -> Mantenimiento
        "warranties" -> Garantias
        "board" -> VisionBoard
        "family" -> Familia
        "settings" -> Ajustes
        "people" -> Personas
        "projects" -> Proyectos
        "commitments" -> Seguimientos
        "objectives" -> Objetivos
        "work_resources" -> Recursos
        "places" -> Lugares
        "inbox" -> Inbox
        else -> null
    }
}
