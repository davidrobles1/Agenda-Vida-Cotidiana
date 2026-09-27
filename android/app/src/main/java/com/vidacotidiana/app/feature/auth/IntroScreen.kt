package com.vidacotidiana.app.feature.auth

import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.core.ui.VidaFonts
import com.vidacotidiana.app.core.ui.brand.Butterfly
import com.vidacotidiana.app.core.ui.brand.Butterfly.drawButterfly
import com.vidacotidiana.app.core.ui.brand.Butterfly.drawWingLayer
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/*
 * ADR-027 · La línea de tiempo del artefacto aprobado «Mariposa Aprobada»
 * (`showIntro` → `grow` → `zoom` → `showLogin`), en milisegundos desde que la
 * pantalla aparece. Son los valores del artefacto, no aproximaciones.
 */

/** `.brand .fig`: opacidad y escala .86 → 1 en 520 ms. */
private const val FIG_IN = 520f
/** `.jor`: opacidad en 520 ms tras 100 ms. */
private const val WORD_IN = 520f
private const val WORD_DELAY = 100f
/** `.hero`: opacidad en 620 ms tras 260 ms. */
private const val HERO_IN = 620f
private const val HERO_DELAY = 260f
/** `later(grow, 2050)`: 0.5 s de aparición + 1.5 s aleteando en su sitio. */
private const val GROW_AT = 2050L
/** `.fading`: cita, «Meraki», pie y «Jornada» se van en 450 ms. */
private const val FADE = 450f
/** `later(start, 700)`: tope de espera a que la batida termine. */
private const val WAIT_MAX = 700L
/** `T` de `zoom()`. */
private const val ZOOM = 1250f
/** El velo índigo entra en `T − 250` y dura 400 ms. */
private const val COVER_AT = 1000L
private const val COVER = 400f
/** `later(showLogin, T + 300)`: aquí se abre el formulario real. */
private const val FORM_AT = 1550L
/** `s1 = max(ancho, alto) × 11`. */
private const val ZOOM_REACH = 11f
/** Batida: 0.62 s; con «reducir movimiento», 1.6 s. */
private const val FLAP = 620L
private const val FLAP_REDUCED = 1600L
/** `rotateY(±64deg)` en el 45 % de la batida. */
private const val FLAP_ANGLE = 64f
private const val FLAP_PEAK = 0.45f
/** `translateY(-2.2%)` del conjunto de alas. */
private const val FLAP_BOB = 0.022f

/** Las curvas CSS usadas por el artefacto. */
private val EaseDecelerate = CubicBezierEasing(0f, 0f, 0.2f, 1f) //   cubic-bezier(0,0,.2,1)
private val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f) //          ease-out
private val EaseCss = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f) //     ease
private val EaseFlap = CubicBezierEasing(0.45f, 0f, 0.55f, 1f) //      cubic-bezier(.45,0,.55,1)

/** El `ease` de `zoom()`: cúbica de entrada y salida. */
private fun easeInOutCubic(t: Float) = if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f

private fun progress(t: Long, delay: Float, duration: Float) = ((t - delay) / duration).coerceIn(0f, 1f)

/** Fase de la batida (0 = alas abiertas, 1 = máximo giro), por tramos como los keyframes CSS. */
private fun flapPhase(t: Long, period: Long): Float {
    val f = (t % period).toFloat() / period
    return if (f < FLAP_PEAK) {
        EaseFlap.transform(f / FLAP_PEAK)
    } else {
        1f - EaseFlap.transform((f - FLAP_PEAK) / (1f - FLAP_PEAK))
    }
}

/**
 * `grow()` no corta la batida: espera a que termine la que está en curso
 * (`animationiteration`) para crecer con las alas abiertas, con un tope de
 * 700 ms.
 */
private fun zoomStartFor(period: Long) = min((GROW_AT / period + 1) * period, GROW_AT + WAIT_MAX)

