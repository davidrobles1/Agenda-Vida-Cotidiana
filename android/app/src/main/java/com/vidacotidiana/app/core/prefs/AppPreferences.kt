package com.vidacotidiana.app.core.prefs

import android.content.Context
import androidx.core.content.edit
import com.vidacotidiana.app.core.ui.VisualTheme
import com.vidacotidiana.app.core.vocabulary.ProfessionalProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferencias de presentación: tema visual activo, perfil profesional y si el
 * modo Laboral está activado.
 *
 * `SharedPreferences` y no DataStore a propósito: el proyecto ya lo usa en
 * `TokenStore` y en `ReminderAlarmScheduler`, así que no se añade una
 * dependencia para guardar tres cadenas. Se expone como `StateFlow` para que
 * la interfaz reaccione igual que a cualquier otro estado.
 *
 * Igual que en la Web (ADR-016(d)), esto vive en el dispositivo: no hay
 * columna nueva en `USER` ni endpoint nuevo, y la limitación —no sincroniza
 * entre dispositivos— es la misma y está aceptada.
 */
@Singleton
class AppPreferences @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("vida_prefs", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(VisualTheme.from(prefs.getString(KEY_THEME, null)))
    val theme: StateFlow<VisualTheme> = _theme.asStateFlow()

    private val _profile = MutableStateFlow(ProfessionalProfile.from(prefs.getString(KEY_PROFILE, null)))
    val profile: StateFlow<ProfessionalProfile> = _profile.asStateFlow()

    private val _laboralEnabled = MutableStateFlow(prefs.getBoolean(KEY_LABORAL, true))
    val laboralEnabled: StateFlow<Boolean> = _laboralEnabled.asStateFlow()

    fun setTheme(value: VisualTheme) {
        _theme.value = value
        prefs.edit { putString(KEY_THEME, value.id) }
    }

    fun setProfile(value: ProfessionalProfile) {
        _profile.value = value
        prefs.edit { putString(KEY_PROFILE, value.id) }
    }

    fun setLaboralEnabled(value: Boolean) {
        _laboralEnabled.value = value
        prefs.edit { putBoolean(KEY_LABORAL, value) }
    }

    private companion object {
        const val KEY_THEME = "visual_theme"
        const val KEY_PROFILE = "professional_profile"
        const val KEY_LABORAL = "laboral_enabled"
    }
}
