package com.eslamielectric.android.feature.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.eslamielectric.android.EslamiElectricApp
import com.eslamielectric.android.MainActivity
import com.eslamielectric.android.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FCM listener for the Android client.
 *
 * Responsibilities:
 *  - [onNewToken] forwards refreshed tokens to [PushTokenManager] (registers with the backend when
 *    the user is logged in, otherwise stashes for next login).
 *  - [onMessageReceived] renders a notification when the app is in the foreground OR when the
 *    payload is data-only (no `notification` field). Background `notification` messages are
 *    rendered automatically by the system and we do not duplicate them here.
 *  - Tap intent → deep link `eslamielectric://push/<route>` so [MainActivity] can route to the
 *    correct screen.
 */
class EslamiFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val app = applicationContext as? EslamiElectricApp ?: return
        app.pushTokenManager?.onTokenRefreshed(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val notif = message.notification
        val title = notif?.title
            ?: data["title"]
            ?: getString(R.string.app_name)
        val body = notif?.body
            ?: data["body"]
            ?: return // No content to display.
        val channelId = data["channel"]?.takeIf { it.isNotBlank() }
            ?: notif?.channelId?.takeIf { !it.isNullOrBlank() }
            ?: NotificationChannels.GENERAL
        val route = data["route"]?.takeIf { it.isNotBlank() }

        showNotification(
            context = this,
            channelId = channelId,
            title = title,
            body = body,
            route = route
        )
    }

    companion object {
        /**
         * Render a notification. Public so test harnesses / [PushTokenManager] integration tests can
         * trigger it without going through FCM.
         */
        fun showNotification(
            context: Context,
            channelId: String,
            title: String,
            body: String,
            route: String?
        ) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (!route.isNullOrBlank()) {
                    action = Intent.ACTION_VIEW
                    data = buildDeepLinkUri(route)
                }
            }
            val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            val pending = PendingIntent.getActivity(context, route.hashCode(), intent, pendingFlags)
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(androidx.core.content.ContextCompat.getColor(context, R.color.notification_accent))
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            val nm = NotificationManagerCompat.from(context)
            // ID = hash(route or channel) so a fresh order-status push replaces the previous one.
            val notificationId = (route ?: channelId).hashCode()
            try {
                nm.notify(notificationId, builder.build())
            } catch (_: SecurityException) {
                // POST_NOTIFICATIONS not granted on Android 13+. Silently skip — the user can grant
                // it later from Account → Notifications.
            }
        }

        /**
         * Map server `route` payloads to in-app deep links handled by [MainActivity].
         * Routes use the convention `kind:value` (e.g. `order:abc-123`, `basket`).
         */
        internal fun buildDeepLinkUri(route: String): Uri {
            val safe = route.trim()
            val builder = Uri.Builder().scheme("eslamielectric").authority("push")
            when {
                safe.startsWith("order:") -> {
                    val id = safe.removePrefix("order:")
                    builder.appendPath("order")
                    if (id.isNotBlank()) builder.appendPath(id)
                }
                safe == "orders" -> builder.appendPath("orders")
                safe == "basket" -> builder.appendPath("basket")
                safe.startsWith("product:") -> {
                    val id = safe.removePrefix("product:")
                    builder.appendPath("product")
                    if (id.isNotBlank()) builder.appendPath(id)
                }
                safe.startsWith("/") -> builder.appendEncodedPath(safe.removePrefix("/"))
                else -> builder.appendPath(safe)
            }
            return builder.build()
        }
    }
}
