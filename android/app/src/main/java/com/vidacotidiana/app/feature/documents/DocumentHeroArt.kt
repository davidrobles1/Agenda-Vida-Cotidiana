package com.vidacotidiana.app.feature.documents

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vidacotidiana.app.core.ui.components.HeroMotif
import com.vidacotidiana.app.core.ui.components.VidaHeroArt

/**
 * La lámina de fondo del detalle de un documento: la común de las fichas
 * (`VidaHeroArt`), con el motivo de su categoría. Imagen REPRESENTATIVA, nunca
 * el documento real (ADR-028).
 */
@Composable
internal fun DocumentHeroArt(categoryKey: String, modifier: Modifier = Modifier) {
    val motif = when (categoryKey) {
        "COMPROBANTES" -> HeroMotif.RECEIPT
        "SEGUROS" -> HeroMotif.SHIELD
        "IDENTIFICACION" -> HeroMotif.ID_CARD
        "CONTRATOS" -> HeroMotif.CONTRACT
        else -> HeroMotif.FOLDER
    }
    VidaHeroArt(motif, categoryVisual(categoryKey).tone, modifier)
}
