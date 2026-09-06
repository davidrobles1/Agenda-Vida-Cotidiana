package com.vidacotidiana.app.feature.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
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
import kotlinx.coroutines.launch

/**
 * Login — el template de la Web, adaptado a Android.
 *
 * QUÉ SE CONSERVA del template (`vida-cotidiana-web/login`): el fondo de
 * hojas, la marca del Portal, la tarjeta crema con su borde y su sombra, el
 * bloque «Agenda / VIDA COTIDIANA», «Bienvenido / Inicia sesión para
 * continuar», la fila de valores, el filete, la cita, «Meraki», el texto del
 * hero, los dos botones con sus iconos a ambos lados y el pie con el candado.
 *
 * QUÉ CAMBIA, Y POR QUÉ:
 *
 *  - La Web es de TRES columnas (libro · eslóganes · tarjeta). Un teléfono no
 *    tiene tres columnas, así que se apilan en el orden en que se leen: marca,
 *    tarjeta con las acciones, y debajo la columna de eslóganes. La tarjeta va
 *    ARRIBA y no al final porque en móvil lo primero que se ve debe ser
 *    aquello a lo que se ha venido.
 *
 *  - LOS CAMPOS DE USUARIO Y CONTRASEÑA NO ESTÁN AQUÍ, y no es una omisión.
 *    En Web el template ES la página de Keycloak, así que el formulario vive
 *    en ella. En Android la autenticación es Authorization Code + PKCE con
 *    AppAuth: las credenciales se escriben en el Custom Tab de Keycloak, nunca
 *    en la aplicación. Poner aquí campos reales exigiría el grant de acceso
 *    directo —desactivado en el cliente `android-app`— y desmontaría PKCE. Lo
 *    que esta pantalla presenta son las ACCIONES del template («Iniciar
 *    sesión», «Crear una cuenta»), que sí son compatibles con ese flujo.
 *
 * El libro (`libro_hojas.jpeg`) tampoco se apila: en la Web es una columna
 * completa a sangre, y reducido a una banda de 120 dp dejaría de ser lo que
 * es. Su papel —dar textura de papel a la pantalla— lo cumple aquí el propio
 * fondo, que es el mismo del template.
 */
@Composable
fun LoginScreen(authManager: AuthManager, onLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var authenticating by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data
        if (data == null) {
            // El usuario cerró el Custom Tab sin terminar. No es un error que
            // haya que contarle: ya sabe lo que hizo.
            authenticating = false
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                authManager.handleLoginResult(data)
                onLoggedIn()
            } catch (e: Exception) {
                error = e.message ?: "No pudimos completar el inicio de sesión."
            } finally {
                authenticating = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(LoginTheme.surface)) {
        Image(
            painter = painterResource(R.drawable.login_fondo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.55f),
        )

        BoxWithConstraints(Modifier.fillMaxSize()) {
            val narrow = maxWidth < 360.dp
            val short = maxHeight < 680.dp
            val gutter = if (narrow) 18.dp else 24.dp

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    // El teclado no aparece en esta pantalla (no hay campos),
                    // pero sí en la de registro del Custom Tab y en rotación:
                    // con esto la composición se comprime en vez de romperse.
                    .imePadding()
                    .padding(horizontal = gutter, vertical = if (short) 20.dp else 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // La marca del Portal corona la pantalla, como corona la
                // columna central en la Web.
                BrandMark(
                    context = BrandContext.PORTAL,
                    size = if (narrow) 26.dp else 30.dp,
                    modifier = Modifier.padding(bottom = if (short) 20.dp else 30.dp),
                )

                LoginCard(
                    narrow = narrow,
                    short = short,
                    authenticating = authenticating,
                    error = error,
                    onLogin = {
                        error = null
                        authenticating = true
                        launcher.launch(authManager.buildLoginIntent())
                    },
                    onRegister = {
                        error = null
                        authenticating = true
                        launcher.launch(authManager.buildRegisterIntent())
                    },
                )

                // La columna de eslóganes de la Web, debajo de la tarjeta.
                Column(
                    Modifier
                        .padding(top = if (short) 28.dp else 40.dp)
                        .widthIn(max = 440.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ValueRow(compact = short)
                    Box(Modifier.padding(vertical = if (short) 18.dp else 26.dp)) { HeroDivider() }
                    HeroText(compact = short)
                }
            }
        }
    }
}

/** `.vc-card`: crema, borde fino, radio 24 y la sombra larga del template. */
@Composable
private fun LoginCard(
    narrow: Boolean,
    short: Boolean,
    authenticating: Boolean,
    error: String?,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier
            // `max-width: 400px` en la Web. En una tableta o en horizontal la
            // tarjeta no se estira a lo ancho de la pantalla: dejaría los
            // botones a media pulgada de distancia entre sí.
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .shadow(elevation = 22.dp, shape = shape, clip = false)
            .background(LoginTheme.surfaceVariant.copy(alpha = 0.94f), shape)
            .border(1.dp, LoginTheme.cardBorder, shape)
            .padding(horizontal = if (narrow) 20.dp else 28.dp, vertical = if (short) 24.dp else 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandBlock(titleSize = if (narrow) 30.sp else 34.sp)
        Box(Modifier.padding(top = 18.dp)) { WelcomeBlock() }

        if (error != null) {
            // `.pf-v5-c-alert` del template: contenedor de error, radio 20,
            // el mismo rojo verificado AA (ACC-001).
            Text(
                error,
                style = TextStyle(fontFamily = VidaFonts.Inter, fontSize = 14.sp),
                color = LoginTheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 18.dp)
                    .fillMaxWidth()
                    .background(LoginTheme.errorContainer, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }

        Column(
            Modifier.padding(top = if (short) 20.dp else 26.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TemplateButton(
                label = if (authenticating) "Abriendo…" else "Iniciar sesión",
                leadingPath = PATH_USER,
                onClick = onLogin,
                enabled = !authenticating,
                primary = true,
                modifier = Modifier.testTag("login_button"),
            )
            TemplateButton(
                label = "Crear una cuenta",
                leadingPath = PATH_USER_PLUS,
                onClick = onRegister,
                enabled = !authenticating,
                primary = false,
                modifier = Modifier.testTag("register_button"),
            )
        }

        // Mientras el Custom Tab arranca, la tarjeta dice que está pasando
        // algo: sin esto el toque parece no haber hecho nada.
        if (authenticating) {
            AuthenticatingHint(Modifier.padding(top = 14.dp))
        }

        Box(Modifier.padding(top = if (short) 16.dp else 20.dp)) { FormFooter() }
    }
}

/** Estado de carga: el hilo del arco, latiendo. Ni un spinner genérico. */
@Composable
private fun AuthenticatingHint(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "authHint")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearOutSlowInEasing), RepeatMode.Reverse),
        label = "authHintAlpha",
    )
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Te llevamos a iniciar sesión…",
            style = TextStyle(fontFamily = VidaFonts.Inter, fontSize = 13.sp),
            color = LoginTheme.textSecondary,
            modifier = Modifier
                .alpha(alpha)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}
