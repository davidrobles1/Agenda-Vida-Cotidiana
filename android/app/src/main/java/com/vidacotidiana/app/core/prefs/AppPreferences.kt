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

    /* ══════════════════════════════════════════════════════════════════════
       AVISOS LEÍDOS
       ══════════════════════════════════════════════════════════════════════ */

    private val _readNotices = MutableStateFlow(prefs.getStringSet(KEY_READ_NOTICES, null).orEmpty())

    /**
     * Qué avisos ha visto ya el usuario.
     *
     * VIVE EN EL DISPOSITIVO, igual que el tema y el perfil, y por la misma
     * razón que allí está aceptada: los avisos de Cotidiana no son mensajes
     * que alguien envía y que haya que guardar en algún sitio — son
     * DERIVADOS (ADR-018) de las garantías, los mantenimientos y los pagos que
     * ya están en el servidor. Crear una tabla de notificaciones para marcar
     * leído lo que se recalcula en cada carga sería inventar un módulo entero
     * para un booleano.
     *
     * La clave lleva la FECHA además del id: cuando un mantenimiento vuelve a
     * tocar dentro de seis meses es un aviso nuevo y tiene que volver a
     * aparecer sin leer. Si la clave fuese solo el id, marcarlo leído una vez
     * lo silenciaría para siempre.
     *
     * `getStringSet` devuelve un conjunto que NO se debe modificar en sitio
     * —está documentado en la API de Android—, por eso siempre se guarda una
     * copia nueva.
     */
    val readNotices: StateFlow<Set<String>> = _readNotices.asStateFlow()

    fun markNoticeRead(key: String) {
        if (key in _readNotices.value) return
        val updated = _readNotices.value + key
        _readNotices.value = updated
        prefs.edit { putStringSet(KEY_READ_NOTICES, updated) }
    }

    /** La contraria. Existe por lo mismo que el resto de vueltas atrás de esta
        versión: marcar leído es un toque, y sin salida un toque de más pierde
        el aviso. */
    fun markNoticeUnread(key: String) {
        if (key !in _readNotices.value) return
        val updated = _readNotices.value - key
        _readNotices.value = updated
        prefs.edit { putStringSet(KEY_READ_NOTICES, updated) }
    }

    fun markNoticesRead(keys: Collection<String>) {
        val updated = _readNotices.value + keys
        if (updated.size == _readNotices.value.size) return
        _readNotices.value = updated
        prefs.edit { putStringSet(KEY_READ_NOTICES, updated) }
    }

    /**
     * Se queda solo con las claves que siguen correspondiendo a un aviso vivo.
     *
     * Sin esto el conjunto crecería para siempre: cada garantía que vence y se
     * borra, cada mantenimiento que pasa, deja su clave dentro. Podar con lo
     * que el motor de avisos acaba de calcular es exacto —esas SON todas las
     * claves posibles ahora mismo— y no puede marcar nada como no leído por
     * error, porque solo quita lo que ya no existe.
     */
    fun pruneNotices(stillValid: Collection<String>) {
        val updated = _readNotices.value.intersect(stillValid.toSet())
        if (updated.size == _readNotices.value.size) return
        _readNotices.value = updated
        prefs.edit { putStringSet(KEY_READ_NOTICES, updated) }
    }

    private companion object {
        const val KEY_THEME = "visual_theme"
        const val KEY_PROFILE = "professional_profile"
        const val KEY_LABORAL = "laboral_enabled"
        const val KEY_READ_NOTICES = "read_notices"
    }
}
