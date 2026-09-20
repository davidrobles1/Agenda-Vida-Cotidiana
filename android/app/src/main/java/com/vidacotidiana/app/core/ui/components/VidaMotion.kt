package com.vidacotidiana.app.core.ui.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * LAS ANIMACIONES QUE VIVEN SOLAS, transcritas del artefacto maestro.
 *
 * Fuente de verdad:
 * https://claude.ai/code/artifact/b31707e4-2719-4a26-b7e8-b870ded5a0ec
 *
 * LO QUE FALTABA. La aplicación ya tenía la entrada escalonada
 * (`StaggeredAppear` = las clases `.r1`…`.r6`), el muelle de la cara, el barrido
 * del anillo (`.arc`, 560 ms) y el destello del esqueleto de carga (`.skel`,
 * 1,2 s). Pero el artefacto tiene además CUATRO animaciones que no esperan a que
 * nadie toque nada —siguen vivas mientras la pantalla está abierta— y ninguna
 * existía aquí:
 *
 *   `.beat`      2,4 s  el punto del «ahora» en la línea del día
 *   `.floaty`    4,6 s  la cara pequeña de Inicio, flotando
 *   `.floatybig` 5,2 s  la cara grande de Bienestar, flotando y balanceándose
 *   `.halo`      5,2 s  el halo que respira detrás de esa cara
 *   `.growy`     700 ms las barras de la semana, creciendo desde su base
 *
 * Son lo que hace que la pantalla se sienta viva en vez de impresa. Van aquí y
 * no dentro de cada pantalla porque son del sistema, igual que los tokens: si
 * cada sitio se inventara su propio periodo, el conjunto dejaría de respirar al
 * mismo ritmo.
 *
 * SE RESPETA «REDUCIR MOVIMIENTO». El artefacto lo hace con
 * `@media (prefers-reduced-motion: reduce)`; aquí el equivalente del sistema es
 * la escala de duración de animaciones, que el usuario puede poner a cero en
 * Accesibilidad. A cero, cada pieza se queda quieta en su estado de reposo — no
 * a mitad de recorrido.
 */
@Composable
fun motionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        }.getOrDefault(true)
    }
}

/**
 * `.floaty` / `.floatybig` — flotar despacio, sin llamar la atención.
 *
 * El artefacto mueve ±4 px en 4,6 s para la cara pequeña y ±7 px con ±1,2° de
 * balanceo en 5,2 s para la grande. La rotación sólo en la grande: a 50 dp no se
 * percibiría y sí emborronaría el trazo.
 */
fun Modifier.vidaFloat(
    amplitude: Dp = 4.dp,
    periodMillis: Int = 4600,
    rotation: Float = 0f,
): Modifier = composed {
    if (!motionEnabled()) return@composed this
    val transition = rememberInfiniteTransition(label = "float")
    // Media onda por tramo y vuelta: el artefacto va de 0 a −amplitud y regresa.
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(periodMillis / 2, easing = VidaEase),
            RepeatMode.Reverse,
        ),
        label = "floatPhase",
    )
    graphicsLayer {
        translationY = -amplitude.toPx() * phase
        rotationZ = -rotation * phase
    }
}

/**
 * `.beat` — el latido del marcador de «ahora».
 *
 * 2,4 s: opacidad .4 → 1 y escala 1 → 1,7. Es lo ÚNICO que se mueve solo en la
 * línea del día, y por eso el ojo lo encuentra sin buscarlo.
 */
fun Modifier.vidaBeat(): Modifier = composed {
    if (!motionEnabled()) return@composed this
    val transition = rememberInfiniteTransition(label = "beat")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "beatPhase",
    )
    graphicsLayer {
        val s = 1f + 0.7f * t
        scaleX = s
        scaleY = s
        alpha = 0.4f + 0.6f * t
    }
}

/**
 * `.halo` — el halo que respira detrás de la cara grande.
 *
 * 5,2 s: escala 1 → 1,08 y opacidad .5 → .85. Va por debajo de la cara y nunca
 * la toca: es atmósfera, no un elemento que se pueda pulsar.
 */
fun Modifier.vidaHalo(): Modifier = composed {
    if (!motionEnabled()) return@composed this
    val transition = rememberInfiniteTransition(label = "halo")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = VidaEase), RepeatMode.Reverse),
        label = "haloPhase",
    )
    graphicsLayer {
        val s = 1f + 0.08f * t
        scaleX = s
        scaleY = s
        alpha = 0.5f + 0.35f * t
    }
}

/**
 * `.growy` — una barra que crece desde su base al aparecer.
 *
 * 700 ms con el easing de la casa y el retardo que le toque, para que la serie
 * se levante en cascada y no de golpe. `transform-origin: bottom` es lo que
 * hace que crezca en vez de estirarse desde el centro.
 */
fun Modifier.vidaGrowY(delayMillis: Int = 0): Modifier = composed {
    if (!motionEnabled()) return@composed this
    // Una sola pasada: arranca en cero, crece y se queda. No es infinita — una
    // barra que volviera a crecer cada 700 ms sería un parpadeo, no una entrada.
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val t by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(700, delayMillis = delayMillis, easing = VidaEase),
        label = "growPhase",
    )
    graphicsLayer {
        scaleY = t
        transformOrigin = TransformOrigin(0.5f, 1f)
    }
}
