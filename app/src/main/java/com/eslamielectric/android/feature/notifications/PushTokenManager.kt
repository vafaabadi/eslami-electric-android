package com.eslamielectric.android.feature.notifications

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.eslamielectric.android.BuildConfig
import com.eslamielectric.android.core.data.SessionStore
import com.eslamielectric.android.core.network.ApiService
import com.eslamielectric.android.core.network.PushTokenDeleteRequest
import com.eslamielectric.android.core.network.PushTokenRegisterRequest
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException

/**
 * Owns the FCM token lifecycle for the Eslami Electric Android client.
 *
 * Triggers a backend register/delete in these cases:
 *   1. App start while logged in — defensive resync (token may have rotated, server might have lost
 *      the row, locale may have changed).
 *   2. Login state flips false → true (login/signup) — register the current FCM token.
 *   3. Login state flips true → false (logout) — DELETE the token on the server.
 *   4. [onTokenRefreshed] is called from [EslamiFirebaseMessagingService.onNewToken] — updates the
 *      cached token and (if logged in) registers it with the backend.
 *   5. Locale changes — re-register so future pushes use the right language.
 *
 * Defensive against missing google-services.json: methods log and exit cleanly if FCM is not
 * configured for the build. CI builds without Firebase secrets stay green.
 */
class PushTokenManager(
    private val context: Context,
    private val api: ApiService,
    private val sessionStore: SessionStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val isFirebaseConfigured: Boolean by lazy {
        if (!BuildConfig.FCM_CONFIGURED) {
            false
        } else {
            try {
                FirebaseApp.getInstance()
                true
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Firebase not initialised: ${e.message}")
                false
            }
        }
    }

    /** Wires up the lifecycle. Call once from [com.eslamielectric.android.EslamiElectricApp.onCreate]. */
    fun start() {
        if (!isFirebaseConfigured) {
            Log.i(TAG, "FCM not configured (google-services.json missing) — push token manager idle.")
            return
        }
        scope.launch {
            try {
                refreshTokenAndPush()
            } catch (e: Exception) {
                Log.w(TAG, "Initial token refresh failed: ${e.message}")
            }
        }
        scope.launch {
            sessionStore.tokenFlow
                .map { !it.isNullOrBlank() }
                .distinctUntilChanged()
                .drop(1) // Initial value handled by refreshTokenAndPush() above.
                .collect { loggedIn ->
                    if (loggedIn) refreshTokenAndPush() else deleteRegisteredTokenFromBackendBestEffort()
                }
        }
        scope.launch {
            sessionStore.localeFlow
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    if (sessionStore.isLoggedIn()) refreshTokenAndPush()
                }
        }
    }

    /** Called from [EslamiFirebaseMessagingService.onNewToken]. */
    fun onTokenRefreshed(token: String) {
        if (!isFirebaseConfigured || token.isBlank()) return
        prefs.edit().putString(KEY_LAST_TOKEN, token).apply()
        if (sessionStore.isLoggedIn()) {
            scope.launch { registerWithBackend(token) }
        }
    }

    fun isFcmReady(): Boolean = isFirebaseConfigured

    private suspend fun refreshTokenAndPush() {
        if (!isFirebaseConfigured) return
        val token = try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: Exception) {
            Log.w(TAG, "Could not fetch FCM token: ${e.message}")
            null
        } ?: return
        prefs.edit().putString(KEY_LAST_TOKEN, token).apply()
        if (sessionStore.isLoggedIn()) registerWithBackend(token)
    }

    private suspend fun registerWithBackend(token: String) {
        val locale = runCatching { sessionStore.localeFlow.first() }.getOrNull() ?: "en"
        val req = PushTokenRegisterRequest(
            token = token,
            platform = "android",
            appVersion = BuildConfig.VERSION_NAME,
            locale = if (locale == "fa") "fa" else "en"
        )
        try {
            api.registerPushToken(req)
            prefs.edit().putString(KEY_REGISTERED_TOKEN, token).apply()
            Log.d(TAG, "FCM token registered with backend")
        } catch (e: HttpException) {
            Log.w(TAG, "Push token register HTTP ${e.code()}: ${e.message()}")
        } catch (e: Exception) {
            Log.w(TAG, "Push token register failed: ${e.message}")
        }
    }

    private suspend fun deleteRegisteredTokenFromBackendBestEffort() {
        val token = prefs.getString(KEY_REGISTERED_TOKEN, null)
            ?: try {
                FirebaseMessaging.getInstance().token.await()
            } catch (_: Exception) {
                null
            }
            ?: return
        try {
            api.deletePushToken(PushTokenDeleteRequest(token))
            Log.d(TAG, "FCM token deleted on backend (logout)")
        } catch (e: Exception) {
            // 401 is expected if the JWT was already cleared before this fires; treat as success.
            Log.d(TAG, "Push token delete best-effort failed: ${e.message}")
        } finally {
            prefs.edit().remove(KEY_REGISTERED_TOKEN).apply()
        }
    }

    companion object {
        private const val TAG = "PushTokenManager"
        private const val PREFS_NAME = "eslami_push_prefs"
        private const val KEY_LAST_TOKEN = "last_token"
        private const val KEY_REGISTERED_TOKEN = "registered_token"
    }
}
