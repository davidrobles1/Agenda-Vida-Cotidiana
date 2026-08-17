package com.vidacotidiana.app.feature.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vidacotidiana.app.core.ui.VidaShape
import com.vidacotidiana.app.core.ui.VidaSpacing
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(authManager: AuthManager, onLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        scope.launch {
            try {
                authManager.handleLoginResult(data)
                onLoggedIn()
            } catch (e: Exception) {
                error = e.message ?: "Login failed"
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(VidaSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Vida Cotidiana",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(bottom = VidaSpacing.xxl),
        )
        Button(
            modifier = Modifier.testTag("login_button"),
            shape = RoundedCornerShape(VidaShape.control),
            contentPadding = PaddingValues(horizontal = VidaSpacing.xl, vertical = VidaSpacing.md),
            onClick = { launcher.launch(authManager.buildLoginIntent()) },
        ) {
            Text("Log in")
        }
        TextButton(
            modifier = Modifier.testTag("register_button").padding(top = VidaSpacing.sm),
            onClick = { launcher.launch(authManager.buildRegisterIntent()) },
        ) {
            Text("Crear cuenta")
        }
        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = VidaSpacing.md).semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}
