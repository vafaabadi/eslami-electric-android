package com.eslamielectric.android.feature.notifications

import com.eslamielectric.android.core.network.ApiService
import com.eslamielectric.android.core.network.PushChannelPatch
import com.eslamielectric.android.core.network.PushPreferencesDto
import com.eslamielectric.android.core.network.PushPreferencesPatchRequest
import com.eslamielectric.android.core.network.SessionExpiredException
import com.eslamielectric.android.core.data.SessionStore
import com.eslamielectric.android.core.network.mapApiException
import retrofit2.HttpException

/**
 * Thin wrapper over the `/api/me/push-preferences` endpoints. Mirrors the pattern used by
 * [com.eslamielectric.android.feature.auth.AuthRepository].
 */
class NotificationPreferencesRepository(
    private val api: ApiService,
    private val sessionStore: SessionStore
) {

    suspend fun load(): PushPreferencesDto = withAuth { api.getPushPreferences() }

    suspend fun setMasterEnabled(enabled: Boolean): PushPreferencesDto = withAuth {
        api.updatePushPreferences(PushPreferencesPatchRequest(masterEnabled = enabled))
    }

    suspend fun setChannels(patch: PushChannelPatch): PushPreferencesDto = withAuth {
        api.updatePushPreferences(PushPreferencesPatchRequest(channels = patch))
    }

    private suspend fun <T> withAuth(block: suspend () -> T): T {
        if (!sessionStore.isLoggedIn()) throw SessionExpiredException()
        return try {
            block()
        } catch (e: HttpException) {
            if (e.code() == 401) {
                sessionStore.setToken(null)
                throw SessionExpiredException()
            }
            throw mapApiException(e)
        }
    }
}
