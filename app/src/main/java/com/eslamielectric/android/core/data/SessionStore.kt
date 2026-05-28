package com.eslamielectric.android.core.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Secure JWT storage (EncryptedSharedPreferences) + locale (DataStore). */
class SessionStore(private val context: Context) {

    private val securePrefs: SharedPreferences = createSecurePrefs(context)

    private val localeKey = stringPreferencesKey("locale")
    private val localeUserSetKey = stringPreferencesKey("locale_user_set")
    private val localeInitializedKey = stringPreferencesKey("locale_initialized")

    private val _tokenFlow = MutableStateFlow(getToken())

    /** Current JWT, or null when logged out. */
    val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    val isLoggedInFlow: Flow<Boolean> = tokenFlow.map { !it.isNullOrBlank() }

    val localeFlow: Flow<String> = context.sessionDataStore.data.map { prefs ->
        prefs[localeKey] ?: "en"
    }

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    suspend fun setLocale(locale: String, userSet: Boolean = true) {
        context.sessionDataStore.edit { prefs ->
            prefs[localeKey] = locale
            if (userSet) prefs[localeUserSetKey] = "1"
        }
    }

    suspend fun isLocaleUserSet(): Boolean =
        context.sessionDataStore.data.map { it[localeUserSetKey] == "1" }.first()

    suspend fun isLocaleInitialized(): Boolean =
        context.sessionDataStore.data.map { it[localeInitializedKey] == "1" }.first()

    suspend fun applyLocaleHint(defaultLang: String) {
        context.sessionDataStore.edit { prefs ->
            if (prefs[localeInitializedKey] != "1" && prefs[localeUserSetKey] != "1") {
                prefs[localeKey] = if (defaultLang == "fa") "fa" else "en"
            }
            prefs[localeInitializedKey] = "1"
        }
    }

    fun getToken(): String? = securePrefs.getString(KEY_TOKEN, null)

    fun setToken(token: String?) {
        securePrefs.edit().apply {
            if (token.isNullOrBlank()) remove(KEY_TOKEN) else putString(KEY_TOKEN, token)
        }.apply()
        _tokenFlow.value = getToken()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val SECURE_PREFS_NAME = "eslami_secure_prefs"
        private const val FALLBACK_PREFS_NAME = "eslami_secure_prefs_fallback"

        private fun createSecurePrefs(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    SECURE_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (_: Exception) {
                context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }
}
