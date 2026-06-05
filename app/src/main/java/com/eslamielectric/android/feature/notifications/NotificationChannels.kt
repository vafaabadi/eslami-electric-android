package com.eslamielectric.android.feature.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.ContextCompat
import com.eslamielectric.android.R

/**
 * Channel identifiers shared between [EslamiFirebaseMessagingService] and the settings screen.
 * They match the server's channel taxonomy (lib/push-notifications.js).
 */
object NotificationChannels {
    const val ORDERS = "orders"
    const val PROMOTIONS = "promotions"
    const val ACCOUNT = "account"
    const val GENERAL = "general"

    val ALL = listOf(ORDERS, PROMOTIONS, ACCOUNT, GENERAL)

    /**
     * Idempotently create all channels on Android 8+ (called from [com.eslamielectric.android.EslamiElectricApp]).
     * Channel importance is fixed at creation time; user-facing per-channel toggles happen in the
     * Android system settings (or via our own remote prefs which short-circuit at the server).
     */
    fun ensureCreated(context: Context) {
        val nm = ContextCompat.getSystemService(context, NotificationManager::class.java) ?: return
        val channels = listOf(
            NotificationChannel(
                ORDERS,
                context.getString(R.string.notif_channel_orders_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_orders_desc)
            },
            NotificationChannel(
                PROMOTIONS,
                context.getString(R.string.notif_channel_promotions_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notif_channel_promotions_desc)
            },
            NotificationChannel(
                ACCOUNT,
                context.getString(R.string.notif_channel_account_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_account_desc)
            },
            NotificationChannel(
                GENERAL,
                context.getString(R.string.notif_channel_general_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_general_desc)
            }
        )
        nm.createNotificationChannels(channels)
    }
}
