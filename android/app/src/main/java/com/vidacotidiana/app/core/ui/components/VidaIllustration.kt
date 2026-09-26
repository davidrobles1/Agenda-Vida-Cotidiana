package com.vidacotidiana.app.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.vidacotidiana.app.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.core.ui.SectionVisual
import com.vidacotidiana.app.core.ui.VidaSpacing
import com.vidacotidiana.app.core.ui.VidaTheme
import kotlin.math.min

/**
 * EL VOCABULARIO GRÁFICO DE LAS LÁMINAS, en piezas reutilizables.
 *
 * Las dos láminas de ilustraciones no son un conjunto de dibujos sueltos:
 * son un lenguaje con reglas. Un icono de sección SIEMPRE vive dentro de un
 * cuadrado redondeado teñido con su color. Un indicador SIEMPRE es un anillo
 * con la cifra dentro. Un estado vacío SIEMPRE es el icono de esa sección en
 * grande, con su título, su frase y su botón.
 *
 * Por eso esto son componentes y no dibujos pegados en cada pantalla: lo que
 * hace que el lenguaje se sienta uno es que las diecisiete secciones usen
 * LAS MISMAS piezas, no que cada una copie el aspecto a mano. Cambiar aquí
 * el radio de la baldosa lo cambia en toda la aplicación, que es justo lo
 * que evita que unas pantallas se queden con el lenguaje anterior.
 */

/* =========================================================
   1 · LA BALDOSA DE SECCIÓN
   ========================================================= */

/**
 * El icono de una sección dentro de su cuadrado teñido.
 *
 * Es la pieza más repetida de las láminas: aparece en el acceso rápido del
 * inicio, en la cabecera de cada widget, en los estados vacíos y en las
 * filas de lista. `size` cambia la escala sin romper la proporción entre
 * cuadrado, radio e icono, que es lo que la hace reconocible a 20 dp y a
 * 64 dp.
 */
@Composable
fun SectionTile(
    section: SectionVisual,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    /** Sólido pinta el cuadrado del color y el icono en blanco. */
    solid: Boolean = false,
) {
    val shape = RoundedCornerShape(size * 0.30f)
    // LA ILUSTRACIÓN MANDA cuando existe: es el dibujo de la lámina, con su
    // volumen y su propio fondo teñido, no un icono de trazo. El icono
    // vectorial queda de respaldo para las secciones que no tienen dibujo y
    // para `solid`, donde hace falta una silueta plana sobre color.
    if (section.art != null && !solid) {
        Image(
            painter = painterResource(section.art),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(shape),
        )
        return
    }
    Box(
        modifier
            .size(size)
            .background(
                if (solid) section.tint else section.container,
                shape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            section.icon,
            contentDescription = null,
            modifier = Modifier.size(size * 0.48f),
            tint = if (solid) Color.White else section.tint,
        )
    }
}

/**
 * La baldosa con su nombre debajo — el «acceso rápido» de la lámina.
 *
 * El contador es opcional y va en una píldora sobre la esquina: la lámina lo
 * llama «indicador de actividad» y sólo aparece «cuando sea necesario», así
 * que un cero no se dibuja.
 */
