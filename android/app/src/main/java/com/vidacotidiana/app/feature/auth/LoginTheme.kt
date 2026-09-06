package com.vidacotidiana.app.feature.auth

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Los tokens del template de login de la Web, transcritos.
 *
 * La pantalla de entrada NO se viste con los nueve temas de ADR-023: todavía
 * no hay usuario, así que no hay preferencia de tema que aplicar, y el Custom
 * Tab de Keycloak que viene justo después tampoco cambia de color. Un login
 * que cambiara de piel y una página de credenciales que no, se leerían como
 * dos productos distintos en mitad del mismo gesto.
 *
 * Valores tomados de `infra/keycloak/themes/vida-cotidiana-web/login/resources/css/login.css`
 * (`:root`), no aproximados a ojo.
 */
object LoginTheme {
    val primary = Color(0xFF2C5F8C)
    val onPrimary = Color(0xFFFFFFFF)
    val terracotta = Color(0xFFB47B48)
    val surface = Color(0xFFF5EEDF)
    val surfaceVariant = Color(0xFFFFFCF6)
    val border = Color(0xFF6E8192)
    val text = Color(0xFF2C425B)
    val textSecondary = Color(0xFF556479)

    /** Verificados WCAG AA en design-system.md §1 (ACC-001). */
    val error = Color(0xFFB91C1C)
    val errorContainer = Color(0xFFFEE2E2)

    /**
     * `--vc-control-h`. Login y «Crear una cuenta» DEBEN compartirlo: que no
     * lo hicieran es parte de por qué se leían como dos botones distintos.
     * Sube de 50 a 52 dp por el mínimo táctil de Android, no por diseño.
     */
    val controlHeight = 52.dp

    /** El icono de marca de la tarjeta: primario aclarado al 70 % sobre blanco. */
    val brandIcon = Color(0xFF6C93B4)

    /** `color-mix(--color-primary 70%, white)` — los iconos de la fila de valores. */
    val valueIcon = Color(0xFF6C93B4)

    /** Borde de la tarjeta: `--color-text` al 15 %. */
    val cardBorder = Color(0x262C425B)

    /** Filete del pie y divisor del hero. */
    val hairline = Color(0x1F2C425B)
}