/**
 * La entrada a Cotidiana: la mariposa y, acto seguido, el formulario real de
 * Keycloak.
 *
 * FUENTE DE VERDAD: el artefacto aprobado «Mariposa Aprobada» (ADR-027). La
 * mariposa entra y aletea, la cita y «Jornada» se desvanecen, y la mariposa
 * crece desde su sitio hasta cubrir la pantalla llevando su cuerpo al centro;
 * un velo índigo cierra la transición y entonces se abre el formulario.
 *
 * NO HAY PANTALLA INTERMEDIA. Quien abre la aplicación ya decidió entrar: no
 * hay botones «Iniciar sesión» / «Crear una cuenta» que pulsar antes.
 *
 * EL FORMULARIO ES EL DE KEYCLOAK, y va en un Custom Tab porque la
 * autenticación es Authorization Code + PKCE (AppAuth): las credenciales no se
 * escriben nunca dentro de la aplicación. Ese formulario ya trae su propia
 * opción de crear cuenta.
 *
 * Si el usuario lo cierra sin terminar, la pantalla vuelve a su estado de
 * reposo —mariposa aleteando, textos visibles— con una salida para
 * reintentar, en vez de quedarse en el velo índigo sin reaccionar. Ese estado
 * no existe en el artefacto (que termina en el login) y es la única adición.
 */
