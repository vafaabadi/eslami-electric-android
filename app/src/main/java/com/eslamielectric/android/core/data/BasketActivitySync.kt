package com.eslamielectric.android.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.eslamielectric.android.core.network.ApiService
import com.eslamielectric.android.core.network.BasketActivityRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID

private val Context.basketSessionDataStore by preferencesDataStore(name = "eslami_basket_session_prefs")

/**
 * Debounced sync of basket snapshots to the server for abandoned-basket push reminders (v2).
 * Logged-in users hit PUT /api/me/basket-activity; guests use a stable session UUID header.
 */
class BasketActivitySync(
    private val context: Context,
    private val api: ApiService,
    private val sessionStore: SessionStore,
    private val basketRepository: BasketRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val sessionKey = stringPreferencesKey("basket_session_id")
    private var debounceJob: Job? = null

    fun start() {
        scope.launch {
            basketRepository.itemsFlow.collect { items ->
                scheduleSync(items)
            }
        }
    }

    private fun scheduleSync(items: List<BasketItem>) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(DEBOUNCE_MS)
            syncNow(items)
        }
    }

    private suspend fun syncNow(items: List<BasketItem>) {
        val body = BasketActivityRequest(items = items)
        try {
            if (sessionStore.isLoggedIn()) {
                api.syncBasketActivity(body)
            } else {
                val sessionId = getOrCreateSessionId()
                api.syncGuestBasketActivity(sessionId, body)
            }
        } catch (_: Exception) {
            // Non-fatal: basket remains local; cron only needs periodic successful syncs.
        }
    }

    suspend fun getOrCreateSessionId(): String {
        val existing = context.basketSessionDataStore.data.map { it[sessionKey] }.first()
        if (!existing.isNullOrBlank()) return existing
        val created = UUID.randomUUID().toString()
        context.basketSessionDataStore.edit { prefs -> prefs[sessionKey] = created }
        return created
    }

    companion object {
        private const val DEBOUNCE_MS = 2000L
    }
}
