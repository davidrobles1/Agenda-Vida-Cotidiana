package com.vidacotidiana.app.feature.auth

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.vidacotidiana.app.R
import com.vidacotidiana.app.core.ui.brand.BrandContext
import com.vidacotidiana.app.core.ui.brand.BrandMark
import kotlinx.coroutines.delay

/**
 * La introducción: el logo del Portal sobre el fondo del template, y de ahí
 * directo al login.
 *
 * NO es una pantalla de navegación. No tiene botones, no se puede volver a
 * ella y no decide nada: su única función es firmar quién abre la puerta antes
 * de pedir credenciales. Por eso continúa sola —el usuario no tiene que
 * confirmar que ha visto un logo— y por eso `LoginScreen` la reemplaza en la
 * pila en vez de apilarse encima.
 *
 * La identidad que corresponde aquí es la del Portal (ADR-024): todavía no hay
 * sesión, así que no hay contexto Personal ni Laboral que firmar.
 */
@Composable
fun IntroScreen(onContinue: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    // Aparece y se va con la misma curva: una entrada brusca seguida de un
    // corte deja la sensación de un error, no de una presentación.
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
        // Lo que tarda en leerse la marca y una línea, no más: pasado ese
        // punto una pantalla que no hace nada se siente como un cuelgue.
        delay(2100)
        onContinue()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(LoginTheme.surface),
    ) {
        // El mismo fondo del template. `Crop` y no `Fit`: es una textura, y
        // dejar franjas de color liso a los lados la delataría como imagen
        // pegada en vez de como superficie.
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
            // En pantallas bajas el bloque de texto sobra antes que la marca:
            // la marca es el motivo de esta pantalla, el eslogan la acompaña.
            // Se capturan aquí: dentro de los `Column` el receptor de
            // `BoxWithConstraints` ya no está disponible.
            val availableHeight = maxHeight
            val availableWidth = maxWidth
            val roomForHero = availableHeight >= 560.dp

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
                        Box(Modifier.padding(top = 22.dp)) {
                            HeroText(compact = availableHeight < 720.dp)
                        }
                    }
                }
            }
        }
    }
}
