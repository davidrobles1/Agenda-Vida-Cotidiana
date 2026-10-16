package com.vidacotidiana.app.feature.wellbeing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.vidacotidiana.app.core.app.AppViewModel
import com.vidacotidiana.app.core.ui.VidaLayout
import com.vidacotidiana.app.core.ui.VidaTheme
import com.vidacotidiana.app.core.ui.VidaFonts
import com.vidacotidiana.app.core.ui.components.MoodFilledFace
import com.vidacotidiana.app.core.ui.components.VidaMoodCard
import com.vidacotidiana.app.core.ui.components.vidaBrandInk
import androidx.compose.ui.unit.em
import com.vidacotidiana.app.core.ui.components.subeSobreElTeclado
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.MoodScale
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaError
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.vidaClickable
import androidx.compose.foundation.Canvas
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.vidacotidiana.app.core.ui.components.VidaMetricRing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize

/**
 * BIENESTAR — la semana del ánimo, en el lenguaje de Ánimo v3 de Inicio.
 *
 * Arriba va LA MISMA tarjeta que Inicio (`VidaMoodCard`) en su versión grande:
 * la pregunta con tu nombre, la cara rellena flotando sobre su halo, las cinco
 * caritas que respiran, la mariposa que se posa y la respuesta de la app. Así
 * marcar el ánimo se siente igual en las dos pantallas y hay una sola pieza
 * que mantener.
 *
 * El resto sigue la gramática de Inicio: títulos de zona en versalitas,
 * tarjetas blancas de radio 24 con filete, azul tinta para lo seleccionado y
 * las caritas rellenas —no las de trazo— en la semana.
 *
 * DATOS REALES. Todo sale de `/api/v1/moods` (V36) vía `WellbeingRepository`.
 * Cuando la API falla se enseña el error; no hay valor de reserva que aparente
 * un ánimo que el usuario nunca marcó.
 *
 * ESTO ES DATO DE SALUD (ver `37-clasificacion-datos.md`): «Solo tú» en la
 * cabecera y el aviso del final son la promesa hecha visible.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BienestarScreen(viewModel: AppViewModel, navController: NavHostController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val t = VidaTheme.type

    // La semana se pide solo aquí: en Inicio la cara es pequeña y le basta hoy.
    LaunchedEffect(Unit) {
        viewModel.loadMood(includeWeek = true)
        viewModel.loadUser()
    }

    val marked = state.moodToday?.value
    val ink = vidaBrandInk()

    // Las etiquetas del «¿qué lo hizo así?» viven en la pantalla porque son
    // parte del gesto de marcar, no un estado que deba sobrevivir a salir.
    var tags by remember(state.moodToday?.id) {
        mutableStateOf(state.moodToday?.tags.orEmpty().toSet())
    }

    VidaScreen(
        title = "Bienestar",
        showBack = true,
        onNavigationClick = { navController.popBackStack() },
        actions = {
            Row(
                modifier = Modifier
                    .background(c.sunken, RoundedCornerShape(50))
                    .padding(horizontal = 13.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = c.textSecondary,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    "Solo tú",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                    color = c.textSecondary,
                )
            }
        },
    ) {
        when {
            // El error se enseña y no se disimula: sin ánimo real no se pinta
            // ninguna cara que parezca marcada.
            state.moodError != null && state.moodToday == null && !state.moodLoading -> {
                VidaError(
                    title = "No se pudo cargar tu bienestar",
                    body = state.moodError ?: "",
                    onRetry = { viewModel.loadMood(includeWeek = true) },
                )
            }

            state.moodLoading && state.moodToday == null && state.moodWeek.isEmpty() -> {
                LoadingRows(2)
            }

            else -> {
                /* ── HOY: la misma tarjeta que Inicio, en grande ─────────── */
                StaggeredAppear(0) {
                    VidaMoodCard(
                        name = state.user?.username?.takeIf { it.isNotBlank() },
                        marked = marked,
                        loading = state.moodLoading,
                        failed = state.moodError != null,
                        onMark = { viewModel.setMood(it, state.moodToday?.note, tags.toList()) },
                        onOpenWellbeing = null,
                        hero = true,
                    )
                }

                /* ── ¿QUÉ LO HIZO ASÍ? ───────────────────────────────────── */
                StaggeredAppear(1) { ZoneTitle("¿Qué lo hizo así?", "Opcional") }
                StaggeredAppear(2) {
                    val options = listOf("Dormí bien", "Familia", "Trabajo", "Me moví", "Pendientes", "Nada en concreto")
                    // Retícula fluida: caben todos y verlos todos es el punto.
                    // Seleccionado = azul tinta, como la carita y el día de hoy.
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        options.forEach { label ->
                            val on = label in tags
                            val fill = if (VidaTheme.spec.isDark) Color(0xFF2B2870) else ink
                            Box(
                                modifier = Modifier
                                    .background(if (on) fill else c.surfaceVariant, RoundedCornerShape(50))
                                    .border(1.dp, if (on) fill else c.line, RoundedCornerShape(50))
                                    .vidaClickable(
                                        onClick = {
                                            tags = if (on) tags - label else tags + label
                                            marked?.let { viewModel.setMood(it, state.moodToday?.note, tags.toList()) }
                                        },
                                    )
                                    .padding(horizontal = 15.dp, vertical = 9.dp),
                            ) {
                                Text(
                                    label,
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontFamily = VidaFonts.Sora, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp,
                                    ),
                                    color = if (on) Color.White else c.textSecondary,
                                )
                            }
                        }
                    }
                }

                /* ── LA NOTA, A MANO ─────────────────────────────────────── */
                StaggeredAppear(3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(c.surfaceVariant, RoundedCornerShape(24.dp))
                            .border(1.dp, c.line, RoundedCornerShape(24.dp))
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = c.textTertiary,
                            modifier = Modifier.size(18.dp),
                        )
                        val noteStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = VidaTheme.spec.fonts.hand ?: VidaTheme.spec.fonts.body,
                            fontSize = 22.sp,
                            lineHeight = 26.sp,
                        )
                        if (marked == null) {
                            // Sin ánimo marcado no hay a qué colgar la nota: el
                            // registro del día nace al elegir la cara. Decirlo
                            // es mejor que ofrecer un campo que no podría
                            // guardar.
                            Text(
                                "Marca cómo te sientes y podrás escribir sobre hoy.",
                                style = noteStyle,
                                color = c.textTertiary,
                            )
                        } else {
                            val focus = LocalFocusManager.current
                            var draft by remember(state.moodToday?.id, state.moodToday?.note) {
                                mutableStateOf(state.moodToday?.note.orEmpty())
                            }
                            fun commit() {
                                val clean = draft.trim()
                                if (clean != state.moodToday?.note.orEmpty()) {
                                    viewModel.setMood(marked, clean, tags.toList())
                                }
                            }
                            BasicTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                textStyle = noteStyle.copy(color = c.text),
                                cursorBrush = SolidColor(c.primary),
                                // UNA línea, como dice el propio texto de
                                // sugerencia. Sin esto el «Listo» del teclado
                                // metía un salto de línea en vez de confirmar,
                                // así que la nota no llegaba a guardarse nunca.
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
    imeAction = ImeAction.Done,
    // La misma regla que `VidaTextField`: una nota es una frase.
    capitalization = KeyboardCapitalization.Sentences,
),
                                keyboardActions = KeyboardActions(onDone = {
                                    commit()
                                    focus.clearFocus()
                                }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .subeSobreElTeclado()
                                    .onFocusChanged { if (!it.isFocused) commit() },
                                decorationBox = { inner ->
                                    if (draft.isEmpty()) {
                                        Text(
                                            "Escribe una línea sobre hoy…",
                                            style = noteStyle,
                                            color = c.textTertiary,
                                        )
                                    }
                                    inner()
                                },
                            )
                        }
                    }
                }

                /* ── TU SEMANA ───────────────────────────────────────────── */
                StaggeredAppear(4) { ZoneTitle("Tu semana") }
                StaggeredAppear(5) {
                    val today = LocalDate.now()
                    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
                    val byDate = state.moodWeek.associateBy { it.date }
                    // Las caritas RELLENAS de la tarjeta de arriba, no las de
                    // trazo: el ánimo tiene que verse igual en todas partes.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(c.surfaceVariant, RoundedCornerShape(24.dp))
                            .border(1.dp, c.line, RoundedCornerShape(24.dp))
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        days.forEach { day ->
                            val mood = byDate[day]?.value
                            val isToday = day == today
                            Column(
                                Modifier
                                    .weight(1f)
                                    .background(
                                        if (isToday) (if (VidaTheme.spec.isDark) Color(0xFF2B2870) else ink).copy(alpha = 0.08f) else Color.Transparent,
                                        RoundedCornerShape(16.dp),
                                    )
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                // Un día sin marcar queda VACÍO: no marcar no es
                                // sentirse normal.
                                if (mood != null) {
                                    MoodFilledFace(mood, Modifier.size(32.dp))
                                } else {
                                    Box(Modifier.size(32.dp).border(1.5.dp, c.border, CircleShape))
                                }
                                Text(
                                    if (isToday) "HOY" else day.dayOfWeek
                                        .getDisplayName(TextStyle.NARROW, Locale("es", "MX")).uppercase(),
                                    style = androidx.compose.ui.text.TextStyle(
                                        fontFamily = VidaFonts.Sora, fontWeight = FontWeight.SemiBold, fontSize = 10.sp,
                                    ),
                                    color = if (isToday) ink else c.textSecondary,
                                )
                            }
                        }
                    }
                }

                /* ── EL MES, EN CIFRAS ───────────────────────────────────── */
                StaggeredAppear(6) {
                    val good = state.moodWeek.count { it.value <= MoodScale.BIEN }
                    // LAS DOS TARJETAS, A LA MISMA ALTURA. Con alturas
                    // naturales la de la derecha quedaba más alta y el par se
                    // leía como dos piezas sueltas en vez de como una fila.
                    Row(
                        Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        /* «INDICADORES Y GRÁFICAS» de la lámina: la cifra va
                           DENTRO de un anillo que enseña la proporción, sobre
                           tarjeta clara.

                           Era un bloque verde macizo con un 0 enorme en
                           blanco. Decía menos —un número suelto no dice si 3
                           de 7 es mucho— y además era la única superficie de
                           color pleno que quedaba en la aplicación después de
                           pasar todo lo demás al lenguaje de las láminas. */
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(c.surfaceVariant, RoundedCornerShape(24.dp))
                                .border(
                                    1.dp, c.line,
                                    RoundedCornerShape(24.dp),
                                )
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            VidaMetricRing(
                                // Sin nada marcado el anillo se queda vacío en
                                // vez de completo: cero de cero no es un pleno.
                                progress = if (state.moodWeek.isEmpty()) 0f else good.toFloat() / state.moodWeek.size,
                                figure = good.toString(),
                                tint = ink,
                                size = 48.dp,
                                stroke = 5.dp,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    "Días buenos",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                    ),
                                    color = c.text,
                                )
                                Text(
                                    "de ${state.moodWeek.size} marcados",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = c.textTertiary,
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(c.surfaceVariant, RoundedCornerShape(24.dp))
                                .border(
                                    1.dp, c.line,
                                    RoundedCornerShape(24.dp),
                                )
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "ESTA SEMANA",
                                style = androidx.compose.ui.text.TextStyle(
                                    fontFamily = VidaFonts.Sora, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.14.em,
                                ),
                                color = c.textSecondary,
                            )
                            Text(
                                state.moodWeek.size.toString(),
                                style = t.heroFigure.copy(fontSize = 30.sp, lineHeight = 34.sp),
                                color = c.text,
                            )
                            Text(
                                if (state.moodWeek.isEmpty()) "Sin marcar todavía" else "días registrados",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = c.textTertiary,
                            )
                        }
                    }
                }

                /* ── LA PROMESA, DONDE SE NECESITA ───────────────────────── */
                StaggeredAppear(7) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ink.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 17.dp, vertical = 15.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = ink,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "No se comparte con nadie, ni con quien comparte tareas contigo. " +
                                "Se borra de golpe desde Ajustes.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
                            color = c.textSecondary,
                        )
                    }
                }

                // Un fallo puntual con datos ya en pantalla no borra la pantalla:
                // se avisa debajo y lo que se veía sigue viéndose.
                state.moodError?.takeIf { state.moodToday != null }?.let { msg ->
                    StaggeredAppear(8) {
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = c.error)
                    }
                }

                Spacer(Modifier.height(VidaLayout.sectionGap))
            }
        }
    }
}

/** El título de zona de Inicio: Sora 600, 11 sp, versalitas con 0,14 em. */
@Composable
private fun ZoneTitle(title: String, trailing: String? = null) {
    val style = androidx.compose.ui.text.TextStyle(
        fontFamily = VidaFonts.Sora, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.14.em,
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), style = style, color = VidaTheme.colors.textSecondary, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = style.copy(letterSpacing = 0.sp), color = VidaTheme.colors.textTertiary)
    }
}
