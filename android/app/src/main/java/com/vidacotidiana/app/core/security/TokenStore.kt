package com.vidacotidiana.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import net.openid.appauth.AuthState
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AND-002/11-auth-security.md: tokens never touch plain SharedPreferences —
 * EncryptedSharedPreferences (AES256-GCM, key material in the Android
 * Keystore, never extractable) is the storage backend for the whole session.
 *
 * AND-009 (Task B §3): stores AppAuth's own `AuthState` (its documented
 * `jsonSerializeString()`/`jsonDeserialize()` round-trip) instead of four
 * separate token fields — AuthState already tracks access/refresh/id token
 * plus expiry and is what AuthManager's refresh flow
 * (`performActionWithFreshTokens`) needs directly, so there's nothing left
 * to hand-roll here. Clean cutover, no migration path: this is a pre-V1
 * local dev store, not a production session format with real users to
 * preserve.
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

    fun saveAuthState(authState: AuthState) {
        prefs.edit().putString(KEY_AUTH_STATE, authState.jsonSerializeString()).apply()
    }

    fun loadAuthState(): AuthState? =
        prefs.getString(KEY_AUTH_STATE, null)?.let { AuthState.jsonDeserialize(it) }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_AUTH_STATE = "auth_state"
    }
}
