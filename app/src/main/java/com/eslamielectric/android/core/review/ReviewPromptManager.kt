package com.eslamielectric.android.core.review

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

private const val REVIEW_LOG_TAG = "ReviewPromptManager"

/** Abstraction over Play In-App Review for unit tests. */
interface PlayReviewLauncher {
    suspend fun requestReviewFlow(): ReviewInfo?
    suspend fun launchReviewFlow(activity: Activity, reviewInfo: ReviewInfo): Boolean
}

class PlayCoreReviewLauncher(private val manager: ReviewManager) : PlayReviewLauncher {
    override suspend fun requestReviewFlow(): ReviewInfo? = try {
        manager.requestReviewFlow().await()
    } catch (e: Exception) {
        Log.d(REVIEW_LOG_TAG, "requestReviewFlow failed: ${e.message}")
        null
    }

    override suspend fun launchReviewFlow(activity: Activity, reviewInfo: ReviewInfo): Boolean = try {
        manager.launchReviewFlow(activity, reviewInfo).await()
        true
    } catch (e: Exception) {
        Log.d(REVIEW_LOG_TAG, "launchReviewFlow failed: ${e.message}")
        false
    }
}

/**
 * Shows the Google Play In-App Review dialog after successful checkout when eligibility rules pass.
 * No-ops on emulators, debug sideloads, and when Play Core is unavailable.
 */
class ReviewPromptManager(
    context: Context,
    private val launcher: PlayReviewLauncher? = null,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val minSuccessfulOrders: Int = MIN_SUCCESSFUL_ORDERS,
    private val cooldownMs: Long = COOLDOWN_MS,
    prefs: SharedPreferences? = null
) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        prefs ?: appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val playLauncher: PlayReviewLauncher? by lazy {
        launcher ?: try {
            PlayCoreReviewLauncher(ReviewManagerFactory.create(appContext))
        } catch (e: Exception) {
            Log.d(REVIEW_LOG_TAG, "Play In-App Review unavailable: ${e.message}")
            null
        }
    }

    /**
     * Records a successful order and may launch the review flow.
     * Call from the checkout success screen only. [orderId] prevents double-counting on recomposition.
     */
    suspend fun onCheckoutSuccess(activity: Activity, orderId: String? = null) {
        if (!orderId.isNullOrBlank()) {
            val lastCounted = prefs.getString(KEY_LAST_COUNTED_ORDER, null)
            if (lastCounted == orderId) return
            prefs.edit().putString(KEY_LAST_COUNTED_ORDER, orderId).apply()
        }
        val successfulOrders = prefs.getInt(KEY_SUCCESSFUL_ORDERS, 0) + 1
        prefs.edit().putInt(KEY_SUCCESSFUL_ORDERS, successfulOrders).apply()

        if (!shouldPrompt(successfulOrders)) return

        val reviewLauncher = playLauncher ?: return
        val reviewInfo = reviewLauncher.requestReviewFlow() ?: return
        if (reviewLauncher.launchReviewFlow(activity, reviewInfo)) {
            prefs.edit().putLong(KEY_LAST_PROMPT_AT, clock()).apply()
        }
    }

    /** Whether a review prompt would be shown (testable without Play Core). */
    fun shouldPrompt(successfulOrderCount: Int? = null): Boolean {
        val count = successfulOrderCount ?: prefs.getInt(KEY_SUCCESSFUL_ORDERS, 0)
        if (count < minSuccessfulOrders) return false
        val lastPrompt = prefs.getLong(KEY_LAST_PROMPT_AT, 0L)
        if (lastPrompt == 0L) return true
        return clock() - lastPrompt >= cooldownMs
    }

    companion object {
        private const val PREFS_NAME = "eslami_review_prefs"
        private const val KEY_SUCCESSFUL_ORDERS = "successful_orders"
        private const val KEY_LAST_PROMPT_AT = "last_prompt_at"
        private const val KEY_LAST_COUNTED_ORDER = "last_counted_order_id"
        const val MIN_SUCCESSFUL_ORDERS = 2
        val COOLDOWN_MS = TimeUnit.DAYS.toMillis(90)
    }
}
