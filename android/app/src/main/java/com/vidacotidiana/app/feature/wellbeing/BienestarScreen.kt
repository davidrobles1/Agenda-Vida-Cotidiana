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
import com.vidacotidiana.app.core.ui.components.subeSobreElTeclado
import com.vidacotidiana.app.core.ui.components.LoadingRows
import com.vidacotidiana.app.core.ui.components.MoodFace
import com.vidacotidiana.app.core.ui.components.MoodScale
import com.vidacotidiana.app.core.ui.components.StaggeredAppear
import com.vidacotidiana.app.core.ui.components.VidaError
import com.vidacotidiana.app.core.ui.components.VidaScreen
import com.vidacotidiana.app.core.ui.components.drawMoodFace
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
import com.vidacotidiana.app.core.ui.components.moodSky
import com.vidacotidiana.app.core.ui.components.moodInk
import com.vidacotidiana.app.core.ui.components.moodSkyStrong
import com.vidacotidiana.app.core.ui.components.vidaHalo
import com.vidacotidiana.app.core.ui.components.vidaFloat

/**
 * BIENESTAR — «¿Cómo te sientes hoy?».
 *
 * Composición transcrita del artefacto maestro: tarjeta de 32 dp de radio cuyo
 * FONDO ENTERO cambia con el ánimo, la cara a 140 dp flotando sobre un halo, y
 * los cinco selectores de 56 dp que se levantan al elegirse.
 *
 * DATOS REALES. Todo sale de `/api/v1/moods` (V36) vía `WellbeingRepository`.
 * Cuando la API falla se enseña el error; no hay valor de reserva que aparente
 * un ánimo que el usuario nunca marcó.
 *
 * ESTO ES DATO DE SALUD (ver `37-clasificacion-datos.md`): la etiqueta «Solo
 * tú» de la cabecera y el aviso violeta del final no son decoración, son la
 * promesa hecha visible donde el usuario la necesita.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BienestarScreen(viewModel: AppViewModel, navController: NavHostController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = VidaTheme.colors
    val t = VidaTheme.type

    // La semana se pide solo aquí: en Inicio la cara es pequeña y le basta hoy.
    LaunchedEffect(Unit) { viewModel.loadMood(includeWeek = true) }

    val marked = state.moodToday?.value
    val shown = marked ?: MoodScale.NORMAL
    // Mismo criterio que Inicio: el tinte del ánimo se compone sobre la
    // superficie del tema activo, así que funciona en los once temas.
    val sky = moodSky(marked)
    val deep = moodInk(marked)

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
                /* ── LA CARA ─────────────────────────────────────────────── */
                StaggeredAppear(0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(sky, RoundedCornerShape(32.dp))
                            .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "¿Cómo te sientes hoy?",
                            style = t.sectionTitle.copy(fontSize = 22.sp, lineHeight = 28.sp),
                            color = c.text,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when {
                                marked != null -> MoodScale.ECHOES[marked]
                                state.moodError != null -> "No pudimos consultarlo"
                                else -> "Toca una carita"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp,
                            ),
                            color = deep,
                        )

                        Box(
                            modifier = Modifier.fillMaxWidth().height(168.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            // El halo: el acento del ánimo, muy desvaído y
                            // desenfocado. Es lo que hace que la cara flote en
                            // vez de estar pegada al fondo.
                            if (marked != null) {
                                Box(
                                    Modifier
                                        .size(154.dp)
                                        // `.halo` del artefacto: 5,2 s, escala
                                        // 1 → 1,08 y opacidad .5 → .85. Estaba
                                        // dibujado pero quieto, y un halo que no
                                        // respira es sólo una mancha.
                                        .vidaHalo()
                                        .blur(18.dp)
                                        .background(
                                            MoodScale.accentOf(marked).copy(alpha = 0.16f),
                                            RoundedCornerShape(50),
                                        ),
                                )
                            }
                            MoodFace(
                                // `.floatybig`: 5,2 s, ±7 px y ±1,2° de
                                // balanceo. Es la pieza central de la pantalla
                                // y la única que el artefacto mueve así.
                                modifier = Modifier.vidaFloat(7.dp, 5200, rotation = 1.2f),
                                value = marked,
                                size = 140.dp,
                                strokeWidth = 3.4f,
                                surface = c.surfaceVariant,
                            )
                        }

                        /* Los cinco selectores: 56 dp, radio 20, y el elegido
                           sube 5 dp y crece un 7 %. */
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            (0..4).forEach { k ->
                                val on = marked == k
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .padding(top = if (on) 0.dp else 5.dp)
                                        .background(
                                            // El elegido lleva el tinte del
                                            // ánimo EN ESTE TEMA; en Noche el
                                            // crema fijo invertía la lectura y
                                            // los no elegidos parecían activos.
                                            // El elegido usa el tinte FUERTE:
                                            // con el suave se confundía con
                                            // la tarjeta, que lleva ese mismo
                                            // tinte de fondo.
                                            if (on) moodSkyStrong(k) else c.sunken,
                                            RoundedCornerShape(20.dp),
                                        )
                                        .vidaClickable(onClick = { viewModel.setMood(k, null, tags.toList()) }),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Canvas(Modifier.size(if (on) 32.dp else 30.dp)) {
                                        drawMoodFace(k.toFloat(), 1f, 1.9f, Color.Transparent)
                                    }
                                }
                            }
                        }
                    }
                }

                /* ── ¿QUÉ LO HIZO ASÍ? ───────────────────────────────────── */
                StaggeredAppear(1) {
                    Text("¿QUÉ LO HIZO ASÍ? · OPCIONAL", style = t.eyebrow, color = c.textTertiary)
                }
                StaggeredAppear(2) {
                    val options = listOf("Dormí bien", "Familia", "Trabajo", "Me moví", "Pendientes", "Nada en concreto")
                    // Retícula fluida: los chips se reparten en las líneas que
                    // hagan falta, como en el artefacto. No es una fila que se
                    // desplaza — aquí caben todos y verlos todos es el punto.
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        options.forEach { label ->
                            val on = label in tags
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (on) c.primaryContainer else c.surfaceVariant,
                                        RoundedCornerShape(50),
                                    )
                                    .border(
                                        1.5.dp,
                                        if (on) c.primary else c.line,
                                        RoundedCornerShape(50),
                                    )
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
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                                    color = if (on) c.primaryDeep else c.textSecondary,
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
                            .background(c.surfaceElevated, RoundedCornerShape(22.dp))
                            .border(1.5.dp, c.line, RoundedCornerShape(22.dp))
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = c.textTertiary,
                            modifier = Modifier.size(18.dp),
                        )
                        // ESTO ERA UN CARTEL CON PINTA DE CAMPO.
                        //
                        // Tenía marco, lápiz y texto de sugerencia, pero era un
                        // `Text`: al tocarlo no pasaba nada, el teclado no
                        // aparecía y `mood_entry.note` se quedaba para siempre
                        // en nulo. El backend aceptaba la nota desde el
                        // principio y `setMood` ya la enviaba; lo único que no
                        // existía era por dónde escribirla.
                        //
                        // Se guarda al terminar —con «Listo» o al salir del
                        // campo— y no en cada tecla: una petición por letra
                        // convertiría una frase en treinta escrituras.
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
                StaggeredAppear(4) {
                    Text("TU SEMANA", style = t.eyebrow, color = c.textTertiary)
                }
                StaggeredAppear(5) {
                    val today = LocalDate.now()
                    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
                    val byDate = state.moodWeek.associateBy { it.date }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(c.surfaceVariant, RoundedCornerShape(VidaTheme.spec.radii.card))
                            .border(VidaTheme.spec.borderWidth, c.line, RoundedCornerShape(VidaTheme.spec.radii.card))
                            .padding(horizontal = 14.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        days.forEach { day ->
                            val mood = byDate[day]?.value
                            val isToday = day == today
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (mood != null) moodSky(mood) else c.sunken,
                                            RoundedCornerShape(13.dp),
                                        )
                                        .then(
                                            if (isToday && mood != null) {
                                                Modifier.border(
                                                    2.dp, MoodScale.accentOf(mood), RoundedCornerShape(13.dp),
                                                )
                                            } else if (mood == null) {
                                                // `line` es el filete de una
                                                // separación, casi invisible
                                                // sobre fondo oscuro; un hueco
                                                // que hay que poder contar pide
                                                // el borde de un control.
                                                Modifier.border(1.5.dp, c.border, RoundedCornerShape(13.dp))
                                            } else {
                                                Modifier
                                            },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    // Un día sin marcar queda VACÍO, no en gris
                                    // neutro: no marcar no es sentirse normal.
                                    if (mood != null) {
                                        Canvas(Modifier.size(22.dp)) {
                                            drawMoodFace(mood.toFloat(), 1f, 2f, Color.Transparent)
                                        }
                                    }
                                }
                                Text(
                                    if (isToday) "HOY" else day.dayOfWeek
                                        .getDisplayName(TextStyle.NARROW, Locale("es", "MX")).uppercase(),
                                    style = t.micro,
                                    color = if (isToday) deep else c.textTertiary,
                                )
                            }
                        }
                    }
                }

                /* ── EL MES, EN CIFRAS ───────────────────────────────────── */
                StaggeredAppear(6) {
                    val good = state.moodWeek.count { it.value <= MoodScale.BIEN }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF10A37F), RoundedCornerShape(VidaTheme.spec.radii.card))
                                .padding(18.dp),
                        ) {
                            Text(
                                "Días buenos",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold, fontSize = 11.sp,
                                ),
                                color = Color.White.copy(alpha = 0.82f),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                good.toString(),
                                style = t.heroFigure.copy(fontSize = 42.sp, lineHeight = 46.sp),
                                color = Color.White,
                            )
                            Text(
                                "de ${state.moodWeek.size} marcados",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.78f),
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(c.surfaceVariant, RoundedCornerShape(VidaTheme.spec.radii.card))
                                .border(
                                    VidaTheme.spec.borderWidth, c.line,
                                    RoundedCornerShape(VidaTheme.spec.radii.card),
                                )
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("ESTA SEMANA", style = t.eyebrow, color = c.textTertiary)
                            Text(
                                state.moodWeek.size.toString(),
                                style = t.heroFigure.copy(fontSize = 34.sp, lineHeight = 38.sp),
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
                            .background(Color(0xFFF3EEFF), RoundedCornerShape(20.dp))
                            .padding(horizontal = 17.dp, vertical = 15.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "No se comparte con nadie, ni con quien comparte tareas contigo. " +
                                "Se borra de golpe desde Ajustes.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
                            color = Color(0xFF5B21B6),
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
