package com.eslamielectric.android.core.data

import android.content.Context
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Persists Stripe session + pending order edit state across Custom Tab / process death. */
class PendingCheckoutStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSessionId(): String? = prefs.getString(KEY_SESSION_ID, null)?.takeIf { it.isNotBlank() }

    fun setSessionId(sessionId: String?) {
        prefs.edit().apply {
            if (sessionId.isNullOrBlank()) remove(KEY_SESSION_ID) else putString(KEY_SESSION_ID, sessionId)
        }.apply()
    }

    fun getPendingEditOrder(): PendingEditOrder? {
        val orderId = prefs.getString(KEY_PENDING_ORDER_ID, null)?.takeIf { it.isNotBlank() } ?: return null
        return PendingEditOrder(
            orderId = orderId,
            orderLabel = prefs.getString(KEY_PENDING_ORDER_LABEL, null),
            fulfillmentType = prefs.getString(KEY_PENDING_FULFILLMENT, null),
            addressLine1 = prefs.getString(KEY_PENDING_ADDR_LINE1, null),
            addressCity = prefs.getString(KEY_PENDING_ADDR_CITY, null),
            addressPostal = prefs.getString(KEY_PENDING_ADDR_POSTAL, null),
            addressExtra = prefs.getString(KEY_PENDING_ADDR_EXTRA, null)
        )
    }

    fun setPendingEditOrder(
        orderId: String,
        orderLabel: String?,
        fulfillmentType: String?,
        shippingAddress: JsonObject?
    ) {
        val line1 = shippingAddress?.get("line1")?.jsonPrimitive?.contentOrNull
        val city = shippingAddress?.get("city")?.jsonPrimitive?.contentOrNull
        val postal = shippingAddress?.get("postal_code")?.jsonPrimitive?.contentOrNull
        val extra = shippingAddress?.get("additional_info")?.jsonPrimitive?.contentOrNull
        prefs.edit()
            .putString(KEY_PENDING_ORDER_ID, orderId)
            .putString(KEY_PENDING_ORDER_LABEL, orderLabel)
            .putString(KEY_PENDING_FULFILLMENT, fulfillmentType)
            .putString(KEY_PENDING_ADDR_LINE1, line1)
            .putString(KEY_PENDING_ADDR_CITY, city)
            .putString(KEY_PENDING_ADDR_POSTAL, postal)
            .putString(KEY_PENDING_ADDR_EXTRA, extra)
            .apply()
    }

    fun clearPendingEditOrder() {
        prefs.edit()
            .remove(KEY_PENDING_ORDER_ID)
            .remove(KEY_PENDING_ORDER_LABEL)
            .remove(KEY_PENDING_FULFILLMENT)
            .remove(KEY_PENDING_ADDR_LINE1)
            .remove(KEY_PENDING_ADDR_CITY)
            .remove(KEY_PENDING_ADDR_POSTAL)
            .remove(KEY_PENDING_ADDR_EXTRA)
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    data class PendingEditOrder(
        val orderId: String,
        val orderLabel: String?,
        val fulfillmentType: String?,
        val addressLine1: String?,
        val addressCity: String?,
        val addressPostal: String?,
        val addressExtra: String?
    )

    companion object {
        private const val PREFS_NAME = "eslami_pending_checkout"
        private const val KEY_SESSION_ID = "stripe_session_id"
        private const val KEY_PENDING_ORDER_ID = "pending_order_id"
        private const val KEY_PENDING_ORDER_LABEL = "pending_order_label"
        private const val KEY_PENDING_FULFILLMENT = "pending_fulfillment"
        private const val KEY_PENDING_ADDR_LINE1 = "pending_addr_line1"
        private const val KEY_PENDING_ADDR_CITY = "pending_addr_city"
        private const val KEY_PENDING_ADDR_POSTAL = "pending_addr_postal"
        private const val KEY_PENDING_ADDR_EXTRA = "pending_addr_extra"
    }
}
