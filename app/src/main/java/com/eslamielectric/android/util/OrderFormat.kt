package com.eslamielectric.android.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eslamielectric.android.R
import com.eslamielectric.android.core.network.OrderDto
import com.eslamielectric.android.core.network.OrderLineItemDto
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun formatOrderCents(cents: Int?, currency: String?): String {
    if (cents == null) return "—"
    val cur = (currency ?: "usd").lowercase()
    val amount = cents / 100.0
    return if (cur == "usd") formatPriceUsd(amount) else "%.2f %s".format(amount, cur.uppercase())
}

fun formatOrderDate(iso: String?, localeTag: String): String {
    if (iso.isNullOrBlank()) return "—"
    return try {
        val parsed = java.time.Instant.parse(iso)
        val date = Date.from(parsed)
        val fmt = if (localeTag == "fa") {
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale("fa", "IR"))
        } else {
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
        }
        fmt.format(date)
    } catch (_: Exception) {
        iso
    }
}

fun normalizeOrderStatus(status: String?): String =
    status?.lowercase()?.trim().orEmpty()

fun isPendingOrder(order: OrderDto): Boolean =
    normalizeOrderStatus(order.status) == "pending"

@Composable
fun orderStatusLabel(status: String?): String {
    return when (normalizeOrderStatus(status)) {
        "paid" -> stringResource(R.string.order_status_paid)
        "pending" -> stringResource(R.string.order_status_pending)
        "cancelled", "canceled" -> stringResource(R.string.order_status_cancelled)
        else -> status?.replaceFirstChar { it.uppercase() }.orEmpty().ifBlank { "—" }
    }
}

@Composable
fun fulfillmentLabel(fulfillmentType: String?): String = when (fulfillmentType) {
    "collection" -> stringResource(R.string.order_fulfillment_collection)
    else -> stringResource(R.string.order_fulfillment_delivery)
}

fun formatShippingAddress(address: JsonObject?): String? {
    if (address == null) return null
    fun field(key: String): String? =
        address[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    val parts = listOfNotNull(
        field("line1"),
        field("line2"),
        field("city"),
        field("state"),
        field("postal_code"),
        field("country")
    )
    val extra = field("additional_info")
    val main = parts.joinToString(", ")
    return when {
        main.isNotEmpty() && extra != null -> "$main\n$extra"
        main.isNotEmpty() -> main
        extra != null -> extra
        else -> null
    }
}

fun lineItemTotalCents(item: OrderLineItemDto): Int? =
    item.amountTotal ?: item.unitAmount?.let { unit ->
        val qty = item.quantity ?: 1
        unit * qty
    }
