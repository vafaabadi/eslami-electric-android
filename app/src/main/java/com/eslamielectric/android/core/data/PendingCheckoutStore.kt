package com.eslamielectric.android.core.data

import android.content.Context

/** Persists Stripe session id while Custom Tab is open (survives process death). */
class PendingCheckoutStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSessionId(): String? = prefs.getString(KEY_SESSION_ID, null)?.takeIf { it.isNotBlank() }

    fun setSessionId(sessionId: String?) {
        prefs.edit().apply {
            if (sessionId.isNullOrBlank()) remove(KEY_SESSION_ID) else putString(KEY_SESSION_ID, sessionId)
        }.apply()
    }

    companion object {
        private const val PREFS_NAME = "eslami_pending_checkout"
        private const val KEY_SESSION_ID = "stripe_session_id"
    }
}