@Composable
fun IntroScreen(authManager: AuthManager, onLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    /**
     * `@media (prefers-reduced-motion: reduce)` del artefacto: la batida se
     * ralentiza a 1.6 s. En Android, la preferencia equivalente es «Quitar
     * animaciones» (escala de animación 0).
     */
    val flapPeriod = remember {
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        if (scale == 0f) FLAP_REDUCED else FLAP
    }
    val zoomStart = remember(flapPeriod) { zoomStartFor(flapPeriod) }
    val formAt = zoomStart + FORM_AT

    /**
     * Si el formulario ya se abrió en esta entrada.
     *
     * `rememberSaveable`: al volver del Custom Tab la actividad puede haberse
     * recreado, y sin esto se relanzaría en bucle sin dejar salir nunca.
     */
    var formLaunched by rememberSaveable { mutableStateOf(false) }

    /** Se cerró el formulario sin terminar: hay que ofrecer volver a entrar. */
    var cancelled by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    /** Cada «Entrar» vuelve a reproducir la transición desde `grow()`. */
    var run by remember { mutableIntStateOf(0) }

    /** Reloj único de la escena, en ms. Todo lo visible se deriva de él al dibujar. */
    var clock by remember { mutableLongStateOf(0L) }

    /** Caja de la mariposa en la pantalla: de ahí parte el crecimiento. */
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var figBox by remember { mutableStateOf<Rect?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (data == null) {
            // Cerró el formulario. No es un error que contarle: ya sabe lo que
            // hizo. Solo hay que dejarle una puerta para volver.
            cancelled = true
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                authManager.handleLoginResult(data)
                onLoggedIn()
            } catch (e: Exception) {
                error = e.message ?: "No pudimos completar el inicio de sesión."
                cancelled = true
            }
        }
    }

    LaunchedEffect(run, cancelled) {
        if (cancelled) {
            // Reposo: solo el aleteo sigue vivo.
            val start = withFrameNanos { it }
            while (true) clock = (withFrameNanos { it } - start) / 1_000_000
        }
        if (run == 0 && formLaunched) {
            // Se recreó la actividad con el formulario abierto: la escena ya
            // había terminado en el velo, y ahí se queda.
            clock = formAt
            return@LaunchedEffect
        }
        val from = if (run == 0) 0L else GROW_AT
        val start = withFrameNanos { it }
        while (true) {
            clock = from + (withFrameNanos { it } - start) / 1_000_000
            if (clock >= formAt) break
        }
        formLaunched = true
        error = null
        launcher.launch(authManager.buildLoginIntent())
    }

    // Opacidades de los textos: entran con sus retardos y se van juntas en `grow()`.
    fun leaving(t: Long) = if (t < GROW_AT) 1f else 1f - EaseCss.transform(progress(t, GROW_AT.toFloat(), FADE))
    val wordAlpha = {
        val t = clock
        if (cancelled) 1f else EaseOut.transform(progress(t, WORD_DELAY, WORD_IN)) * leaving(t)
    }
    val heroAlpha = {
        val t = clock
        if (cancelled) 1f else EaseOut.transform(progress(t, HERO_DELAY, HERO_IN)) * leaving(t)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(LoginTheme.introSurface)
            .onGloballyPositioned { rootOrigin = it.positionInWindow() },
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
        ) {
            val roomForHero = maxHeight >= 560.dp

            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // `.brand`: mariposa de 124 y «Jornada», separados 10.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Canvas(
                        Modifier
                            .size(124.dp)
                            .onGloballyPositioned {
                                val p = it.positionInWindow()
                                figBox = Rect(p, Size(it.size.width.toFloat(), it.size.height.toFloat()))
                            }
                            .graphicsLayer {
                                val enter = if (cancelled) 1f else EaseDecelerate.transform(progress(clock, 0f, FIG_IN))
                                alpha = enter
                                scaleX = 0.86f + 0.14f * enter
                                scaleY = scaleX
                            },
                    ) {
                        val t = clock
                        // En `zoom()` el logo se oculta y lo sustituye la mariposa que crece.
                        if (!cancelled && t >= zoomStart) return@Canvas
                        val phase = flapPhase(t, flapPeriod)
                        val side = size.width
                        val bob = -FLAP_BOB * side * phase
                        drawWingLayer(side, Butterfly.CLIP_LEFT, FLAP_ANGLE * phase, bob)
                        drawWingLayer(side, Butterfly.CLIP_RIGHT, -FLAP_ANGLE * phase, bob)
                        drawWingLayer(side, Butterfly.CLIP_BODY, 0f, bob)
                    }
                    IntroWordmark(Modifier.graphicsLayer { alpha = wordAlpha() })
                }

                if (roomForHero) {
                    // `.hero`: 34 por debajo, hasta 420 de ancho.
                    Column(
                        Modifier
                            .padding(top = 34.dp)
                            .widthIn(max = 420.dp)
                            .fillMaxWidth()
                            .graphicsLayer { alpha = heroAlpha() },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        HeroDivider()
                        Box(Modifier.padding(top = 22.dp)) { HeroText() }
                        Box(Modifier.padding(top = 28.dp)) { FormFooter() }
                    }
                }

                // Solo aparece si el usuario cerró el formulario: no es un paso
                // del camino, es la salida cuando se sale de él.
                if (cancelled) {
                    Column(
                        Modifier
                            .padding(top = 36.dp)
                            .widthIn(max = 420.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        error?.let {
                            Text(
                                it,
                                style = TextStyle(fontFamily = VidaFonts.Inter, fontSize = 14.sp),
                                color = LoginTheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(bottom = 12.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        }
                        TemplateButton(
                            label = "Entrar",
                            leadingPath = PATH_USER,
                            onClick = {
                                cancelled = false
                                run += 1
                            },
                            primary = true,
                        )
                    }
                }
            }
        }

        // `zoom()` + `.cover`: la mariposa crece hasta cubrir la pantalla y el
        // velo índigo cierra la escena. Se traza como vector en cada cuadro.
        Canvas(Modifier.fillMaxSize()) {
            val t = clock
            if (cancelled || t < zoomStart) return@Canvas
            figBox?.let { box ->
                val e = easeInOutCubic(progress(t, zoomStart.toFloat(), ZOOM))
                val s0 = box.width
                val s1 = max(size.width, size.height) * ZOOM_REACH
                // Escala geométrica: el crecimiento se siente uniforme.
                val s = s0 * (s1 / s0).pow(e)
                val bx0 = box.left - rootOrigin.x + s0 * Butterfly.BODY.x
                val by0 = box.top - rootOrigin.y + s0 * Butterfly.BODY.y
                val k = min(1f, e * 1.6f)
                val bx = bx0 + (size.width / 2f - bx0) * k
                val by = by0 + (size.height / 2f - by0) * k
                drawButterfly(Offset(bx - s * Butterfly.BODY.x, by - s * Butterfly.BODY.y), s)
            }
            val cover = EaseCss.transform(progress(t, (zoomStart + COVER_AT).toFloat(), COVER))
            if (cover > 0f) drawRect(LoginTheme.introCover, alpha = cover)
        }
    }
}
