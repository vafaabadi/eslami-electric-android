package com.eslamielectric.android.core.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.eslamielectric.android.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

/**
 * Central analytics facade. Logs to Firebase Analytics when [BuildConfig.FCM_CONFIGURED] is true
 * and Firebase initialised (i.e. `google-services.json` present). Otherwise all methods are no-ops.
 */
class AnalyticsLogger(@Suppress("UNUSED_PARAMETER") context: Context) {

    private val isConfigured: Boolean by lazy {
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

    private val firebaseAnalytics: FirebaseAnalytics? by lazy {
        if (!isConfigured) return@lazy null
        try {
            Firebase.analytics
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Analytics unavailable: ${e.message}")
            null
        }
    }

    fun isEnabled(): Boolean = firebaseAnalytics != null

    /** Logs the standard Firebase [FirebaseAnalytics.Event.SCREEN_VIEW] event. */
    fun logScreen(screenName: String) {
        val analytics = firebaseAnalytics ?: return
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }

    fun logEvent(name: String, params: Map<String, String> = emptyMap()) {
        val analytics = firebaseAnalytics ?: return
        val bundle = Bundle().apply {
            params.forEach { (key, value) -> putString(key, value) }
        }
        analytics.logEvent(name, bundle)
    }

    companion object {
        private const val TAG = "AnalyticsLogger"
    }
}
