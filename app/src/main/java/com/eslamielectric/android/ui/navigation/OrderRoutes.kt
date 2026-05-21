package com.eslamielectric.android.ui.navigation

object OrderRoutes {
    const val ORDERS_LIST = "account/orders"
    const val ORDER_DETAIL = "account/orders/{orderId}"
    const val GUEST_TRACK = "account/guest-track"
    const val GUEST_ORDER_DETAIL = "account/guest-order/{orderId}?token={guestToken}"

    fun orderDetail(orderId: String) = "account/orders/$orderId"

    fun guestOrderDetail(orderId: String, guestToken: String? = null): String =
        "account/guest-order/$orderId?token=${guestToken?.takeIf { it.isNotBlank() } ?: "-"}"
}
