package com.vidacotidiana.app.feature.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vidacotidiana.app.R
import com.vidacotidiana.app.core.ui.VidaFonts
import com.vidacotidiana.app.core.ui.brand.BrandContext
import com.vidacotidiana.app.core.ui.brand.BrandMark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * La entrada a Cotidiana: el logo del Portal y, acto seguido, el formulario
 * real de Keycloak.
 *
 * NO HAY PANTALLA INTERMEDIA. Antes existía una pantalla de login propia con
 * botones «Iniciar sesión» y «Crear una cuenta» que obligaba a pulsar para
 * continuar; se eliminó porque quien abre la aplicación ya decidió entrar, y
 * verla aparecer un instante antes del formulario delataba un paso que no
 * pinta nada en el camino.
 *
 * EL FORMULARIO ES EL DE KEYCLOAK, y va en un Custom Tab porque la
 * autenticación es Authorization Code + PKCE (AppAuth): las credenciales no se
 * escriben nunca dentro de la aplicación. Ese formulario ya trae su propia
 * opción de crear cuenta, así que ese camino sigue existiendo sin necesidad de
 * un botón aparte aquí.
 *
 * Esta pantalla se queda debajo mientras el formulario está abierto. Si el
 * usuario lo cierra sin terminar, vuelve a verla —con una salida para
 * reintentar— en vez de quedarse mirando una aplicación que no reacciona.
 */
@Composable
fun IntroScreen(authManager: AuthManager, onLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    /**
     * Si el formulario ya se abrió en esta entrada.
     *
     * `rememberSaveable`: al volver del Custom Tab la actividad puede haberse
     * recreado, y sin esto se relanzaría en bucle sin dejar salir nunca.
     */
    var formLaunched by rememberSaveable { mutableStateOf(false) }

    /** Se cerró el formulario sin terminar: hay que ofrecer volver a entrar. */
    var cancelled by remember { mutableStateOf(false) }

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

    fun openForm() {
        cancelled = false
        error = null
        launcher.launch(authManager.buildLoginIntent())
    }

    // Aparece con la misma curva con la que se irá: una entrada brusca
    // seguida de un corte se lee como un fallo, no como una presentación.
    val markAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 520, easing = LinearOutSlowInEasing),
        label = "introMark",
    )
    val heroAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 620, delayMillis = 260, easing = LinearOutSlowInEasing),
        label = "introHero",
    )

    LaunchedEffect(Unit) {
        visible = true
        if (!formLaunched) {
            formLaunched = true
            // Lo que tarda en leerse la marca y una línea. Pasado ese punto,
            // una pantalla que no hace nada se siente como un cuelgue.
            delay(1900)
            openForm()
        }
    }

    Box(Modifier.fillMaxSize().background(LoginTheme.surface)) {
        // El mismo fondo del template de login: la introducción y el
        // formulario comparten superficie, así que entrar no cambia de mundo.
        Image(
            painter = painterResource(R.drawable.login_fondo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.55f),
        )

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
        ) {
            // Se capturan aquí: dentro de los `Column` el receptor de
            // `BoxWithConstraints` ya no está disponible.
            val availableHeight = maxHeight
            val availableWidth = maxWidth
            val roomForHero = availableHeight >= 560.dp
            val compact = availableHeight < 720.dp

            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                BrandMark(
                    context = BrandContext.PORTAL,
                    size = if (availableWidth < 360.dp) 30.dp else 38.dp,
                    modifier = Modifier.alpha(markAlpha),
                )

                if (roomForHero) {
                    Column(
                        Modifier
                            .padding(top = 40.dp)
                            .widthIn(max = 420.dp)
                            .fillMaxWidth()
                            .alpha(heroAlpha),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        HeroDivider()
                        Box(Modifier.padding(top = 22.dp)) { HeroText(compact = compact) }
                        // El pie del template: cierra la composición con la
                        // misma frase con la que cierra la tarjeta en la Web.
                        Box(Modifier.padding(top = if (compact) 20.dp else 28.dp)) { FormFooter() }
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
                            onClick = { openForm() },
                            primary = true,
                        )
                    }
                }
            }
        }
    }
}
