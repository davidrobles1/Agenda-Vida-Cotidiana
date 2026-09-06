package com.vidacotidiana.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.vidacotidiana.app.core.ui.VidaCotidianaTheme
import com.vidacotidiana.app.navigation.DeepLinks
import com.vidacotidiana.app.navigation.PushDestinations
import com.vidacotidiana.app.feature.auth.AuthManager
import com.vidacotidiana.app.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// AND-002/AND-003: real login (AppAuth, Authorization Code + PKCE against
// Keycloak's android-app client) and a real reminders CRUD screen.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: AuthManager

    /**
     * El destino que pide el intent entrante, si lo pide.
     *
     * Es estado observable y no un valor leído una vez porque `MainActivity`
     * es `singleTask`: cuando la actividad ya existe, el sistema NO la vuelve
     * a crear sino que entrega el intent nuevo en `onNewIntent`. Leerlo solo
     * en `onCreate` habría hecho que la primera notificación navegara y las
     * siguientes no hicieran nada.
     */
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Borde a borde: el artefacto dibuja hasta el borde de la pantalla y
        // las pantallas ya reservan su propio espacio con `statusBarsPadding`
        // y `navigationBarsPadding`.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // El tema lo aplica `AppNavGraph`, que es quien conoce el elegido por
        // el usuario (ADR-023). Envolverlo aquí con el tema por defecto habría
        // pintado la primera composición con la agenda equivocada.
        pendingRoute.value = routeFromIntent(intent)

        setContent {
            AppNavGraph(
                authManager = authManager,
                pendingRoute = pendingRoute.value,
                onRouteConsumed = { pendingRoute.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // `singleTask` reutiliza esta instancia: sin esto, tocar una
        // notificación con la aplicación ya abierta no llevaría a ningún sitio.
        setIntent(intent)
        pendingRoute.value = routeFromIntent(intent)
    }

    /**
     * De dónde puede venir un destino, y son DOS sitios distintos:
     *
     *  - El `data` del intent, cuando la notificación la construimos nosotros
     *    (aplicación en primer plano) o cuando llega de un acceso directo del
     *    icono. Es un enlace `vidacotidiana://…`.
     *  - Los EXTRAS, cuando la notificación la dibujó FCM porque la aplicación
     *    estaba en segundo plano: ahí el sistema entrega los datos del mensaje
     *    como extras del intent de lanzamiento, sin `data`.
     *
     * Atender solo el primero habría dejado sin destino el caso más frecuente.
     */
    private fun routeFromIntent(intent: Intent?): String? {
        if (intent == null) return null

        intent.data?.let { uri ->
            if (uri.scheme == DeepLinks.SCHEME) {
                return when (uri.host) {
                    DeepLinks.SECTION_HOST -> uri.pathSegments.firstOrNull()
                    DeepLinks.CREATE_HOST -> DeepLinks.ACTION_CREATE_TASK
                    else -> null
                }
            }
        }

        // FCM entrega el `data` del mensaje como extras al abrir por toque.
        val type = intent.getStringExtra("type") ?: return null
        return PushDestinations.routeFor(type)
    }
}
