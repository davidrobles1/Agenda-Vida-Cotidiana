package com.vidacotidiana.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AND-002/11-auth-security.md: tokens never touch plain SharedPreferences —
 * EncryptedSharedPreferences (AES256-GCM, key material in the Android
 * Keystore, never extractable) is the storage backend for the whole session
 * (access token, refresh token, expiry), not just a wrapper around one field.
 */
@Singleton
class TokenStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "vida_cotidiana_auth",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    data class Session(
        val accessToken: String,
        val refreshToken: String?,
        val idToken: String?,
        val accessTokenExpirationMillis: Long?,
    )

    fun save(session: Session) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_ID_TOKEN, session.idToken)
            .putLong(KEY_EXPIRES_AT, session.accessTokenExpirationMillis ?: -1L)
            .apply()
    }

    fun load(): Session? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, -1L)
        return Session(
            accessToken = accessToken,
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null),
            idToken = prefs.getString(KEY_ID_TOKEN, null),
            accessTokenExpirationMillis = if (expiresAt >= 0) expiresAt else null,
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_ID_TOKEN = "id_token"
        const val KEY_EXPIRES_AT = "expires_at"
    }
}