@Composable
fun SectionQuickAccess(
    section: SectionVisual,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int = 0,
) {
    Column(
        modifier.vidaClickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box {
            SectionTile(section, size = 54.dp)
            if (badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .background(section.tint, CircleShape)
                        .border(2.dp, VidaTheme.colors.surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (badge > 9) "9+" else badge.toString(),
                        style = VidaTheme.type.micro.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
            }
        }
        Text(
            section.label,
            style = VidaTheme.type.micro.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            color = VidaTheme.colors.text,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/* =========================================================
   2 · EL ANILLO DE INDICADOR
   ========================================================= */

/**
 * Un anillo con la cifra dentro — «INDICADORES Y GRÁFICAS» de la lámina.
 *
 * El hueco del anillo no es decorativo: la lámina lo usa siempre para meter
 * DENTRO el dato (75 %, 3, 1), de modo que el indicador se lee sin leyenda.
 * Por eso el texto va aquí y no al lado.
 */
@Composable
fun VidaMetricRing(
    progress: Float,
    figure: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    stroke: Dp = 6.dp,
    track: Color = tint.copy(alpha = 0.16f),
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val inset = w / 2f
            val arcSize = Size(this.size.width - w, this.size.height - w)
            drawArc(
                color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(w, cap = StrokeCap.Round),
            )
            drawArc(
                color = tint, startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(w, cap = StrokeCap.Round),
            )
        }
        Text(
            figure,
            style = VidaTheme.type.micro.copy(
                fontSize = (size.value * 0.22f).sp,
                fontWeight = FontWeight.Bold,
            ),
            color = tint,
        )
    }
}

/**
 * La barra de progreso teñida — «BARRAS DE PROGRESO» de la lámina.
 *
 * Se diferencia de `VidaProgress` en que lleva SU color: la lámina pinta la
 * del 75 % en índigo, la del 50 % en verde y la del 10 % en rojo, porque el
 * color dice de qué va la barra antes que el número.
 */
@Composable
fun VidaTintedBar(
    progress: Float,
    tint: Color,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(tint.copy(alpha = 0.14f), RoundedCornerShape(999.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(height)
                .background(tint, RoundedCornerShape(999.dp)),
        )
    }
}

/* =========================================================
   3 · EL ESTADO VACÍO ILUSTRADO
   ========================================================= */

/**
 * «Ilustraciones para estados vacíos» de la lámina 2.
 *
 * Su composición es siempre la misma —icono grande de la sección sobre su
 * halo, título, frase y un botón con el verbo de esa sección— y por eso vive
 * aquí: cada pantalla vacía dice lo suyo, pero todas se ven de la misma
 * familia. Sustituye al estado vacío de sólo texto que había antes.
 */
@Composable
fun SectionEmptyState(
    section: SectionVisual,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(section.wash, RoundedCornerShape(VidaTheme.spec.radii.card))
            .padding(vertical = 28.dp, horizontal = VidaSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(VidaSpacing.sm),
    ) {
        // El icono sobre su halo: es la «ilustración» a la escala que un
        // icono vectorial aguanta sin verse pobre.
        // A 96 dp la ilustración de 270 px se ve a su tamaño natural: es
        // justo donde estos dibujos lucen, y por eso el vacío es el sitio
        // donde la lámina los pone más grandes.
        SectionTile(section, size = 96.dp)
        Text(
            title,
            style = VidaTheme.type.cardTitle,
            color = VidaTheme.colors.text,
            textAlign = TextAlign.Center,
        )
        Text(
            body,
            style = VidaTheme.type.caption,
            color = VidaTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Box(Modifier.padding(top = 4.dp)) {
                SectionActionButton(actionLabel, section.tint, onAction)
            }
        }
    }
}

/** El botón del estado vacío: relleno del color de SU sección, como la lámina. */
@Composable
fun SectionActionButton(label: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .background(tint, RoundedCornerShape(999.dp))
            .vidaClickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = VidaTheme.type.caption.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
    }
}

/* =========================================================
   4 · LA BANDA DE PAISAJE
   ========================================================= */

/**
 * «FONDOS Y BANNERS» — la banda del encabezado del inicio.
 *
 * El paisaje se DIBUJA, no se empaqueta como fotografía: una foto de 1 MB
 * por banda engorda el instalable, se ve mal al estirarse y obliga a tener
 * una versión para tema claro y otra para oscuro. Un amanecer de montaña son
 * cuatro degradados y dos siluetas, pesa cero y se adapta a cualquier
 * tamaño.
 */
@Composable
fun LandscapeBanner(
    modifier: Modifier = Modifier,
    height: Dp = 132.dp,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(VidaTheme.spec.radii.card)
    Image(
        painter = painterResource(R.drawable.ill_hero_banner),
        // EL TEXTO VA DENTRO DE LA ILUSTRACIÓN, tal como la propuesta lo
        // entrega: la frase y su tipografía son parte del dibujo. Por eso no
        // se superpone un `Text` encima — se vería dos veces.
        //
        // La contrapartida es que esa frase no crece con el tamaño de letra
        // del sistema. Se compensa con `contentDescription`: un lector de
        // pantalla la lee completa, que es donde de verdad importaba.
        contentDescription = "No tienes que acordarte de todo, estoy aquí para ayudarte. Tu vida, en un solo lugar.",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .then(if (onClick != null) Modifier.vidaClickable(onClick = onClick) else Modifier),
    )
}

/**
 * EL PAISAJE DEL WIDGET DE VISION BOARD — como FONDO de la tarjeta, no como
 * una estampa debajo del texto.
 *
 * En la lámina («WIDGETS Y TARJETAS») el amanecer no es un adorno colocado al
 * final de la tarjeta: ES la tarjeta. Ocupa su mitad inferior a sangre —sin
 * margen a izquierda, derecha ni abajo— y por encima del panel nítido la
 * misma imagen sigue ahí, desvaída, tiñendo el espacio donde van el título y
 * la frase. Por eso se ven cordilleras tenues detrás de las letras.
 *
 * SON DOS CAPAS DE LA MISMA ILUSTRACIÓN, y cada una hace un trabajo distinto:
 *
 *  1. EL LAVADO — la imagen entera, muy transparente, anclada arriba. Su
 *     parte alta es cielo y sol, así que al bajarle la opacidad queda un
 *     degradado rosa-lavanda: el fondo de la tarjeta es la propia imagen y no
 *     un color plano que alguien eligió a ojo.
 *  2. EL PANEL — la misma imagen a plena opacidad abajo, a sangre. Aquí sí se
 *     leen las montañas y el bosque.
 *
 * LAS LETRAS NO ESTORBAN LA ILUSTRACIÓN porque viven en el tercio superior,
 * sobre el lavado —donde no hay más que cielo—, nunca sobre las crestas. Es
 * la razón de que el panel no llegue arriba: el hueco es para el texto.
 *
 * El dibujo sale de `Ilustraciones1.jpeg`; la flecha que la lámina trae
 * pintada dentro se borró del recorte, porque la pinta el componente y si no
 * saldría dos veces.
 */
@Composable
fun VisionBoardBackdrop(
    modifier: Modifier = Modifier,
    /** Cuánto de la tarjeta ocupa el panel nítido. La lámina usa ~0.46. */
    panelFraction: Float = 0.46f,
) {
    Box(modifier) {
        Image(
            painter = painterResource(R.drawable.ill_visionboard_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            alpha = 0.30f,
            modifier = Modifier.fillMaxSize(),
        )
        Image(
            painter = painterResource(R.drawable.ill_visionboard_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(panelFraction)
                // Arriba redondeado, abajo recto: los de abajo los redondea el
                // recorte de la propia tarjeta, que es lo que hace que el
                // paisaje muera justo en su borde en vez de flotar dentro.
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
        )
    }
}

/* =========================================================
   5 · EL AVISO EN CONTEXTO
   ========================================================= */

/**
 * «NOTIFICACIONES / TOAST» de la lámina: icono teñido, dos líneas y la equis.
 *
 * Discreto y no invasivo, que es como la propia lámina lo describe: no tapa
 * la pantalla ni exige respuesta, se puede cerrar y se va.
 */
@Composable
fun VidaToast(
    icon: ImageVector,
    title: String,
    body: String,
    tint: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VidaTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(c.surface, RoundedCornerShape(18.dp))
            .border(1.dp, c.line, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            Modifier.size(34.dp).background(tint.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, Modifier.size(18.dp), tint = tint)
        }
        Column(Modifier.fillMaxWidth(0.82f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = VidaTheme.type.caption.copy(fontWeight = FontWeight.Bold), color = c.text)
            Text(body, style = VidaTheme.type.micro, color = c.textSecondary)
        }
        Box(Modifier.vidaClickable(onClick = onDismiss).padding(4.dp)) {
            Icon(Icons.Outlined.Close, "Cerrar", Modifier.size(16.dp), tint = c.textTertiary)
        }
    }
}

/* =========================================================
   6 · EL DÍA DEL CALENDARIO
   ========================================================= */

/**
 * «CALENDARIO (DÍAS)» de la lámina: la abreviatura, el número y un punto de
 * color por lo que hay ese día.
 *
 * El día de hoy se distingue por su BORDE y su color, no por un relleno:
 * así la tira se lee como una semana y no como un botón seleccionado entre
 * seis apagados.
 */
@Composable
fun DayChip(
    weekday: String,
    day: Int,
    isToday: Boolean,
    dotColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = VidaTheme.colors
    Column(
        modifier
            .size(width = 52.dp, height = 74.dp)
            .background(if (isToday) c.primaryContainer.copy(alpha = 0.45f) else c.surface, RoundedCornerShape(18.dp))
            .border(
                if (isToday) 1.5.dp else 1.dp,
                if (isToday) c.primary else c.line,
                RoundedCornerShape(18.dp),
            )
            .vidaClickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            weekday,
            style = VidaTheme.type.micro.copy(fontSize = 10.sp),
            color = if (isToday) c.primary else c.textTertiary,
        )
        Text(
            day.toString(),
            style = VidaTheme.type.cardTitle.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold),
            color = if (isToday) c.primary else c.text,
        )
        Box(Modifier.padding(top = 4.dp).size(5.dp).background(dotColor ?: Color.Transparent, CircleShape))
    }
}

/** Cuánto de un total, sin dividir por cero. */
fun safeRatio(part: Double, total: Double): Float =
    if (total <= 0.0) 0f else min(1.0, part / total).toFloat()
